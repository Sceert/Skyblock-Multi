package cl.treecs.skyblockmulti;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class PlayerReconnectHandler implements ModInitializer {
    private static final String MENU_SHOWN_TAG = "skyblock_menu_shown_v1";

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handler.player.removeTag(MENU_SHOWN_TAG)
        );
    }
}
