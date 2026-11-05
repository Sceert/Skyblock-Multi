package cl.treecs.skyblockmulti.addon;

import cl.treecs.skyblockmulti.ConfigPaths;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persistent global defaults for optional addon capabilities. */
public final class AddonFeatureConfig {
    private static final Pattern TOML_FEATURE = Pattern.compile(
            "(?m)^\\s*\\\"([^\\\"]+)\\\"\\s*=\\s*(true|false)\\s*(?:#.*)?$",
            Pattern.CASE_INSENSITIVE);

    private AddonFeatureConfig() {
    }

    /** Creates server-editable files even when Mod Menu is not installed. */
    public static void initialize() {
        migratePerAddonJsonFiles();
        Map<String, Boolean> legacy = readFile(ConfigPaths.legacyAddons());
        Map<String, Boolean> requested = configuredStates(legacy);
        if (!save(requested)) return;
        try {
            backupMigrationSource(ConfigPaths.legacyAddons());
            Files.deleteIfExists(ConfigPaths.legacyAddons());
        } catch (Exception error) {
            System.err.println("[Skyblock Multi] No se pudo retirar la configuración antigua de addons: "
                    + error.getMessage());
        }
    }

    public static boolean isEnabled(AddonFeatureDefinition feature) {
        return readAddonStates(feature.addonModId())
                .getOrDefault(feature.id(), feature.enabledByDefault());
    }

    public static Map<String, Boolean> configuredStates() {
        return configuredStates(Map.of());
    }

    private static Map<String, Boolean> configuredStates(Map<String, Boolean> fallback) {
        Map<String, Boolean> states = new LinkedHashMap<>();
        Map<String, Map<String, Boolean>> byAddon = new LinkedHashMap<>();
        for (AddonFeatureDefinition feature : AddonFeatureRegistry.features()) {
            states.put(feature.id(), byAddon.computeIfAbsent(feature.addonModId(),
                            AddonFeatureConfig::readAddonStates)
                    .getOrDefault(feature.id(), fallback.getOrDefault(feature.id(), feature.enabledByDefault())));
        }
        return states;
    }

    public static boolean save(Map<String, Boolean> requestedStates) {
        Map<String, Map<String, Boolean>> byAddon = new LinkedHashMap<>();
        for (AddonFeatureDefinition feature : AddonFeatureRegistry.features()) {
            byAddon.computeIfAbsent(feature.addonModId(), AddonFeatureConfig::readAddonStates)
                    .put(feature.id(), requestedStates.getOrDefault(
                            feature.id(), feature.enabledByDefault()));
        }
        try {
            Files.createDirectories(ConfigPaths.addonsDirectory());
            for (Map.Entry<String, Map<String, Boolean>> addon : byAddon.entrySet()) {
                writeAddonFile(addon.getKey(), addon.getValue());
            }
            return true;
        } catch (Exception error) {
            System.err.println("[Skyblock Multi] No se pudo guardar la configuración de addons: "
                    + error.getMessage());
            return false;
        }
    }

    private static Map<String, Boolean> readAddonStates(String addonModId) {
        return readFile(pathFor(addonModId));
    }

    private static Map<String, Boolean> readFile(Path path) {
        Map<String, Boolean> states = new LinkedHashMap<>();
        if (Files.notExists(path)) return states;
        try {
            String raw = Files.readString(path);
            if (path.getFileName().toString().endsWith(".toml")) {
                Matcher matcher = TOML_FEATURE.matcher(raw);
                while (matcher.find()) {
                    states.put(matcher.group(1), Boolean.parseBoolean(matcher.group(2)));
                }
                return states;
            }
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonElement element = root.get("features");
            if (element == null || !element.isJsonObject()) return states;
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    states.put(entry.getKey(), entry.getValue().getAsBoolean());
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Invalid addon configuration; preserving " + path, error);
        }
        return states;
    }

    private static void writeAddonFile(String addonModId, Map<String, Boolean> states)
            throws Exception {
        StringBuilder toml = new StringBuilder();
        toml.append("# Skyblock Multi addon configuration: ").append(addonModId).append("\n");
        toml.append("# Edit only while the server is stopped. Boolean values: true or false.\n\n");
        toml.append("[features]\n");
        for (Map.Entry<String, Boolean> state : states.entrySet()) {
            AddonFeatureRegistry.features().stream()
                    .filter(feature -> feature.id().equals(state.getKey()))
                    .findFirst()
                    .ifPresent(feature -> toml.append("# Type: ")
                            .append(feature.type().name().toLowerCase(java.util.Locale.ROOT))
                            .append(feature.reloadRequired() ? "; requires reload" : "")
                            .append("\n"));
            toml.append('"').append(state.getKey()).append("\" = ")
                    .append(Boolean.TRUE.equals(state.getValue())).append("\n\n");
        }
        Files.writeString(pathFor(addonModId), toml.toString(), StandardCharsets.UTF_8);
    }

    private static Path pathFor(String addonModId) {
        String safeName = addonModId.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return ConfigPaths.addonsDirectory().resolve(safeName + ".toml");
    }

    private static void migratePerAddonJsonFiles() {
        Path directory = ConfigPaths.addonsDirectory();
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.list(directory)) {
            for (Path json : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String base = json.getFileName().toString().replaceFirst("\\.json$", "");
                Path toml = directory.resolve(base + ".toml");
                if (Files.notExists(toml)) {
                    writeAddonFile(base, readFile(json));
                }
                backupMigrationSource(json);
                Files.deleteIfExists(json);
            }
        } catch (Exception error) {
            throw new IllegalStateException("No se pudieron migrar las configuraciones de addons: "
                    + error.getMessage(), error);
        }
    }

    private static void backupMigrationSource(Path path) throws java.io.IOException {
        Path backup = path.resolveSibling(path.getFileName() + ".pre-toml.bak");
        if (Files.exists(path) && Files.notExists(backup)) Files.copy(path, backup);
    }
}
