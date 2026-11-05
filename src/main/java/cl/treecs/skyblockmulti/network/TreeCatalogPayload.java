package cl.treecs.skyblockmulti.network;

import cl.treecs.skyblockmulti.SkyblockMultiMod;
import cl.treecs.skyblockmulti.tree.DataTreeDefinition;
import cl.treecs.skyblockmulti.tree.TreeCatalog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record TreeCatalogPayload(List<Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_CATEGORIES = 32;
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_TEXT_LENGTH = 512;

    public static final Type<TreeCatalogPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, "tree_catalog")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TreeCatalogPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> payload.write(buffer),
            TreeCatalogPayload::read
    );

    public TreeCatalogPayload {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("El catálogo supera " + MAX_ENTRIES + " árboles");
        }
    }

    public static TreeCatalogPayload create(Map<String, Boolean> configuredStates) {
        List<Entry> entries = TreeCatalog.activeDefinitions().values().stream()
                .sorted(Comparator.comparing(DataTreeDefinition::id))
                .map(definition -> Entry.create(
                        definition,
                        Boolean.TRUE.equals(configuredStates.get(definition.id()))
                ))
                .toList();
        return new TreeCatalogPayload(entries);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) entry.write(buffer);
    }

    private static TreeCatalogPayload read(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Tamaño de catálogo inválido");
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) entries.add(Entry.read(buffer));
        return new TreeCatalogPayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
            String id,
            String nameTranslationKey,
            String literalName,
            String descriptionTranslationKey,
            String literalDescription,
            String iconItem,
            List<String> categories,
            String sourceModId,
            String sourceModName,
            boolean enabled,
            boolean selectable
    ) {
        public Entry {
            categories = List.copyOf(categories);
            if (categories.size() > MAX_CATEGORIES) {
                throw new IllegalArgumentException("Demasiadas categorías para " + id);
            }
        }

        public static Entry create(DataTreeDefinition definition, boolean enabled) {
            DataTreeDefinition.TextValue description = definition.description();
            String sourceModId = definition.requiredMods().isEmpty()
                    ? "minecraft" : definition.requiredMods().getFirst();
            String sourceModName = FabricLoader.getInstance().getModContainer(sourceModId)
                    .map(container -> container.getMetadata().getName())
                    .orElse(sourceModId);
            return new Entry(
                    definition.id(),
                    definition.name().translationKey(),
                    definition.name().literal(),
                    description == null ? null : description.translationKey(),
                    description == null ? null : description.literal(),
                    definition.iconItem(),
                    definition.categories(),
                    sourceModId,
                    sourceModName,
                    enabled,
                    true
            );
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(id, MAX_ID_LENGTH);
            writeNullable(buffer, nameTranslationKey);
            writeNullable(buffer, literalName);
            writeNullable(buffer, descriptionTranslationKey);
            writeNullable(buffer, literalDescription);
            buffer.writeUtf(iconItem, MAX_ID_LENGTH);
            buffer.writeVarInt(categories.size());
            for (String category : categories) buffer.writeUtf(category, MAX_ID_LENGTH);
            buffer.writeUtf(sourceModId, MAX_ID_LENGTH);
            buffer.writeUtf(sourceModName, MAX_TEXT_LENGTH);
            buffer.writeBoolean(enabled);
            buffer.writeBoolean(selectable);
        }

        private static Entry read(RegistryFriendlyByteBuf buffer) {
            String id = buffer.readUtf(MAX_ID_LENGTH);
            String nameTranslationKey = readNullable(buffer);
            String literalName = readNullable(buffer);
            String descriptionTranslationKey = readNullable(buffer);
            String literalDescription = readNullable(buffer);
            String iconItem = buffer.readUtf(MAX_ID_LENGTH);
            int categoryCount = buffer.readVarInt();
            if (categoryCount < 0 || categoryCount > MAX_CATEGORIES) {
                throw new IllegalArgumentException("Cantidad de categorías inválida");
            }
            List<String> categories = new ArrayList<>(categoryCount);
            for (int i = 0; i < categoryCount; i++) categories.add(buffer.readUtf(MAX_ID_LENGTH));
            String sourceModId = buffer.readUtf(MAX_ID_LENGTH);
            String sourceModName = buffer.readUtf(MAX_TEXT_LENGTH);
            return new Entry(
                    id, nameTranslationKey, literalName, descriptionTranslationKey,
                    literalDescription, iconItem, categories, sourceModId, sourceModName,
                    buffer.readBoolean(), buffer.readBoolean()
            );
        }

        private static void writeNullable(RegistryFriendlyByteBuf buffer, String value) {
            buffer.writeBoolean(value != null);
            if (value != null) buffer.writeUtf(value, MAX_TEXT_LENGTH);
        }

        private static String readNullable(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean() ? buffer.readUtf(MAX_TEXT_LENGTH) : null;
        }
    }
}
