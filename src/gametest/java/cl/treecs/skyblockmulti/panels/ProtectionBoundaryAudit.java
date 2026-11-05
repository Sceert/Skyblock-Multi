package cl.treecs.skyblockmulti.panels;

import cl.treecs.skyblockmulti.NexusFoundation;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import java.util.ArrayList;
import java.util.List;

/** New boundary cases only; all fixtures belong to a disposable generated world. */
final class ProtectionBoundaryAudit {
    static void run(ClientGameTestContext context) {
        List<String> failures = new ArrayList<>();
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var protectedEdge = new BlockPos(45, 170, 0);
                check(NexusFoundation.isProtectedStructurePosition(level, protectedEdge),
                        "radius 45 edge is protected", failures);
                check(!NexusFoundation.isProtectedStructurePosition(level, protectedEdge.east()),
                        "radius 46 is outside protection", failures);
                // Force exact fixture chunks so the outside control ticks too.
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "forceload add 32 -16 48 16");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "forceload add 592 -16 608 16");
                for (int x : new int[]{45, 600}) {
                    var target = new BlockPos(x, 170, 0);
                    for (int offset = -1; offset <= 2; offset++) {
                        level.setBlockAndUpdate(target.offset(offset, -1, 0), Blocks.GRASS_BLOCK.defaultBlockState());
                        level.setBlockAndUpdate(target.offset(offset, 0, -1), Blocks.STONE.defaultBlockState());
                        level.setBlockAndUpdate(target.offset(offset, 0, 1), Blocks.STONE.defaultBlockState());
                        level.setBlockAndUpdate(target.offset(offset, 1, 0), Blocks.AIR.defaultBlockState());
                        level.setBlockAndUpdate(target.offset(offset, 0, 0), Blocks.AIR.defaultBlockState());
                    }
                    level.setBlockAndUpdate(target.west(), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(target.east(2), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(target, Blocks.POPPY.defaultBlockState());
                    check(level.getBlockState(target).canSurvive(level, target),
                            "flower fixture has valid soil at x=" + x, failures);
                    level.setBlockAndUpdate(target.east(), Blocks.WATER.defaultBlockState());
                }
            });
            context.waitTicks(80);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                check(level.getBlockState(new BlockPos(45, 170, 0)).is(Blocks.POPPY),
                        "water entering from outside preserves protected flower", failures);
                check(level.getBlockState(new BlockPos(600, 170, 0)).is(Blocks.WATER),
                        "outside control water flows and removes flower", failures);
                var piston = new BlockPos(46, 175, 0);
                level.setBlockAndUpdate(piston, Blocks.PISTON.defaultBlockState()
                        .setValue(PistonBaseBlock.FACING, Direction.WEST));
                level.setBlockAndUpdate(piston.west(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(piston.west(2), Blocks.AIR.defaultBlockState());
                check(!new PistonStructureResolver(level, piston, Direction.WEST, true).resolve(),
                        "outside piston cannot move protected edge block", failures);
                piston = new BlockPos(600, 175, 0);
                level.setBlockAndUpdate(piston, Blocks.PISTON.defaultBlockState()
                        .setValue(PistonBaseBlock.FACING, Direction.WEST));
                level.setBlockAndUpdate(piston.west(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(piston.west(2), Blocks.AIR.defaultBlockState());
                check(new PistonStructureResolver(level, piston, Direction.WEST, true).resolve(),
                        "outside control piston remains functional", failures);
            });
        }
        System.out.println("[BOUNDARY-AUDIT] SUMMARY checks=8 failures=" + failures.size());
        if (!failures.isEmpty()) throw new AssertionError("Boundary audit failures: " + failures);
    }

    private static void check(boolean passed, String name, List<String> failures) {
        System.out.println("[BOUNDARY-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures.add(name);
    }
}
