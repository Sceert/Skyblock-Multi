package cl.treecs.skyblockmulti.island;

import java.util.List;
import java.util.Set;

/** Validated, persistent description of the configurable island rings. */
public record IslandLayoutConfiguration(
        int baseRadius,
        int capacity,
        Set<IslandRingLayout.Ring> enabledRings,
        IslandRingLayout.AllocationPolicy allocationPolicy,
        boolean technicalCapacity
) {
    public IslandLayoutConfiguration {
        allocationPolicy = allocationPolicy == null
                ? IslandRingLayout.AllocationPolicy.DIFFICULTY : allocationPolicy;
        enabledRings = enabledRings == null ? Set.of() : Set.copyOf(enabledRings);
        if (baseRadius < 16) throw new IllegalArgumentException("baseRadius must be at least 16");
        if (enabledRings.isEmpty()) throw new IllegalArgumentException("rings cannot be empty");
        int ceiling = technicalCapacity
                ? IslandRingLayout.TECHNICAL_LIMIT : IslandRingLayout.ADVANCED_LIMIT;
        if (capacity < 1 || capacity > ceiling) {
            throw new IllegalArgumentException("capacity must be between 1 and " + ceiling);
        }
        if (!technicalCapacity && enabledRings.contains(IslandRingLayout.Ring.DEEP_FRONTIER)) {
            throw new IllegalArgumentException("the fifth ring requires technical capacity opt-in");
        }
        // Reject impossible capacities before saving or locking world geometry.
        IslandRingLayout.positions(baseRadius, enabledRings, capacity, allocationPolicy);
    }

    public static IslandLayoutConfiguration defaults(int radius) {
        return custom(radius, 24, Set.of(IslandRingLayout.Ring.INNER, IslandRingLayout.Ring.OUTER),
                IslandRingLayout.AllocationPolicy.DIFFICULTY, false);
    }

    public static IslandLayoutConfiguration custom(int radius, int capacity,
            Set<IslandRingLayout.Ring> rings, IslandRingLayout.AllocationPolicy policy,
            boolean technicalCapacity) {
        return new IslandLayoutConfiguration(radius, capacity, rings, policy, technicalCapacity);
    }

    public List<IslandRingLayout.Position> positions() {
        return IslandRingLayout.positions(baseRadius, enabledRings, capacity, allocationPolicy);
    }
}
