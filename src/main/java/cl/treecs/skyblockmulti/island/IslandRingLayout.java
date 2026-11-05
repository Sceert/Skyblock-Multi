package cl.treecs.skyblockmulti.island;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Deterministic, world-agnostic geometry for current and future island rings. */
public final class IslandRingLayout {
    public static final int RECOMMENDED_LIMIT = 48;
    public static final int ADVANCED_LIMIT = 80;
    public static final int TECHNICAL_LIMIT = 128;

    private IslandRingLayout() {
    }

    public enum Ring {
        INNER(1, 1, 8, 0.5D, 0.0D),
        OUTER(2, 9, 16, 1.0D, 0.5D),
        PERIPHERY(3, 25, 24, 1.5D, 0.0D),
        FRONTIER(4, 49, 32, 2.0D, 0.5D),
        /** Optional technical ring. Disabled by normal presets; completes the 128-slot ceiling. */
        DEEP_FRONTIER(5, 81, 48, 2.5D, 0.0D);

        private final int number;
        private final int firstSlot;
        private final int positions;
        private final double radiusMultiplier;
        private final double angularOffsetSteps;

        Ring(int number, int firstSlot, int positions, double radiusMultiplier,
             double angularOffsetSteps) {
            this.number = number;
            this.firstSlot = firstSlot;
            this.positions = positions;
            this.radiusMultiplier = radiusMultiplier;
            this.angularOffsetSteps = angularOffsetSteps;
        }

        public int number() { return number; }
        public int firstSlot() { return firstSlot; }
        public int positions() { return positions; }
        public double radiusMultiplier() { return radiusMultiplier; }
    }

    public enum AllocationPolicy {
        BALANCED,
        INNER_FIRST,
        OUTER_FIRST,
        DIFFICULTY,
        RANDOM_BALANCED
    }

    public record Position(int slotId, Ring ring, int ringIndex, int x, int z) {
    }

    public static java.util.Optional<Ring> ringForSlot(int slot) {
        return java.util.Arrays.stream(Ring.values())
                .filter(ring -> slot >= ring.firstSlot() && slot < ring.firstSlot() + ring.positions()).findFirst();
    }

    public static List<Position> positions(int baseRadius, Set<Ring> enabledRings,
                                           int capacity, AllocationPolicy policy) {
        if (baseRadius < 16) throw new IllegalArgumentException("baseRadius must be at least 16");
        if (capacity < 1 || capacity > TECHNICAL_LIMIT) {
            throw new IllegalArgumentException("capacity must be between 1 and " + TECHNICAL_LIMIT);
        }
        EnumSet<Ring> enabled = enabledRings == null || enabledRings.isEmpty()
                ? EnumSet.noneOf(Ring.class)
                : EnumSet.copyOf(enabledRings);
        int available = enabled.stream().mapToInt(Ring::positions).sum();
        if (capacity > available) {
            throw new IllegalArgumentException("capacity exceeds enabled ring positions");
        }

        List<Ring> order = new ArrayList<>(enabled);
        if (policy == AllocationPolicy.OUTER_FIRST) order.sort((a, b) -> b.number - a.number);
        else order.sort((a, b) -> a.number - b.number);

        int[] counts = allocateCounts(order, capacity, policy);
        List<Position> result = new ArrayList<>(capacity);
        for (int i = 0; i < order.size(); i++) {
            addEvenlySpaced(result, baseRadius, order.get(i), counts[i]);
        }
        return List.copyOf(result);
    }

    private static int[] allocateCounts(List<Ring> rings, int capacity, AllocationPolicy policy) {
        int[] counts = new int[rings.size()];
        if (policy == AllocationPolicy.BALANCED || policy == AllocationPolicy.RANDOM_BALANCED) {
            int remaining = capacity;
            while (remaining > 0) {
                boolean allocated = false;
                for (int i = 0; i < rings.size() && remaining > 0; i++) {
                    if (counts[i] < rings.get(i).positions) {
                        counts[i]++;
                        remaining--;
                        allocated = true;
                    }
                }
                if (!allocated) break;
            }
            return counts;
        }
        int remaining = capacity;
        for (int i = 0; i < rings.size() && remaining > 0; i++) {
            counts[i] = Math.min(remaining, rings.get(i).positions);
            remaining -= counts[i];
        }
        return counts;
    }

    private static void addEvenlySpaced(List<Position> target, int baseRadius,
                                        Ring ring, int count) {
        if (count == 0) return;
        double radius = baseRadius * ring.radiusMultiplier;
        for (int selected = 0; selected < count; selected++) {
            int ringIndex = (int) Math.floor((double) selected * ring.positions / count);
            double step = Math.PI * 2.0D / ring.positions;
            double angle = -Math.PI / 2.0D + (ringIndex + ring.angularOffsetSteps) * step;
            int x = roundToChunk(Math.cos(angle) * radius);
            int z = roundToChunk(Math.sin(angle) * radius);
            target.add(new Position(ring.firstSlot + ringIndex, ring, ringIndex, x, z));
        }
    }

    private static int roundToChunk(double coordinate) {
        return (int) (Math.round(coordinate / 16.0D) * 16L);
    }
}
