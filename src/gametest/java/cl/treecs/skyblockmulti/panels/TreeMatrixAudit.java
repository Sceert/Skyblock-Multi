package cl.treecs.skyblockmulti.panels;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** Real starter and tree functions over 44 separated sites in one generated save. */
final class TreeMatrixAudit {
    static void run(ClientGameTestContext context) {
        List<String> failures = new ArrayList<>();
        String[] trees = {"oak", "spruce", "birch", "jungle", "acacia", "cherry", "mangrove", "dark_oak", "pale_oak", "azalea", "flowering_azalea"};
        try (var world = context.worldBuilder().create()) {
            world.getServer().waitFor(server -> !server.getPlayerList().getPlayers().isEmpty());
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var level = server.overworld();
                for (int tree = 0; tree < trees.length; tree++) for (int difficulty = 0; difficulty < 4; difficulty++) {
                    int x = 512 + tree * 32, z = 512 + difficulty * 32;
                    for (int cx = (x - 8) >> 4; cx <= (x + 8) >> 4; cx++)
                        for (int cz = (z - 8) >> 4; cz <= (z + 8) >> 4; cz++) level.getChunk(cx, cz);
                    String label = trees[tree] + "/difficulty=" + difficulty;
                    String at = "execute as " + player.getScoreboardName() + " in minecraft:overworld positioned " + x + " 200 " + z + " run ";
                    command(server, "scoreboard players set " + player.getScoreboardName() + " sb_tree " + (tree + 1));
                    command(server, "scoreboard players set " + player.getScoreboardName() + " sb_chest " + difficulty);
                    command(server, at + "function skyblockmulti:island/base_from_anchor");
                    command(server, at + "function skyblockmulti:island/starter_chest");
                    command(server, at + "function skyblockmulti:island/starter_chest/planting_stock");
                    command(server, at + "function skyblockmulti:island/tree/" + trees[tree]);
                    var blockEntity = level.getBlockEntity(new BlockPos(x + 2, 201, z));
                    Container chest = blockEntity instanceof Container container ? container : null;
                    check(difficulty == 0 ? chest == null : chest != null,
                            label + " respects starter chest policy (none in Extreme)", failures);
                    int logs = 0;
                    for (var pos : BlockPos.betweenClosed(x - 8, 201, z - 8, x + 8, 220, z + 8)) {
                        String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
                        if (id.endsWith("_log") || id.endsWith("_wood")) logs++;
                    }
                    check(logs > 0, label + " tree contains harvestable trunk", failures);
                    if (chest == null) continue;
                    if (difficulty == 1) check(count(chest, Items.WATER_BUCKET) == 1 && count(chest, Items.LAVA_BUCKET) == 1,
                            label + " Hard base materials", failures);
                    if (difficulty == 2 || difficulty == 3) check(count(chest, Items.ICE) == 2 && count(chest, Items.LAVA_BUCKET) == 1
                                    && count(chest, Items.BREAD) == (difficulty == 2 ? 4 : 8), label + " difficulty supplies", failures);
                    var expected = tree == 6 ? Items.MANGROVE_PROPAGULE : tree == 7 ? Items.DARK_OAK_SAPLING : tree == 8 ? Items.PALE_OAK_SAPLING : null;
                    check(expected == null ? chest.getItem(26).isEmpty() : chest.getItem(26).is(expected)
                            && chest.getItem(26).getCount() == (tree == 6 ? 1 : 4), label + " planting stock policy", failures);
                    chest.setItem(25, new ItemStack(Items.DIAMOND, 3));
                    int bread = count(chest, Items.BREAD);
                    command(server, at + "function skyblockmulti:island/starter_chest");
                    check(count(chest, Items.DIAMOND) == 3 && count(chest, Items.BREAD) == bread,
                            label + " repeated initialization preserves stored items", failures);
                }
            });
        }
        if (!failures.isEmpty()) throw new AssertionError("Tree matrix failures: " + failures);
    }
    private static int count(Container inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) if (inventory.getItem(slot).is(item)) count += inventory.getItem(slot).getCount();
        return count;
    }
    private static void command(MinecraftServer server, String value) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), value);
    }
    private static void check(boolean passed, String name, List<String> failures) {
        System.out.println("[TREES-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
