package cl.treecs.skyblockmulti;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;

/** Switch for the mod's vanilla-material recipes; progression items stay available. */
public final class VanillaRecipeConfig {
    private static final Pattern SETTING = Pattern.compile(
            "(?m)^[\\t ]*vanilla_recipes_enabled[\\t ]*=[\\t ]*(true|false)[\\t ]*(?:#.*)?$");
    private static final Set<String> RECIPES = Set.of(
            "calcite_from_diorite_and_bone_meal", "clay_from_dirt_and_water",
            "cobweb_from_string_and_slime", "crimson_nylium_from_netherrack_and_red_mushroom",
            "deepslate_from_stone_and_coal", "dirt_from_mob_drops",
            "gilded_blackstone_from_blackstone_and_gold", "grass_from_bone_meal",
            "grass_from_wheat_seeds", "grass_from_wheat", "gravel_from_cobblestone",
            "mycelium_from_mushroom_blocks", "nether_wart_from_trial_materials",
            "netherrack_from_dirt_bone_and_blaze", "ochre_froglight_from_glowstone",
            "pearlescent_froglight_from_glowstone", "red_sand_from_sand", "sand_from_gravel",
            "soul_sand_from_sand_and_mob_drops", "soul_soil_from_dirt_and_soul_sand",
            "tuff_from_stone_and_flint", "verdant_froglight_from_glowstone",
            "warped_nylium_from_netherrack_and_brown_mushroom");

    private VanillaRecipeConfig() {}

    public static boolean enabled() {
        return read(ConfigPaths.general());
    }

    static boolean read(Path path) {
        try {
            if (Files.notExists(path)) return true;
            var matcher = SETTING.matcher(Files.readString(path));
            return !matcher.find() || Boolean.parseBoolean(matcher.group(1));
        } catch (IOException error) {
            throw new IllegalStateException("Could not read vanilla recipe configuration", error);
        }
    }

    public static boolean isManagedRecipe(Identifier id) {
        return id.getNamespace().equals("skyblockmulti") && RECIPES.contains(id.getPath());
    }

    public static void save(boolean enabled) {
        write(ConfigPaths.general(), enabled);
    }

    public static void initialize() {
        var path = ConfigPaths.general();
        try {
            if (!Files.exists(path) || !SETTING.matcher(Files.readString(path)).find()) {
                write(path, true);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not initialize vanilla recipe configuration", error);
        }
    }

    static void write(Path path, boolean enabled) {
        try {
            String raw = Files.exists(path) ? Files.readString(path) : "";
            var matcher = SETTING.matcher(raw);
            String setting = "vanilla_recipes_enabled = " + enabled;
            String updated;
            if (matcher.find()) {
                updated = matcher.replaceFirst(setting);
            } else {
                var section = Pattern.compile("(?m)^[\\t ]*\\[difficulty\\.crafting][\\t ]*(?:#.*)?$")
                        .matcher(raw);
                updated = section.find()
                        ? raw.substring(0, section.end()) + "\n" + setting + raw.substring(section.end())
                        : raw.stripTrailing() + "\n\n[difficulty.crafting]\n" + setting + "\n";
            }
            Files.createDirectories(path.getParent());
            Files.writeString(path, updated);
        } catch (IOException error) {
            throw new IllegalStateException("Could not save vanilla recipe configuration", error);
        }
    }
}
