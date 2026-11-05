package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.ConfigPaths;
import cl.treecs.skyblockmulti.island.PanelAppearance;

public final class ClientPanelAppearance {
    private static PanelAppearance current = PanelAppearance.DEFAULT;
    private ClientPanelAppearance() {}
    public static PanelAppearance get() { return current; }
    public static void load() {
        try { current = PanelAppearance.load(ConfigPaths.directory().resolve("client.toml")); }
        catch (java.io.IOException failure) { System.err.println("[Skyblock Multi] Cannot read panel appearance: " + failure); }
    }
    public static void save(PanelAppearance value) {
        try {
            value.save(ConfigPaths.directory().resolve("client.toml"));
            current = value;
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot save panel appearance", failure); }
    }
}
