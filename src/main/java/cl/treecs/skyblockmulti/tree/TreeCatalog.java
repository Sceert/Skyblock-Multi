package cl.treecs.skyblockmulti.tree;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** Catálogo ordenado de árboles conocidos por el núcleo. */
public final class TreeCatalog {
    private static final List<TreeDefinition> BUILT_INS = List.of(
            builtIn("oak", 1),
            builtIn("spruce", 2),
            builtIn("birch", 3),
            builtIn("jungle", 4),
            builtIn("acacia", 5),
            builtIn("cherry", 6),
            builtIn("mangrove", 7),
            builtIn("dark_oak", 8),
            builtIn("pale_oak", 9),
            builtIn("azalea", 10),
            builtIn("flowering_azalea", 11)
    );

    private static final Map<String, TreeDefinition> BY_ID = indexById(BUILT_INS);
    private static final Map<String, TreeDefinition> BY_CONFIG_KEY = indexByConfigKey(BUILT_INS);
    private static final Map<Integer, TreeDefinition> BY_LEGACY_TRIGGER = indexByLegacyTrigger(BUILT_INS);
    private static volatile Map<String, DataTreeDefinition> dataPackDefinitions = Map.of();

    private TreeCatalog() {
    }

    public static List<TreeDefinition> builtIns() {
        return BUILT_INS;
    }

    public static Optional<TreeDefinition> find(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Optional<TreeDefinition> findConfigKey(String configKey) {
        return Optional.ofNullable(BY_CONFIG_KEY.get(configKey));
    }

    public static Optional<TreeDefinition> findLegacyTrigger(int triggerValue) {
        return Optional.ofNullable(BY_LEGACY_TRIGGER.get(triggerValue));
    }

    public static Map<String, DataTreeDefinition> activeDefinitions() {
        Map<String, DataTreeDefinition> active = builtInDataDefinitions();
        active.putAll(dataPackDefinitions);
        return Map.copyOf(active);
    }

    public static Map<String, DataTreeDefinition> dataPackDefinitions() {
        return dataPackDefinitions;
    }

    public static List<DataTreeDefinition> enabledDefinitions(Map<String, Boolean> configuredStates) {
        return activeDefinitions().values().stream()
                .filter(definition -> Boolean.TRUE.equals(configuredStates.get(definition.id())))
                .toList();
    }

    public static DataTreeDefinition chooseRandom(Map<String, Boolean> configuredStates,
                                                  RandomGenerator random) {
        return TreeRandomSelector.choose(enabledDefinitions(configuredStates), random);
    }

    static void replaceDataPackDefinitions(Map<String, DataTreeDefinition> definitions) {
        dataPackDefinitions = Map.copyOf(definitions);
    }

    private static TreeDefinition builtIn(String path, int legacyTriggerValue) {
        return new TreeDefinition(
                "minecraft:" + path,
                path,
                "skyblockmulti.config.tree." + path,
                legacyTriggerValue,
                "#tree_" + path
        );
    }

    private static Map<String, TreeDefinition> indexById(List<TreeDefinition> definitions) {
        Map<String, TreeDefinition> indexed = new LinkedHashMap<>();
        for (TreeDefinition definition : definitions) {
            TreeDefinition previous = indexed.put(definition.id(), definition);
            if (previous != null) {
                throw new IllegalStateException("ID de árbol duplicado: " + definition.id());
            }
        }
        return Map.copyOf(indexed);
    }

    private static Map<Integer, TreeDefinition> indexByLegacyTrigger(List<TreeDefinition> definitions) {
        Map<Integer, TreeDefinition> indexed = new LinkedHashMap<>();
        for (TreeDefinition definition : definitions) {
            TreeDefinition previous = indexed.put(definition.legacyTriggerValue(), definition);
            if (previous != null) {
                throw new IllegalStateException(
                        "Valor legacy de árbol duplicado: " + definition.legacyTriggerValue()
                );
            }
        }
        return Map.copyOf(indexed);
    }

    private static Map<String, TreeDefinition> indexByConfigKey(List<TreeDefinition> definitions) {
        Map<String, TreeDefinition> indexed = new LinkedHashMap<>();
        for (TreeDefinition definition : definitions) {
            TreeDefinition previous = indexed.put(definition.configKey(), definition);
            if (previous != null) {
                throw new IllegalStateException("Clave legacy de árbol duplicada: " + definition.configKey());
            }
        }
        return Map.copyOf(indexed);
    }

    private static Map<String, DataTreeDefinition> builtInDataDefinitions() {
        Map<String, DataTreeDefinition> definitions = new LinkedHashMap<>();
        for (TreeDefinition tree : BUILT_INS) {
            DataTreeDefinition.Placement placement;
            String icon;
            if (tree.configKey().equals("azalea") || tree.configKey().equals("flowering_azalea")) {
                boolean flowering = tree.configKey().equals("flowering_azalea");
                placement = new DataTreeDefinition.AzaleaPlacement(flowering);
                icon = "minecraft:" + tree.configKey();
            } else {
                icon = tree.configKey().equals("mangrove")
                        ? "minecraft:mangrove_propagule"
                        : "minecraft:" + tree.configKey() + "_sapling";
                placement = new DataTreeDefinition.SaplingPlacement(
                        icon,
                        tree.configKey().equals("dark_oak")
                                ? DataTreeDefinition.SaplingPlacement.Pattern.TWO_BY_TWO
                                : DataTreeDefinition.SaplingPlacement.Pattern.SINGLE
                );
            }
            definitions.put(tree.id(), new DataTreeDefinition(
                    tree.id(),
                    new DataTreeDefinition.TextValue(tree.translationKey(), null),
                    new DataTreeDefinition.TextValue(
                            "skyblockmulti.menu.tree." + tree.configKey() + ".hover",
                            null
                    ),
                    icon,
                    placement,
                    null,
                    List.of("skyblockmulti:vanilla"),
                    true,
                    List.of()
            ));
        }
        return definitions;
    }
}
