package cl.treecs.skyblockmulti.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coloca una definición ya validada sin modificar la lógica de asignación de islas. */
public final class TreePlacementService {
    private TreePlacementService() {
    }

    public static Result place(ServerLevel level, BlockPos origin, String treeId,
                               Map<String, Boolean> configuredStates) {
        DataTreeDefinition definition = TreeCatalog.activeDefinitions().get(treeId);
        if (definition == null) return Result.DEFINITION_NOT_FOUND;
        if (!Boolean.TRUE.equals(configuredStates.get(treeId))) return Result.DEFINITION_DISABLED;
        return place(level, origin, definition);
    }

    public static Result place(ServerLevel level, BlockPos origin, DataTreeDefinition definition) {
        if (!isChunkLoaded(level, origin)) return Result.CHUNK_NOT_LOADED;

        return switch (definition.placement()) {
            case DataTreeDefinition.SaplingPlacement sapling -> placeSapling(level, origin, sapling);
            case DataTreeDefinition.AzaleaPlacement azalea -> placeAzalea(level, origin, azalea);
            case DataTreeDefinition.StructurePlacement structure -> placeStructure(level, origin, structure);
        };
    }

    private static Result placeSapling(ServerLevel level, BlockPos origin,
                                       DataTreeDefinition.SaplingPlacement placement) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(placement.item()));
        if (item.isEmpty() || !(item.get() instanceof BlockItem blockItem)) {
            return Result.INVALID_BLOCK_ITEM;
        }

        List<BlockPos> positions = saplingPositions(origin, placement.pattern());
        if (positions.stream().anyMatch(pos -> !isChunkLoaded(level, pos))) return Result.CHUNK_NOT_LOADED;
        if (positions.stream().anyMatch(pos -> !level.getBlockState(pos).isAir())) return Result.TARGET_BLOCKED;

        for (BlockPos position : positions) {
            level.setBlock(position, blockItem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
        }

        // Las islas vanilla reciben un árbol ya formado. Conservamos la misma expectativa
        // para los retoños aportados por datapacks, usando su generador registrado por el mod.
        // Si una especie no utiliza SaplingBlock, queda plantada como respaldo seguro.
        BlockPos growthOrigin = positions.getFirst();
        for (int attempt = 0; attempt < 8; attempt++) {
            var state = level.getBlockState(growthOrigin);
            if (!(state.getBlock() instanceof SaplingBlock saplingBlock)) break;
            saplingBlock.advanceTree(level, growthOrigin, state, level.getRandom());
        }
        return Result.SUCCESS;
    }

    private static Result placeAzalea(ServerLevel level, BlockPos origin,
                                      DataTreeDefinition.AzaleaPlacement placement) {
        if (!level.getBlockState(origin).isAir()) return Result.TARGET_BLOCKED;
        Block block = placement.flowering() ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA;
        level.setBlock(origin, block.defaultBlockState(), Block.UPDATE_ALL);
        return Result.SUCCESS;
    }

    private static Result placeStructure(ServerLevel level, BlockPos origin,
                                         DataTreeDefinition.StructurePlacement placement) {
        Identifier structureId = Identifier.parse(placement.structure());
        var template = level.getStructureTemplateManager().get(structureId);
        if (template.isEmpty()) return Result.STRUCTURE_NOT_FOUND;

        BlockPos target = origin.offset(placement.offsetX(), placement.offsetY(), placement.offsetZ());
        if (!isChunkLoaded(level, target)) return Result.CHUNK_NOT_LOADED;
        Rotation rotation = placement.randomRotation()
                ? Rotation.getRandom(level.getRandom())
                : Rotation.NONE;
        var size = template.get().getSize(rotation);
        if (size.getX() > 64 || size.getY() > 64 || size.getZ() > 64) {
            return Result.STRUCTURE_TOO_LARGE;
        }
        var bounds = template.get().getBoundingBox(target, rotation, target, Mirror.NONE);
        BlockPos firstCorner = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        BlockPos secondCorner = new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ());
        if (!areChunksLoaded(level, firstCorner, secondCorner)) return Result.CHUNK_NOT_LOADED;
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        boolean placed = template.get().placeInWorld(
                level,
                target,
                target,
                settings,
                level.getRandom(),
                Block.UPDATE_ALL
        );
        return placed ? Result.SUCCESS : Result.PLACEMENT_FAILED;
    }

    static List<BlockPos> saplingPositions(BlockPos origin,
                                           DataTreeDefinition.SaplingPlacement.Pattern pattern) {
        if (pattern == DataTreeDefinition.SaplingPlacement.Pattern.SINGLE) {
            return List.of(origin);
        }
        return List.of(origin, origin.east(), origin.south(), origin.east().south());
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean areChunksLoaded(ServerLevel level, BlockPos first, BlockPos second) {
        int minChunkX = Math.min(first.getX(), second.getX()) >> 4;
        int maxChunkX = Math.max(first.getX(), second.getX()) >> 4;
        int minChunkZ = Math.min(first.getZ(), second.getZ()) >> 4;
        int maxChunkZ = Math.max(first.getZ(), second.getZ()) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) return false;
            }
        }
        return true;
    }

    public enum Result {
        SUCCESS,
        DEFINITION_NOT_FOUND,
        DEFINITION_DISABLED,
        CHUNK_NOT_LOADED,
        INVALID_BLOCK_ITEM,
        TARGET_BLOCKED,
        STRUCTURE_NOT_FOUND,
        STRUCTURE_TOO_LARGE,
        PLACEMENT_FAILED
    }
}
