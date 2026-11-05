package cl.treecs.skyblockmulti.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;

import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Discovers addon capabilities from each installed mod's fabric.mod.json. */
public final class AddonFeatureRegistry {
    private static volatile List<AddonFeatureDefinition> cached;

    private AddonFeatureRegistry() {
    }

    public static List<AddonFeatureDefinition> features() {
        List<AddonFeatureDefinition> result = cached;
        if (result == null) {
            result = discover();
            cached = result;
        }
        return result;
    }

    public static void invalidate() {
        cached = null;
    }

    public static boolean shouldDisableRecipe(Identifier recipeId) {
        for (AddonFeatureDefinition feature : features()) {
            if (feature.type() != AddonFeatureDefinition.Type.RECIPES
                    || AddonFeatureConfig.isEnabled(feature)) continue;
            if (!feature.recipeNamespaces().contains(recipeId.getNamespace())) continue;
            for (String prefix : feature.recipePathPrefixes()) {
                if (recipeId.getPath().startsWith(prefix)) return true;
            }
        }
        return false;
    }

    /** Snapshot configuration once per reload, rather than reading disk for every recipe. */
    public static java.util.function.Predicate<Identifier> disabledRecipePredicate() {
        Map<String, Boolean> states = AddonFeatureConfig.configuredStates();
        var disabled = features().stream()
                .filter(feature -> feature.type() == AddonFeatureDefinition.Type.RECIPES
                        && !states.getOrDefault(feature.id(), feature.enabledByDefault()))
                .toList();
        return id -> disabled.stream().anyMatch(feature ->
                feature.recipeNamespaces().contains(id.getNamespace())
                        && feature.recipePathPrefixes().stream().anyMatch(id.getPath()::startsWith));
    }

    /** Public runtime hook for Java addons contributing trades, loot or other systems. */
    public static boolean isEnabled(String featureId) {
        return features().stream()
                .filter(feature -> feature.id().equals(featureId))
                .findFirst()
                .map(AddonFeatureConfig::isEnabled)
                .orElse(false);
    }

    private static List<AddonFeatureDefinition> discover() {
        Map<String, AddonFeatureDefinition> definitions = new LinkedHashMap<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            mod.findPath("fabric.mod.json").ifPresent(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject custom = object(root, "custom");
                    JsonObject integration = custom == null ? null : object(custom, "skyblockmulti");
                    JsonArray features = integration == null ? null : array(integration, "addon_features");
                    if (features == null) return;
                    String modId = string(root, "id", mod.getMetadata().getId());
                    String modName = string(root, "name", mod.getMetadata().getName());
                    for (JsonElement element : features) {
                        if (!element.isJsonObject()) continue;
                        AddonFeatureDefinition definition = parse(modId, modName, element.getAsJsonObject());
                        if (definition != null) definitions.putIfAbsent(definition.id(), definition);
                    }
                } catch (Exception error) {
                    System.err.println("[Skyblock Multi] Descriptor de addon inválido en "
                            + mod.getMetadata().getId() + ": " + error.getMessage());
                }
            });
        }
        return definitions.values().stream()
                .sorted(Comparator.comparing(AddonFeatureDefinition::addonName)
                        .thenComparing(AddonFeatureDefinition::id))
                .toList();
    }

    private static AddonFeatureDefinition parse(String modId, String modName, JsonObject json) {
        String localId = string(json, "id", "").trim().toLowerCase(java.util.Locale.ROOT);
        if (!localId.matches("[a-z0-9_.-]+")) return null;
        String typeName = string(json, "type", "");
        AddonFeatureDefinition.Type type = switch (typeName) {
            case "recipes" -> AddonFeatureDefinition.Type.RECIPES;
            case "starter_chest" -> AddonFeatureDefinition.Type.STARTER_CHEST;
            case "wandering_trader" -> AddonFeatureDefinition.Type.WANDERING_TRADER;
            case "challenge_loot" -> AddonFeatureDefinition.Type.CHALLENGE_LOOT;
            case "other" -> AddonFeatureDefinition.Type.OTHER;
            default -> null;
        };
        if (type == null) return null;
        List<String> namespaces = strings(json, "recipe_namespaces");
        List<String> prefixes = strings(json, "recipe_path_prefixes");
        if (type == AddonFeatureDefinition.Type.RECIPES
                && (namespaces.isEmpty() || prefixes.isEmpty())) return null;
        return new AddonFeatureDefinition(
                modId + ":" + localId,
                modId,
                modName,
                nullableString(json, "name_translation_key"),
                nullableString(json, "name"),
                type,
                namespaces,
                prefixes,
                bool(json, "enabled_by_default", true),
                bool(json, "reload_required", true)
        );
    }

    private static JsonObject object(JsonObject owner, String key) {
        JsonElement value = owner.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject owner, String key) {
        JsonElement value = owner.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static String string(JsonObject owner, String key, String fallback) {
        JsonElement value = owner.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    private static String nullableString(JsonObject owner, String key) {
        String value = string(owner, key, "").trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean bool(JsonObject owner, String key, boolean fallback) {
        JsonElement value = owner.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    private static List<String> strings(JsonObject owner, String key) {
        JsonArray values = array(owner, key);
        if (values == null) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement value : values) {
            if (value.isJsonPrimitive()) {
                String text = value.getAsString().trim();
                if (!text.isEmpty()) result.add(text);
            }
        }
        return List.copyOf(result);
    }
}
