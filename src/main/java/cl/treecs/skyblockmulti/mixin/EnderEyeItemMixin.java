package cl.treecs.skyblockmulti.mixin;

import cl.treecs.skyblockmulti.NexusFoundation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {
    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;findNearestMapStructure(Lnet/minecraft/tags/TagKey;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;"
            )
    )
    private BlockPos skyblockmulti$locateAscensionNexusPortal(
            ServerLevel level,
            TagKey<Structure> structureTag,
            BlockPos origin,
            int searchRadius,
            boolean skipKnownStructures
    ) {
        BlockPos nexusPortal = NexusFoundation.getEndPortalLocatorTarget(level);
        if (nexusPortal != null) {
            return nexusPortal;
        }

        return level.findNearestMapStructure(
                structureTag,
                origin,
                searchRadius,
                skipKnownStructures
        );
    }
}
