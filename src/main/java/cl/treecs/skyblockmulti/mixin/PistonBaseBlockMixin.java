package cl.treecs.skyblockmulti.mixin;

import cl.treecs.skyblockmulti.NexusFoundation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Inject(method = "moveBlocks", at = @At("HEAD"), cancellable = true)
    private void skyblockmulti$stopNexusPistonMovement(
            Level level,
            BlockPos pistonPos,
            Direction direction,
            boolean extending,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        BlockPos movementStart = pistonPos.relative(direction);
        if (NexusFoundation.isProtectedStructurePosition(level, pistonPos)
                || NexusFoundation.isProtectedStructurePosition(level, movementStart)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
