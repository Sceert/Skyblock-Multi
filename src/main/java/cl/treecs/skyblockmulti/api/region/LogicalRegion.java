package cl.treecs.skyblockmulti.api.region;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** Stable public description of a named Skyblock Multi location. */
public record LogicalRegion(
        Identifier id,
        ResourceKey<Level> dimension,
        RegionShape shape,
        int priority,
        String translationKey
) {
    public LogicalRegion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(translationKey, "translationKey");
    }

    /** Entity tag exposed to commands and datapacks while a player is inside. */
    public String entityTag() {
        return "skyblockmulti_region_" + id.getPath().replace('/', '_').replace('.', '_');
    }
}
