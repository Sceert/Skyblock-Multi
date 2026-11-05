package cl.treecs.skyblockmulti.tree;

import java.util.List;

/** Definición declarativa de árbol cargada desde recursos del servidor. */
public record DataTreeDefinition(
        String id,
        TextValue name,
        TextValue description,
        String iconItem,
        Placement placement,
        PlantingStock plantingStock,
        List<String> categories,
        boolean enabledByDefault,
        List<String> requiredMods
) {
    public DataTreeDefinition {
        id = TreeDefinition.requireNamespacedId(id);
        categories = List.copyOf(categories);
        requiredMods = List.copyOf(requiredMods);
    }

    /** Extra reproductive material delivered in the starter chest by the addon. */
    public record PlantingStock(String item, int count) {
        public PlantingStock {
            item = TreeDefinition.requireNamespacedId(item);
            if (count < 1 || count > 64) {
                throw new IllegalArgumentException("planting_stock.count debe estar entre 1 y 64");
            }
        }
    }

    public record TextValue(String translationKey, String literal) {
        public TextValue {
            if ((translationKey == null || translationKey.isBlank())
                    && (literal == null || literal.isBlank())) {
                throw new IllegalArgumentException("El texto necesita 'translate' o 'text'");
            }
        }
    }

    public sealed interface Placement permits SaplingPlacement, AzaleaPlacement, StructurePlacement {
        String type();
    }

    public record SaplingPlacement(String item, Pattern pattern) implements Placement {
        public SaplingPlacement {
            item = TreeDefinition.requireNamespacedId(item);
        }

        @Override
        public String type() {
            return "skyblockmulti:sapling";
        }

        public enum Pattern {
            SINGLE,
            TWO_BY_TWO
        }
    }

    public record AzaleaPlacement(boolean flowering) implements Placement {
        @Override
        public String type() {
            return "skyblockmulti:azalea";
        }
    }

    public record StructurePlacement(String structure, int offsetX, int offsetY, int offsetZ,
                                     boolean randomRotation) implements Placement {
        public StructurePlacement {
            structure = TreeDefinition.requireNamespacedId(structure);
        }

        @Override
        public String type() {
            return "skyblockmulti:structure";
        }
    }
}
