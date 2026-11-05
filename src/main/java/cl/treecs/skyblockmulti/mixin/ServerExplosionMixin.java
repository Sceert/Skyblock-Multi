package cl.treecs.skyblockmulti.mixin;

import cl.treecs.skyblockmulti.NexusFoundation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void skyblockmulti$protectNexusBlocks(
            List<BlockPos> explodedPositions,
            CallbackInfo callbackInfo
    ) {
        ServerLevel level = ((ServerExplosion) (Object) this).level();

        // La lista también se reutiliza para crear fuego después de destruir
        // bloques. Filtrarla mantiene daño, sonido y partículas sin permitir
        // destrucción ni fuego de explosión dentro del Nexus.
        explodedPositions.removeIf(
                pos -> NexusFoundation.isProtectedStructurePosition(level, pos)
        );
    }
}
