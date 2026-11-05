package cl.treecs.skyblockmulti;

import cl.treecs.skyblockmulti.network.RequiredClientPayload;
import cl.treecs.skyblockmulti.network.TreeCatalogPayload;
import cl.treecs.skyblockmulti.network.TreeSelectPayload;
import cl.treecs.skyblockmulti.network.OpenTreeSelectionPayload;
import cl.treecs.skyblockmulti.network.OpenDifficultySelectionPayload;
import cl.treecs.skyblockmulti.network.DifficultySelectPayload;
import cl.treecs.skyblockmulti.network.BackToTreeSelectionPayload;
import cl.treecs.skyblockmulti.network.SelectionMenuClosedPayload;
import cl.treecs.skyblockmulti.tree.PlayerTreeSelectionState;
import cl.treecs.skyblockmulti.tree.TreeCatalog;
import cl.treecs.skyblockmulti.tree.TreePlacementService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.scores.ScoreHolder;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

public final class RequiredClientMod implements ModInitializer {
    private static final Set<UUID> DIFFICULTY_SCREEN_SENT = new HashSet<>();
    private static cl.treecs.skyblockmulti.island.IslandTreeHistory treeHistory;

    public static Component islandTreeName(int slot) {
        String id = treeHistory == null ? null : treeHistory.tree(slot);
        if (id == null) return Component.translatable("skyblockmulti.panel.unrecorded");
        var definition = TreeCatalog.activeDefinitions().get(id);
        if (definition == null) return Component.literal(id);
        return definition.name().translationKey() != null ? Component.translatable(definition.name().translationKey())
                : Component.literal(definition.name().literal());
    }

    private static void recordAssignedTree(net.minecraft.server.level.ServerPlayer player) {
        if (treeHistory == null || selectionState(player) != 2) return;
        var objective = player.level().getScoreboard().getObjective("sb3_slot");
        var slot = objective == null ? null : player.level().getScoreboard().getPlayerScoreInfo(player, objective);
        if (slot != null && slot.value() >= 1 && slot.value() <= cl.treecs.skyblockmulti.island.IslandRingLayout.TECHNICAL_LIMIT)
            treeHistory.commit(player.getUUID(), slot.value());
    }

