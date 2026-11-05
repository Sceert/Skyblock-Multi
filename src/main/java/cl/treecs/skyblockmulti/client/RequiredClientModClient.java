package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.RequiredClientPayload;
import cl.treecs.skyblockmulti.network.TreeCatalogPayload;
import cl.treecs.skyblockmulti.network.OpenTreeSelectionPayload;
import cl.treecs.skyblockmulti.network.OpenDifficultySelectionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RequiredClientModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientIslandPanel.register();
        ClientConfigurationNetworking.registerGlobalReceiver(
                RequiredClientPayload.TYPE,
                (payload, context) -> {
                    // Registrar el canal confirma al servidor que Skyblock Multi está instalado.
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TreeCatalogPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ClientTreeCatalog.replace(payload.entries())
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(
                OpenTreeSelectionPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (!ClientTreeCatalog.entries().isEmpty()) {
                        context.client().setScreenAndShow(new TreeSelectionScreen());
                    }
                })
        );
        ClientPlayNetworking.registerGlobalReceiver(
                OpenDifficultySelectionPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreenAndShow(new DifficultySelectionScreen())
                )
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientTreeCatalog.clear();
            ClientIslandSelectionDraft.clear();
        });
    }
}
