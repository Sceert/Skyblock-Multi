package cl.treecs.skyblockmulti;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class VersionJoinMessage implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String version = FabricLoader.getInstance()
                    .getModContainer(SkyblockMultiMod.MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");

            handler.player.sendSystemMessage(
                    Component.literal("[Skyblock Multi] ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(
                                    Component.literal("Skyblock Multi " + version)
                                            .withStyle(ChatFormatting.GRAY)
                            )
            );
        });
    }
}
