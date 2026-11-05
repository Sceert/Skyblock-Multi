package cl.treecs.skyblockmulti.island;

import java.util.EnumSet;
import java.util.HashSet;

public final class IslandRingLayoutVerification {
    private IslandRingLayoutVerification() {
    }

    public static void main(String[] args) {
        var defaults = IslandLayoutConfiguration.defaults(2048).positions();
        require(defaults.size() == 24, "default rings must expose 24 positions");
        require(defaults.get(0).x() == 0 && defaults.get(0).z() == -1024,
                "inner slot 1 uses half the base radius");
        require(defaults.get(8).x() == 400 && defaults.get(8).z() == -2016,
                "outer slot 9 keeps its half-step angular offset");
        for (int capacity : new int[]{8, 16, 24}) {
            var configured = IslandLayoutConfiguration.custom(2048, capacity,
                    EnumSet.of(IslandRingLayout.Ring.INNER, IslandRingLayout.Ring.OUTER),
                    IslandRingLayout.AllocationPolicy.BALANCED, false);
            require(configured.positions().size() == capacity,
                    "configurable rings must support capacity " + capacity);
        }
        require(defaults.equals(IslandLayoutConfiguration.defaults(2048).positions()),
                "reloaded configuration must preserve deterministic geometry");

        var sparse = IslandRingLayout.positions(2048,
                EnumSet.of(IslandRingLayout.Ring.PERIPHERY), 8,
                IslandRingLayout.AllocationPolicy.BALANCED);
        require(sparse.size() == 8, "third ring must support an eight-island layout");
        for (int i = 0; i < sparse.size(); i++) {
            require(sparse.get(i).ringIndex() == i * 3,
                    "sparse third-ring positions must be evenly distributed");
        }

        var normal = IslandRingLayout.positions(2048,
                EnumSet.of(IslandRingLayout.Ring.INNER, IslandRingLayout.Ring.OUTER,
                        IslandRingLayout.Ring.PERIPHERY, IslandRingLayout.Ring.FRONTIER), 80,
                IslandRingLayout.AllocationPolicy.INNER_FIRST);
        require(normal.size() == 80, "four normal rings must expose 80 positions");
        require(new HashSet<>(normal.stream().map(IslandRingLayout.Position::slotId).toList()).size() == 80,
                "slot IDs must be unique and stable");

        var technical = IslandRingLayout.positions(2048,
                EnumSet.allOf(IslandRingLayout.Ring.class), IslandRingLayout.TECHNICAL_LIMIT,
                IslandRingLayout.AllocationPolicy.INNER_FIRST);
        require(technical.size() == 128, "the optional fifth ring must complete the technical ceiling");
        require(technical.get(80).slotId() == 81,
                "the optional technical ring must start at stable slot 81");

        expectFailure(() -> IslandLayoutConfiguration.custom(2048, 81,
                        EnumSet.allOf(IslandRingLayout.Ring.class),
                        IslandRingLayout.AllocationPolicy.BALANCED, false),
                "normal custom layouts must stop at 80 slots");
        expectFailure(() -> IslandLayoutConfiguration.custom(2048, 8,
                        EnumSet.of(IslandRingLayout.Ring.DEEP_FRONTIER),
                        IslandRingLayout.AllocationPolicy.BALANCED, false),
                "the fifth ring must require explicit technical opt-in");
        System.out.println("Island ring layout verification passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
