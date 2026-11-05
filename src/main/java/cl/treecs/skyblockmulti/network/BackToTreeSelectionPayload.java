package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BackToTreeSelectionPayload() implements CustomPacketPayload {
    public static final BackToTreeSelectionPayload INSTANCE = new BackToTreeSelectionPayload();
    public static final Type<BackToTreeSelectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "back_to_tree_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BackToTreeSelectionPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
