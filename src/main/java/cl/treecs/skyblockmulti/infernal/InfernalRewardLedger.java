package cl.treecs.skyblockmulti.infernal;

import cl.treecs.skyblockmulti.NexusBlocks;
import cl.treecs.skyblockmulti.api.party.PartyApi;
import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.literal;

/** Persistent, per-player rewards for the built-in Infernal Coliseum. */
public final class InfernalRewardLedger {
    private static final Set<String> COMPLETED = new LinkedHashSet<>();
    private static boolean loaded;
    private static final BlockPos VAULT_POS = new BlockPos(0, 64, 5);
    private static final String FILE_NAME = "skyblockmulti_infernal_rewards.properties";
    private static final Map<UUID, Deque<Reward>> PENDING = new LinkedHashMap<>();
    private static final Set<UUID> IN_FLIGHT = new LinkedHashSet<>();
    private static final Set<UUID> PARTICIPANTS = new LinkedHashSet<>();
    private static final Set<UUID> SCALED_ENEMIES = new LinkedHashSet<>();
    private static final Map<UUID, Boolean> ABANDON_VOTES = new LinkedHashMap<>();
    private static final Set<UUID> VOTE_ELECTORATE = new LinkedHashSet<>();
    private static final Set<UUID> DISTANCE_WARNED = new LinkedHashSet<>();
    private static final Map<UUID, Integer> LAST_DISTANCE_WARNING_TICK = new LinkedHashMap<>();
    private static final Map<UUID, Long> LAST_VAULT_INTERACTION_TICK = new LinkedHashMap<>();
    private static final int DISTANCE_WARNING_COOLDOWN_TICKS = 60 * 20;
    private static int abandonVoteTicks;
    private static long voteGeneration;
    private static int previousState;
    private static int previousPhase = -1;
    private static int ticker;
    private static double phaseHealthMaximum;

    private InfernalRewardLedger() {}

