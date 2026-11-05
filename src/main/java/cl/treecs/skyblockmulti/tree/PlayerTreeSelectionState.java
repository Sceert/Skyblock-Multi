package cl.treecs.skyblockmulti.tree;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Selección temporal autoritativa; no representa una isla ya creada. */
public final class PlayerTreeSelectionState {
    private static final Map<UUID, String> SELECTIONS = new ConcurrentHashMap<>();

    private PlayerTreeSelectionState() {
    }

    public static Result select(UUID playerId, String requestedId,
                                Map<String, Boolean> configuredStates,
                                boolean selectionAllowed) {
        if (!selectionAllowed) return Result.NOT_IN_SELECTION;
        String treeId = TreeConfigCodec.normalizeId(requestedId);
        if (treeId == null) return Result.INVALID_ID;
        if (!TreeCatalog.activeDefinitions().containsKey(treeId)) return Result.UNKNOWN_TREE;
        if (!Boolean.TRUE.equals(configuredStates.get(treeId))) return Result.DISABLED_TREE;
        SELECTIONS.put(playerId, treeId);
        return Result.ACCEPTED;
    }

    public static Result selectRandom(UUID playerId, Map<String, Boolean> configuredStates,
                                      java.util.function.IntUnaryOperator nextInt,
                                      boolean selectionAllowed) {
        if (!selectionAllowed) return Result.NOT_IN_SELECTION;
        var enabled = TreeCatalog.enabledDefinitions(configuredStates);
        if (enabled.isEmpty()) return Result.NO_AVAILABLE_TREES;
        String selectedId = TreeRandomSelector.choose(enabled, nextInt).id();
        SELECTIONS.put(playerId, selectedId);
        return Result.ACCEPTED;
    }

    public static Optional<String> get(UUID playerId) {
        return Optional.ofNullable(SELECTIONS.get(playerId));
    }

    public static void clear(UUID playerId) {
        SELECTIONS.remove(playerId);
    }

    public static void clearAll() {
        SELECTIONS.clear();
    }

    public enum Result {
        ACCEPTED,
        NOT_IN_SELECTION,
        INVALID_ID,
        UNKNOWN_TREE,
        DISABLED_TREE,
        NO_AVAILABLE_TREES
    }
}
