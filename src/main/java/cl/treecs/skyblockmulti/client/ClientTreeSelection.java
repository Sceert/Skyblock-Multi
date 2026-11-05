package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.TreeSelectPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Solicitudes de UI; el servidor vuelve a validar toda selección. */
public final class ClientTreeSelection {
    private ClientTreeSelection() {
    }

    public static boolean request(String treeId) {
        boolean visibleAndEnabled = ClientTreeCatalog.entries().stream()
                .anyMatch(entry -> entry.enabled() && entry.selectable() && entry.id().equals(treeId));
        if (!visibleAndEnabled || !ClientPlayNetworking.canSend(TreeSelectPayload.TYPE)) return false;
        ClientPlayNetworking.send(new TreeSelectPayload(treeId));
        return true;
    }

    public static boolean requestRandom() {
        boolean hasAvailableTree = ClientTreeCatalog.entries().stream()
                .anyMatch(entry -> entry.enabled() && entry.selectable());
        if (!hasAvailableTree || !ClientPlayNetworking.canSend(TreeSelectPayload.TYPE)) return false;
        ClientPlayNetworking.send(new TreeSelectPayload(TreeSelectPayload.RANDOM_SELECTION));
        return true;
    }
}