    @Override
    public void onInitialize() {
        IslandPanelService.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> treeHistory = new cl.treecs.skyblockmulti.island.IslandTreeHistory(
                server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("skyblockmulti_island_trees_v1.properties")));
        PayloadTypeRegistry.clientboundConfiguration().register(
                RequiredClientPayload.TYPE,
                RequiredClientPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                TreeCatalogPayload.TYPE,
                TreeCatalogPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                TreeSelectPayload.TYPE,
                TreeSelectPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                OpenTreeSelectionPayload.TYPE,
                OpenTreeSelectionPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                OpenDifficultySelectionPayload.TYPE,
                OpenDifficultySelectionPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                DifficultySelectPayload.TYPE,
                DifficultySelectPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                BackToTreeSelectionPayload.TYPE,
                BackToTreeSelectionPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                SelectionMenuClosedPayload.TYPE,
                SelectionMenuClosedPayload.CODEC
        );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("skyblockmulti").then(
                        literal("trees").executes(command -> {
                            if (!(command.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                                return 0;
                            }
                            if (isAwaitingTreeSelection(player) && areIslandAnchorsReady(player)
                                    && ServerPlayNetworking.canSend(player, OpenTreeSelectionPayload.TYPE)) {
                                ServerPlayNetworking.send(player, OpenTreeSelectionPayload.INSTANCE);
                                return 1;
                            }
                            if (isAwaitingDifficultySelection(player)
                                    && ServerPlayNetworking.canSend(player, OpenDifficultySelectionPayload.TYPE)) {
                                ServerPlayNetworking.send(player, OpenDifficultySelectionPayload.INSTANCE);
                                return 1;
                            }
                            return 0;
                        })
                ))
        );

        ServerPlayNetworking.registerGlobalReceiver(
                TreeSelectPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    boolean selectionAllowed = isAwaitingTreeSelection(context)
                            && areIslandAnchorsReady(context.player());
                    Map<String, Boolean> selectableStates = selectableTreeStates();
                    PlayerTreeSelectionState.Result result;
                    if (TreeSelectPayload.RANDOM_SELECTION.equals(payload.treeId())) {
                        result = PlayerTreeSelectionState.selectRandom(
                                context.player().getUUID(),
                                selectableStates,
                                bound -> context.player().getRandom().nextInt(bound),
                                selectionAllowed
                        );
                    } else {
                        result = PlayerTreeSelectionState.select(
                                context.player().getUUID(),
                                payload.treeId(),
                                selectableStates,
                                selectionAllowed
                        );
                    }
                    if (result == PlayerTreeSelectionState.Result.ACCEPTED) {
                        PlayerTreeSelectionState.get(context.player().getUUID()).ifPresent(treeId -> {
                                if (treeHistory != null) treeHistory.remember(context.player().getUUID(), treeId);
                                TreeCatalog.find(treeId)
                                        .ifPresentOrElse(
                                                tree -> applyLegacyTrigger(context, tree.legacyTriggerValue()),
                                                () -> advanceExternalTreeSelection(context)
                                        );
                        });
                    }
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(
                DifficultySelectPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (!isAwaitingDifficultySelection(context.player())
                            || payload.value() < 1 || payload.value() > 4) {
                        return;
                    }
                    var objective = context.server().getScoreboard().getObjective("sb_difficulty");
                    if (objective != null) {
                        DIFFICULTY_SCREEN_SENT.remove(context.player().getUUID());
                        context.server().getScoreboard()
                                .getOrCreatePlayerScore(context.player(), objective)
                                .set(payload.value());
                    }
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(
                BackToTreeSelectionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (!isAwaitingDifficultySelection(context.player())) return;

                    var scoreboard = context.server().getScoreboard();
                    var stateObjective = scoreboard.getObjective("sb3_state");
                    var treeObjective = scoreboard.getObjective("sb_tree");
                    var difficultyObjective = scoreboard.getObjective("sb_difficulty");
                    if (stateObjective == null || treeObjective == null || difficultyObjective == null) return;

                    scoreboard.getOrCreatePlayerScore(context.player(), stateObjective).set(1);
                    scoreboard.getOrCreatePlayerScore(context.player(), treeObjective).set(0);
                    scoreboard.getOrCreatePlayerScore(context.player(), difficultyObjective).set(0);
                    PlayerTreeSelectionState.clear(context.player().getUUID());
                    DIFFICULTY_SCREEN_SENT.remove(context.player().getUUID());

                    if (ServerPlayNetworking.canSend(context.player(), OpenTreeSelectionPayload.TYPE)) {
                        ServerPlayNetworking.send(context.player(), OpenTreeSelectionPayload.INSTANCE);
                    }
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SelectionMenuClosedPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (isAwaitingTreeSelection(context.player())
                            || isAwaitingDifficultySelection(context.player())) {
                        context.player().sendSystemMessage(IslandPanelService.openLink());
                    }
                })
        );

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (ServerPlayNetworking.canSend(player, TreeCatalogPayload.TYPE)) {
                ServerPlayNetworking.send(
                        player,
                        TreeCatalogPayload.create(SkyblockMultiMod.getConfiguredTreeStatesById())
                );
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var scoreboard = server.getScoreboard();
            var objective = scoreboard.getObjective("sb3_state");
            if (objective == null) return;
            for (var player : server.getPlayerList().getPlayers()) {
                String pendingTree = treeHistory == null ? null : treeHistory.pending(player.getUUID());
                if (pendingTree != null && TreeCatalog.find(pendingTree).isPresent()) recordAssignedTree(player);
                placePendingExternalTree(server, player);
                var score = scoreboard.getPlayerScoreInfo(player, objective);
                boolean selectingDifficulty = score != null && score.value() == 4;
                if (!selectingDifficulty) {
                    DIFFICULTY_SCREEN_SENT.remove(player.getUUID());
                } else if (DIFFICULTY_SCREEN_SENT.add(player.getUUID())
                        && ServerPlayNetworking.canSend(player, OpenDifficultySelectionPayload.TYPE)) {
                    ServerPlayNetworking.send(player, OpenDifficultySelectionPayload.INSTANCE);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DIFFICULTY_SCREEN_SENT.remove(handler.getPlayer().getUUID())
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DIFFICULTY_SCREEN_SENT.clear();
            treeHistory = null;
        });

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, RequiredClientPayload.TYPE)) {
                handler.disconnect(Component.literal(
                        "Skyblock Multi debe estar instalado en tu cliente. "
                                + "Instala la misma versión del mod, Fabric Loader y Fabric API. / "
                                + "Skyblock Multi must be installed on your client. "
                                + "Install the same mod version, Fabric Loader, and Fabric API."
                ));
            }
        });
    }

    private static boolean isAwaitingTreeSelection(ServerPlayNetworking.Context context) {
        return isAwaitingTreeSelection(context.player());
    }

    private static boolean isAwaitingTreeSelection(net.minecraft.server.level.ServerPlayer player) {
        return selectionState(player) == 1;
    }

    private static boolean isAwaitingDifficultySelection(net.minecraft.server.level.ServerPlayer player) {
        return selectionState(player) == 4;
    }

    private static boolean areIslandAnchorsReady(net.minecraft.server.level.ServerPlayer player) {
        var scoreboard = player.level().getScoreboard();
        var objective = scoreboard.getObjective("sb3_const");
        if (objective == null) return false;
        var score = scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("#slots_built"), objective);
        return score != null && score.value() == 1;
    }

    private static int selectionState(net.minecraft.server.level.ServerPlayer player) {
        var scoreboard = player.level().getScoreboard();
        var objective = scoreboard.getObjective("sb3_state");
        if (objective == null) return Integer.MIN_VALUE;
        var score = scoreboard.getPlayerScoreInfo(player, objective);
        return score == null ? Integer.MIN_VALUE : score.value();
    }

    private static Map<String, Boolean> selectableTreeStates() {
        return new LinkedHashMap<>(SkyblockMultiMod.getConfiguredTreeStatesById());
    }

    private static void advanceExternalTreeSelection(ServerPlayNetworking.Context context) {
        var scoreboard = context.server().getScoreboard();
        var stateObjective = scoreboard.getObjective("sb3_state");
        var treeObjective = scoreboard.getObjective("sb_tree");
        var difficultyObjective = scoreboard.getObjective("sb_difficulty");
        if (stateObjective == null || treeObjective == null || difficultyObjective == null) return;

        // 12 queda reservado como puente hacia el flujo legacy. No ejecuta ningún árbol vanilla.
        scoreboard.getOrCreatePlayerScore(context.player(), treeObjective).set(12);
        scoreboard.getOrCreatePlayerScore(context.player(), difficultyObjective).set(0);
        scoreboard.getOrCreatePlayerScore(context.player(), stateObjective).set(4);
    }

    private static void placePendingExternalTree(net.minecraft.server.MinecraftServer server,
                                                 net.minecraft.server.level.ServerPlayer player) {
        var selected = PlayerTreeSelectionState.get(player.getUUID());
        if (selected.isEmpty() || TreeCatalog.find(selected.get()).isPresent() || selectionState(player) != 2) {
            return;
        }

        var scoreboard = server.getScoreboard();
        var xObjective = scoreboard.getObjective("sb3_x");
        var zObjective = scoreboard.getObjective("sb3_z");
        if (xObjective == null || zObjective == null) return;
        var xScore = scoreboard.getPlayerScoreInfo(player, xObjective);
        var zScore = scoreboard.getPlayerScoreInfo(player, zObjective);
        if (xScore == null || zScore == null) return;

        var overworld = server.overworld();
        BlockPos origin = new BlockPos(xScore.value() - 2, 65, zScore.value());
        TreePlacementService.Result result = TreePlacementService.place(
                overworld,
                origin,
                selected.get(),
                SkyblockMultiMod.getConfiguredTreeStatesById()
        );
        if (result == TreePlacementService.Result.CHUNK_NOT_LOADED) return;

        if (result != TreePlacementService.Result.SUCCESS) {
            System.err.println("[Skyblock Multi] No se pudo colocar el árbol externo "
                    + selected.get() + " para " + player.getGameProfile().name() + ": " + result);
            player.sendSystemMessage(Component.literal(
                    "Skyblock Multi: no se pudo colocar el árbol seleccionado (" + result + ")."
            ).withStyle(ChatFormatting.RED));
        } else {
            recordAssignedTree(player);
            var definition = TreeCatalog.activeDefinitions().get(selected.get());
            if (definition != null && definition.plantingStock() != null) {
                var stock = definition.plantingStock();
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getOptional(net.minecraft.resources.Identifier.parse(stock.item()));
                var blockEntity = overworld.getBlockEntity(new BlockPos(xScore.value() + 2, 65, zScore.value()));
                if (item.isPresent() && blockEntity instanceof net.minecraft.world.Container container) {
                    int slot = container.getContainerSize() > 26 && container.getItem(26).isEmpty() ? 26 : -1;
                    if (slot < 0) {
                        for (int candidate = 0; candidate < container.getContainerSize(); candidate++) {
                            if (container.getItem(candidate).isEmpty()) { slot = candidate; break; }
                        }
                    }
                    if (slot >= 0) {
                        container.setItem(slot, new net.minecraft.world.item.ItemStack(item.get(), stock.count()));
                        container.setChanged();
                    }
                }
            }
        }
        PlayerTreeSelectionState.clear(player.getUUID());
    }

    private static void applyLegacyTrigger(ServerPlayNetworking.Context context, int triggerValue) {
        var scoreboard = context.server().getScoreboard();
        var objective = scoreboard.getObjective("sb_tree");
        if (objective != null) {
            scoreboard.getOrCreatePlayerScore(context.player(), objective).set(triggerValue);
        }
    }
}
