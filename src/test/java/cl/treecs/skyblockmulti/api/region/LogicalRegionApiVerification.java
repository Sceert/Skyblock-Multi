package cl.treecs.skyblockmulti.api.region;

import cl.treecs.skyblockmulti.region.BuiltInLogicalRegions;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

public final class LogicalRegionApiVerification {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInLogicalRegions.register();

        expect("last_refuge", Level.OVERWORLD, new BlockPos(0, 161, 0));
        expect("end_portal_chamber", Level.OVERWORLD, new BlockPos(0, 6, 0));
        expect("nexus_fortress", Level.OVERWORLD, new BlockPos(62, 16, 0));
        expect("infernal_coliseum", Level.NETHER, new BlockPos(0, 64, 0));

        if (!LogicalRegionApi.regionsAt(Level.OVERWORLD, new BlockPos(0, 6, 0)).stream()
                .anyMatch(region -> region.id().getPath().equals("ascension_nexus"))) {
            throw new AssertionError("The portal chamber must also belong to the Ascension Nexus.");
        }
        if (LogicalRegionApi.primaryRegionAt(Level.OVERWORLD, new BlockPos(500, 64, 500)).isPresent()) {
            throw new AssertionError("Remote wilderness must not resolve to a built-in region.");
        }
        System.out.println("Logical region API verification passed.");
    }

    private static void expect(String path, net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
        LogicalRegion region = LogicalRegionApi.primaryRegionAt(dimension, pos)
                .orElseThrow(() -> new AssertionError("Missing region at " + pos));
        if (!region.id().getPath().equals(path)) {
            throw new AssertionError("Expected " + path + " at " + pos + ", got " + region.id());
        }
    }
}
