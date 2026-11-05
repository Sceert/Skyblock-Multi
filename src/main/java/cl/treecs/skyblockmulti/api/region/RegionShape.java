package cl.treecs.skyblockmulti.api.region;

import net.minecraft.core.BlockPos;

/** Geometric test used by logical regions. Coordinates are block coordinates. */
@FunctionalInterface
public interface RegionShape {
    boolean contains(BlockPos pos);

    static RegionShape box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return pos -> pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    static RegionShape cylinder(int centerX, int centerZ, int radius, int minY, int maxY) {
        long radiusSquared = (long) radius * radius;
        return pos -> {
            long dx = (long) pos.getX() - centerX;
            long dz = (long) pos.getZ() - centerZ;
            return pos.getY() >= minY && pos.getY() <= maxY
                    && dx * dx + dz * dz <= radiusSquared;
        };
    }
}
