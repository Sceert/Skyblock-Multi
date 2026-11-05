package cl.treecs.skyblockmulti.region;

import cl.treecs.skyblockmulti.api.region.LogicalRegion;
import cl.treecs.skyblockmulti.api.region.LogicalRegionApi;
import cl.treecs.skyblockmulti.api.region.RegionShape;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** Canonical regions for the structures placed once at the center of each world. */
public final class BuiltInLogicalRegions {
    private static boolean registered;

    private BuiltInLogicalRegions() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        register("ascension_nexus", Level.OVERWORLD, pos -> {
            int radius = pos.getY() >= 150 ? 45 : pos.getY() >= 41 ? 12 : 74;
            long x = pos.getX();
            long z = pos.getZ();
            return x * x + z * z <= (long) radius * radius;
        }, 10);
        register("last_refuge", Level.OVERWORLD,
                RegionShape.cylinder(0, 0, 45, 150, 205), 90);
        register("nexus_fortress", Level.OVERWORLD,
                RegionShape.cylinder(0, 0, 74, 0, 40), 80);
        register("end_portal_chamber", Level.OVERWORLD,
                RegionShape.box(-12, 0, -12, 12, 13, 12), 100);
        register("infernal_coliseum", Level.NETHER,
                RegionShape.box(-32, 59, -32, 32, 96, 32), 80);
    }

    private static void register(String path, net.minecraft.resources.ResourceKey<Level> dimension,
                                 RegionShape shape, int priority) {
        LogicalRegionApi.register(new LogicalRegion(
                Identifier.fromNamespaceAndPath("skyblockmulti", path),
                dimension,
                shape,
                priority,
                "skyblockmulti.region." + path
        ));
    }
}
