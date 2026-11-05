package cl.treecs.skyblockmulti.region;

import cl.treecs.skyblockmulti.api.region.LogicalRegion;
import cl.treecs.skyblockmulti.api.region.LogicalRegionApi;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Keeps command/datapack entity tags synchronized and exposes a small diagnostic command. */
public final class LogicalRegionService implements ModInitializer {
    private int ticks;

    @Override
    public void onInitialize() {
        BuiltInLogicalRegions.register();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks < 10) return;
            ticks = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                synchronizeTags(player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("skyblockmulti")
                        .then(literal("region")
                                .executes(context -> showCurrent(context.getSource().getPlayerOrException()))
                                .then(literal("check")
                                        .then(argument("id", StringArgumentType.word())
                                                .executes(context -> check(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "id")
                                                ))))))
        );
    }

    private static void synchronizeTags(ServerPlayer player) {
        List<LogicalRegion> matches = LogicalRegionApi.regionsAt(player.level().dimension(), player.blockPosition());
        for (LogicalRegion region : LogicalRegionApi.regions()) {
            if (matches.contains(region)) {
                player.addTag(region.entityTag());
            } else {
                player.removeTag(region.entityTag());
            }
        }
    }

    private static int showCurrent(ServerPlayer player) {
        List<LogicalRegion> matches = LogicalRegionApi.regionsAt(player.level().dimension(), player.blockPosition());
        if (matches.isEmpty()) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.region.none"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable(
                "skyblockmulti.region.current",
                Component.translatable(matches.getFirst().translationKey()),
                matches.getFirst().id().toString()
        ));
        return matches.size();
    }

    private static int check(ServerPlayer player, String rawId) {
        Identifier id = rawId.contains(":")
                ? Identifier.parse(rawId)
                : Identifier.fromNamespaceAndPath("skyblockmulti", rawId);
        LogicalRegion region = LogicalRegionApi.get(id).orElse(null);
        if (region == null) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.region.unknown", rawId));
            return 0;
        }
        boolean inside = region.dimension().equals(player.level().dimension())
                && region.shape().contains(player.blockPosition());
        player.sendSystemMessage(Component.translatable(
                inside ? "skyblockmulti.region.check.inside" : "skyblockmulti.region.check.outside",
                Component.translatable(region.translationKey()),
                region.entityTag()
        ));
        return inside ? 1 : 0;
    }
}
