package cl.treecs.skyblockmulti.infernal;

import com.google.gson.JsonParser;
import cl.treecs.skyblockmulti.api.party.PartyApi;
import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

/** Executes datapack-defined Infernal Trials without replacing the built-in I-X encounters. */
public final class InfernalTrialRunner {
    private static final String DYNAMIC_TAG = "skyblockmulti_dynamic_enemy";
    private static final String VICTORY_TAG = "skyblockmulti_dynamic_victory";
    private static final int ACTIVATION_INTERVAL = 5;
    private static final int VICTORY_CHECK_INTERVAL = 10;
    private static final int ABANDON_TICKS = 15 * 60 * 20;
    private static final AtomicInteger RUN_IDS = new AtomicInteger();
    private static final Map<String, Cooldown> COOLDOWNS = new HashMap<>();
    private static ActiveTrial active;
    private static int activationTicker;
    private static int spawnReleaseTicks;

    private InfernalTrialRunner() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(InfernalTrialRunner::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> setDynamicLock(server, false));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> resetRuntime());
    }

    public static boolean isSpawnLocked(ServerLevel level) {
        if (active != null && active.level == level) return true;
        return spawnReleaseTicks > 0 && level.dimension().identifier().toString().equals(lastArenaDimension);
    }

    static Set<UUID> activeParticipants() {
        return active == null ? Set.of() : Set.copyOf(active.participants);
    }

    static void abandonActive(MinecraftServer server) {
        if (active != null) finish(server, active, false);
    }

    private static String lastArenaDimension = "";

    private static void tick(MinecraftServer server) {
        tickCooldowns(server);
        if (spawnReleaseTicks > 0) spawnReleaseTicks--;

        if (active != null) {
            tickActive(server, active);
            return;
        }

        activationTicker = (activationTicker + 1) % ACTIVATION_INTERVAL;
        if (activationTicker != 0 || builtInTrialBusy(server)) return;
        detectActivation(server);
    }

    private static void detectActivation(MinecraftServer server) {
        InfernalDataRegistry.Snapshot snapshot = InfernalDataRegistry.snapshot();
        for (InfernalDefinitions.Trial trial : snapshot.trials().values()) {
            InfernalDefinitions.Arena arena = snapshot.arenas().get(trial.arena());
            if (arena == null || COOLDOWNS.containsKey(arena.id())) continue;
            ServerLevel level = level(server, arena.dimension());
            if (level == null || nearbyPlayers(level, arena, 4).isEmpty()) continue;
            ItemEntity catalyst = matchingCatalyst(level, arena, trial);
            if (catalyst == null) continue;

            InfernalDefinitions.Activation activation = matchingActivation(catalyst.getItem(), trial);
            if (activation == null) continue;
            if (activation.consume()) consumeOne(catalyst);
            begin(server, level, arena, trial);
            return;
        }
    }

    private static void begin(MinecraftServer server, ServerLevel level,
                              InfernalDefinitions.Arena arena,
                              InfernalDefinitions.Trial trial) {
        String runTag = "sb_dyn_" + RUN_IDS.incrementAndGet();
        Set<UUID> participants = eligibleParticipants(level, arena);
        active = new ActiveTrial(level, arena, trial, runTag,
                Stage.COUNTDOWN, -1, trial.countdownSeconds() * 20, 0, 0, participants);
        setDynamicLock(server, true);
        lastArenaDimension = arena.dimension();
        tell(server, arena, "[{\"text\":\"[Skyblock Multi] \",\"color\":\"dark_red\",\"bold\":true},"
                + display(trial.displayJson(), "Infernal Trial") + "]");
        title(server, arena, display(trial.displayJson(), "Infernal Trial"));
        if (trial.countdownSeconds() > 0) {
            configureCountdownPresentation(server, active);
        }
        System.out.println("[Skyblock Multi] API infernal: inició " + trial.id() + " con ejecución " + runTag + ".");
    }

    private static void tickActive(MinecraftServer server, ActiveTrial state) {
        InfernalRewardLedger.warnDistantParticipants(server, state.level,
                state.arena.origin().x() + 0.5, state.arena.origin().y(),
                state.arena.origin().z() + 0.5, state.arena.cleanupRadius(), state.participants);
        if (nearbyPlayers(state.level, state.arena, state.arena.cleanupRadius()).stream()
                .noneMatch(player -> state.participants.contains(player.getUUID()))) {
            state.noPlayerTicks++;
            if (state.noPlayerTicks >= ABANDON_TICKS) {
                finish(server, state, false);
                return;
            }
        } else {
            state.noPlayerTicks = 0;
        }

        if (state.stage == Stage.COUNTDOWN || state.stage == Stage.PHASE_DELAY) {
            if (state.stage == Stage.COUNTDOWN && !state.prepared
                    && state.ticksRemaining <= Math.min(5, state.trial.countdownSeconds()) * 20) {
                prepareArena(state);
            }
            if (state.ticksRemaining > 0 && state.ticksRemaining % 20 == 0) {
                showCountdown(server, state);
            }
            if (--state.ticksRemaining <= 0) startNextPhase(server, state);
            return;
        }

        if (state.stage != Stage.ACTIVE) return;
        state.phaseTicks++;
        var pending = state.pendingSpawns.iterator();
        while (pending.hasNext()) {
            var next = pending.next();
            if (next.tick() <= state.phaseTicks) {
                spawn(server, state, next.mob(), next.ordinal(), next.participants(), next.difficulty());
                pending.remove();
            }
        }
        state.checkTicker = (state.checkTicker + 1) % VICTORY_CHECK_INTERVAL;
        if (state.checkTicker != 0) return;
        int remaining = remainingEnemies(state);
        bossbar(server, state, remaining);
        if (remaining == 0 && state.pendingSpawns.isEmpty()) completePhase(server, state);
    }

    private static void startNextPhase(MinecraftServer server, ActiveTrial state) {
        state.phaseIndex++;
        InfernalDefinitions.Phase phase = state.trial.phases().get(state.phaseIndex);
        runFunction(server, state.arena, phase.arenaSetupFunction());
        int participants = Math.max(1, (int) nearbyPlayers(state.level, state.arena,
                state.arena.cleanupRadius()).stream()
                .filter(player -> state.participants.contains(player.getUUID())).count());
        String difficulty = difficulty(state);
        state.phaseTicks = 0;
        state.pendingSpawns.clear();
        state.phaseHealthMaximum = 0;
        state.phaseEnemyMaximum = 0;
        int spawned = 0;
        for (InfernalDefinitions.Mob mob : phase.mobs()) {
            int count = scaledCount(mob.count(), state.trial.scaling(), difficulty, participants);
            for (int index = 0; index < count; index++) {
                if (Math.random() <= mob.chance()) {
                    if (mob.countsForVictory()) {
                        state.phaseEnemyMaximum++;
                        state.phaseHealthMaximum += expectedHealth(mob, state.trial.scaling(), participants, difficulty);
                    }
                    int delay = index * mob.spawnDelayTicks();
                    if (delay == 0) spawn(server, state, mob, index, participants, difficulty);
                    else state.pendingSpawns.add(new PendingSpawn(mob, index, participants, difficulty, delay));
                    spawned++;
                }
            }
        }
        state.stage = Stage.ACTIVE;
        InfernalRewardLedger.announceAbandonAvailable(server, state.participants);
        int present = remainingEnemies(state);
        state.phaseEnemyMaximum = Math.max(1, state.phaseEnemyMaximum);
        tell(server, state.arena, "[{\"translate\":\"skyblockmulti.infernal_trial.phase\",\"color\":\"gold\"},"
                + "{\"text\":\" " + (state.phaseIndex + 1) + ": \",\"color\":\"gold\"},"
                + display(phase.displayJson(), phase.id()) + "]");
        title(server, state.arena,
                "{\"translate\":\"skyblockmulti.infernal_trial.combat_started\",\"color\":\"gold\",\"bold\":true}");
        bossbar(server, state, present);
        System.out.println("[Skyblock Multi] API infernal: fase " + (state.phaseIndex + 1)
                + " de " + state.trial.id() + ": " + spawned + " invocaciones solicitadas, "
                + present + " enemigos detectados.");
    }

    private static void completePhase(MinecraftServer server, ActiveTrial state) {
        InfernalDefinitions.Phase completed = state.trial.phases().get(state.phaseIndex);
        runFunction(server, state.arena, completed.arenaRestoreFunction());
        if (state.trial.rewards().phaseLootTable() != null) {
            reward(server, state, state.trial.rewards().phaseLootTable());
        }
        if (state.phaseIndex + 1 >= state.trial.phases().size()) {
            finish(server, state, true);
            return;
        }
        InfernalDefinitions.Phase next = state.trial.phases().get(state.phaseIndex + 1);
        state.stage = Stage.PHASE_DELAY;
        state.ticksRemaining = Math.max(1, next.delaySeconds() * 20);
        configureCountdownPresentation(server, state);
    }

    private static void finish(MinecraftServer server, ActiveTrial state, boolean completed) {
        if (completed) InfernalRewardLedger.completeAddonTrial(server, state.rewardId, state.trial.id(), state.participants);
        InfernalRewardLedger.clearDynamicTrialState();
        killTagged(server, state);
        runFunction(server, state.arena, state.arena.restoreFunction());
        if (completed && state.trial.rewards().completionLootTable() != null) {
            reward(server, state, state.trial.rewards().completionLootTable());
        }
        tell(server, state.arena, completed
                ? "{\"translate\":\"skyblockmulti.infernal_trial.completed\",\"color\":\"gold\",\"bold\":true}"
                : "{\"translate\":\"skyblockmulti.infernal_trial.abandoned\",\"color\":\"red\"}");
        if (state.trial.cooldownSeconds() > 0) {
            COOLDOWNS.put(state.arena.id(), new Cooldown(state.arena,
                    state.trial.cooldownSeconds() * 20));
            actionbar(server, state.arena,
                    "{\"translate\":\"skyblockmulti.infernal_trial.cooldown\",\"with\":[{\"text\":\""
                            + state.trial.cooldownSeconds() + "\"}],\"color\":\"gold\"}");
        }
        if (state.trial.cooldownSeconds() == 0) setDynamicLock(server, false);
        spawnReleaseTicks = state.trial.spawnReleaseSeconds() * 20;
        command(server, "bossbar remove skyblockmulti:dynamic_trial");
        active = null;
    }

    private static void prepareArena(ActiveTrial state) {
        double radiusSquared = (double) state.arena.cleanupRadius() * state.arena.cleanupRadius();
        if (state.trial.cleanupMobs()) {
            List<? extends Entity> monsters = state.level.getEntities(EntityTypeTest.forClass(Entity.class),
                    entity -> (entity instanceof Monster || entity instanceof AbstractCubeMob)
                            && !entity.entityTags().contains(state.runTag)
                            && entity.distanceToSqr(state.arena.origin().x() + 0.5,
                            state.arena.origin().y(), state.arena.origin().z() + 0.5) <= radiusSquared);
            monsters.forEach(InfernalTrialRunner::discardForCleanup);
        }
        if (state.trial.cleanupItems()) {
            List<? extends ItemEntity> items = state.level.getEntities(EntityTypeTest.forClass(ItemEntity.class),
                    entity -> entity.distanceToSqr(state.arena.origin().x() + 0.5,
                            state.arena.origin().y(), state.arena.origin().z() + 0.5) <= radiusSquared);
            items.forEach(Entity::discard);
        }
        state.prepared = true;
    }

    private static void showCountdown(MinecraftServer server, ActiveTrial state) {
        int seconds = Math.max(1, (state.ticksRemaining + 19) / 20);
        title(server, state.arena, "{\"text\":\"" + seconds + "\",\"color\":\"red\",\"bold\":true}");
        command(server, "execute in " + state.arena.dimension() + " positioned "
                + state.arena.origin().x() + " " + state.arena.origin().y() + " " + state.arena.origin().z()
                + " run playsound minecraft:block.note_block.hat master @a[distance=.."
                + state.arena.cleanupRadius() + "] ~ ~ ~ 0.8 1.0");
    }

    private static void configureCountdownPresentation(MinecraftServer server, ActiveTrial state) {
        String subtitle = state.stage == Stage.PHASE_DELAY
                ? "[{\"translate\":\"skyblockmulti.infernal_trial.phase\",\"color\":\"gold\"},"
                + "{\"text\":\" " + (state.phaseIndex + 2) + ": \",\"color\":\"gold\"},"
                + display(state.trial.phases().get(state.phaseIndex + 1).displayJson(),
                state.trial.phases().get(state.phaseIndex + 1).id()) + "]"
                : "{\"translate\":\"skyblockmulti.infernal_trial.reposition\",\"color\":\"gold\"}";
        command(server, "execute in " + state.arena.dimension() + " positioned "
                + state.arena.origin().x() + " " + state.arena.origin().y() + " " + state.arena.origin().z()
                + " run title @a[distance=.." + state.arena.cleanupRadius() + "] times 0 25 0");
        command(server, "execute in " + state.arena.dimension() + " positioned "
                + state.arena.origin().x() + " " + state.arena.origin().y() + " " + state.arena.origin().z()
                + " run title @a[distance=.." + state.arena.cleanupRadius() + "] subtitle " + subtitle);
    }

    private static void tickCooldowns(MinecraftServer server) {
        var iterator = COOLDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Cooldown cooldown = iterator.next().getValue();
            cooldown.ticks--;
            if (cooldown.ticks > 0 && cooldown.ticks % 20 == 0) {
                int seconds = (cooldown.ticks + 19) / 20;
                actionbar(server, cooldown.arena,
                        "{\"translate\":\"skyblockmulti.infernal_trial.cooldown\",\"with\":[{\"text\":\""
                                + seconds + "\"}],\"color\":\"gold\"}");
            }
            if (cooldown.ticks <= 0) {
                tell(server, cooldown.arena,
                        "{\"translate\":\"skyblockmulti.infernal_trial.ready\",\"color\":\"green\"}");
                iterator.remove();
                if (COOLDOWNS.isEmpty() && active == null) setDynamicLock(server, false);
            }
        }
    }

    private static void spawn(MinecraftServer server, ActiveTrial state,
                              InfernalDefinitions.Mob mob, int ordinal,
                              int participants, String difficulty) {
        String floor = mob.floors().get(Math.floorMod(ordinal, mob.floors().size()));
        List<InfernalDefinitions.Point> points = state.arena.floors().get(floor);
        InfernalDefinitions.Point point = switch (mob.placement()) {
            case "fixed" -> points.getFirst();
            case "round_robin" -> points.get(Math.floorMod(ordinal, points.size()));
            default -> points.get((int) (Math.random() * points.size()));
        };
        double x = state.arena.origin().x() + point.x() + 0.5;
        double y = state.arena.origin().y() + point.y();
        double z = state.arena.origin().z() + point.z() + 0.5;
        String spawnTag = state.runTag + "_new";
        List<String> tags = new ArrayList<>(List.of(DYNAMIC_TAG, state.runTag, spawnTag));
        if (mob.champion()) tags.add("skyblockmulti_dynamic_champion");
        if (mob.countsForVictory()) tags.add(VICTORY_TAG);
        if (mob.childrenCountForVictory()) tags.add("skyblockmulti_dynamic_children");
        String tagNbt = tags.stream().map(tag -> "\"" + tag + "\"").reduce((a, b) -> a + "," + b).orElse("");
        String name = mob.nameJson() == null ? "" : ",CustomName:" + mob.nameJson() + ",CustomNameVisible:1b";
        command(server, "execute in " + state.arena.dimension() + " run summon " + mob.entity() + " "
                + x + " " + y + " " + z + " {Tags:[" + tagNbt + "],PersistenceRequired:1b" + name + "}");
        configureSpawned(server, state, mob, spawnTag, participants, difficulty);
    }

    private static void configureSpawned(MinecraftServer server, ActiveTrial state,
                                         InfernalDefinitions.Mob mob, String spawnTag,
                                         int participants, String difficulty) {
        InfernalDefinitions.Scaling scaling = state.trial.scaling();
        InfernalDefinitions.DifficultyScaling difficultyScaling = scaling.difficulties().get(difficulty);
        double difficultyHealth = difficultyScaling == null ? 1 : difficultyScaling.healthMultiplier();
        double difficultyDamage = difficultyScaling == null ? 1 : difficultyScaling.damageMultiplier();
        double participantHealth = participantHealthMultiplier(mob.champion(), participants, scaling);
        List<? extends Entity> entities = state.level.getEntities(EntityTypeTest.forClass(Entity.class),
                entity -> entity.entityTags().contains(spawnTag));
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                multiply(living, Attributes.MAX_HEALTH,
                        mob.healthMultiplier() * difficultyHealth * participantHealth);
                living.setHealth(living.getMaxHealth());
                multiply(living, Attributes.ATTACK_DAMAGE,
                        mob.damageMultiplier() * difficultyDamage);
                set(living, Attributes.KNOCKBACK_RESISTANCE, mob.knockbackResistance());
                set(living, Attributes.SCALE, mob.scale());
            }
        }
        for (Map.Entry<String, InfernalDefinitions.Equipment> entry : mob.equipment().entrySet()) {
            InfernalDefinitions.Equipment equipment = entry.getValue();
            command(server, "execute in " + state.arena.dimension() + " run item replace entity @e[tag="
                    + state.runTag + ",tag=" + spawnTag + ",limit=1] " + slot(entry.getKey()) + " with "
                    + equipment.item() + " " + equipment.count());
        }
        for (InfernalDefinitions.Effect effect : mob.effects()) {
            command(server, "execute in " + state.arena.dimension() + " run effect give @e[tag="
                    + state.runTag + ",tag=" + spawnTag + ",limit=1] " + effect.id() + " "
                    + Math.max(1, effect.durationTicks() / 20) + " " + effect.amplifier() + " "
                    + (!effect.particles()));
        }
        command(server, "execute in " + state.arena.dimension() + " run tag @e[tag=" + spawnTag
                + "] remove " + spawnTag);
    }

    private static void multiply(LivingEntity entity,
                                 net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                 double multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(instance.getBaseValue() * multiplier);
    }

    @SuppressWarnings("unchecked")
    private static double expectedHealth(InfernalDefinitions.Mob mob, InfernalDefinitions.Scaling scaling,
                                         int participants, String difficulty) {
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(mob.entity()));
        var attributes = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(
                (net.minecraft.world.entity.EntityType<? extends LivingEntity>) type);
        if (attributes == null || !attributes.hasAttribute(Attributes.MAX_HEALTH)) return 0;
        var difficultyScale = scaling.difficulties().get(difficulty);
        return attributes.getBaseValue(Attributes.MAX_HEALTH) * mob.healthMultiplier()
                * (difficultyScale == null ? 1 : difficultyScale.healthMultiplier())
                * participantHealthMultiplier(mob.champion(), participants, scaling);
    }

    static double participantHealthMultiplier(boolean champion, int participants,
                                              InfernalDefinitions.Scaling scaling) {
        int counted = Math.max(1, Math.min(participants, scaling.maximumParticipants()));
        double perExtraPlayer = scaling.healthPerExtraPlayer() * (champion ? 1.75 : 1.0);
        return 1.0 + perExtraPlayer * (counted - 1);
    }

    private static void set(LivingEntity entity,
                            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                            double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private static int remainingEnemies(ActiveTrial state) {
        return state.level.getEntities(EntityTypeTest.forClass(Entity.class),
                entity -> (entity.isAlive() || entity instanceof AbstractCubeMob && !entity.isRemoved())
                        && entity.entityTags().contains(state.runTag)
                        && entity.entityTags().contains(VICTORY_TAG)).size();
    }

    private static ItemEntity matchingCatalyst(ServerLevel level, InfernalDefinitions.Arena arena,
                                               InfernalDefinitions.Trial trial) {
        return level.getEntities(EntityTypeTest.forClass(ItemEntity.class), entity ->
                        entity.distanceToSqr(arena.origin().x() + 0.5, arena.origin().y() + 0.2,
                                arena.origin().z() + 0.5) <= 2.25
                                && matchingActivation(entity.getItem(), trial) != null)
                .stream().findFirst().orElse(null);
    }

    private static InfernalDefinitions.Activation matchingActivation(ItemStack stack,
                                                                      InfernalDefinitions.Trial trial) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (InfernalDefinitions.Activation activation : trial.activations()) {
            if (!itemId.toString().equals(activation.item())) continue;
            if (activation.customDataJson() == null) return activation;
            try {
                CompoundTag expected = TagParser.parseCompoundFully(activation.customDataJson());
                CustomData actual = stack.get(DataComponents.CUSTOM_DATA);
                if (actual != null && actual.matchedBy(expected)) return activation;
            } catch (Exception exception) {
                System.err.println("[Skyblock Multi] Activación infernal inválida para " + trial.id()
                        + ": " + exception.getMessage());
            }
        }
        return null;
    }

    private static void consumeOne(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) entity.discard();
        else entity.setItem(stack);
    }

    private static int scaledCount(int base, InfernalDefinitions.Scaling scaling,
                                   String difficulty, int participants) {
        InfernalDefinitions.DifficultyScaling difficultyScaling = scaling.difficulties().get(difficulty);
        double difficultyMultiplier = difficultyScaling == null ? 1 : difficultyScaling.countMultiplier();
        int extras = Math.max(0, Math.min(participants, scaling.maximumParticipants()) - 1)
                * scaling.extraMobsPerPlayer();
        int calculated = (int) Math.round(base * difficultyMultiplier) + extras;
        return Math.max(1, Math.min(calculated, (int) Math.ceil(base * scaling.maximumCountMultiplier())));
    }

    private static String difficulty(ActiveTrial state) {
        ServerLevel level = state.level;
        InfernalDefinitions.Arena arena = state.arena;
        Objective objective = level.getScoreboard().getObjective("sb_chest");
        if (objective == null) return "standard";
        return nearbyPlayers(level, arena, arena.cleanupRadius()).stream()
                .filter(player -> state.participants.contains(player.getUUID())).findFirst()
                .map(player -> {
                    ReadOnlyScoreInfo score = level.getScoreboard().getPlayerScoreInfo(player, objective);
                    int value = score == null ? 2 : score.value();
                    return switch (value) {
                        case 0 -> "extreme";
                        case 1 -> "hard";
                        case 3 -> "easy";
                        default -> "standard";
                    };
                }).orElse("standard");
    }

    private static List<ServerPlayer> nearbyPlayers(ServerLevel level, InfernalDefinitions.Arena arena, int radius) {
        double radiusSquared = (double) radius * radius;
        return level.getPlayers(player -> !player.isSpectator()
                && player.distanceToSqr(arena.origin().x() + 0.5, arena.origin().y(),
                arena.origin().z() + 0.5) <= radiusSquared);
    }

    private static Set<UUID> eligibleParticipants(ServerLevel level, InfernalDefinitions.Arena arena) {
        List<ServerPlayer> nearby = nearbyPlayers(level, arena, arena.cleanupRadius());
        if (nearby.isEmpty() || !PartyApi.hasExternalProvider()) {
            return nearby.stream().map(ServerPlayer::getUUID).collect(Collectors.toUnmodifiableSet());
        }
        ServerPlayer activator = nearbyPlayers(level, arena, 4).stream().findFirst().orElse(nearby.getFirst());
        PartySnapshot party = PartyApi.snapshot(activator);
        return nearby.stream().filter(player -> InfernalParticipantPolicy.eligible(true,
                activator.getUUID(), party, player.getUUID(),
                party.inParty() ? PartyApi.snapshot(player) : PartySnapshot.solo()))
                .map(ServerPlayer::getUUID).collect(Collectors.toUnmodifiableSet());
    }

    private static boolean builtInTrialBusy(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Objective objective = level.getScoreboard().getObjective("sb_infernal");
            if (objective == null) continue;
            ReadOnlyScoreInfo state = level.getScoreboard().getPlayerScoreInfo(
                    ScoreHolder.forNameOnly("#trial_state"), objective);
            if (state != null && state.value() >= 1 && state.value() <= 3) return true;
            ReadOnlyScoreInfo cooldown = level.getScoreboard().getPlayerScoreInfo(
                    ScoreHolder.forNameOnly("#cooldown"), objective);
            if (cooldown != null && cooldown.value() > 0) return true;
        }
        return false;
    }

    private static void setDynamicLock(MinecraftServer server, boolean locked) {
        command(server, "scoreboard players set #dynamic_trial_lock sb_infernal " + (locked ? 1 : 0));
    }

    private static ServerLevel level(MinecraftServer server, String dimension) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
        return server.getLevel(key);
    }

    private static void runFunction(MinecraftServer server, InfernalDefinitions.Arena arena, String function) {
        if (function != null) command(server, "execute in " + arena.dimension() + " positioned "
                + arena.origin().x() + " " + arena.origin().y() + " " + arena.origin().z()
                + " run function " + function);
    }

    private static void reward(MinecraftServer server, ActiveTrial state, String lootTable) {
        for (UUID participant : state.participants) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant);
            if (player != null) {
                server.getCommands().performPrefixedCommand(
                        player.createCommandSourceStack().withSuppressedOutput(),
                        "loot give @s loot " + lootTable);
            }
        }
    }

    private static void killTagged(MinecraftServer server, ActiveTrial state) {
        state.pendingSpawns.clear();
        state.level.getEntities(EntityTypeTest.forClass(Entity.class),
                entity -> entity.entityTags().contains(state.runTag)).forEach(InfernalTrialRunner::discardForCleanup);
    }

    private static void discardForCleanup(Entity entity) {
        entity.addTag("skyblockmulti_cleanup");
        entity.discard();
    }

    private static void tell(MinecraftServer server, InfernalDefinitions.Arena arena, String json) {
        command(server, "execute in " + arena.dimension() + " positioned " + arena.origin().x() + " "
                + arena.origin().y() + " " + arena.origin().z() + " run tellraw @a[distance=.."
                + arena.cleanupRadius() + "] " + json);
    }

    private static void title(MinecraftServer server, InfernalDefinitions.Arena arena, String json) {
        command(server, "execute in " + arena.dimension() + " positioned " + arena.origin().x() + " "
                + arena.origin().y() + " " + arena.origin().z() + " run title @a[distance=.."
                + arena.cleanupRadius() + "] title " + json);
    }

    private static void actionbar(MinecraftServer server, InfernalDefinitions.Arena arena, String json) {
        command(server, "execute in " + arena.dimension() + " positioned " + arena.origin().x() + " "
                + arena.origin().y() + " " + arena.origin().z() + " run title @a[distance=.."
                + arena.cleanupRadius() + "] actionbar " + json);
    }

    private static void bossbar(MinecraftServer server, ActiveTrial state, int remaining) {
        double pendingHealth = state.pendingSpawns.stream().filter(next -> next.mob().countsForVictory())
                .mapToDouble(next -> expectedHealth(next.mob(), state.trial.scaling(), next.participants(), next.difficulty())).sum();
        double currentHealth = pendingHealth;
        double maximumHealth = pendingHealth;
        List<? extends Entity> tracked = state.level.getEntities(EntityTypeTest.forClass(Entity.class),
                entity -> entity.isAlive() && entity.entityTags().contains(state.runTag)
                        && entity.entityTags().contains(VICTORY_TAG));
        for (Entity entity : tracked) {
            if (entity instanceof LivingEntity living) {
                currentHealth += Math.max(0, living.getHealth());
                maximumHealth += Math.max(1, living.getMaxHealth());
            }
        }
        // Keep defeated mobs in the phase maximum instead of redefining the
        // surviving group as a new 100% baseline.
        state.phaseHealthMaximum = Math.max(state.phaseHealthMaximum, maximumHealth);
        int healthMaximum = Math.max(1, (int) Math.ceil(state.phaseHealthMaximum * 10));
        int healthValue = Math.max(0, Math.min(healthMaximum, (int) Math.ceil(currentHealth * 10)));
        command(server, "bossbar add skyblockmulti:dynamic_trial "
                + display(state.trial.displayJson(), "Infernal Trial"));
        command(server, "bossbar set skyblockmulti:dynamic_trial name "
                + "[" + display(state.trial.displayJson(), "Infernal Trial")
                + ",{\"text\":\" — " + remaining + "/" + state.phaseEnemyMaximum
                + "\",\"color\":\"white\"}]");
        command(server, "bossbar set skyblockmulti:dynamic_trial max " + healthMaximum);
        command(server, "bossbar set skyblockmulti:dynamic_trial value " + healthValue);
        command(server, "execute in " + state.arena.dimension() + " positioned " + state.arena.origin().x() + " "
                + state.arena.origin().y() + " " + state.arena.origin().z()
                + " run bossbar set skyblockmulti:dynamic_trial players @a[distance=.."
                + state.arena.cleanupRadius() + "]");
    }

    private static String display(String json, String fallback) {
        if (json == null) return "{\"text\":\"" + fallback + "\"}";
        try {
            JsonParser.parseString(json);
            return json;
        } catch (Exception ignored) {
            return "{\"text\":\"" + fallback + "\"}";
        }
    }

    private static String slot(String configured) {
        return switch (configured) {
            case "mainhand" -> "weapon.mainhand";
            case "offhand" -> "weapon.offhand";
            case "head" -> "armor.head";
            case "chest" -> "armor.chest";
            case "legs" -> "armor.legs";
            case "feet" -> "armor.feet";
            default -> configured;
        };
    }

    private static void command(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    private static void resetRuntime() {
        active = null;
        COOLDOWNS.clear();
        activationTicker = 0;
        spawnReleaseTicks = 0;
        lastArenaDimension = "";
    }

    private enum Stage { COUNTDOWN, PHASE_DELAY, ACTIVE }

    private record PendingSpawn(InfernalDefinitions.Mob mob, int ordinal, int participants,
                                String difficulty, int tick) { }

    private static final class Cooldown {
        private final InfernalDefinitions.Arena arena;
        private int ticks;

        private Cooldown(InfernalDefinitions.Arena arena, int ticks) {
            this.arena = arena;
            this.ticks = ticks;
        }
    }

    private static final class ActiveTrial {
        private final ServerLevel level;
        private final InfernalDefinitions.Arena arena;
        private final InfernalDefinitions.Trial trial;
        private final String runTag;
        private final String rewardId = "datapack/" + UUID.randomUUID();
        private final Set<UUID> participants;
        private Stage stage;
        private int phaseIndex;
        private int ticksRemaining;
        private int noPlayerTicks;
        private int checkTicker;
        private int phaseEnemyMaximum = 1;
        private double phaseHealthMaximum;
        private boolean prepared;
        private int phaseTicks;
        private final List<PendingSpawn> pendingSpawns = new ArrayList<>();

        private ActiveTrial(ServerLevel level, InfernalDefinitions.Arena arena,
                            InfernalDefinitions.Trial trial,
                            String runTag, Stage stage, int phaseIndex, int ticksRemaining,
                            int noPlayerTicks, int checkTicker, Set<UUID> participants) {
            this.level = level;
            this.arena = arena;
            this.trial = trial;
            this.runTag = runTag;
            this.stage = stage;
            this.phaseIndex = phaseIndex;
            this.ticksRemaining = ticksRemaining;
            this.noPlayerTicks = noPlayerTicks;
            this.checkTicker = checkTicker;
            this.participants = participants;
        }
    }
}
