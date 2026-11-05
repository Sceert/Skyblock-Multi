package cl.treecs.skyblockmulti.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IslandPanelRequestPayload(long revision, String action, long session, long request) implements CustomPacketPayload {
    public IslandPanelRequestPayload(long revision, String action) { this(revision, action, 0, 0); }
    public static final Type<IslandPanelRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("skyblockmulti", "island_panel_request_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IslandPanelRequestPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeLong(value.revision()); buf.writeUtf(value.action(), 64); buf.writeLong(value.session()); buf.writeLong(value.request()); },
            buf -> new IslandPanelRequestPayload(buf.readLong(), buf.readUtf(64), buf.readLong(), buf.readLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
