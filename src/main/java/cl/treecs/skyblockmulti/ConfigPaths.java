package cl.treecs.skyblockmulti;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Canonical paths for global Skyblock Multi configuration. */
public final class ConfigPaths {
    private ConfigPaths() {
    }

    public static Path directory() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyblockmulti");
    }

    public static Path general() {
        return directory().resolve("general.toml");
    }

    public static Path legacyJsonGeneral() {
        return directory().resolve("general.json");
    }

    public static Path addonsDirectory() {
        return directory().resolve("addons");
    }

    public static Path treesDirectory() {
        return directory().resolve("trees");
    }

    public static Path legacyGeneral() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyblockmulti.json");
    }

    public static Path legacyNexus() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyblockmulti_nexus.json");
    }

    public static Path legacyAddons() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyblockmulti-addons.json");
    }
}
