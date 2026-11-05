package cl.treecs.skyblockmulti.panels;

import cl.treecs.skyblockmulti.NexusBlocks;
import cl.treecs.skyblockmulti.api.addon.AddonRewardApi;
import cl.treecs.skyblockmulti.infernal.InfernalRewardLedger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

final class VaultAudit {
    static void run(ClientGameTestContext context) {
        AddonRewardApi.registerTrial("pending_audit:reward", ignored -> List.of(new AddonRewardApi.ItemGrant("minecraft:diamond", 2)));
        UUID offline = UUID.randomUUID();
        int[] before = new int[1];
        try (var world = context.worldBuilder().create()) {
            world.getServer().waitFor(server -> !server.getPlayerList().getPlayers().isEmpty());
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "execute in minecraft:the_nether run tp " + player.getScoreboardName() + " 0 65 5");
            });
            context.waitTicks(10);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var complete = InfernalRewardLedger.class.getDeclaredMethod("completeAddonTrial", MinecraftServer.class,
                        String.class, String.class, Set.class);
                complete.setAccessible(true);
                var participants = Set.of(player.getUUID(), offline);
                complete.invoke(null, server, "pending_audit/run1", "pending_audit:trial", participants);
                complete.invoke(null, server, "pending_audit/run1", "pending_audit:trial", participants);
                check(read(server).getProperty(offline.toString()).equals("0,2,minecraft:diamond=2"), "offline participant gets persisted supplement once");
                complete.invoke(null, server, "pending_audit/run2", "pending_audit:trial", participants);
                check(read(server).getProperty(offline.toString()).split(";").length == 2, "later run preserves earlier offline entitlement");
                var load = InfernalRewardLedger.class.getDeclaredMethod("load", MinecraftServer.class);
                load.setAccessible(true); load.invoke(null, server);
                complete.invoke(null, server, "pending_audit/run1", "pending_audit:trial", participants);
                check(read(server).getProperty(offline.toString()).split(";").length == 2, "completion deduplication survives ledger reload");
                var nether = server.getLevel(Level.NETHER);
                BlockPos vault = new BlockPos(0, 64, 5);
                nether.setBlockAndUpdate(vault, NexusBlocks.INFERNAL_VAULT.defaultBlockState());
                var hit = new BlockHitResult(Vec3.atCenterOf(vault), Direction.UP, vault, false);
                before[0] = player.getInventory().countItem(Items.DIAMOND);
                UseBlockCallback.EVENT.invoker().interact(player, nether, InteractionHand.OFF_HAND, hit);
                check(player.getInventory().countItem(Items.DIAMOND) == before[0], "offhand does not claim reward");
                UseBlockCallback.EVENT.invoker().interact(player, nether, InteractionHand.MAIN_HAND, hit);
                UseBlockCallback.EVENT.invoker().interact(player, nether, InteractionHand.MAIN_HAND, hit);
                check(player.getInventory().countItem(Items.DIAMOND) == before[0] + 2, "two callbacks in same tick consume one entitlement");
                check(read(server).getProperty(player.getUUID().toString()).split(";").length == 1, "one entitlement remains after repeated callback");
            });
            context.waitTicks(2);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var vault = new BlockPos(0, 64, 5);
                UseBlockCallback.EVENT.invoker().interact(player, server.getLevel(Level.NETHER), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(vault), Direction.UP, vault, false));
                check(player.getInventory().countItem(Items.DIAMOND) == before[0] + 4, "later click claims remaining entitlement");
                check(read(server).getProperty(offline.toString()).split(";").length == 2, "online claims never consume offline participant rewards");
            });
        } catch (Exception error) { throw new RuntimeException(error); }
    }
    private static Properties read(MinecraftServer server) throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(server.getWorldPath(LevelResource.ROOT).resolve("skyblockmulti_infernal_rewards.properties"))) {
            properties.load(reader);
        }
        return properties;
    }
    private static void check(boolean passed, String name) {
        System.out.println("[VAULT-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) throw new AssertionError(name);
    }
}
