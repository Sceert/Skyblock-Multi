package cl.treecs.skyblockmulti;

import cl.treecs.skyblockmulti.network.IslandPanelPayload;
import cl.treecs.skyblockmulti.network.IslandPanelRequestPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static net.minecraft.commands.Commands.literal;

/** Subscriptions never alter world state. Actions revalidate a fresh snapshot before dispatch. */
public final class IslandPanelService {
    private static final Map<UUID, IslandPanelPayload> OPEN = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static long revision;
    private static final Map<String, String> COMMANDS = Map.of(
            "selection", "skyblockmulti trees", "home_keep", "skyblockmulti partyhome keep",
            "home_move", "skyblockmulti partyhome move", "island_party", "skyblockmulti partyisland party",
            "island_own", "skyblockmulti partyisland own", "abandon_request", "skyblockmulti infernal abandon request",
            "abandon_yes", "skyblockmulti infernal abandon yes", "abandon_no", "skyblockmulti infernal abandon no");

    private IslandPanelService() {}
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(IslandPanelPayload.TYPE, IslandPanelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(IslandPanelRequestPayload.TYPE, IslandPanelRequestPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
                literal("skyblockmulti").then(literal("panel").executes(ctx -> {
                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                    send(player, true);
                    return 1;
                }))));
        ServerPlayNetworking.registerGlobalReceiver(IslandPanelRequestPayload.TYPE, (request, context) ->
                context.server().execute(() -> handle(context.player(), request)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.overworld().getGameTime() % 10 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (OPEN.containsKey(player.getUUID())) send(player, false);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            OPEN.remove(handler.getPlayer().getUUID()); LAST_REQUEST.remove(handler.getPlayer().getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { OPEN.clear(); LAST_REQUEST.clear(); });
    }

    private static void handle(ServerPlayer player, IslandPanelRequestPayload request) {
        UUID id = player.getUUID();
        if (request.action().equals("close")) {
            var shown = OPEN.get(id);
            if (shown != null && shown.session() == request.session()) OPEN.remove(id);
            return;
        }
        long tick = player.level().getGameTime();
        Long last = LAST_REQUEST.get(id);
        if (request.action().equals("open") && request.request() == 0 && last != null && tick - last < 4) return;
        LAST_REQUEST.put(id, tick);
        if (request.action().equals("open")) { send(player, true, request.session(), request.request(), true); return; }
        IslandPanelPayload shown = OPEN.get(id);
        if (shown == null || shown.session() != request.session()) return;
        var current = SkyblockMultiMod.islandPanelEntries(player);
        boolean allowed = shown.permits(request.revision(), current, request.action());
        String command = COMMANDS.get(request.action());
        if (allowed && command != null) {
            // Invalidate before execution: duplicated packets cannot consume the same decision twice.
            OPEN.remove(id);
            player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
        }
        OPEN.remove(id); // Always acknowledge rejected/stale responses as well.
        send(player, false, request.session(), request.request(), allowed && command != null);
    }

    private static void send(ServerPlayer player, boolean open) {
        var previous = OPEN.get(player.getUUID());
        send(player, open, previous == null ? 0 : previous.session(), 0, true);
    }
    private static void send(ServerPlayer player, boolean open, long session, long request, boolean accepted) {
        if (!ServerPlayNetworking.canSend(player, IslandPanelPayload.TYPE)) return;
        var entries = SkyblockMultiMod.islandPanelEntries(player);
        var previous = OPEN.get(player.getUUID());
        if (!open && request == 0 && previous != null && previous.entries().equals(entries)) return;
        var payload = new IslandPanelPayload(++revision, open, entries, session, request, accepted);
        OPEN.put(player.getUUID(), payload);
        ServerPlayNetworking.send(player, payload);
    }

    public static Component openLink() {
        return Component.translatable("skyblockmulti.panel.open_link").withStyle(style -> style.withColor(0x55FFFF)
                .withUnderlined(true).withClickEvent(new ClickEvent.RunCommand("/skyblockmulti panel")));
    }
}
