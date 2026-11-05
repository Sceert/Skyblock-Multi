package cl.treecs.skyblockmulti.infernal;

import java.util.List;
import java.util.Map;

/** Contrato de datos de la API de Pruebas Infernales, schema_version 1. */
public final class InfernalDefinitions {
    private InfernalDefinitions() {
    }

    public record Point(int x, int y, int z) {
    }

    public record Arena(String id, String dimension, Point origin, int cleanupRadius,
                        Map<String, List<Point>> floors, String baselineStructure,
                        String restoreFunction) {
        public Arena {
            floors = Map.copyOf(floors);
        }
    }

    public record Trial(String id, String arena, String displayJson,
                        List<Activation> activations, int countdownSeconds,
                        int cooldownSeconds, boolean cleanupMobs, boolean cleanupItems,
                        int spawnReleaseSeconds, List<Phase> phases, Scaling scaling,
                        Rewards rewards) {
        public Trial {
            activations = List.copyOf(activations);
            phases = List.copyOf(phases);
        }
    }

    /** Un ítem registrado y, opcionalmente, custom_data que lo distingue. */
    public record Activation(String item, String customDataJson, boolean consume) {
    }

    public record Phase(String id, String displayJson, int delaySeconds,
                        String arenaSetupFunction, String arenaRestoreFunction,
                        List<Mob> mobs) {
        public Phase {
            mobs = List.copyOf(mobs);
        }
    }

    public record Mob(String entity, int count, List<String> floors, String placement,
                      boolean champion, double scale, double healthMultiplier,
                      double damageMultiplier, double knockbackResistance,
                      String nameJson, Map<String, Equipment> equipment,
                      List<Effect> effects, String alliance, String dropPolicy,
                      boolean countsForVictory, boolean childrenCountForVictory,
                      boolean persistent, boolean playerTargetingOnly,
                      int spawnDelayTicks, double chance) {
        public Mob {
            floors = List.copyOf(floors);
            equipment = Map.copyOf(equipment);
            effects = List.copyOf(effects);
        }
    }

    public record Equipment(String item, int count, double dropChance,
                            String componentsJson) {
    }

    public record Effect(String id, int amplifier, int durationTicks,
                         boolean particles, boolean icon) {
    }

    public record Scaling(int maximumParticipants, int extraMobsPerPlayer,
                          double healthPerExtraPlayer,
                          Map<String, DifficultyScaling> difficulties,
                          double maximumCountMultiplier) {
        public Scaling {
            difficulties = Map.copyOf(difficulties);
        }
    }

    public record DifficultyScaling(double countMultiplier,
                                    double healthMultiplier,
                                    double damageMultiplier) {
    }

    public record Rewards(String completionLootTable, boolean perParticipant,
                          String phaseLootTable) {
    }
}
