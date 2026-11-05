package cl.treecs.skyblockmulti.mixin;

import cl.treecs.skyblockmulti.NexusFoundation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Includes distant blocks and slime/honey branches in boundary protection. */
@Mixin(PistonStructureResolver.class)
abstract class PistonStructureResolverMixin {
    @Shadow @Final private Level level;

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private void skyblockmulti$protectMovement(CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValueZ()) return;
        var resolver = (PistonStructureResolver) (Object) this;
        for (var pos : resolver.getToPush()) {
            if (NexusFoundation.isProtectedStructurePosition(level, pos)
                    || NexusFoundation.isProtectedStructurePosition(level,
                            pos.relative(resolver.getPushDirection()))) {
                callback.setReturnValue(false);
                return;
            }
        }
        for (var pos : resolver.getToDestroy()) {
            if (NexusFoundation.isProtectedStructurePosition(level, pos)) {
                callback.setReturnValue(false);
                return;
            }
        }
    }
}
