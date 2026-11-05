package cl.treecs.skyblockmulti;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

/** Registered item identities used by Nexus progression and recipe viewers. */
public final class NexusItems {
    public static final Item OFFERING_EARTH = register("offering_earth");
    public static final Item OFFERING_TREES = register("offering_trees");
    public static final Item OFFERING_METALS = register("offering_metals");
    public static final Item OFFERING_WAR = register("offering_war");
    public static final Item LAVA_CATALYST = register("lava_catalyst");
    public static final Item INFERNAL_CORE = registerInfernalCore("infernal_core");
    public static final Item INFERNAL_CORE_2 = registerInfernalCore("infernal_core_2");
    public static final Item INFERNAL_CORE_3 = registerInfernalCore("infernal_core_3");
    public static final Item INFERNAL_CORE_4 = registerInfernalCore("infernal_core_4");
    public static final Item INFERNAL_CORE_5 = registerInfernalCore("infernal_core_5");
    public static final Item INFERNAL_CORE_6 = registerInfernalCore("infernal_core_6");

    private NexusItems() {
    }

    private static Item register(String name) {
        return register(name, Item::new, new Item.Properties().stacksTo(1));
    }

    private static Item registerInfernalCore(String name) {
        return register(name, Item::new, new Item.Properties().stacksTo(1).fireResistant());
    }

    private static <T extends Item> T register(
            String name,
            Function<Item.Properties, T> factory,
            Item.Properties properties
    ) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, name)
        );
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // Loading this class performs the static registrations above.
    }

    public static Item offering(String type) {
        return switch (type) {
            case "earth" -> OFFERING_EARTH;
            case "trees" -> OFFERING_TREES;
            case "metals" -> OFFERING_METALS;
            case "war" -> OFFERING_WAR;
            default -> throw new IllegalArgumentException("Unknown Nexus offering: " + type);
        };
    }
}
