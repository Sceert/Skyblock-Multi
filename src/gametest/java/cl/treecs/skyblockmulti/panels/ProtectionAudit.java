package cl.treecs.skyblockmulti.panels;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Exercises actual mixed-in environmental mechanisms in a disposable world. */
final class ProtectionAudit {
    static void run(ClientGameTestContext context) {
        List<String> failures = new ArrayList<>();
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                for (int x : new int[] { 20, 600 }) {
                    boolean inside = x == 20;
                    String region = inside ? "inside" : "outside";
                    BlockPos target = new BlockPos(x, 170, 0);
                    level.setBlockAndUpdate(target, Blocks.OAK_PLANKS.defaultBlockState());
                    new ServerExplosion(level, null, null, null, Vec3.atCenterOf(target), 4, false,
                            Explosion.BlockInteraction.DESTROY).explode();
                    check(level.getBlockState(target).is(Blocks.OAK_PLANKS) == inside,
                            "explosion " + region + " respects regional destruction", failures);

                    level.setBlockAndUpdate(target, Blocks.OAK_PLANKS.defaultBlockState());
                    var burn = FireBlock.class.getDeclaredMethod("checkBurnOut", Level.class, BlockPos.class,
                            int.class, RandomSource.class, int.class);
                    burn.setAccessible(true);
                    burn.invoke(Blocks.FIRE, level, target, 1, RandomSource.create(42), 0);
                    check(level.getBlockState(target).is(Blocks.OAK_PLANKS) == inside,
                            "fire burn " + region + " respects regional protection", failures);
                    level.setBlockAndUpdate(target, Blocks.OAK_PLANKS.defaultBlockState());
                    var odds = FireBlock.class.getDeclaredMethod("getIgniteOdds", LevelReader.class, BlockPos.class);
                    odds.setAccessible(true);
                    level.setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
                    check(((int) odds.invoke(Blocks.FIRE, level, target.above()) == 0) == inside,
                            "fire spread odds " + region + " preserve boundary", failures);

                    for (var block : List.of(Blocks.STONE, Blocks.SLIME_BLOCK, Blocks.HONEY_BLOCK)) {
                        level.setBlockAndUpdate(target, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
                        level.setBlockAndUpdate(target.east(), block.defaultBlockState());
                        level.setBlockAndUpdate(target.east(2), Blocks.AIR.defaultBlockState());
                        check(new PistonStructureResolver(level, target, Direction.EAST, true).resolve() != inside,
                                "piston " + block + " " + region, failures);
                    }
                }
            });
        } catch (Exception error) { throw new RuntimeException(error); }
        if (!failures.isEmpty()) throw new AssertionError("Protection audit failures: " + failures);
    }
    private static void check(boolean passed, String name, List<String> failures) {
        System.out.println("[PROTECTION-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
