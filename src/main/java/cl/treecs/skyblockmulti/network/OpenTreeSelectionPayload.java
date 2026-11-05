package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenTreeSelectionPayload() implements CustomPacketPayload {
    public static final OpenTreeSelectionPayload INSTANCE = new OpenTreeSelectionPayload();
    public static final Type<OpenTreeSelectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "open_tree_selection")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTreeSelectionPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
