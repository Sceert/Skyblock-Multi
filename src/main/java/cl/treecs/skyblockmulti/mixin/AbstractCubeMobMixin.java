package cl.treecs.skyblockmulti.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve actual ancestry instead of claiming unrelated nearby cubes. */
@Mixin(AbstractCubeMob.class)
public abstract class AbstractCubeMobMixin {
    @Inject(method = "setUpSplitCube", at = @At("TAIL"))
    private void skyblockmulti$inheritTrial(AbstractCubeMob child, int size, float x, float z, CallbackInfo ci) {
        var parent = (AbstractCubeMob) (Object) this;
        var tags = parent.entityTags();
        if (tags.contains("skyblock_infernal_enemy")) {
            child.addTag("skyblock_infernal_enemy");
            child.addTag("skyblock_infernal_regular");
        }
        if (tags.contains("skyblockmulti_dynamic_enemy")) {
            child.addTag("skyblockmulti_dynamic_enemy");
            tags.stream().filter(tag -> tag.matches("sb_dyn_[0-9]+" )).forEach(child::addTag);
            if (tags.contains("skyblockmulti_dynamic_children")) {
                child.addTag("skyblockmulti_dynamic_children");
                child.addTag("skyblockmulti_dynamic_victory");
            } else {
                child.removeTag("skyblockmulti_dynamic_victory");
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void skyblockmulti$preventCleanupSplit(Entity.RemovalReason reason, CallbackInfo ci) {
        var cube = (AbstractCubeMob) (Object) this;
        if (cube.entityTags().contains("skyblockmulti_cleanup")) cube.setSize(1, false);
    }
}
