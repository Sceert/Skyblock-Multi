package cl.treecs.skyblockmulti.infernal;

import java.util.Map;
import java.util.Optional;

/** Instantánea inmutable de las definiciones infernales validadas del datapack. */
public final class InfernalDataRegistry {
    private static volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of());

    private InfernalDataRegistry() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    static void replace(Snapshot next) {
        snapshot = next;
    }

    public record Snapshot(Map<String, InfernalDefinitions.Arena> arenas,
                           Map<String, InfernalDefinitions.Trial> trials) {
        public Snapshot {
            arenas = Map.copyOf(arenas);
            trials = Map.copyOf(trials);
        }

        public Optional<InfernalDefinitions.Arena> arena(String id) {
            return Optional.ofNullable(arenas.get(id));
        }

        public Optional<InfernalDefinitions.Trial> trial(String id) {
            return Optional.ofNullable(trials.get(id));
        }
    }
}
