package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Notifies the server that an unfinished island selection menu was closed voluntarily. */
public record SelectionMenuClosedPayload() implements CustomPacketPayload {
    public static final SelectionMenuClosedPayload INSTANCE = new SelectionMenuClosedPayload();
    public static final Type<SelectionMenuClosedPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "selection_menu_closed")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectionMenuClosedPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
