package cl.treecs.skyblockmulti.panels;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Real preset, assignment and save/reopen; only newly generated disposable saves. */
final class CoreWorldAudit {
    private static final List<String> failures = new ArrayList<>();
    private static final BlockPos HUB_SENTINEL = new BlockPos(10, 164, 0);
    static void run(ClientGameTestContext context) {
        failures.clear();
        TestWorldSave save;
        int[] owned = new int[3];
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setWorldType(settings.getNormalPresetList().stream().filter(entry -> entry.preset().unwrapKey()
                    .map(key -> key.identifier().toString().equals("skyblockmulti:skyblockmulti")).orElse(false)).findFirst().orElseThrow());
        }).create()) {
            save = world.getWorldSave();
            world.getServer().waitFor(server -> !server.getPlayerList().getPlayers().isEmpty()
                    && score(server, "#slots_built", "sb3_const") == 1, 2400);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                String name = player.getScoreboardName();
                check(score(server, "#active", "sb3_const") == 1, "Skyblock preset activates core");
                check(server.overworld().getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource,
                        "Overworld uses void flat generator");
                check(server.getLevel(Level.NETHER).getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource,
                        "Nether uses void flat generator");
                check(server.getLevel(Level.END).getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,
                        "End retains normal noise generator");
                check(score(server, name, "sb3_state") == 1 && Math.abs(player.getY() - 161) < 2,
                        "first arrival enters selection at Last Refuge");
                check(player.getInventory().countItem(Items.WRITTEN_BOOK) == 1, "lore book is delivered before island selection");
                player.setGameMode(GameType.SURVIVAL);
                var level = server.overworld();
                level.setBlockAndUpdate(HUB_SENTINEL, Blocks.DIAMOND_BLOCK.defaultBlockState());
                check(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, HUB_SENTINEL,
                        level.getBlockState(HUB_SENTINEL), null), "Survival cannot break Nexus sentinel");
                player.setGameMode(GameType.ADVENTURE);
                check(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, HUB_SENTINEL,
                        level.getBlockState(HUB_SENTINEL), null), "Adventure cannot break Nexus sentinel");
                player.setGameMode(GameType.CREATIVE);
                check(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, HUB_SENTINEL,
                        level.getBlockState(HUB_SENTINEL), null), "Creative bypass permits Nexus break");
                player.setGameMode(GameType.SURVIVAL);
                BlockPos outside = new BlockPos(600, 160, 0);
                check(PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, outside,
                        Blocks.STONE.defaultBlockState(), null), "outside Nexus break callback preserves vanilla");
                var originalHand = player.getMainHandItem().copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 8));
                var hit = new BlockHitResult(Vec3.atCenterOf(HUB_SENTINEL), Direction.UP, HUB_SENTINEL, false);
                check(UseBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL
                        && player.getMainHandItem().getCount() == 8, "Nexus placement denied before stack consumption");
                var button = HUB_SENTINEL.above();
                level.setBlockAndUpdate(button, Blocks.STONE_BUTTON.defaultBlockState());
                var buttonHit = new BlockHitResult(Vec3.atCenterOf(button), Direction.UP, button, false);
                check(UseBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, buttonHit) == InteractionResult.PASS,
                        "safe button interaction passes while holding stone");
                player.setItemInHand(InteractionHand.MAIN_HAND, originalHand);
                command(server, "scoreboard players set " + name + " sb_tree 7");
            });
            world.getServer().waitFor(server -> score(server, server.getPlayerList().getPlayers().getFirst().getScoreboardName(), "sb3_state") == 4);
            world.getServer().runOnServer(server -> command(server, "scoreboard players set "
                    + server.getPlayerList().getPlayers().getFirst().getScoreboardName() + " sb_difficulty 2"));
            world.getServer().waitFor(server -> score(server, server.getPlayerList().getPlayers().getFirst().getScoreboardName(), "sb3_state") == 2, 2400);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                String name = player.getScoreboardName();
                owned[0] = score(server, name, "sb3_slot"); owned[1] = score(server, name, "sb3_x"); owned[2] = score(server, name, "sb3_z");
                check(owned[0] > 0 && (owned[1] != 0 || owned[2] != 0), "solo Mangrove Hard creates valid OWN");
                check(score(server, name, "sb_active_x") == owned[1] && score(server, name, "sb_active_z") == owned[2], "initial ACTIVE matches OWN");
                check(score(server, "#geometry_locked", "sb3_cfg") == 1, "first assignment locks geometry");
                check(score(server, name, "sb_chest") == 1, "Hard difficulty remains numeric value 1");
                check(player.getInventory().countItem(Items.WRITTEN_BOOK) == 1, "one lore book after assignment");
                server.overworld().setBlockAndUpdate(new BlockPos(owned[1] + 2, 70, owned[2]), Blocks.EMERALD_BLOCK.defaultBlockState());
            });
        }
        try (var world = save.open()) {
            world.getServer().waitFor(server -> !server.getPlayerList().getPlayers().isEmpty());
            context.waitTicks(40);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                String name = player.getScoreboardName();
                check(score(server, name, "sb3_slot") == owned[0] && score(server, name, "sb3_x") == owned[1]
                        && score(server, name, "sb3_z") == owned[2], "OWN survives world reopen");
                check(player.distanceToSqr(owned[1] + .5, player.getY(), owned[2] + .5) < 100, "established player returns to island");
                check(server.overworld().getBlockState(new BlockPos(owned[1] + 2, 70, owned[2])).is(Blocks.EMERALD_BLOCK), "island build survives reopen");
                check(server.overworld().getBlockState(HUB_SENTINEL).is(Blocks.DIAMOND_BLOCK), "Nexus is not rebuilt on reopen");
                check(score(server, "#geometry_locked", "sb3_cfg") == 1, "geometry lock survives reopen");
                check(player.getInventory().countItem(Items.WRITTEN_BOOK) == 1, "reopen does not duplicate lore book");
            });
        }
        if (!failures.isEmpty()) throw new AssertionError("Core audit failures: " + failures);
    }
    private static int score(MinecraftServer server, String holder, String objectiveName) {
        var objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) return -999;
        var info = server.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return info == null ? -999 : info.value();
    }
    private static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
    private static void check(boolean passed, String name) {
        System.out.println("[CORE-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
