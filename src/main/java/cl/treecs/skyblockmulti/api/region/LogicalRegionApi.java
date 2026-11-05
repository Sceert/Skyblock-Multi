package cl.treecs.skyblockmulti.api.region;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Public registry/query API for logical locations. Higher priority wins primaryRegionAt. */
public final class LogicalRegionApi {
    public static final int API_VERSION = 1;
    private static final Map<Identifier, LogicalRegion> REGIONS = new LinkedHashMap<>();

    private LogicalRegionApi() {
    }

    public static synchronized void register(LogicalRegion region) {
        if (REGIONS.putIfAbsent(region.id(), region) != null) {
            throw new IllegalArgumentException("Logical region already registered: " + region.id());
        }
    }

    public static synchronized List<LogicalRegion> regions() {
        return List.copyOf(REGIONS.values());
    }

    public static synchronized Optional<LogicalRegion> get(Identifier id) {
        return Optional.ofNullable(REGIONS.get(id));
    }

    public static List<LogicalRegion> regionsAt(ResourceKey<Level> dimension, BlockPos pos) {
        List<LogicalRegion> matches = new ArrayList<>();
        for (LogicalRegion region : regions()) {
            if (region.dimension().equals(dimension) && region.shape().contains(pos)) {
                matches.add(region);
            }
        }
        matches.sort(Comparator.comparingInt(LogicalRegion::priority).reversed());
        return List.copyOf(matches);
    }

    public static Optional<LogicalRegion> primaryRegionAt(ResourceKey<Level> dimension, BlockPos pos) {
        List<LogicalRegion> matches = regionsAt(dimension, pos);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }
}