    public static void appendPanelEntries(ServerPlayer player,
            List<cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry> entries) {
        var rewards = PENDING.get(player.getUUID());
        if (rewards != null && !rewards.isEmpty()) entries.add(cl.treecs.skyblockmulti.SkyblockMultiMod.panelEntry(
                2, "rewards", Component.translatable("skyblockmulti.panel.rewards", rewards.size()), List.of()));
        boolean eligible = !player.isSpectator() && activeVoteParticipants(player.level().getServer()).contains(player.getUUID());
        if (!eligible) return;
        if (abandonVoteTicks > 0 && VOTE_ELECTORATE.contains(player.getUUID())) {
            boolean voted = ABANDON_VOTES.containsKey(player.getUUID());
            entries.add(cl.treecs.skyblockmulti.SkyblockMultiMod.panelEntry(2, "vote:" + voteGeneration,
                    Component.translatable(voted ? "skyblockmulti.panel.voted" : "skyblockmulti.panel.vote"), voted ? List.of() : List.of(
                            cl.treecs.skyblockmulti.SkyblockMultiMod.panelAction("abandon_yes", "skyblockmulti.panel.accept"),
                            cl.treecs.skyblockmulti.SkyblockMultiMod.panelAction("abandon_no", "skyblockmulti.panel.reject"))));
        } else entries.add(cl.treecs.skyblockmulti.SkyblockMultiMod.panelEntry(2, "trial",
                Component.translatable("skyblockmulti.infernal_trial.abandon.available"), List.of(
                        cl.treecs.skyblockmulti.SkyblockMultiMod.panelAction("abandon_request", "skyblockmulti.infernal_trial.abandon.button"))));
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            load(server);
            previousState = score(server, "#trial_state", 0);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(InfernalRewardLedger::save);
        ServerTickEvents.END_SERVER_TICK.register(InfernalRewardLedger::tick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("skyblockmulti").then(literal("infernal")
                        .then(literal("abandon")
                                .then(literal("request").executes(context -> requestAbandonVote(context.getSource())))
                                .then(literal("yes").executes(context -> vote(context.getSource(), true)))
                                .then(literal("no").executes(context -> vote(context.getSource(), false)))))));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND
                    || level.dimension() != Level.NETHER
                    || !hit.getBlockPos().equals(VAULT_POS)
                    || !level.getBlockState(VAULT_POS).is(NexusBlocks.INFERNAL_VAULT)) {
                return InteractionResult.PASS;
            }
            long interactionTick = level.getGameTime();
            Long previousTick = LAST_VAULT_INTERACTION_TICK.put(serverPlayer.getUUID(), interactionTick);
            if (previousTick != null && previousTick == interactionTick) {
                return InteractionResult.SUCCESS;
            }
            return claim(serverPlayer) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    private static void tick(MinecraftServer server) {
        int state = score(server, "#trial_state", 0);
        if (state == 1 && previousState != 1) captureParticipants(server);
        if (state == 4 && previousState >= 1 && previousState <= 3) complete(server);
        if (state == 0 && previousState >= 1 && previousState <= 3) clearRuntimeTrialState();
        previousState = state;

        if (state == 2) {
            int phase = score(server, "#phase", -1);
            if (phase != previousPhase) {
                phaseHealthMaximum = 0;
                announceAbandonAvailable(server, PARTICIPANTS);
                previousPhase = phase;
            }
            ServerLevel nether = server.getLevel(Level.NETHER);
            if (nether != null) warnDistantParticipants(server, nether, 0.5, 64, 0.5, 64, PARTICIPANTS);
        }

        if (abandonVoteTicks > 0) {
            abandonVoteTicks--;
            if (abandonVoteTicks == 0) finishVote(server, false);
        }
        if ((ticker++ % 5) == 0 && state == 2) {
            scaleNewEnemies(server);
            updateHealthBar(server);
        }
    }

    private static void captureParticipants(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return;
        List<ServerPlayer> nearby = nether.getPlayers(player -> !player.isSpectator()
                && player.distanceToSqr(0.5, 64, 0.5) <= 64 * 64);
        ServerPlayer activator = nearby.stream()
                .filter(player -> player.entityTags().contains("skyblock_infernal_activator"))
                .findFirst().orElse(nearby.isEmpty() ? null : nearby.getFirst());
        PARTICIPANTS.clear();
        if (activator == null) return;

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.removeTag("skyblock_infernal_participant");
        }

        PartySnapshot activatingParty = PartyApi.snapshot(activator);
        boolean providerPresent = PartyApi.hasExternalProvider();
        for (ServerPlayer candidate : nearby) {
            boolean eligible = InfernalParticipantPolicy.eligible(providerPresent,
                    activator.getUUID(), activatingParty, candidate.getUUID(),
                    providerPresent && activatingParty.inParty()
                            ? PartyApi.snapshot(candidate) : PartySnapshot.solo());
            candidate.removeTag("skyblock_infernal_participant");
            if (!eligible) continue;
            candidate.addTag("skyblock_infernal_participant");
            PARTICIPANTS.add(candidate.getUUID());
            command(server, "scoreboard players operation " + candidate.getScoreboardName()
                    + " sb_infernal_run = #run_id sb_infernal");
        }
        command(server, "scoreboard players set #participant_count sb_infernal " + PARTICIPANTS.size());
        SCALED_ENEMIES.clear();
        ABANDON_VOTES.clear();
        abandonVoteTicks = 0;
        previousPhase = -1;
        phaseHealthMaximum = 0;
    }

    static void announceAbandonAvailable(MinecraftServer server, Set<UUID> participants) {
        for (UUID participant : participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant);
            if (player != null) player.sendSystemMessage(Component.translatable(
                    "skyblockmulti.infernal_trial.abandon.available").append(" ")
                    .append(cl.treecs.skyblockmulti.IslandPanelService.openLink()));
        }
    }

    static void warnDistantParticipants(MinecraftServer server, ServerLevel arenaLevel,
                                        double x, double y, double z, int radius,
                                        Set<UUID> participants) {
        double radiusSquared = (double) radius * radius;
        for (UUID participant : participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant);
            if (player == null) continue;
            boolean distant = player.level() != arenaLevel || player.distanceToSqr(x, y, z) > radiusSquared;
            if (distant && DISTANCE_WARNED.add(participant)) {
                int lastWarning = LAST_DISTANCE_WARNING_TICK.getOrDefault(
                        participant, ticker - DISTANCE_WARNING_COOLDOWN_TICKS);
                if (ticker - lastWarning >= DISTANCE_WARNING_COOLDOWN_TICKS) {
                    announceAbandonAvailable(server, Set.of(participant));
                    LAST_DISTANCE_WARNING_TICK.put(participant, ticker);
                }
            } else if (!distant) {
                DISTANCE_WARNED.remove(participant);
            }
        }
    }

    static void clearDynamicTrialState() {
        ABANDON_VOTES.clear();
        VOTE_ELECTORATE.clear();
        DISTANCE_WARNED.clear();
        LAST_DISTANCE_WARNING_TICK.clear();
        abandonVoteTicks = 0;
        previousPhase = -1;
    }

    private static void complete(MinecraftServer server) {
        int level = score(server, "#trial_level", 1);
        int difficulty = score(server, "#trial_difficulty", 2);
        String completion = "builtin/" + score(server, "#run_id", 0);
        if (!COMPLETED.add(completion)) { clearRuntimeTrialState(); return; }
        for (UUID participant : PARTICIPANTS) {
            PENDING.computeIfAbsent(participant, ignored -> new ArrayDeque<>())
                    .addLast(new Reward(level, difficulty, cl.treecs.skyblockmulti.api.addon.AddonRewardApi.trialItems(
                            new cl.treecs.skyblockmulti.api.addon.AddonRewardApi.TrialContext(
                                    participant, "skyblockmulti:infernal_colosseum", completion, level, difficulty))));
        }
        clearRuntimeTrialState();
        save(server);
    }

    static void completeAddonTrial(MinecraftServer server, String completion, String trialId, Set<UUID> participants) {
        if (!COMPLETED.add(completion)) return;
        for (UUID participant : participants) {
            var items = cl.treecs.skyblockmulti.api.addon.AddonRewardApi.trialItems(
                    new cl.treecs.skyblockmulti.api.addon.AddonRewardApi.TrialContext(participant, trialId, completion, 1, 2));
            if (!items.isEmpty()) PENDING.computeIfAbsent(participant, ignored -> new ArrayDeque<>())
                    .addLast(new Reward(0, 2, items));
        }
        save(server);
    }

    private static int requestAbandonVote(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        Set<UUID> activeParticipants = activeVoteParticipants(source.getServer());
        if (!activeParticipants.contains(player.getUUID())) return 0;
        if (abandonVoteTicks > 0) {
            sendVotePrompt(source.getServer());
            return 1;
        }
        ABANDON_VOTES.clear();
        VOTE_ELECTORATE.clear();
        VOTE_ELECTORATE.addAll(activeParticipants);
        abandonVoteTicks = 60 * 20;
        voteGeneration++;
        sendVotePrompt(source.getServer());
        return 1;
    }

    private static int vote(CommandSourceStack source, boolean abandon) {
        if (!(source.getEntity() instanceof ServerPlayer player)
                || abandonVoteTicks <= 0 || !VOTE_ELECTORATE.contains(player.getUUID())) return 0;
        ABANDON_VOTES.put(player.getUUID(), abandon);
        broadcast(source.getServer(), VOTE_ELECTORATE, Component.translatable(
                "skyblockmulti.infernal_trial.abandon.vote_recorded", player.getDisplayName(),
                Component.translatable(abandon ? "skyblockmulti.common.yes" : "skyblockmulti.common.no")));
        if (yesVotes() > VOTE_ELECTORATE.size() / 2) finishVote(source.getServer(), true);
        return 1;
    }

    private static void sendVotePrompt(MinecraftServer server) {
        broadcast(server, VOTE_ELECTORATE, Component.translatable("skyblockmulti.infernal_trial.abandon.vote_prompt")
                .append(" ").append(cl.treecs.skyblockmulti.IslandPanelService.openLink()));
    }

    private static int yesVotes() {
        return (int) ABANDON_VOTES.values().stream().filter(Boolean::booleanValue).count();
    }

    private static void finishVote(MinecraftServer server, boolean approved) {
        if (approved) {
            broadcast(server, VOTE_ELECTORATE, Component.translatable(
                    "skyblockmulti.infernal_trial.abandon.approved", yesVotes(), VOTE_ELECTORATE.size()));
            if (builtInActive(server)) command(server, "function skyblockmulti:infernal_trial/abandon");
            else InfernalTrialRunner.abandonActive(server);
        } else {
            broadcast(server, VOTE_ELECTORATE, Component.translatable(
                    "skyblockmulti.infernal_trial.abandon.rejected", yesVotes(), VOTE_ELECTORATE.size()));
        }
        ABANDON_VOTES.clear();
        VOTE_ELECTORATE.clear();
        abandonVoteTicks = 0;
    }

    private static void broadcast(MinecraftServer server, Set<UUID> recipients, Component message) {
        for (UUID participant : recipients) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant);
            if (player != null) player.sendSystemMessage(message);
        }
    }

    private static void scaleNewEnemies(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return;
        int players = Math.max(1, Math.min(PARTICIPANTS.size(), 8));
        for (Entity entity : nether.getEntities(EntityTypeTest.forClass(Entity.class),
                candidate -> candidate.isAlive() && candidate.entityTags().contains("skyblock_infernal_enemy"))) {
            if (!(entity instanceof LivingEntity living) || !SCALED_ENEMIES.add(entity.getUUID())) continue;
            boolean champion = entity.entityTags().contains("skyblock_infernal_champion");
            double multiplier = 1.0 + (champion ? 0.175 : 0.10) * (players - 1);
            AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(health.getBaseValue() * multiplier);
                living.setHealth(living.getMaxHealth());
            }
        }
    }

    private static void clearRuntimeTrialState() {
        PARTICIPANTS.clear();
        SCALED_ENEMIES.clear();
        ABANDON_VOTES.clear();
        VOTE_ELECTORATE.clear();
        DISTANCE_WARNED.clear();
        LAST_DISTANCE_WARNING_TICK.clear();
        abandonVoteTicks = 0;
        previousPhase = -1;
    }

    private static Set<UUID> activeVoteParticipants(MinecraftServer server) {
        if (builtInActive(server)) return Set.copyOf(PARTICIPANTS);
        return InfernalTrialRunner.activeParticipants();
    }

    private static boolean builtInActive(MinecraftServer server) {
        int state = score(server, "#trial_state", 0);
        return state >= 1 && state <= 3;
    }

    private static boolean claim(ServerPlayer player) {
        if (!loaded || IN_FLIGHT.contains(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.infernal_trial.reward_review"));
            return true;
        }
        Deque<Reward> rewards = PENDING.get(player.getUUID());
        if (rewards == null || rewards.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "skyblockmulti.infernal_trial.no_pending_reward"));
            return true;
        }
        Reward reward = rewards.getFirst();
        // Keep the whole reward pending if an item provider is temporarily absent.
        if (reward.items().stream().anyMatch(item -> !net.minecraft.core.registries.BuiltInRegistries.ITEM
                .containsKey(net.minecraft.resources.Identifier.parse(item.item())))) return true;
        MinecraftServer server = player.level().getServer();
        // Persist intent while retaining the complete reward. An interrupted delivery
        // must never become automatically claimable a second time after reload.
        IN_FLIGHT.add(player.getUUID());
        try {
            save(server);
        } catch (RuntimeException error) {
            IN_FLIGHT.remove(player.getUUID());
            System.err.println("[Skyblock Multi] Reward not delivered: " + error);
            player.sendSystemMessage(Component.translatable("skyblockmulti.infernal_trial.reward_save_failed"));
            return true;
        }
        if (reward.level() > 0) {
        int previousLevel = score(server, "#trial_level", 1);
        int previousDifficulty = score(server, "#trial_difficulty", 2);
        command(server, "scoreboard players set #trial_level sb_infernal " + reward.level());
        command(server, "scoreboard players set #trial_difficulty sb_infernal " + reward.difficulty());
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(),
                "function skyblockmulti:infernal_trial/reward/completion_bonus");
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(),
                "function skyblockmulti:infernal_trial/reward/grant");
        command(server, "scoreboard players set #trial_level sb_infernal " + previousLevel);
        command(server, "scoreboard players set #trial_difficulty sb_infernal " + previousDifficulty);
        }
        for (var grant : reward.items()) {
            var stack = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getOptional(net.minecraft.resources.Identifier.parse(grant.item())).orElseThrow(), grant.count());
            if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        ((cl.treecs.skyblockmulti.mixin.PlayerListInvoker) server.getPlayerList()).skyblockmulti$savePlayer(player);
        rewards.removeFirst();
        if (rewards.isEmpty()) PENDING.remove(player.getUUID());
        IN_FLIGHT.remove(player.getUUID());
        try {
            save(server);
        } catch (RuntimeException error) {
            rewards.addFirst(reward);
            PENDING.put(player.getUUID(), rewards);
            IN_FLIGHT.add(player.getUUID());
            System.err.println("[Skyblock Multi] Reward delivery requires review for " + player.getUUID() + ": " + error);
            player.sendSystemMessage(Component.translatable("skyblockmulti.infernal_trial.reward_review"));
        }
        return true;
    }

    private static void updateHealthBar(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return;
        List<? extends Entity> enemies = nether.getEntities(EntityTypeTest.forClass(Entity.class),
                entity -> entity.isAlive() && entity.entityTags().contains("skyblock_infernal_enemy"));
        double current = 0;
        double maximum = 0;
        for (Entity enemy : enemies) {
            if (enemy instanceof LivingEntity living) {
                current += Math.max(0, living.getHealth());
                maximum += Math.max(1, living.getMaxHealth());
            }
        }
        // Defeated mobs disappear from the query, but their maximum health must
        // remain part of the phase baseline. Delayed reinforcements may expand it.
        phaseHealthMaximum = Math.max(phaseHealthMaximum, maximum);
        int max = Math.max(1, (int) Math.ceil(phaseHealthMaximum * 10));
        int value = Math.max(0, Math.min(max, (int) Math.ceil(current * 10)));
        command(server, "bossbar set skyblockmulti:infernal_trial max " + max);
        command(server, "bossbar set skyblockmulti:infernal_trial value " + value);
    }

    private static int score(MinecraftServer server, String holder, int fallback) {
        Objective objective = server.getScoreboard().getObjective("sb_infernal");
        if (objective == null) return fallback;
        ReadOnlyScoreInfo info = server.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), objective);
        return info == null ? fallback : info.value();
    }

    private static void command(MinecraftServer server, String value) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), value);
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static void load(MinecraftServer server) {
        loaded = false;
        PENDING.clear();
        IN_FLIGHT.clear();
        COMPLETED.clear();
        clearRuntimeTrialState();
        LAST_VAULT_INTERACTION_TICK.clear();
        Path path = path(server);
        if (!Files.isRegularFile(path)) { loaded = true; return; }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            for (String key : properties.stringPropertyNames()) {
                if (key.equals("@inflight")) {
                    for (String id : properties.getProperty(key).split(";")) if (!id.isBlank()) IN_FLIGHT.add(UUID.fromString(id));
                    continue;
                }
                if (key.equals("@completed")) {
                    for (String id : properties.getProperty(key).split(";")) if (!id.isBlank()) COMPLETED.add(id);
                    continue;
                }
                UUID uuid = UUID.fromString(key);
                Deque<Reward> rewards = new ArrayDeque<>();
                for (String encoded : properties.getProperty(key, "").split(";")) {
                    if (encoded.isBlank()) continue;
                    String[] parts = encoded.split(",", 3);
                    List<cl.treecs.skyblockmulti.api.addon.AddonRewardApi.ItemGrant> items = new ArrayList<>();
                    if (parts.length == 3 && !parts[2].isBlank()) for (String item : parts[2].split("\\|")) {
                        String[] grant = item.split("=");
                        items.add(new cl.treecs.skyblockmulti.api.addon.AddonRewardApi.ItemGrant(grant[0], Integer.parseInt(grant[1])));
                    }
                    rewards.addLast(new Reward(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), List.copyOf(items)));
                }
                if (!rewards.isEmpty()) PENDING.put(uuid, rewards);
            }
            loaded = true;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load personal rewards; preserving original file", exception);
        }
    }

    private static void save(MinecraftServer server) {
        if (!loaded) return;
        Properties properties = new Properties();
        properties.setProperty("@completed", String.join(";", COMPLETED));
        properties.setProperty("@inflight", IN_FLIGHT.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(";")));
        for (Map.Entry<UUID, Deque<Reward>> entry : PENDING.entrySet()) {
            List<String> encoded = new ArrayList<>();
            for (Reward reward : entry.getValue()) encoded.add(reward.level() + "," + reward.difficulty() + ","
                    + reward.items().stream().map(item -> item.item() + "=" + item.count()).collect(java.util.stream.Collectors.joining("|")));
            properties.setProperty(entry.getKey().toString(), String.join(";", encoded));
        }
        cl.treecs.skyblockmulti.addon.RewardFile.write(path(server), properties);
    }

    private record Reward(int level, int difficulty, List<cl.treecs.skyblockmulti.api.addon.AddonRewardApi.ItemGrant> items) {}
}
