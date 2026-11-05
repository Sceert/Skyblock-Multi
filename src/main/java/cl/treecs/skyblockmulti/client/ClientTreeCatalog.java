package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.TreeCatalogPayload;
import cl.treecs.skyblockmulti.tree.DataTreeDefinition;
import cl.treecs.skyblockmulti.tree.TreeDefinitionReloadListener;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Copia de solo lectura del último catálogo enviado por el servidor. */
public final class ClientTreeCatalog {
    private static volatile List<TreeCatalogPayload.Entry> entries = List.of();
    private static volatile Map<String, DataTreeDefinition> bundledDefinitions;

    private ClientTreeCatalog() {
    }

    public static void replace(List<TreeCatalogPayload.Entry> received) {
        entries = List.copyOf(received);
    }

    public static List<TreeCatalogPayload.Entry> entries() {
        return entries;
    }

    /** Finds tree definitions bundled in installed mods before a world/server is opened. */
    public static List<TreeCatalogPayload.Entry> discoverBundled(Map<String, Boolean> configuredStates) {
        Map<String, DataTreeDefinition> definitions = bundledDefinitions;
        if (definitions == null) {
            synchronized (ClientTreeCatalog.class) {
                definitions = bundledDefinitions;
                if (definitions == null) {
                    definitions = scanBundled();
                    bundledDefinitions = definitions;
                }
            }
        }
        return definitions.values().stream()
                .map(definition -> TreeCatalogPayload.Entry.create(definition,
                        configuredStates.getOrDefault(definition.id(), definition.enabledByDefault())))
                .sorted(Comparator.comparing(TreeCatalogPayload.Entry::id))
                .toList();
    }

    private static Map<String, DataTreeDefinition> scanBundled() {
        Map<String, DataTreeDefinition> discovered = new LinkedHashMap<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            for (Path root : mod.getRootPaths()) {
                Path data = root.resolve("data");
                if (!Files.isDirectory(data)) continue;
                try (var paths = Files.walk(data)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .forEach(path -> loadBundled(data, path, discovered));
                } catch (Exception exception) {
                    System.err.println("[Skyblock Multi] No se pudo revisar árboles de "
                            + mod.getMetadata().getId() + ": " + exception.getMessage());
                }
            }
        }
        return Map.copyOf(discovered);
    }

    private static void loadBundled(Path data, Path file, Map<String, DataTreeDefinition> output) {
        Path relative = data.relativize(file);
        if (relative.getNameCount() < 4
                || !relative.getName(1).toString().equals("skyblockmulti")
                || !relative.getName(2).toString().equals("tree_options")) return;

        String namespace = relative.getName(0).toString();
        List<String> idParts = new ArrayList<>();
        for (int i = 3; i < relative.getNameCount(); i++) idParts.add(relative.getName(i).toString());
        String definitionPath = String.join("/", idParts);
        definitionPath = definitionPath.substring(0, definitionPath.length() - ".json".length());
        String id = namespace + ":" + definitionPath;
        try (Reader reader = Files.newBufferedReader(file)) {
            DataTreeDefinition definition = TreeDefinitionReloadListener.parse(
                    id, JsonParser.parseReader(reader).getAsJsonObject());
            if (definition.requiredMods().stream()
                    .anyMatch(required -> !FabricLoader.getInstance().isModLoaded(required))) return;
            output.put(id, definition);
        } catch (Exception exception) {
            System.err.println("[Skyblock Multi] Definición cliente inválida " + id
                    + ": " + exception.getMessage());
        }
    }

    public static void clear() {
        entries = List.of();
    }
}
