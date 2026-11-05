package cl.treecs.skyblockmulti.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TreeDefinitionReloadListener
        extends SimpleReloadListener<Map<String, DataTreeDefinition>> {
    public static final String DIRECTORY = "skyblockmulti/tree_options";

    @Override
    protected Map<String, DataTreeDefinition> prepare(PreparableReloadListener.SharedState state) {
        Map<String, DataTreeDefinition> definitions = new LinkedHashMap<>();
        Map<Identifier, Resource> resources = state.resourceManager().listResources(
                DIRECTORY,
                id -> id.getPath().endsWith(".json")
        );

        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> load(entry.getKey(), entry.getValue(), definitions));
        return Map.copyOf(definitions);
    }

    @Override
    protected void apply(Map<String, DataTreeDefinition> definitions,
                         PreparableReloadListener.SharedState state) {
        TreeCatalog.replaceDataPackDefinitions(definitions);
        TreeConfigStorage.synchronizeWithCatalog();
        System.out.println("[Skyblock Multi] Definiciones externas de árboles cargadas: "
                + definitions.size());
    }

    private static void load(Identifier resourceId, Resource resource,
                             Map<String, DataTreeDefinition> definitions) {
        String definitionId = definitionId(resourceId);
        try (Reader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            DataTreeDefinition definition = parse(definitionId, json);
            if (!requirementsPresent(definition)) {
                System.out.println("[Skyblock Multi] Árbol omitido por requisitos ausentes: " + definitionId);
                return;
            }
            if (!referencedItemsPresent(definition)) {
                System.err.println("[Skyblock Multi] Árbol omitido por ítems ausentes: " + definitionId);
                return;
            }
            definitions.put(definitionId, definition);
        } catch (Exception exception) {
            System.err.println("[Skyblock Multi] Definición de árbol inválida " + definitionId
                    + " (" + resource.sourcePackId() + "): " + exception.getMessage());
        }
    }

    public static DataTreeDefinition parse(String id, JsonObject json) {
        int schemaVersion = requiredInt(json, "schema_version");
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("schema_version no soportada: " + schemaVersion);
        }
        if (json.has("random_weight")) {
            throw new IllegalArgumentException(
                    "random_weight no está soportado; todos los árboles tienen la misma probabilidad"
            );
        }

        JsonObject display = requiredObject(json, "display");
        DataTreeDefinition.TextValue name = parseText(requiredObject(display, "name"));
        DataTreeDefinition.TextValue description = display.has("description")
                ? parseText(requiredObject(display, "description"))
                : null;
        String iconItem = requiredString(requiredObject(display, "icon"), "item");
        TreeDefinition.requireNamespacedId(iconItem);

        DataTreeDefinition.Placement placement = parsePlacement(requiredObject(json, "placement"));
        DataTreeDefinition.PlantingStock plantingStock = json.has("planting_stock")
                ? parsePlantingStock(requiredObject(json, "planting_stock"))
                : null;
        List<String> categories = parseIds(json.getAsJsonArray("categories"));
        boolean enabledByDefault = !json.has("enabled_by_default")
                || json.get("enabled_by_default").getAsBoolean();
        List<String> requiredMods = json.has("requirements")
                ? parseStrings(requiredObject(json, "requirements").getAsJsonArray("mods"))
                : List.of();

        return new DataTreeDefinition(
                id, name, description, iconItem, placement, plantingStock, categories, enabledByDefault, requiredMods
        );
    }

    private static DataTreeDefinition.PlantingStock parsePlantingStock(JsonObject json) {
        return new DataTreeDefinition.PlantingStock(
                requiredString(json, "item"), requiredInt(json, "count"));
    }

    private static DataTreeDefinition.Placement parsePlacement(JsonObject json) {
        String type = requiredString(json, "type");
        return switch (type) {
            case "skyblockmulti:sapling" -> new DataTreeDefinition.SaplingPlacement(
                    requiredString(json, "item"),
                    switch (json.has("pattern") ? json.get("pattern").getAsString() : "single") {
                        case "single" -> DataTreeDefinition.SaplingPlacement.Pattern.SINGLE;
                        case "two_by_two" -> DataTreeDefinition.SaplingPlacement.Pattern.TWO_BY_TWO;
                        default -> throw new IllegalArgumentException("Patrón de sapling desconocido");
                    }
            );
            case "skyblockmulti:azalea" -> new DataTreeDefinition.AzaleaPlacement(
                    json.has("flowering") && json.get("flowering").getAsBoolean()
            );
            case "skyblockmulti:structure" -> {
                int[] offset = json.has("offset") ? parseOffset(json.getAsJsonArray("offset")) : new int[]{0, 0, 0};
                String rotation = json.has("rotation") ? json.get("rotation").getAsString() : "none";
                if (!rotation.equals("none") && !rotation.equals("random")) {
                    throw new IllegalArgumentException("rotation debe ser 'none' o 'random'");
                }
                yield new DataTreeDefinition.StructurePlacement(
                        requiredString(json, "structure"), offset[0], offset[1], offset[2],
                        rotation.equals("random")
                );
            }
            default -> throw new IllegalArgumentException("Tipo de placement desconocido: " + type);
        };
    }

    private static String definitionId(Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = DIRECTORY + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("Ruta de definición inválida: " + resourceId);
        }
        return resourceId.getNamespace() + ":"
                + path.substring(prefix.length(), path.length() - ".json".length());
    }

    private static boolean requirementsPresent(DataTreeDefinition definition) {
        for (String modId : definition.requiredMods()) {
            if (!FabricLoader.getInstance().isModLoaded(modId)) return false;
        }
        return true;
    }

    private static boolean referencedItemsPresent(DataTreeDefinition definition) {
        if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(definition.iconItem()))) return false;
        if (definition.plantingStock() != null
                && !BuiltInRegistries.ITEM.containsKey(Identifier.parse(definition.plantingStock().item()))) {
            return false;
        }
        if (definition.placement() instanceof DataTreeDefinition.SaplingPlacement sapling) {
            return BuiltInRegistries.ITEM.getOptional(Identifier.parse(sapling.item()))
                    .filter(BlockItem.class::isInstance)
                    .isPresent();
        }
        return true;
    }

    private static DataTreeDefinition.TextValue parseText(JsonObject json) {
        String translate = optionalString(json, "translate");
        String text = optionalString(json, "text");
        return new DataTreeDefinition.TextValue(translate, text);
    }

    private static List<String> parseIds(JsonArray array) {
        if (array == null) return List.of();
        List<String> result = parseStrings(array);
        result.forEach(TreeDefinition::requireNamespacedId);
        return result;
    }

    private static List<String> parseStrings(JsonArray array) {
        if (array == null) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) result.add(element.getAsString());
        return List.copyOf(result);
    }

    private static int[] parseOffset(JsonArray array) {
        if (array == null || array.size() != 3) {
            throw new IllegalArgumentException("offset debe contener exactamente 3 enteros");
        }
        return new int[]{array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()};
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalArgumentException("Falta el objeto '" + key + "'");
        }
        return parent.getAsJsonObject(key);
    }

    private static String requiredString(JsonObject parent, String key) {
        String value = optionalString(parent, key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta '" + key + "'");
        return value;
    }

    private static String optionalString(JsonObject parent, String key) {
        return parent.has(key) && !parent.get(key).isJsonNull() ? parent.get(key).getAsString() : null;
    }

    private static int requiredInt(JsonObject parent, String key) {
        if (!parent.has(key)) throw new IllegalArgumentException("Falta '" + key + "'");
        return parent.get(key).getAsInt();
    }
}
