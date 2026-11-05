package cl.treecs.skyblockmulti.api.addon;

import cl.treecs.skyblockmulti.addon.AddonFeatureRegistry;

/** Stable public entry point for addon-owned configurable capabilities. */
public final class AddonFeatureApi {
    public static final int API_VERSION = 1;

    private AddonFeatureApi() {
    }

    /**
     * Returns the administrator-selected state for a fully qualified feature ID.
     * Unknown or currently unavailable features are disabled safely.
     */
    public static boolean isEnabled(String featureId) {
        return AddonFeatureRegistry.isEnabled(featureId);
    }
}
