package cl.treecs.skyblockmulti.island;

import cl.treecs.skyblockmulti.addon.RewardFile;
import cl.treecs.skyblockmulti.tree.TreeDefinition;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

/** Additive presentation metadata indexed by slot, so ownership transfers preserve the tree. */
public final class IslandTreeHistory {
    private final Path path;
    private final Properties data;

    public IslandTreeHistory(Path path) {
        this.path = path;
        data = RewardFile.read(path);
        String schema = data.getProperty("schema", "1");
        if (!schema.equals("1")) throw new IllegalStateException("Unsupported island tree history: " + schema);
        for (String key : data.stringPropertyNames()) {
            if (key.equals("schema")) continue;
            if (key.startsWith("slot.")) validateSlot(Integer.parseInt(key.substring(5)));
            else if (key.startsWith("pending.")) UUID.fromString(key.substring(8));
            else throw new IllegalStateException("Invalid island tree history key: " + key);
            TreeDefinition.requireNamespacedId(data.getProperty(key));
        }
        data.setProperty("schema", "1");
    }

    public String tree(int slot) { return data.getProperty("slot." + slot); }
    public void remember(UUID player, String tree) {
        TreeDefinition.requireNamespacedId(tree);
        if (tree.equals(data.getProperty("pending." + player))) return;
        data.setProperty("pending." + player, tree);
        RewardFile.write(path, data);
    }
    public String pending(UUID player) { return data.getProperty("pending." + player); }
    public void commit(UUID player, int slot) {
        validateSlot(slot);
        String tree = pending(player);
        if (tree == null) return;
        // An occupied slot's recorded identity never changes through UI or party membership.
        data.putIfAbsent("slot." + slot, tree);
        data.remove("pending." + player);
        RewardFile.write(path, data);
    }
    private static void validateSlot(int slot) {
        if (slot < 1 || slot > IslandRingLayout.TECHNICAL_LIMIT) throw new IllegalArgumentException("Invalid island slot");
    }
}
