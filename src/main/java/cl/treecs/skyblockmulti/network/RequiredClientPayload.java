package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequiredClientPayload() implements CustomPacketPayload {
    public static final RequiredClientPayload INSTANCE = new RequiredClientPayload();
    public static final Type<RequiredClientPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "required_client")
    );
    public static final StreamCodec<FriendlyByteBuf, RequiredClientPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
