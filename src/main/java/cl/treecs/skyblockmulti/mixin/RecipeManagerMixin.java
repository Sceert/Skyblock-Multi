package cl.treecs.skyblockmulti.mixin;

import cl.treecs.skyblockmulti.addon.AddonFeatureRegistry;
import cl.treecs.skyblockmulti.VanillaRecipeConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.stream.Stream;

/** Filters the recipe map each time the reloadable recipe registry is loaded. */
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeMap;create(Lnet/minecraft/core/HolderLookup;)Lnet/minecraft/world/item/crafting/RecipeMap;"))
    private RecipeMap skyblockmulti$filterAddonRecipes(HolderLookup<Recipe<?>> original) {
        boolean vanillaEnabled = VanillaRecipeConfig.enabled();
        var disabledAddonRecipe = AddonFeatureRegistry.disabledRecipePredicate();
        return RecipeMap.create(new HolderLookup<Recipe<?>>() {
            private boolean enabled(ResourceKey<Recipe<?>> key) {
                return (vanillaEnabled || !VanillaRecipeConfig.isManagedRecipe(key.identifier()))
                        && !disabledAddonRecipe.test(key.identifier());
            }

            @Override
            public Stream<Holder.Reference<Recipe<?>>> listElements() {
                return original.listElements().filter(holder -> enabled(holder.key()));
            }

            @Override
            public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> key) {
                return enabled(key) ? original.get(key) : Optional.empty();
            }

            @Override
            public Stream<HolderSet.Named<Recipe<?>>> listTags() { return original.listTags(); }

            @Override
            public Optional<HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> tag) { return original.get(tag); }
        });
    }
}
