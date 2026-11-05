package cl.treecs.skyblockmulti.panels;

import cl.treecs.skyblockmulti.infernal.InfernalRewardLedger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

/** Opt-in destructive fault injection restricted to the freshly generated test world. */
final class PendingAudit {
    private static final List<String> failures = new ArrayList<>();
    static void run(ClientGameTestContext context) {
        failures.clear();
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null);
            world.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                Path ledger = server.getWorldPath(LevelResource.ROOT).resolve("skyblockmulti_infernal_rewards.properties");
                // Fixture rewards only: no personal save or installed profile is accessed.
                fixture(server, ledger, player, "0,2,minecraft:diamond=3;0,2,minecraft:diamond=5");
                int before = player.getInventory().countItem(Items.DIAMOND);
                invoke("claim", ServerPlayer.class, player);
                check(player.getInventory().countItem(Items.DIAMOND) == before + 3, "one claim delivers exactly the first reward");
                check(read(ledger).getProperty(player.getUUID().toString()).equals("0,2,minecraft:diamond=5"), "second reward remains persisted");
                invoke("load", MinecraftServer.class, server);
                invoke("claim", ServerPlayer.class, player);
                invoke("claim", ServerPlayer.class, player);
                check(player.getInventory().countItem(Items.DIAMOND) == before + 8, "reload preserves remaining reward and empty claim grants nothing");
                check(!read(ledger).containsKey(player.getUUID().toString()), "claimed queue removed from disk");

                fixture(server, ledger, player, "0,2,pending_audit:missing_item=1");
                byte[] missing = Files.readAllBytes(ledger);
                invoke("claim", ServerPlayer.class, player);
                check(Arrays.equals(missing, Files.readAllBytes(ledger)), "absent addon item keeps reward unchanged");

                fixture(server, ledger, player, "1,2");
                invoke("save", MinecraftServer.class, server);
                check(read(ledger).getProperty(player.getUUID().toString()).equals("1,2,"), "legacy two-field reward survives migration");

                Files.writeString(ledger, "not-a-uuid=bad-data\n");
                boolean rejected = false;
                try { invoke("load", MinecraftServer.class, server); }
                catch (Exception expected) { rejected = true; }
                invoke("save", MinecraftServer.class, server);
                check(rejected && Files.readString(ledger).equals("not-a-uuid=bad-data\n"), "corrupt ledger fails closed and is not overwritten");

                fixture(server, ledger, player, "0,2,minecraft:diamond=7");
                Path temporary = ledger.resolveSibling(ledger.getFileName() + ".tmp");
                Files.createDirectory(temporary); // Force a real write failure without changing permissions.
                int preFailure = player.getInventory().countItem(Items.DIAMOND);
                boolean writeRejected = false;
                try { invoke("claim", ServerPlayer.class, player); }
                catch (Exception expected) { writeRejected = true; }
                finally { Files.delete(temporary); } // Exact empty directory created above, never recursive.
                check(!writeRejected && player.getInventory().countItem(Items.DIAMOND) == preFailure,
                        "write failure is contained and delivers nothing");
                invoke("load", MinecraftServer.class, server); // Model restart from durable state.
                invoke("claim", ServerPlayer.class, player);
                check(player.getInventory().countItem(Items.DIAMOND) == preFailure + 7,
                        "reward is not duplicated after failed save and ledger reload");

                fixture(server, ledger, player, "0,2,minecraft:diamond=11");
                Properties interrupted = read(ledger);
                interrupted.setProperty("@inflight", player.getUUID().toString());
                try (var writer = Files.newBufferedWriter(ledger)) { interrupted.store(writer, "Interrupted delivery fixture"); }
                invoke("load", MinecraftServer.class, server);
                int heldBefore = player.getInventory().countItem(Items.DIAMOND);
                invoke("claim", ServerPlayer.class, player);
                invoke("save", MinecraftServer.class, server);
                invoke("load", MinecraftServer.class, server);
                invoke("claim", ServerPlayer.class, player);
                check(player.getInventory().countItem(Items.DIAMOND) == heldBefore,
                        "interrupted delivery cannot be reclaimed after repeated reload");
                check(read(ledger).getProperty(player.getUUID().toString()).equals("0,2,minecraft:diamond=11")
                        && read(ledger).getProperty("@inflight").contains(player.getUUID().toString()),
                        "interrupted reward and review marker remain recoverable");

                // Direct function regression; do not claim this covers the full party lifecycle.
                command(server, "execute as " + player.getScoreboardName() + " run tag @s add skyblock_party_reentry");
                command(server, "tp " + player.getScoreboardName() + " 4.5 161 0.5");
                command(server, "execute as " + player.getScoreboardName() + " at @s run function skyblockmulti:player/freeze_selection");
                check(Math.abs(player.getX() - 4.5) < 0.01, "party reentry selection permits movement without repeated teleport");
                player.removeTag("skyblock_party_reentry");
                command(server, "tp " + player.getScoreboardName() + " 4.5 161 0.5");
                command(server, "execute as " + player.getScoreboardName() + " at @s run function skyblockmulti:player/freeze_selection");
                check(Math.abs(player.getX() - 4.5) < 0.01, "ordinary selection does not teleport");
            });
        } catch (Exception error) { throw new RuntimeException(error); }
        if (!failures.isEmpty()) throw new AssertionError("Pending audit failures: " + String.join("; ", failures));
    }
    private static void fixture(MinecraftServer server, Path path, ServerPlayer player, String rewards) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(player.getUUID().toString(), rewards);
        try (var writer = Files.newBufferedWriter(path)) { properties.store(writer, "Disposable audit fixture"); }
        invoke("load", MinecraftServer.class, server);
    }
    private static Properties read(Path path) throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) { properties.load(reader); }
        return properties;
    }
    private static void invoke(String name, Class<?> type, Object value) throws Exception {
        var method = InfernalRewardLedger.class.getDeclaredMethod(name, type);
        method.setAccessible(true);
        method.invoke(null, value);
    }
    private static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
    private static void check(boolean passed, String name) {
        System.out.println("[PENDING-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
