package cl.treecs.skyblockmulti.compatibility.jei;

import cl.treecs.skyblockmulti.NexusCreativeTab;
import cl.treecs.skyblockmulti.SkyblockMultiMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Makes component-based Nexus items discoverable while keeping JEI optional. */
@JeiPlugin
public final class SkyblockMultiJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID =
            Identifier.parse(SkyblockMultiMod.MOD_ID + ":jei");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(List.of(
                NexusCreativeTab.createLavaCatalyst(),
                NexusCreativeTab.createOffering("earth"),
                NexusCreativeTab.createOffering("trees"),
                NexusCreativeTab.createOffering("metals"),
                NexusCreativeTab.createOffering("war")
        ));
    }
}
