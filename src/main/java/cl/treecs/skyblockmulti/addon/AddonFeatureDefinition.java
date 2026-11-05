package cl.treecs.skyblockmulti.addon;

import java.util.List;

/** A configurable capability declared by an optional Skyblock Multi addon. */
public record AddonFeatureDefinition(
        String id,
        String addonModId,
        String addonName,
        String nameTranslationKey,
        String literalName,
        Type type,
        List<String> recipeNamespaces,
        List<String> recipePathPrefixes,
        boolean enabledByDefault,
        boolean reloadRequired
) {
    public enum Type {
        RECIPES,
        STARTER_CHEST,
        WANDERING_TRADER,
        CHALLENGE_LOOT,
        OTHER
    }
}
