package cl.treecs.skyblockmulti;

import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VanillaRecipeVerification {
    public static void main(String[] args) throws Exception {
        var migrated = SkyblockMultiMod.readLegacyLayout("{\"islandRadius\":8192,\"islandCapacity\":16}");
        require(migrated.baseRadius() == 8192 && migrated.capacity() == 16,
                "Legacy migration must preserve radius and capacity");
        var custom = SkyblockMultiMod.readLegacyLayout("{\"islandRadius\":4096,\"islandCapacity\":48,"
                + "\"enabledIslandRings\":[5],\"technicalIslandCapacity\":true,\"islandAllocationPolicy\":\"outer_first\"}");
        require(custom.capacity() == 48 && custom.technicalCapacity()
                        && custom.enabledRings().size() == 1, "Preserve custom legacy rings");
        try {
            SkyblockMultiMod.readLegacyLayout("{\"islandRadius\":1234}");
            throw new AssertionError("Unsupported geometry must not be silently replaced");
        } catch (IllegalArgumentException expected) {
            // The migration stops before touching the source file.
        }
        Path directory = Files.createTempDirectory("skyblock-recipe-check-");
        Path config = directory.resolve("general.toml");
        try {
            require(VanillaRecipeConfig.read(config), "Missing setting must preserve recipes");
            Files.writeString(config, "[islands]\nradius = 4096\n[difficulty.crafting]\n# keep me\n[parties]\nisland_change_cooldown_hours = 24\n");
            VanillaRecipeConfig.write(config, false);
            require(!VanillaRecipeConfig.read(config), "Disabled state must round-trip");
            String saved = Files.readString(config);
            require(saved.contains("radius = 4096") && saved.contains("# keep me"), "Preserve other settings");
            require(saved.indexOf("[difficulty.crafting]") == saved.lastIndexOf("[difficulty.crafting]"), "Do not duplicate tables");
            VanillaRecipeConfig.write(config, true);
            require(VanillaRecipeConfig.read(config), "Re-enabling must round-trip");
            Files.writeString(config, "[difficulty]\nminimum = \"beginner\"\n");
            VanillaRecipeConfig.write(config, false);
            require(!VanillaRecipeConfig.read(config), "Add setting to existing config");
            require(!VanillaRecipeConfig.isManagedRecipe(Identifier.parse("minecraft:sand_from_gravel")), "Never filter vanilla namespace");
            require(!VanillaRecipeConfig.isManagedRecipe(Identifier.parse("example:sand_from_gravel")), "Never filter addon namespace");
            int managed = 0;
            try (var recipes = Files.list(Path.of(args[0]))) {
                for (Path recipe : recipes.filter(p -> p.toString().endsWith(".json")).toList()) {
                    var json = JsonParser.parseString(Files.readString(recipe)).getAsJsonObject();
                    boolean vanilla = json.getAsJsonObject("result").get("id").getAsString().startsWith("minecraft:");
                    String name = recipe.getFileName().toString().replaceFirst("\\.json$", "");
                    require(VanillaRecipeConfig.isManagedRecipe(Identifier.parse("skyblockmulti:" + name)) == vanilla,
                            "Recipe coverage/progression isolation: " + name);
                    if (vanilla) managed++;
                }
            }
            require(managed == 23, "Update recipe coverage and tooltip when recipes change");
        } finally {
            Files.deleteIfExists(config);
            Files.deleteIfExists(directory);
        }
        System.out.println("Vanilla recipe configuration checks passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
