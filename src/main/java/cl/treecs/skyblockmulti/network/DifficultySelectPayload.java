package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DifficultySelectPayload(int value) implements CustomPacketPayload {
    public static final Type<DifficultySelectPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "difficulty_select")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DifficultySelectPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.value),
            buffer -> new DifficultySelectPayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
