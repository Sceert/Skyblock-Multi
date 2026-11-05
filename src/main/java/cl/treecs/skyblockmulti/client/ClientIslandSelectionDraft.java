package cl.treecs.skyblockmulti.client;

import net.minecraft.network.chat.Component;

/** Client-only summary of the choice being confirmed before island creation. */
public final class ClientIslandSelectionDraft {
    private static Component tree = Component.translatable("skyblockmulti.tree_screen.random");

    private ClientIslandSelectionDraft() {
    }

    public static void selectTree(Component displayName) {
        tree = displayName.copy();
    }

    public static void selectRandom() {
        tree = Component.translatable("skyblockmulti.tree_screen.random");
    }

    public static Component tree() {
        return tree.copy();
    }

    public static void clear() {
        tree = Component.translatable("skyblockmulti.tree_screen.random");
    }
}
