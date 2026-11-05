package cl.treecs.skyblockmulti.tree;

import cl.treecs.skyblockmulti.ConfigPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stores tree switches separately from general world configuration. */
public final class TreeConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern TOML_TREE = Pattern.compile(
            "(?m)^\\s*\\\"([^\\\"]+)\\\"\\s*=\\s*(true|false)\\s*(?:#.*)?$",
            Pattern.CASE_INSENSITIVE);

    private TreeConfigStorage() {
    }

    public static void initialize(Path generalConfig) throws IOException {
        Files.createDirectories(ConfigPaths.treesDirectory());
        migrateEmbeddedStates(generalConfig);
        migrateProviderJsonFiles();
        if (Files.notExists(providerPath("minecraft"))) {
            save(TreeConfigCodec.defaultStates());
        }
    }

    public static Map<String, Boolean> load() throws IOException {
        Map<String, Boolean> configured = new LinkedHashMap<>();
        Path directory = ConfigPaths.treesDirectory();
        if (Files.isDirectory(directory)) {
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".toml"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList()) {
                    Matcher matcher = TOML_TREE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                    while (matcher.find()) {
                        String id = TreeConfigCodec.normalizeId(matcher.group(1));
                        if (id != null) configured.put(id, Boolean.parseBoolean(matcher.group(2)));
                    }
                }
            }
        }
        return TreeConfigCodec.normalize(configured);
    }

    public static void save(Map<String, Boolean> states) throws IOException {
        save(ConfigPaths.treesDirectory(), states);
    }

    static void save(Path directory, Map<String, Boolean> states) throws IOException {
        Files.createDirectories(directory);
        Map<String, Map<String, Boolean>> byProvider = groupByProvider(states);
        for (Map.Entry<String, Map<String, Boolean>> provider : byProvider.entrySet()) {
            StringBuilder toml = new StringBuilder();
            toml.append("# Skyblock Multi tree configuration: ").append(provider.getKey()).append("\n");
            toml.append("# Edit only while the server is stopped. Boolean values: true or false.\n\n");
            toml.append("[trees]\n");
            provider.getValue().forEach((id, enabled) -> toml.append('"').append(id)
                    .append("\" = ").append(Boolean.TRUE.equals(enabled)).append("\n"));
            Files.writeString(providerPath(directory, provider.getKey()), toml.toString(), StandardCharsets.UTF_8);
        }
    }

    /** Adds newly discovered datapack trees without losing settings for absent providers. */
    public static void synchronizeWithCatalog() {
        try {
            save(load());
        } catch (IOException error) {
            System.err.println("[Skyblock Multi] No se pudo sincronizar la configuración de árboles: "
                    + error.getMessage());
        }
    }

    static Map<String, Map<String, Boolean>> groupByProvider(Map<String, Boolean> states) {
        Map<String, Map<String, Boolean>> grouped = new LinkedHashMap<>();
        if (states == null) return grouped;
        states.forEach((rawId, enabled) -> {
            String id = TreeConfigCodec.normalizeId(rawId);
            if (id == null) return;
            String provider = id.substring(0, id.indexOf(':'));
            grouped.computeIfAbsent(provider, ignored -> new LinkedHashMap<>())
                    .put(id, Boolean.TRUE.equals(enabled));
        });
        return grouped;
    }

    private static void migrateEmbeddedStates(Path generalConfig) throws IOException {
        if (generalConfig == null || Files.notExists(generalConfig)) return;
        String raw = Files.readString(generalConfig, StandardCharsets.UTF_8);
        if (!raw.stripLeading().startsWith("{")) return;
        JsonObject general = JsonParser.parseString(raw).getAsJsonObject();
        if (!general.has("enabledTrees")) return;

        Map<String, Boolean> embedded = TreeConfigCodec.readExplicitStates(raw);
        backupMigrationSource(generalConfig);
        if (!embedded.isEmpty()) migrateStates(ConfigPaths.treesDirectory(), embedded);
        general.remove("enabledTrees");
        Files.writeString(generalConfig, GSON.toJson(general) + System.lineSeparator(),
                StandardCharsets.UTF_8);
        System.out.println("[Skyblock Multi] Configuración de árboles migrada a "
                + ConfigPaths.treesDirectory());
    }

    private static Path providerPath(String provider) {
        return providerPath(ConfigPaths.treesDirectory(), provider);
    }

    private static Path providerPath(Path directory, String provider) {
        String safe = provider.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_");
        if (safe.equals("minecraft")) safe = "vanilla";
        return directory.resolve(safe + ".toml");
    }

    private static void migrateProviderJsonFiles() throws IOException {
        migrateProviderJsonFiles(ConfigPaths.treesDirectory());
    }

    static void migrateProviderJsonFiles(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            for (Path json : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                String raw = Files.readString(json, StandardCharsets.UTF_8);
                Map<String, Boolean> states = TreeConfigCodec.readExplicitStates(raw);
                // Parse first: an invalid legacy file must never be silently deleted.
                JsonParser.parseString(raw).getAsJsonObject();
                backupMigrationSource(json);
                if (!states.isEmpty()) migrateStates(directory, states);
                Files.deleteIfExists(json);
            }
        }
    }

    private static void migrateStates(Path directory, Map<String, Boolean> legacy) throws IOException {
        Map<String, Boolean> merged = new LinkedHashMap<>(legacy);
        for (String provider : groupByProvider(legacy).keySet()) {
            Path target = providerPath(directory, provider);
            if (Files.notExists(target)) continue;
            Matcher matcher = TOML_TREE.matcher(Files.readString(target, StandardCharsets.UTF_8));
            while (matcher.find()) {
                String id = TreeConfigCodec.normalizeId(matcher.group(1));
                if (id != null) merged.put(id, Boolean.parseBoolean(matcher.group(2)));
            }
        }
        save(directory, merged);
    }

    private static void backupMigrationSource(Path path) throws IOException {
        Path backup = path.resolveSibling(path.getFileName() + ".pre-toml.bak");
        if (Files.notExists(backup)) Files.copy(path, backup);
    }
}
