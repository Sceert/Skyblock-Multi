package cl.treecs.skyblockmulti.infernal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carga y valida arenas y encuentros aportados por datapacks. */
public final class InfernalDefinitionReloadListener
        extends SimpleReloadListener<InfernalDataRegistry.Snapshot> {
    static final String ARENAS = "skyblockmulti/infernal_arenas";
    static final String TRIALS = "skyblockmulti/infernal_trials";
    static final int MAX_PHASES = 16;
    static final int MAX_MOBS_PER_ENTRY = 64;
    static final int MAX_BASE_MOBS_PER_TRIAL = 128;

    @Override
    protected InfernalDataRegistry.Snapshot prepare(PreparableReloadListener.SharedState state) {
        Map<String, InfernalDefinitions.Arena> arenas = new LinkedHashMap<>();
        resources(state, ARENAS).forEach((id, resource) -> loadArena(id, resource, arenas));

        Map<String, InfernalDefinitions.Trial> trials = new LinkedHashMap<>();
        resources(state, TRIALS).forEach((id, resource) -> loadTrial(id, resource, arenas, trials));
        return new InfernalDataRegistry.Snapshot(arenas, trials);
    }

    @Override
    protected void apply(InfernalDataRegistry.Snapshot snapshot,
                         PreparableReloadListener.SharedState state) {
        InfernalDataRegistry.replace(snapshot);
        System.out.println("[Skyblock Multi] API infernal: " + snapshot.arenas().size()
                + " arenas y " + snapshot.trials().size() + " pruebas validadas.");
    }

    private static Map<Identifier, Resource> resources(PreparableReloadListener.SharedState state,
                                                        String directory) {
        return state.resourceManager().listResources(directory, id -> id.getPath().endsWith(".json"));
    }

    private static void loadArena(Identifier resourceId, Resource resource,
                                  Map<String, InfernalDefinitions.Arena> output) {
        String id = definitionId(resourceId, ARENAS);
        try (Reader reader = resource.openAsReader()) {
            output.put(id, parseArena(id, JsonParser.parseReader(reader).getAsJsonObject()));
        } catch (Exception exception) {
            invalid("arena", id, resource, exception);
        }
    }

    private static void loadTrial(Identifier resourceId, Resource resource,
                                  Map<String, InfernalDefinitions.Arena> arenas,
                                  Map<String, InfernalDefinitions.Trial> output) {
        String id = definitionId(resourceId, TRIALS);
        try (Reader reader = resource.openAsReader()) {
            output.put(id, parseTrial(id, JsonParser.parseReader(reader).getAsJsonObject(), arenas));
        } catch (Exception exception) {
            invalid("prueba", id, resource, exception);
        }
    }

    static InfernalDefinitions.Arena parseArena(String id, JsonObject json) {
        schema(json);
        String dimension = namespaced(requiredString(json, "dimension"));
        InfernalDefinitions.Point origin = point(requiredArray(json, "origin"));
        int radius = boundedInt(json, "cleanup_radius", 1, 256);
        JsonObject floorJson = requiredObject(json, "floors");
        Map<String, List<InfernalDefinitions.Point>> floors = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : floorJson.entrySet()) {
            if (!entry.getValue().isJsonArray()) fail("El piso '" + entry.getKey() + "' debe ser una lista");
            List<InfernalDefinitions.Point> points = new ArrayList<>();
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                InfernalDefinitions.Point point = point(element.getAsJsonArray());
                if (Math.abs(point.x()) > radius || Math.abs(point.z()) > radius) {
                    fail("Punto fuera del radio en piso '" + entry.getKey() + "'");
                }
                points.add(point);
            }
            if (points.isEmpty()) fail("El piso '" + entry.getKey() + "' está vacío");
            floors.put(entry.getKey(), List.copyOf(points));
        }
        if (floors.isEmpty()) fail("La arena necesita al menos un piso");
        return new InfernalDefinitions.Arena(id, dimension, origin, radius, floors,
                optionalId(json, "baseline_structure"), optionalId(json, "restore_function"));
    }

    static InfernalDefinitions.Trial parseTrial(String id, JsonObject json,
                                                Map<String, InfernalDefinitions.Arena> arenas) {
        schema(json);
        String arenaId = namespaced(requiredString(json, "arena"));
        InfernalDefinitions.Arena arena = arenas.get(arenaId);
        if (arena == null) fail("Arena desconocida: " + arenaId);
        List<InfernalDefinitions.Activation> activations = parseActivations(requiredArray(json, "activations"));
        JsonArray phasesJson = requiredArray(json, "phases");
        if (phasesJson.isEmpty() || phasesJson.size() > MAX_PHASES) fail("Cantidad de etapas inválida");
        List<InfernalDefinitions.Phase> phases = new ArrayList<>();
        int totalMobs = 0;
        for (int index = 0; index < phasesJson.size(); index++) {
            InfernalDefinitions.Phase phase = parsePhase(phasesJson.get(index).getAsJsonObject(), index + 1, arena);
            phases.add(phase);
            totalMobs += phase.mobs().stream().mapToInt(InfernalDefinitions.Mob::count).sum();
        }
        if (totalMobs > MAX_BASE_MOBS_PER_TRIAL) fail("La prueba supera 128 mobs base");
        return new InfernalDefinitions.Trial(id, arenaId, jsonText(json.get("display")), activations,
                optionalBoundedInt(json, "countdown_seconds", 10, 0, 60),
                optionalBoundedInt(json, "cooldown_seconds", 60, 0, 3600),
                optionalBoolean(json, "cleanup_mobs", true),
                optionalBoolean(json, "cleanup_items", true),
                optionalBoundedInt(json, "spawn_release_seconds", 30, 0, 300), phases,
                parseScaling(json.getAsJsonObject("scaling")), parseRewards(json.getAsJsonObject("rewards")));
    }

    private static List<InfernalDefinitions.Activation> parseActivations(JsonArray array) {
        if (array.isEmpty()) fail("La prueba necesita al menos una activación");
        List<InfernalDefinitions.Activation> result = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String item = namespaced(requiredString(json, "item"));
            if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(item))) fail("Ítem desconocido: " + item);
            result.add(new InfernalDefinitions.Activation(item, jsonText(json.get("custom_data")),
                    !json.has("consume") || json.get("consume").getAsBoolean()));
        }
        return List.copyOf(result);
    }

    private static InfernalDefinitions.Phase parsePhase(JsonObject json, int number,
                                                        InfernalDefinitions.Arena arena) {
        String phaseId = json.has("id") ? json.get("id").getAsString() : "phase_" + number;
        JsonArray mobsJson = requiredArray(json, "mobs");
        if (mobsJson.isEmpty()) fail("La etapa " + phaseId + " no contiene mobs");
        List<InfernalDefinitions.Mob> mobs = new ArrayList<>();
        for (JsonElement element : mobsJson) mobs.add(parseMob(element.getAsJsonObject(), arena));
        return new InfernalDefinitions.Phase(phaseId, jsonText(json.get("display")),
                optionalBoundedInt(json, "delay_after_previous_seconds", 8, 0, 300),
                optionalId(json, "arena_setup_function"), optionalId(json, "arena_restore_function"), mobs);
    }

    private static InfernalDefinitions.Mob parseMob(JsonObject json, InfernalDefinitions.Arena arena) {
        String entity = namespaced(requiredString(json, "entity"));
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(entity))) fail("Mob desconocido: " + entity);
        int count = boundedInt(json, "count", 1, MAX_MOBS_PER_ENTRY);
        List<String> floors = strings(json.get("floor"));
        if (floors.isEmpty()) fail("Un mob necesita 'floor'");
        for (String floor : floors) if (!arena.floors().containsKey(floor)) fail("Piso desconocido: " + floor);
        Map<String, InfernalDefinitions.Equipment> equipment = parseEquipment(json.getAsJsonObject("equipment"));
        List<InfernalDefinitions.Effect> effects = parseEffects(json.getAsJsonArray("effects"));
        return new InfernalDefinitions.Mob(entity, count, floors,
                optionalEnum(json, "placement", "random", List.of("fixed", "random", "spread", "round_robin")),
                optionalBoolean(json, "champion", false), optionalDouble(json, "scale", 1, 0.5, 2),
                optionalDouble(json, "health_multiplier", 1, 0.1, 10),
                optionalDouble(json, "damage_multiplier", 1, 0.1, 10),
                optionalDouble(json, "knockback_resistance", 0, 0, 1), jsonText(json.get("name")), equipment,
                effects, optionalString(json, "alliance"),
                optionalEnum(json, "drop_policy", "none", List.of("vanilla", "none", "trial_only")),
                optionalBoolean(json, "counts_for_victory", true),
                optionalBoolean(json, "children_count_for_victory", true),
                optionalBoolean(json, "persistent", true),
                optionalBoolean(json, "player_targeting_only", true),
                optionalBoundedInt(json, "spawn_delay_ticks", 0, 0, 1200),
                optionalDouble(json, "chance", 1, 0, 1));
    }

    private static Map<String, InfernalDefinitions.Equipment> parseEquipment(JsonObject json) {
        if (json == null) return Map.of();
        Map<String, InfernalDefinitions.Equipment> result = new LinkedHashMap<>();
        List<String> slots = List.of("mainhand", "offhand", "head", "chest", "legs", "feet");
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!slots.contains(entry.getKey())) fail("Slot de equipo desconocido: " + entry.getKey());
            JsonObject value = entry.getValue().getAsJsonObject();
            String item = namespaced(requiredString(value, "item"));
            if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(item))) fail("Equipo desconocido: " + item);
            result.put(entry.getKey(), new InfernalDefinitions.Equipment(item,
                    optionalBoundedInt(value, "count", 1, 1, 64),
                    optionalDouble(value, "drop_chance", 0, 0, 1), jsonText(value.get("components"))));
        }
        return result;
    }

    private static List<InfernalDefinitions.Effect> parseEffects(JsonArray array) {
        if (array == null) return List.of();
        List<InfernalDefinitions.Effect> result = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            String id = namespaced(requiredString(json, "id"));
            if (!BuiltInRegistries.MOB_EFFECT.containsKey(Identifier.parse(id))) fail("Efecto desconocido: " + id);
            result.add(new InfernalDefinitions.Effect(id,
                    optionalBoundedInt(json, "amplifier", 0, 0, 255),
                    optionalBoundedInt(json, "duration_ticks", 600, 1, 72000),
                    optionalBoolean(json, "particles", false), optionalBoolean(json, "icon", true)));
        }
        return List.copyOf(result);
    }

    private static InfernalDefinitions.Scaling parseScaling(JsonObject json) {
        if (json == null) return new InfernalDefinitions.Scaling(8, 0, 0.10, Map.of(), 2);
        double maxCount = optionalDouble(json, "maximum_count_multiplier", 2, 1, 2);
        Map<String, InfernalDefinitions.DifficultyScaling> difficulties = new LinkedHashMap<>();
        JsonObject difficultyJson = json.getAsJsonObject("island_difficulty");
        if (difficultyJson != null) for (Map.Entry<String, JsonElement> entry : difficultyJson.entrySet()) {
            if (!List.of("easy", "standard", "hard", "extreme").contains(entry.getKey())) {
                fail("Dificultad desconocida: " + entry.getKey());
            }
            JsonObject value = entry.getValue().getAsJsonObject();
            difficulties.put(entry.getKey(), new InfernalDefinitions.DifficultyScaling(
                    optionalDouble(value, "count_multiplier", 1, 0.25, maxCount),
                    optionalDouble(value, "health_multiplier", 1, 0.25, 5),
                    optionalDouble(value, "damage_multiplier", 1, 0.25, 5)));
        }
        JsonObject participants = json.getAsJsonObject("participants");
        return new InfernalDefinitions.Scaling(
                participants == null ? 8 : optionalBoundedInt(participants, "maximum_participants_counted", 8, 1, 32),
                participants == null ? 0 : optionalBoundedInt(participants, "extra_mobs_per_player", 0, 0, 16),
                participants == null ? 0.10 : optionalDouble(participants, "health_per_extra_player", 0.10, 0, 2),
                difficulties, maxCount);
    }

    private static InfernalDefinitions.Rewards parseRewards(JsonObject json) {
        if (json == null) return new InfernalDefinitions.Rewards(null, true, null);
        return new InfernalDefinitions.Rewards(optionalId(json, "completion"),
                optionalBoolean(json, "per_participant", true), optionalId(json, "phase_bonus"));
    }

    private static List<String> strings(JsonElement element) {
        if (element == null || element.isJsonNull()) return List.of();
        if (element.isJsonPrimitive()) return List.of(element.getAsString());
        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) values.add(value.getAsString());
        return List.copyOf(values);
    }

    private static InfernalDefinitions.Point point(JsonArray array) {
        if (array.size() != 3) fail("Una coordenada necesita exactamente tres enteros");
        return new InfernalDefinitions.Point(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    private static void schema(JsonObject json) {
        if (!json.has("schema_version") || json.get("schema_version").getAsInt() != 1) {
            fail("schema_version debe ser 1");
        }
    }

    private static String definitionId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        return resourceId.getNamespace() + ":" + path.substring(directory.length() + 1, path.length() - 5);
    }

    private static String namespaced(String value) {
        Identifier.parse(value);
        if (!value.contains(":")) fail("El ID debe incluir namespace: " + value);
        return value;
    }

    private static String optionalId(JsonObject json, String key) {
        String value = optionalString(json, key);
        return value == null ? null : namespaced(value);
    }

    private static JsonObject requiredObject(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) fail("Falta el objeto '" + key + "'");
        return json.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) fail("Falta la lista '" + key + "'");
        return json.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject json, String key) {
        String value = optionalString(json, key);
        if (value == null || value.isBlank()) fail("Falta '" + key + "'");
        return value;
    }

    private static String optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    private static boolean optionalBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int boundedInt(JsonObject json, String key, int minimum, int maximum) {
        if (!json.has(key)) fail("Falta '" + key + "'");
        return bounded(json.get(key).getAsInt(), key, minimum, maximum);
    }

    private static int optionalBoundedInt(JsonObject json, String key, int fallback, int minimum, int maximum) {
        return json.has(key) ? bounded(json.get(key).getAsInt(), key, minimum, maximum) : fallback;
    }

    private static int bounded(int value, String key, int minimum, int maximum) {
        if (value < minimum || value > maximum) fail(key + " debe estar entre " + minimum + " y " + maximum);
        return value;
    }

    private static double optionalDouble(JsonObject json, String key, double fallback,
                                         double minimum, double maximum) {
        double value = json.has(key) ? json.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            fail(key + " debe estar entre " + minimum + " y " + maximum);
        }
        return value;
    }

    private static String optionalEnum(JsonObject json, String key, String fallback, List<String> allowed) {
        String value = json.has(key) ? json.get(key).getAsString() : fallback;
        if (!allowed.contains(value)) fail("Valor desconocido para " + key + ": " + value);
        return value;
    }

    private static String jsonText(JsonElement element) {
        return element == null || element.isJsonNull() ? null : element.toString();
    }

    private static void invalid(String type, String id, Resource resource, Exception exception) {
        System.err.println("[Skyblock Multi] " + type + " infernal inválida " + id + " ("
                + resource.sourcePackId() + "): " + exception.getMessage());
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message);
    }
}
