package cl.treecs.skyblockmulti.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

/** Read-only, localized server snapshot; buttons carry opaque action IDs, never commands. */
public record IslandPanelPayload(long revision, boolean open, List<Entry> entries, long session, long request, boolean accepted) implements CustomPacketPayload {
    public IslandPanelPayload(long revision, boolean open, List<Entry> entries) { this(revision, open, entries, 0, 0, true); }
    public static final Type<IslandPanelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("skyblockmulti", "island_panel_v3"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IslandPanelPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeLong(value.revision());
                buf.writeBoolean(value.open());
                buf.writeLong(value.session()); buf.writeLong(value.request()); buf.writeBoolean(value.accepted());
                buf.writeVarInt(value.entries().size());
                for (Entry entry : value.entries()) {
                    buf.writeVarInt(entry.tab());
                    buf.writeUtf(entry.context(), 512);
                    ComponentSerialization.STREAM_CODEC.encode(buf, entry.body());
                    buf.writeVarInt(entry.rows().size());
                    for (Row row : entry.rows()) {
                        ComponentSerialization.STREAM_CODEC.encode(buf, row.label());
                        ComponentSerialization.STREAM_CODEC.encode(buf, row.value());
                    }
                    buf.writeVarInt(entry.actions().size());
                    for (Action action : entry.actions()) {
                        buf.writeUtf(action.id(), 64);
                        ComponentSerialization.STREAM_CODEC.encode(buf, action.label());
                    }
                }
            }, buf -> {
                long revision = buf.readLong();
                boolean open = buf.readBoolean();
                long session = buf.readLong(), request = buf.readLong();
                boolean accepted = buf.readBoolean();
                int count = bounded(buf.readVarInt(), 16);
                List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int tab = bounded(buf.readVarInt(), 2);
                    String context = buf.readUtf(512);
                    Component body = ComponentSerialization.STREAM_CODEC.decode(buf);
                    int rowCount = bounded(buf.readVarInt(), 16);
                    List<Row> rows = new ArrayList<>();
                    for (int j = 0; j < rowCount; j++) rows.add(new Row(ComponentSerialization.STREAM_CODEC.decode(buf), ComponentSerialization.STREAM_CODEC.decode(buf)));
                    int actions = bounded(buf.readVarInt(), 2);
                    List<Action> buttons = new ArrayList<>();
                    for (int j = 0; j < actions; j++) buttons.add(new Action(buf.readUtf(64), ComponentSerialization.STREAM_CODEC.decode(buf)));
                    entries.add(new Entry(tab, context, body, buttons, rows));
                }
                return new IslandPanelPayload(revision, open, entries, session, request, accepted);
            });

    public IslandPanelPayload { entries = List.copyOf(entries); bounded(entries.size(), 16); }
    public boolean permits(long receivedRevision, List<Entry> current, String action) {
        return revision == receivedRevision && entries.equals(current)
                && current.stream().flatMap(entry -> entry.actions().stream()).anyMatch(button -> button.id().equals(action));
    }
    private static int bounded(int value, int max) {
        if (value < 0 || value > max) throw new IllegalArgumentException("Invalid island panel size");
        return value;
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public record Action(String id, Component label) {}
    public record Row(Component label, Component value) {}
    public record Entry(int tab, String context, Component body, List<Action> actions, List<Row> rows) {
        public Entry(int tab, String context, Component body, List<Action> actions) { this(tab, context, body, actions, List.of()); }
        public Entry { actions = List.copyOf(actions); rows = List.copyOf(rows); bounded(tab, 2); bounded(actions.size(), 2); bounded(rows.size(), 16); }
    }
}
