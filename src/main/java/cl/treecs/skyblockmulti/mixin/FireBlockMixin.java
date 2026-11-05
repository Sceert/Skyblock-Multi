package cl.treecs.skyblockmulti.mixin;

import cl.treecs.skyblockmulti.NexusFoundation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void skyblockmulti$protectBurnTarget(Level level, BlockPos pos, int chance,
            RandomSource random, int age, CallbackInfo callback) {
        if (NexusFoundation.isProtectedStructurePosition(level, pos)) callback.cancel();
    }

    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true)
    private void skyblockmulti$protectSpreadTarget(LevelReader reader, BlockPos pos,
            CallbackInfoReturnable<Integer> callback) {
        if (reader instanceof Level level && NexusFoundation.isProtectedStructurePosition(level, pos)) {
            callback.setReturnValue(0);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void skyblockmulti$stopNexusFireSpread(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo callbackInfo
    ) {
        if (NexusFoundation.isProtectedStructurePosition(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
