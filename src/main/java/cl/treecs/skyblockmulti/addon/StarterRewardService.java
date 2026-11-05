package cl.treecs.skyblockmulti.addon;

import cl.treecs.skyblockmulti.api.addon.AddonRewardApi;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import static net.minecraft.commands.Commands.literal;

public final class StarterRewardService {
    private static final java.util.Set<String> STARTING = new java.util.HashSet<>();
    private StarterRewardService() {}
    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> STARTING.clear());
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            dispatcher.register(literal("skyblockmulti").then(literal("initialize_starter")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> initialize(context.getSource())))
                .then(literal("prepare_starter")
                .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> prepare(context.getSource())))));
    }
    private static int prepare(CommandSourceStack source) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos chest = origin.offset(2, 1, 0);
        if (!level.getBlockState(origin.below()).is(Blocks.BEDROCK)) return 0;
        var path = source.getServer().getWorldPath(LevelResource.ROOT).resolve("skyblockmulti_starter_rewards.properties");
        var markers = RewardFile.read(path);
        String key = level.dimension().identifier() + "/" + origin.asLong();
        if (markers.containsKey(key)) return 0;
        // Old islands with a chest must never be initialized again.
        if (level.getBlockEntity(chest) instanceof Container) {
            markers.setProperty(key, "existing");
            RewardFile.write(path, markers);
            return 0;
        }
        if (!level.getBlockState(chest).isAir()) return 0;
        STARTING.add(key);
        return 1;
    }
    private static int initialize(CommandSourceStack source) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos chest = origin.offset(2, 1, 0);
        String key = level.dimension().identifier() + "/" + origin.asLong();
        if (!STARTING.remove(key)) return 0;
        var path = source.getServer().getWorldPath(LevelResource.ROOT).resolve("skyblockmulti_starter_rewards.properties");
        var markers = RewardFile.read(path);
        if (markers.containsKey(key)) return 0;
        var items = AddonRewardApi.starterItems(new AddonRewardApi.StarterContext(player.getUUID()));
        if (!items.isEmpty() && level.getBlockState(chest).isAir())
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chest) instanceof Container container) {
            int slot = 0;
            for (var grant : items) {
                while (slot < container.getContainerSize() && !container.getItem(slot).isEmpty()) slot++;
                if (slot >= container.getContainerSize()) throw new IllegalStateException("Starter chest full");
                container.setItem(slot++, new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse(grant.item())).orElseThrow(), grant.count()));
            }
            container.setChanged();
        }
        markers.setProperty(key, "initialized");
        RewardFile.write(path, markers);
        return 1;
    }
}
