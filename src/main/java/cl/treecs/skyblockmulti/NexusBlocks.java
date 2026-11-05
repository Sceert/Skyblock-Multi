package cl.treecs.skyblockmulti;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Blocks owned by SkyblockMulti; none of them alter their vanilla counterparts. */
public final class NexusBlocks {
    public static final Block INFERNAL_VAULT = register("infernal_vault");

    private NexusBlocks() {}

    private static Block register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(SkyblockMultiMod.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        // A vanilla vault's copied properties include a light function that reads
        // VaultBlock.STATE. This plain custom block does not own that property, so
        // copying Vault directly crashes while its default state is constructed.
        // Iron supplies the intended metal durability without inheriting
        // state-dependent vanilla-vault behaviour.
        Block block = new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        BlockItem item = new BlockItem(block, new Item.Properties().stacksTo(1).setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return block;
    }

    public static void initialize() {
        // Static registration.
    }
}
