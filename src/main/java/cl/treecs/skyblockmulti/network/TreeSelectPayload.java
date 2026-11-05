package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TreeSelectPayload(String treeId) implements CustomPacketPayload {
    public static final String RANDOM_SELECTION = "skyblockmulti:random";
    private static final int MAX_ID_LENGTH = 256;

    public static final Type<TreeSelectPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "tree_select")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TreeSelectPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.treeId, MAX_ID_LENGTH),
            buffer -> new TreeSelectPayload(buffer.readUtf(MAX_ID_LENGTH))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
