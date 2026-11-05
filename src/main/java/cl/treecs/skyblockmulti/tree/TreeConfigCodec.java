package cl.treecs.skyblockmulti.tree;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compatibilidad entre la configuración legacy y los IDs estables de árboles. */
public final class TreeConfigCodec {
    private static final Pattern ENABLED_TREES_PATTERN = Pattern.compile(
            "\\\"enabledTrees\\\"\\s*:\\s*\\{([^}]*)}",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TREE_STATE_PATTERN = Pattern.compile(
            "\\\"([a-z0-9_.-]+(?::[a-z0-9_./-]+)?)\\\"\\s*:\\s*(true|false)",
            Pattern.CASE_INSENSITIVE
    );

    private TreeConfigCodec() {
    }

    public static Map<String, Boolean> readStates(String rawConfig) {
        Map<String, Boolean> trees = defaultStates();
        trees.putAll(readExplicitStates(rawConfig));
        return trees;
    }

    /** Reads only values present in the JSON, without injecting catalog defaults. */
    public static Map<String, Boolean> readExplicitStates(String rawConfig) {
        Map<String, Boolean> trees = new LinkedHashMap<>();
        Matcher enabledTreesMatcher = ENABLED_TREES_PATTERN.matcher(rawConfig);
        if (!enabledTreesMatcher.find()) return trees;

        Matcher treeMatcher = TREE_STATE_PATTERN.matcher(enabledTreesMatcher.group(1));
        while (treeMatcher.find()) {
            String configuredId = normalizeId(treeMatcher.group(1));
            if (configuredId != null) {
                trees.put(configuredId, Boolean.parseBoolean(treeMatcher.group(2)));
            }
        }
        return trees;
    }

    public static Map<String, Boolean> normalize(Map<String, Boolean> requested) {
        Map<String, Boolean> normalizedRequested = new LinkedHashMap<>();
        if (requested != null) {
            requested.forEach((id, enabled) -> {
                String normalizedId = normalizeId(id);
                if (normalizedId != null) {
                    normalizedRequested.put(normalizedId, Boolean.TRUE.equals(enabled));
                }
            });
        }

        Map<String, Boolean> trees = new LinkedHashMap<>();
        for (DataTreeDefinition tree : TreeCatalog.activeDefinitions().values()) {
            trees.put(
                    tree.id(),
                    normalizedRequested.containsKey(tree.id())
                            ? Boolean.TRUE.equals(normalizedRequested.get(tree.id()))
                            : tree.enabledByDefault()
            );
        }
        normalizedRequested.forEach(trees::putIfAbsent);
        if (enabledAvailableCount(trees) == 0) {
            trees.put("minecraft:oak", true);
        }
        return trees;
    }

    public static Map<String, Boolean> defaultStates() {
        Map<String, Boolean> trees = new LinkedHashMap<>();
        for (DataTreeDefinition tree : TreeCatalog.activeDefinitions().values()) {
            trees.put(tree.id(), tree.enabledByDefault());
        }
        return trees;
    }

    public static int enabledAvailableCount(Map<String, Boolean> trees) {
        int count = 0;
        for (DataTreeDefinition tree : TreeCatalog.activeDefinitions().values()) {
            if (Boolean.TRUE.equals(trees.get(tree.id()))) count++;
        }
        return count;
    }

    public static String normalizeId(String configuredId) {
        if (configuredId == null) return null;
        String candidate = configuredId.trim().toLowerCase(Locale.ROOT);
        if (!candidate.contains(":")) {
            return TreeCatalog.findConfigKey(candidate).map(TreeDefinition::id).orElse(null);
        }
        try {
            return TreeDefinition.requireNamespacedId(candidate);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
