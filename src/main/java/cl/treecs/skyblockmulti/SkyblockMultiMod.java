package cl.treecs.skyblockmulti;

import cl.treecs.skyblockmulti.tree.TreeCatalog;
import cl.treecs.skyblockmulti.tree.TreeConfigCodec;
import cl.treecs.skyblockmulti.tree.TreeConfigStorage;
import cl.treecs.skyblockmulti.tree.TreeDefinition;
import cl.treecs.skyblockmulti.tree.TreeDefinitionReloadListener;
import cl.treecs.skyblockmulti.tree.PlayerTreeSelectionState;
import cl.treecs.skyblockmulti.infernal.InfernalDefinitionReloadListener;
import cl.treecs.skyblockmulti.infernal.InfernalTrialRunner;
import cl.treecs.skyblockmulti.infernal.InfernalRewardLedger;
import cl.treecs.skyblockmulti.addon.AddonFeatureConfig;
import cl.treecs.skyblockmulti.island.IslandLayoutConfiguration;
import cl.treecs.skyblockmulti.island.IslandRingLayout;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cl.treecs.skyblockmulti.compatibility.openpac.OpenPacCompat;
import cl.treecs.skyblockmulti.compatibility.openpac.OpenPacPartyProvider;
import cl.treecs.skyblockmulti.compatibility.nexusparties.NexusPartiesPartyProvider;
import cl.treecs.skyblockmulti.api.party.PartyApi;
import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import cl.treecs.skyblockmulti.api.party.PartyHomeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.util.HashMap;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class SkyblockMultiMod implements ModInitializer {
    public static final String MOD_ID = "skyblockmulti";

    /**
     * Geometría del mapa SkyblockMulti.
     * La distancia configurada pasa a representar el RADIO desde el HUB (0,0),
     * no la separación entre islas.
     */
    public static final int DEFAULT_RADIUS = 2048;
    public static final int[] RADIUS_OPTIONS = {2048, 4096, 8192, 16384};
    private static final int MAX_CONFIGURED_SLOTS = IslandRingLayout.TECHNICAL_LIMIT;
    public static final int DEFAULT_CAPACITY = 24;
    private static final int PARTY_CHECK_INTERVAL_TICKS = 20;
    private static final int LAYOUT_VERSION = 3;

    private static final Pattern RADIUS_PATTERN = Pattern.compile("(?:\\\"islandRadius\\\"\\s*:|^\\s*radius\\s*=)\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern CAPACITY_PATTERN = Pattern.compile("(?:\\\"islandCapacity\\\"\\s*:|^\\s*capacity\\s*=)\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ENABLED_RINGS_PATTERN = Pattern.compile("(?:\\\"enabledIslandRings\\\"\\s*:|^\\s*enabled_rings\\s*=)\\s*\\[([^]]*)]", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern ALLOCATION_POLICY_PATTERN = Pattern.compile("(?:\\\"islandAllocationPolicy\\\"\\s*:\\s*\\\"|^\\s*allocation_policy\\s*=\\s*\\\")([a-z_]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern TECHNICAL_CAPACITY_PATTERN = Pattern.compile("(?:\\\"technicalIslandCapacity\\\"\\s*:|^\\s*technical_capacity\\s*=)\\s*(true|false)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern BONUS_CHEST_MODE_PATTERN = Pattern.compile("(?:\\\"bonusChestMode\\\"\\s*:\\s*\\\"|^\\s*minimum\\s*=\\s*\\\")([a-z_]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern PARTY_LEAVE_DIFFICULTY_PATTERN = Pattern.compile("(?:\\\"partyLeaveDifficultyMode\\\"\\s*:\\s*\\\"|^\\s*after_leaving_party\\s*=\\s*\\\")([a-z_]+)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern END_PORTAL_EYES_PATTERN = Pattern.compile("(?:\\\"endPortalEyesPercent\\\"\\s*:|^\\s*initial_eyes_percent\\s*=)\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern PARTY_HOME_COOLDOWN_PATTERN = Pattern.compile("^\\s*island_change_cooldown_hours\\s*=\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final int DEFAULT_PARTY_HOME_COOLDOWN_HOURS = 24;
    private static final Pattern LEGACY_BONUS_CHEST_ENABLED_PATTERN = Pattern.compile("\\\"bonusChestEnabled\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_BONUS_CHEST_TIER_PATTERN = Pattern.compile("\\\"bonusChestTier\\\"\\s*:\\s*\\\"([a-z_]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static Path configPath;
    private static volatile Object activeServer;
    private static volatile boolean worldGeometryLocked = false;
    private static volatile int activeWorldRadius = -1;
    private static volatile int activeWorldCapacity = -1;
    private static volatile IslandLayoutConfiguration activeWorldLayout;
	
	// Estado conocido del proveedor de parties activo.
	private static final Map<UUID, PartyState> PARTY_STATES = new HashMap<>();
	private static int partyCheckTicks = 0;
    private static int geometryLockCheckTicks = 0;
    private static AutoCloseable nexusPartyEvents;
	private record PartyState(
        String providerId,
        boolean inParty,
        UUID partyId,
        UUID ownerUuid
) {
    private static PartyState from(String providerId, PartySnapshot info) {
        return new PartyState(
                providerId,
                info.inParty(),
                info.partyId(),
                info.ownerUuid()
        );
    }
}

    public enum TreeOption {
        OAK("minecraft:oak"),
        SPRUCE("minecraft:spruce"),
        BIRCH("minecraft:birch"),
        JUNGLE("minecraft:jungle"),
        ACACIA("minecraft:acacia"),
        CHERRY("minecraft:cherry"),
        MANGROVE("minecraft:mangrove"),
        DARK_OAK("minecraft:dark_oak"),
        PALE_OAK("minecraft:pale_oak"),
        AZALEA("minecraft:azalea"),
        FLOWERING_AZALEA("minecraft:flowering_azalea");

        private final TreeDefinition definition;

        TreeOption(String id) {
            this.definition = TreeCatalog.find(id)
                    .orElseThrow(() -> new IllegalArgumentException("Árbol integrado desconocido: " + id));
        }

        public TreeDefinition definition() {
            return definition;
        }

        public String id() {
            return definition.id();
        }

        public String configKey() {
            return definition.configKey();
        }

        public int triggerValue() {
            return definition.legacyTriggerValue();
        }

        public String scoreHolder() {
            return definition.legacyScoreHolder();
        }
    }


    public enum BonusChestMode {
        EMPTY("empty", 0),
        BASIC("basic", 1),
        STANDARD("standard", 2),
        BEGINNER("beginner", 3);

        private final String configKey;
        private final int scoreValue;

        BonusChestMode(String configKey, int scoreValue) {
            this.configKey = configKey;
            this.scoreValue = scoreValue;
        }

        public String configKey() {
            return configKey;
        }

        public int scoreValue() {
            return scoreValue;
        }

        public BonusChestMode next() {
            return switch (this) {
                case EMPTY -> BASIC;
                case BASIC -> STANDARD;
                case STANDARD -> BEGINNER;
                case BEGINNER -> EMPTY;
            };
        }

        public static BonusChestMode fromConfig(String value) {
            if (value != null) {
                for (BonusChestMode mode : values()) {
                    if (mode.configKey.equalsIgnoreCase(value.trim())) return mode;
                }
            }
            return BEGINNER;
        }
    }

    @Override
    public void onInitialize() {
        configPath = ConfigPaths.general();
        migrateLegacyGeneralConfig();
        try {
            TreeConfigStorage.initialize(configPath);
        } catch (IOException error) {
            System.err.println("[Skyblock Multi] No se pudo preparar la configuración de árboles: "
                    + error.getMessage());
        }
        ensureConfigExists();
        ensurePartyCooldownConfig();
        VanillaRecipeConfig.initialize();
        try {
            ensureServerConfigReadme();
        } catch (IOException error) {
            System.err.println("[Skyblock Multi] No se pudo crear el README de configuración: "
                    + error.getMessage());
        }
        AddonFeatureConfig.initialize();
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "tree_options"),
                new TreeDefinitionReloadListener()
        );
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "infernal_trials"),
                new InfernalDefinitionReloadListener()
        );
        NexusItems.initialize();
        NexusBlocks.initialize();
        NexusCreativeTab.register();
        InfernalTrialSpawnControl.register();
        InfernalTrialRunner.register();
        InfernalRewardLedger.register();
        cl.treecs.skyblockmulti.addon.StarterRewardService.register();
        OpenPacCompat.initialize();
        if (FabricLoader.getInstance().isModLoaded("nexus_parties")
                && NexusPartiesPartyProvider.isCompatible()) {
            PartyApi.registerProvider(new NexusPartiesPartyProvider());
            nexusPartyEvents = NexusPartiesPartyProvider.registerEvents(event -> {
                if (cl.treecs.skyblockmulti.api.party.IslandTransferApi.coordinated(event)) return;
                PartyHomeRegistry.onEvent(event, System.currentTimeMillis());
                Object currentServer = activeServer;
                if (currentServer instanceof MinecraftServer server) {
                    server.execute(() -> {
                        if (event.type() == cl.treecs.skyblockmulti.api.party.PartyChangeEvent.Type.OWNER_CHANGED) {
                            sendPartyHomeDecision(server, event.partyId(), event.actorId(), event.subjectId());
                        }
                        if (event.type() == cl.treecs.skyblockmulti.api.party.PartyChangeEvent.Type.MEMBER_JOINED) {
                            ServerPlayer joined = server.getPlayerList().getPlayer(event.subjectId());
                            if (joined != null) {
                                setPartyIslandPreference(server, joined, -1);
                                activateOwnIsland(server, joined, false);
                                PartySnapshot joinedSnapshot = PartyApi.snapshot(joined);
                                sendPartyOwnershipNotice(joined);
                                sendPartyIslandChoice(server, joined, joinedSnapshot);
                                PARTY_STATES.put(joined.getUUID(),
                                        PartyState.from(PartyApi.activeProviderId(), joinedSnapshot));
                            }
                        } else if (event.type() == cl.treecs.skyblockmulti.api.party.PartyChangeEvent.Type.MEMBER_LEFT) {
                            ServerPlayer departed = server.getPlayerList().getPlayer(event.subjectId());
                            if (departed != null) {
                                PartySnapshot current = PartyApi.snapshot(departed);
                                reconcileActiveIsland(server, departed, current, false);
                                returnPlayerAfterLeavingParty(server, departed);
                                PARTY_STATES.put(departed.getUUID(),
                                        PartyState.from(PartyApi.activeProviderId(), current));
                            }
                        } else if (event.type() == cl.treecs.skyblockmulti.api.party.PartyChangeEvent.Type.DISBANDED) {
                            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                                PartyState previous = PARTY_STATES.get(online.getUUID());
                                if (previous != null && event.partyId().equals(previous.partyId())) {
                                    PartySnapshot current = PartyApi.snapshot(online);
                                    reconcileActiveIsland(server, online, current, false);
                                    returnPlayerAfterLeavingParty(server, online);
                                    PARTY_STATES.put(online.getUUID(),
                                            PartyState.from(PartyApi.activeProviderId(), current));
                                }
                            }
                        }
                    });
                }
            });
        }
        PartyApi.registerProvider(new OpenPacPartyProvider());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("skyblockmulti")
                            .then(
                                    literal("openpac_claim")
                                            .then(
                                                    argument("x", IntegerArgumentType.integer())
                                                            .then(
                                                                    argument("z", IntegerArgumentType.integer())
                                                                            .executes(context -> {
                                                                                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                                                                                    return 0;
                                                                                }

                                                                                // La primera isla asignada fija y persiste la geometría de este mundo.
                                                                                recoverAndPersistWorldGeometryLock(
                                                                                        context.getSource().getServer(),
                                                                                        player
                                                                                );

                                                                                int x = IntegerArgumentType.getInteger(context, "x");
                                                                                int z = IntegerArgumentType.getInteger(context, "z");

                                                                                notifyPartyHomeAvailable(
                                                                                        context.getSource().getServer(),
                                                                                        player
                                                                                );

                                                                                return 1;
                                                                            })
                                                            )
                                            )
                            )
            );
            dispatcher.register(literal("skyblockmulti")
                    .then(literal("partyhome")
                            .then(literal("keep").executes(context ->
                                    resolvePartyHomeDecision(context.getSource(), false)))
                            .then(literal("move").executes(context ->
                                    resolvePartyHomeDecision(context.getSource(), true)))));
            dispatcher.register(literal("skyblockmulti")
                    .then(literal("partyisland")
                            .then(literal("own").executes(context ->
                                    choosePersonalIsland(context.getSource())))
                            .then(literal("party").executes(context ->
                                    choosePartyIsland(context.getSource())))));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;

            try {
                if (scoreValue(server, player, "sb3_state") == 2) {
                    recoverAndPersistWorldGeometryLock(server, player);
                }
            } catch (Exception ignored) {
                // En un mundo nuevo los objetivos pueden no estar listos durante los primeros instantes.
            }

            PartySnapshot partyInfo = PartyApi.snapshot(player);

            boolean personalHomeResolved = resolveDepartedPersonalHome(server, player, partyInfo);
            if (partyInfo.inParty()) sendPartyOwnershipNotice(player);
            if (personalHomeResolved) {
                // Persistent ACTIVE/preferences already reflect the fallback destination.
            } else if (PartyApi.hasExternalProvider() && partyInfo.inParty() && !partyInfo.owner()) {
                Integer preference = optionalScoreValue(server, player, "sb_party_pref");
                if (preference != null && preference == 1) {
                    reconcileActiveIsland(server, player, partyInfo, false);
                } else if (preference == null || preference == -1) {
                    setPartyIslandPreference(server, player, -1);
                    activateOwnIsland(server, player, false);
                    sendPartyIslandChoice(server, player, partyInfo);
                } else {
                    activateOwnIsland(server, player, false);
                }
            } else if (partyInfo.inParty() && !player.getUUID().equals(PartyHomeRegistry.homeOwner(partyInfo))
                    && !hasOwnIsland(server, player)) {
                // A leader can remain a guest of the retained PARTY_HOME.
                reconcileActiveIsland(server, player, partyInfo, false);
            } else {
                activateOwnIsland(server, player, false);
            }

            if (PartyApi.hasExternalProvider()) {
                PARTY_STATES.put(
                        player.getUUID(),
                        PartyState.from(PartyApi.activeProviderId(), partyInfo)
                );
            }

            if (!PartyApi.hasExternalProvider()) {
                return;
            }

            if (!partyInfo.inParty()) {
                System.out.println(
                        "[Skyblock Multi] Party provider " + PartyApi.activeProviderId() + ": "
                                + player.getGameProfile().name()
                                + " no pertenece a una party."
                );
                return;
            }

            if (partyInfo.owner()) {
                System.out.println(
                        "[Skyblock Multi] Party provider " + PartyApi.activeProviderId() + ": "
                                + player.getGameProfile().name()
                                + " es owner de la party "
                                + partyInfo.partyId()
                                + "."
                );
            } else {
                System.out.println(
                        "[Skyblock Multi] Party provider " + PartyApi.activeProviderId() + ": "
                                + player.getGameProfile().name()
                                + " pertenece a la party "
                                + partyInfo.partyId()
                                + ". Owner: "
                                + partyInfo.ownerName()
                                + "."
                );
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PARTY_STATES.remove(handler.player.getUUID());
            PlayerTreeSelectionState.clear(handler.player.getUUID());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            geometryLockCheckTicks++;
            if (!worldGeometryLocked && geometryLockCheckTicks >= 20) {
                geometryLockCheckTicks = 0;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    try {
                        int state = scoreValue(server, player, "sb3_state");
                        int slot = scoreValue(server, player, "sb3_slot");
                        if (state == 2 && slot >= 1 && slot <= MAX_CONFIGURED_SLOTS) {
                            recoverAndPersistWorldGeometryLock(server, player);
                            break;
                        }
                    } catch (Exception ignored) {
                        // Los objetivos pueden no existir durante los primeros ticks de un mundo nuevo.
                    }
                }
            }

            if (!PartyApi.hasExternalProvider()) {
                return;
            }

            partyCheckTicks++;

            // Comprobar cambios reales de party una vez por segundo.
            if (partyCheckTicks < PARTY_CHECK_INTERVAL_TICKS) {
                return;
            }

            partyCheckTicks = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPartyChange(server, player);
            }
            expirePartyHomeDecisions(server);
        });

        registerServerStartedEvent();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            OpenPacCompat.prepareWorldClaims(server);
        });

        // El bloqueo es persistente DENTRO de cada mundo. Al cerrar el servidor integrado,
        // limpiamos solo el estado runtime para que Mod Menu vuelva a mostrar los valores
        // predeterminados destinados a mundos nuevos. Al cargar un mundo existente,
        // applyConfiguration() restaura su geometría persistida.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            cl.treecs.skyblockmulti.api.party.IslandTransferApi.close();
            activeServer = null;
            worldGeometryLocked = false;
            activeWorldRadius = -1;
            activeWorldCapacity = -1;
            activeWorldLayout = null;
            geometryLockCheckTicks = 0;
            PARTY_STATES.clear();
            PartyHomeRegistry.clearRuntime();
            PlayerTreeSelectionState.clearAll();
        });

        String modVersion = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        System.out.println("[Skyblock Multi] Mod " + modVersion + " inicializado. Configuración: " + configPath);
    }

    public static int getConfiguredRadius() {
        if (activeServer != null && activeWorldRadius > 0) {
            return activeWorldRadius;
        }
        return loadConfig().radius();
    }

    /** Alias conservado para código antiguo: ahora devuelve el radio desde el HUB. */
    public static int getConfiguredDistance() {
        return getConfiguredRadius();
    }

    public static EnumMap<TreeOption, Boolean> getConfiguredTreeStates() {
        EnumMap<TreeOption, Boolean> legacyStates = new EnumMap<>(TreeOption.class);
        Map<String, Boolean> states = loadConfig().trees();
        for (TreeOption tree : TreeOption.values()) {
            legacyStates.put(tree, Boolean.TRUE.equals(states.get(tree.id())));
        }
        return legacyStates;
    }

    public static Map<String, Boolean> getConfiguredTreeStatesById() {
        return new LinkedHashMap<>(loadConfig().trees());
    }

    public static int getPlayerCapacity(int ignoredDistance) {
        return MAX_CONFIGURED_SLOTS;
    }

    public static int getConfiguredCapacity() {
        if (activeServer != null && activeWorldCapacity > 0) {
            return activeWorldCapacity;
        }
        return loadConfig().capacity();
    }

    public static boolean isOpenPacInstalled() {
        return OpenPacCompat.isTerritorialIntegrationEnabled();
    }

    public static boolean isWorldGeometryLocked() {
        return worldGeometryLocked;
    }

    public static BonusChestMode getConfiguredBonusChestMode() {
        return loadConfig().bonusChestMode();
    }

    public static BonusChestMode getConfiguredPartyLeaveDifficultyMode() {
        return loadConfig().partyLeaveDifficultyMode();
    }

    public static IslandLayoutConfiguration getConfiguredIslandLayout() {
        if (activeServer != null && activeWorldLayout != null) return activeWorldLayout;
        return loadConfig().layout();
    }

    public static boolean saveConfiguredRadius(int requestedRadius) {
        ConfigData current = loadConfig();
        return saveConfigurationByTreeId(
                requestedRadius,
                current.capacity(),
                current.trees(),
                current.bonusChestMode(),
                current.partyLeaveDifficultyMode()
        );
    }

    public static boolean saveConfiguredDistance(int requestedDistance) {
        return saveConfiguredRadius(requestedDistance);
    }

    public static boolean saveConfiguration(int requestedRadius, Map<TreeOption, Boolean> requestedTrees) {
        ConfigData current = loadConfig();
        return saveConfigurationByTreeId(
                requestedRadius,
                current.capacity(),
                legacyTreeStatesToIds(requestedTrees),
                current.bonusChestMode(),
                current.partyLeaveDifficultyMode()
        );
    }

    public static boolean saveConfiguration(int requestedRadius, Map<TreeOption, Boolean> requestedTrees,
                                            BonusChestMode bonusChestMode) {
        ConfigData current = loadConfig();
        return saveConfigurationByTreeId(
                requestedRadius,
                current.capacity(),
                legacyTreeStatesToIds(requestedTrees),
                bonusChestMode,
                current.partyLeaveDifficultyMode()
        );
    }

    public static boolean saveConfiguration(int requestedRadius, int requestedCapacity,
                                            Map<TreeOption, Boolean> requestedTrees,
                                            BonusChestMode bonusChestMode) {
        ConfigData current = loadConfig();
        return saveConfigurationByTreeId(
                requestedRadius,
                requestedCapacity,
                legacyTreeStatesToIds(requestedTrees),
                bonusChestMode,
                current.partyLeaveDifficultyMode()
        );
    }

    public static boolean saveConfiguration(int requestedRadius, int requestedCapacity,
                                            Map<TreeOption, Boolean> requestedTrees,
                                            BonusChestMode bonusChestMode,
                                            BonusChestMode partyLeaveDifficultyMode) {
        return saveConfigurationByTreeId(
                requestedRadius,
                requestedCapacity,
                legacyTreeStatesToIds(requestedTrees),
                bonusChestMode,
                partyLeaveDifficultyMode
        );
    }

    public static boolean saveConfigurationByTreeId(int requestedRadius, int requestedCapacity,
                                                     Map<String, Boolean> requestedTrees,
                                                     BonusChestMode bonusChestMode,
                                                     BonusChestMode partyLeaveDifficultyMode) {
        return saveConfigurationByTreeId(requestedRadius, requestedCapacity, requestedTrees,
                bonusChestMode, partyLeaveDifficultyMode, loadConfig().layout());
    }

    public static boolean saveConfigurationByTreeId(int requestedRadius, int requestedCapacity,
                                                     Map<String, Boolean> requestedTrees,
                                                     BonusChestMode bonusChestMode,
                                                     BonusChestMode partyLeaveDifficultyMode,
                                                     IslandLayoutConfiguration requestedLayout) {
        ensureConfigReady();
        ConfigData current = loadConfig();
        IslandLayoutConfiguration layout = requestedLayout == null
                ? IslandLayoutConfiguration.custom(normalizeRadius(requestedRadius), requestedCapacity,
                        current.layout().enabledRings(), current.layout().allocationPolicy(),
                        current.layout().technicalCapacity())
                : requestedLayout;
        int normalizedCapacity = layout.capacity();
        int normalizedRadius = normalizeRadius(requestedRadius);

        int lockedCapacity = activeWorldCapacity > 0 ? activeWorldCapacity : current.capacity();
        int lockedRadius = activeWorldRadius > 0 ? activeWorldRadius : current.radius();

        IslandLayoutConfiguration lockedLayout = activeWorldLayout != null
                ? activeWorldLayout : current.layout();
        if (worldGeometryLocked
                && (normalizedCapacity != lockedCapacity || normalizedRadius != lockedRadius
                || !layout.equals(lockedLayout))) {
            System.err.println(
                    "[Skyblock Multi] Radio/capacidad bloqueados: el mundo ya tiene una isla asignada."
            );
            return false;
        }

        Map<String, Boolean> trees = normalizeTrees(requestedTrees);
        BonusChestMode safeMode = bonusChestMode == null ? BonusChestMode.BEGINNER : bonusChestMode;
        BonusChestMode safePartyLeaveMode = partyLeaveDifficultyMode == null
                ? BonusChestMode.BEGINNER
                : partyLeaveDifficultyMode;

        // Si estamos dentro de un mundo ya bloqueado, modificar árboles/dificultades
        // no debe reemplazar los valores predeterminados de geometría para mundos nuevos.
        int radiusToWrite = worldGeometryLocked ? current.radius() : normalizedRadius;
        int capacityToWrite = worldGeometryLocked ? current.capacity() : normalizedCapacity;

        try {
            IslandLayoutConfiguration requestedNormalizedLayout = IslandLayoutConfiguration.custom(
                    radiusToWrite, capacityToWrite, layout.enabledRings(),
                    layout.allocationPolicy(), layout.technicalCapacity());
            IslandLayoutConfiguration layoutToWrite = worldGeometryLocked
                    ? current.layout() : requestedNormalizedLayout;
            writeConfig(radiusToWrite, capacityToWrite, trees, safeMode, safePartyLeaveMode,
                    layoutToWrite);
            System.out.println("[Skyblock Multi] Configuración guardada: radio_hub=" + normalizedRadius
                    + ", capacidad=" + normalizedCapacity
                    + ", árboles=" + countEnabled(trees)
                    + ", cofre=" + safeMode.configKey()
                    + ", salida_party=" + safePartyLeaveMode.configKey());
            Object server = activeServer;
            if (server != null) {
                applyConfiguration(server);
            }
            return true;
        } catch (IOException e) {
            System.err.println("[Skyblock Multi] No se pudo guardar la configuración: " + e.getMessage());
            return false;
        }
    }

    public static int normalizeRadius(int value) {
        int best = RADIUS_OPTIONS[0];
        long bestDelta = Math.abs((long) value - best);
        for (int option : RADIUS_OPTIONS) {
            long delta = Math.abs((long) value - option);
            if (delta < bestDelta) {
                best = option;
                bestDelta = delta;
            }
        }
        return best;
    }

    private static void ensureConfigReady() {
        if (configPath == null) {
            configPath = ConfigPaths.general();
            migrateLegacyGeneralConfig();
        }
        ensureConfigExists();
    }

    private static void ensureConfigExists() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.notExists(configPath)) {
                writeConfig(
                        DEFAULT_RADIUS,
                        DEFAULT_CAPACITY,
                        defaultTrees(),
                        BonusChestMode.BEGINNER,
                        BonusChestMode.BEGINNER,
                        IslandLayoutConfiguration.defaults(DEFAULT_RADIUS)
                );
            }
        } catch (IOException e) {
            System.err.println("[Skyblock Multi] No se pudo crear el archivo de configuración: " + e.getMessage());
        }
    }

    private static void writeConfig(int radius, int capacity, Map<String, Boolean> trees,
                                    BonusChestMode bonusChestMode,
                                    BonusChestMode partyLeaveDifficultyMode,
                                    IslandLayoutConfiguration layout) throws IOException {
        StringBuilder toml = new StringBuilder("""
                # Skyblock Multi - General Server Configuration
                # Edit this file only while the server is stopped.
                # Island geometry is locked for a world after its first island assignment.

                [islands]
                """);
        toml.append("# Total island slots. It cannot exceed enabled ring positions.\n");
        toml.append("capacity = ").append(capacity).append("\n\n");
        toml.append("""
                # Base radius in blocks (2048, 4096, 8192 or 16384).
                # This is Ring 2's radius:
                # Ring 1=50%, Ring 2=100%, Ring 3=150%, Ring 4=200%, Ring 5=250%.
                radius = """).append(radius).append("\n\n");
        toml.append("# Enter ring numbers only. Example: enabled_rings = [2, 3, 4]\n");
        toml.append("# Available positions: Ring 1=8, Ring 2=16, Ring 3=24, Ring 4=32, Ring 5=48.\n");
        toml.append("# Ring 5 requires technical_capacity=true.\n");
        toml.append("enabled_rings = [");
        var rings = layout.enabledRings().stream().sorted().toList();
        for (int i = 0; i < rings.size(); i++) {
            if (i > 0) toml.append(", ");
            toml.append(rings.get(i).number());
        }
        toml.append("]\n\n");
        toml.append("# difficulty | balanced | random_balanced | inner_first | outer_first\n");
        toml.append("allocation_policy = \"")
                .append(layout.allocationPolicy().name().toLowerCase(Locale.ROOT)).append("\"\n\n");
        toml.append("# Enables Ring 5 and raises the limit from 80 to 128.\n");
        toml.append("technical_capacity = ").append(layout.technicalCapacity()).append("\n\n");
        toml.append("""
                [difficulty]
                # empty | basic | standard | beginner
                minimum = """).append('"').append(bonusChestMode.configKey()).append("\"\n");
        toml.append("after_leaving_party = \"")
                .append(partyLeaveDifficultyMode.configKey()).append("\"\n\n");
        toml.append("[difficulty.crafting]\nvanilla_recipes_enabled = ")
                .append(VanillaRecipeConfig.enabled()).append("\n\n");
        toml.append("""
                [parties]
                # Minimum real-world hours between effective PARTY_HOME changes.
                # Leadership can still transfer, but the shared island remains unchanged during cooldown.
                # Use 0 to disable the cooldown.
                island_change_cooldown_hours = """)
                .append(readConfiguredPartyHomeCooldownHours()).append("\n\n");
        toml.append("""
                [end_portal]
                # Initial filled frames: 0 | 25 | 50 | 75 percent.
                initial_eyes_percent = """).append(readPersistedEndPortalEyes()).append("\n");
        TreeConfigStorage.save(trees);
        Files.writeString(configPath, toml.toString(), StandardCharsets.UTF_8);
    }

    private static void ensurePartyCooldownConfig() {
        if (configPath == null || Files.notExists(configPath)) return;
        try {
            String raw = Files.readString(configPath, StandardCharsets.UTF_8);
            if (PARTY_HOME_COOLDOWN_PATTERN.matcher(raw).find()) return;
            Files.writeString(configPath, raw.stripTrailing() + """


                    [parties]
                    # Minimum real-world hours between effective PARTY_HOME changes.
                    # Leadership can still transfer, but the shared island remains unchanged during cooldown.
                    # Use 0 to disable the cooldown.
                    island_change_cooldown_hours = 24
                    """, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Could not add the party-island cooldown setting", error);
        }
    }

    private static void ensureServerConfigReadme() throws IOException {
        Path readme = ConfigPaths.directory().resolve("README.md");
        if (Files.exists(readme)) return;
        Files.writeString(readme, """
                # Skyblock Multi server configuration

                Edit configuration only while the server is stopped. World geometry is locked
                after the first island is assigned, so later changes apply only to new worlds.

                - `general.toml`: island layout, difficulty and initial End Portal eyes.
                - `trees/vanilla.toml`: vanilla tree availability.
                - `trees/<namespace>.toml`: tree availability supplied by an addon or datapack.
                - `addons/<addon>.toml`: optional addon features.

                ## Party island cooldown

                `[parties].island_change_cooldown_hours` limits how often a leadership
                transfer may change the shared `PARTY_HOME`. The default is 24 real-world
                hours. Leadership may still transfer during the cooldown, but the current
                shared island is retained. Set it to `0` to disable this restriction.

                ## Island rings

                `enabled_rings` contains ring numbers, not their capacities. For example,
                `enabled_rings = [2, 3, 4]` enables Rings 2, 3 and 4.

                | Ring | Positions | Radius |
                | ---: | --------: | --------------------: |
                | 1 | 8  | 50% of `radius`  |
                | 2 | 16 | 100% of `radius` |
                | 3 | 24 | 150% of `radius` |
                | 4 | 32 | 200% of `radius` |
                | 5 | 48 | 250% of `radius` |

                Ring 5 requires `technical_capacity = true`. The `capacity` cannot
                exceed the combined positions of `enabled_rings`; an invalid configuration stops
                startup instead of silently changing the requested geometry.

                ## Radius presets

                - Configurable rings: 2048, 4096, 8192, 16384

                Example: `enabled_rings = [2, 3, 4]` enables Rings 2, 3 and 4.
                Enter ring numbers only; the capacity numbers listed above are informational.
                """, StandardCharsets.UTF_8);
    }

    private static int readPersistedEndPortalEyes() {
        if (configPath == null || Files.notExists(configPath)) return NexusFoundation.DEFAULT_END_EYES;
        try {
            Matcher matcher = END_PORTAL_EYES_PATTERN.matcher(
                    Files.readString(configPath, StandardCharsets.UTF_8));
            if (matcher.find()) return NexusFoundation.normalizeConfiguredEndEyes(
                    Integer.parseInt(matcher.group(1)));
        } catch (Exception ignored) {
            // A later validated write restores the safe default.
        }
        return NexusFoundation.DEFAULT_END_EYES;
    }

    public static int getConfiguredPartyHomeCooldownHours() {
        ensureConfigReady();
        return readConfiguredPartyHomeCooldownHours();
    }

    public static boolean savePartyHomeCooldownHours(int hours) {
        ensureConfigReady();
        int normalized = Math.max(0, Math.min(8760, hours));
        try {
            String raw = Files.readString(configPath, StandardCharsets.UTF_8);
            Matcher matcher = PARTY_HOME_COOLDOWN_PATTERN.matcher(raw);
            String updated = matcher.find()
                    ? matcher.replaceFirst("island_change_cooldown_hours = " + normalized)
                    : raw.stripTrailing() + "\n\n[parties]\nisland_change_cooldown_hours = "
                            + normalized + "\n";
            Files.writeString(configPath, updated, StandardCharsets.UTF_8);
            return true;
        } catch (IOException error) {
            System.err.println("[Skyblock Multi] Could not save party-island cooldown: "
                    + error.getMessage());
            return false;
        }
    }

    private static int readConfiguredPartyHomeCooldownHours() {
        if (configPath == null || Files.notExists(configPath)) {
            return DEFAULT_PARTY_HOME_COOLDOWN_HOURS;
        }
        try {
            Matcher matcher = PARTY_HOME_COOLDOWN_PATTERN.matcher(
                    Files.readString(configPath, StandardCharsets.UTF_8));
            if (matcher.find()) return Math.max(0, Integer.parseInt(matcher.group(1)));
        } catch (Exception ignored) {
            // A later validated write restores the safe default.
        }
        return DEFAULT_PARTY_HOME_COOLDOWN_HOURS;
    }

    private static ConfigData loadConfig() {
        ensureConfigReady();
        int radius = DEFAULT_RADIUS;
        int capacity = DEFAULT_CAPACITY;
        Map<String, Boolean> trees = defaultTrees();
        BonusChestMode bonusChestMode = BonusChestMode.BEGINNER;
        BonusChestMode partyLeaveDifficultyMode = BonusChestMode.BEGINNER;
        IslandLayoutConfiguration layout = IslandLayoutConfiguration.defaults(radius);
        try {
            String raw = Files.readString(configPath, StandardCharsets.UTF_8);

            radius = matchInt(RADIUS_PATTERN, raw, DEFAULT_RADIUS);
            capacity = matchInt(CAPACITY_PATTERN, raw, DEFAULT_CAPACITY);

            Matcher modeMatcher = BONUS_CHEST_MODE_PATTERN.matcher(raw);
            if (modeMatcher.find()) {
                bonusChestMode = BonusChestMode.fromConfig(modeMatcher.group(1));
            } else {
                boolean legacyEnabled = true;
                Matcher enabledMatcher = LEGACY_BONUS_CHEST_ENABLED_PATTERN.matcher(raw);
                if (enabledMatcher.find()) {
                    legacyEnabled = Boolean.parseBoolean(enabledMatcher.group(1));
                }
                if (!legacyEnabled) {
                    bonusChestMode = BonusChestMode.EMPTY;
                } else {
                    Matcher tierMatcher = LEGACY_BONUS_CHEST_TIER_PATTERN.matcher(raw);
                    if (tierMatcher.find()) {
                        bonusChestMode = BonusChestMode.fromConfig(tierMatcher.group(1));
                    }
                }
            }

            Matcher partyLeaveMatcher = PARTY_LEAVE_DIFFICULTY_PATTERN.matcher(raw);
            if (partyLeaveMatcher.find()) {
                partyLeaveDifficultyMode = BonusChestMode.fromConfig(partyLeaveMatcher.group(1));
            }

            layout = parseIslandLayout(raw, radius, capacity);

            trees = TreeConfigStorage.load();
        } catch (Exception e) {
            throw new IllegalStateException("Configuración general inválida en " + configPath
                    + ": " + e.getMessage(), e);
        }

        return new ConfigData(
                normalizeRadius(radius),
                capacity,
                normalizeTrees(trees),
                bonusChestMode,
                partyLeaveDifficultyMode,
                layout
        );
    }

    private static void migrateLegacyGeneralConfig() {
        if (configPath == null || Files.exists(configPath)) return;
        Path legacy = Files.exists(ConfigPaths.legacyJsonGeneral())
                ? ConfigPaths.legacyJsonGeneral() : ConfigPaths.legacyGeneral();
        if (Files.notExists(legacy)) return;
        try {
            Files.createDirectories(configPath.getParent());
            String raw = Files.readString(legacy, StandardCharsets.UTF_8);
            IslandLayoutConfiguration migratedLayout = readLegacyLayout(raw);
            Path backup = legacy.resolveSibling(legacy.getFileName() + ".pre-toml.bak");
            if (Files.notExists(backup)) Files.copy(legacy, backup);
            TreeConfigStorage.initialize(legacy);
            int radius = migratedLayout.baseRadius();
            int capacity = migratedLayout.capacity();
            BonusChestMode minimum = matchMode(BONUS_CHEST_MODE_PATTERN, raw);
            BonusChestMode afterParty = matchMode(PARTY_LEAVE_DIFFICULTY_PATTERN, raw);
            int eyes = matchInt(END_PORTAL_EYES_PATTERN, raw, NexusFoundation.DEFAULT_END_EYES);
            Files.writeString(configPath, "[end_portal]\ninitial_eyes_percent = " + eyes + "\n",
                    StandardCharsets.UTF_8);
            writeConfig(radius, capacity, TreeConfigStorage.load(), minimum, afterParty,
                    migratedLayout);
            Files.deleteIfExists(legacy);
            System.out.println("[Skyblock Multi] Configuración JSON migrada a " + configPath);
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo migrar la configuración general desde "
                    + legacy + ": " + error.getMessage(), error);
        }
    }

    private static int matchInt(Pattern pattern, String raw, int fallback) {
        Matcher matcher = pattern.matcher(raw);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    static IslandLayoutConfiguration readLegacyLayout(String raw) {
        int radius = matchInt(RADIUS_PATTERN, raw, DEFAULT_RADIUS);
        int capacity = matchInt(CAPACITY_PATTERN, raw, DEFAULT_CAPACITY);
        if (normalizeRadius(radius) != radius) {
            throw new IllegalArgumentException("Unsupported legacy island radius: " + radius);
        }
        if (!ENABLED_RINGS_PATTERN.matcher(raw).find()) {
            // Pre-ring JSON layouts used the original inner/outer ring geometry.
            return IslandLayoutConfiguration.custom(radius, capacity,
                    EnumSet.of(IslandRingLayout.Ring.INNER, IslandRingLayout.Ring.OUTER),
                    IslandRingLayout.AllocationPolicy.DIFFICULTY, false);
        }
        return parseIslandLayout(raw, radius, capacity);
    }

    private static BonusChestMode matchMode(Pattern pattern, String raw) {
        Matcher matcher = pattern.matcher(raw);
        return matcher.find() ? BonusChestMode.fromConfig(matcher.group(1)) : BonusChestMode.BEGINNER;
    }

    private static IslandLayoutConfiguration parseIslandLayout(String raw, int radius, int capacity) {
        EnumSet<IslandRingLayout.Ring> rings = EnumSet.noneOf(IslandRingLayout.Ring.class);
        Matcher ringsMatcher = ENABLED_RINGS_PATTERN.matcher(raw);
        if (ringsMatcher.find()) {
            for (String value : ringsMatcher.group(1).split(",")) {
                if (value.isBlank()) continue;
                int number = Integer.parseInt(value.trim());
                for (IslandRingLayout.Ring ring : IslandRingLayout.Ring.values()) {
                    if (ring.number() == number) rings.add(ring);
                }
            }
        }

        IslandRingLayout.AllocationPolicy policy = IslandRingLayout.AllocationPolicy.RANDOM_BALANCED;
        Matcher policyMatcher = ALLOCATION_POLICY_PATTERN.matcher(raw);
        if (policyMatcher.find()) {
            try {
                policy = IslandRingLayout.AllocationPolicy.valueOf(
                        policyMatcher.group(1).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Unknown future values retain the safe default.
            }
        }
        Matcher technicalMatcher = TECHNICAL_CAPACITY_PATTERN.matcher(raw);
        boolean technical = technicalMatcher.find() && Boolean.parseBoolean(technicalMatcher.group(1));
        IslandLayoutConfiguration configured = IslandLayoutConfiguration.custom(
                normalizeRadius(radius), capacity, rings, policy, technical);
        try {
            configured.positions();
        } catch (IllegalArgumentException error) {
            int available = rings.stream().mapToInt(IslandRingLayout.Ring::positions).sum();
            throw new IllegalArgumentException("capacity=" + capacity + " but enabled_rings="
                    + rings.stream().map(IslandRingLayout.Ring::number).sorted().toList()
                    + " provide only " + available + " positions", error);
        }
        return configured;
    }

    private static Map<String, Boolean> defaultTrees() {
        return TreeConfigCodec.defaultStates();
    }

    private static Map<String, Boolean> normalizeTrees(Map<String, Boolean> requested) {
        return TreeConfigCodec.normalize(requested);
    }

    private static int countEnabled(Map<String, Boolean> trees) {
        int count = 0;
        for (TreeDefinition tree : TreeCatalog.builtIns()) {
            if (Boolean.TRUE.equals(trees.get(tree.id()))) count++;
        }
        return count;
    }

    private static Map<String, Boolean> legacyTreeStatesToIds(Map<TreeOption, Boolean> requested) {
        if (requested == null) return null;
        Map<String, Boolean> trees = new LinkedHashMap<>();
        requested.forEach((tree, enabled) -> trees.put(tree.id(), Boolean.TRUE.equals(enabled)));
        return trees;
    }

    /** Read the existing world state without assigning, moving or creating islands. */
    public static java.util.List<cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry> islandPanelEntries(ServerPlayer player) {
        var server = player.level().getServer();
        var entries = new java.util.ArrayList<cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry>();
        boolean own = hasOwnIsland(server, player);
        int state = scoreValue(server, player, "sb3_state");
        Integer activeX = optionalScoreValue(server, player, "sb_active_x");
        Integer activeZ = optionalScoreValue(server, player, "sb_active_z");
        Component activeText = activeX != null && activeZ != null
                ? Component.translatable("skyblockmulti.panel.coordinates", activeX, activeZ)
                : Component.translatable("skyblockmulti.panel.none");
        var rows = new java.util.ArrayList<cl.treecs.skyblockmulti.network.IslandPanelPayload.Row>();
        if (own) {
            int slot = scoreValue(server, player, "sb3_slot");
            int ring = IslandRingLayout.ringForSlot(slot).map(IslandRingLayout.Ring::number).orElse(0);
            rows.add(panelRow("number", Component.literal(Integer.toString(slot))));
            rows.add(panelRow("ring", Component.literal(Integer.toString(ring))));
            rows.add(panelRow("tree", RequiredClientMod.islandTreeName(slot)));
            int tier = scoreValue(server, player, "sb_chest");
            String[] difficulties = {"extreme", "hard", "standard", "easy"};
            if (tier >= 0 && tier < difficulties.length) rows.add(panelRow("difficulty",
                    Component.translatable("skyblockmulti.panel.difficulty." + difficulties[tier])));
        } else rows.add(panelRow("number", Component.translatable("skyblockmulti.panel.none")));
        int milestones = cl.treecs.skyblockmulti.island.IslandProgressScore.completedMilestones(player);
        rows.add(panelRow("score", Component.literal(Integer.toString(cl.treecs.skyblockmulti.island.IslandProgressScore.points(milestones)))));
        var body = Component.translatable("skyblockmulti.panel.score_basis", milestones,
                cl.treecs.skyblockmulti.island.IslandProgressScore.POINTS_PER_MILESTONE);
        var selection = state == 1 || state == 4;
        if (selection) body.append("\n\n").append(Component.translatable("skyblockmulti.panel.selection_pending"));
        entries.add(new cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry(0, "island:" + state, body,
                selection ? java.util.List.of(panelAction("selection", "skyblockmulti.panel.continue")) : java.util.List.of(), rows));
        if (activeX != null && activeZ != null && (!own || activeX != scoreValue(server, player, "sb3_x")
                || activeZ != scoreValue(server, player, "sb3_z"))) entries.add(panelEntry(0, "active",
                        Component.translatable("skyblockmulti.panel.active", activeText), java.util.List.of()));

        PartySnapshot party = PartyApi.snapshot(player);
        if (party.inParty()) {
            UUID home = PartyHomeRegistry.peekHomeOwner(party);
            ServerPlayer homePlayer = home == null ? null : server.getPlayerList().getPlayer(home);
            Component homeName = homePlayer != null ? homePlayer.getDisplayName()
                    : home == null ? Component.translatable("skyblockmulti.panel.none")
                    : Component.literal(server.services().nameToIdCache().get(home).map(profile -> profile.name())
                            .orElse(home.equals(party.ownerUuid()) ? party.ownerName() : home.toString()));
            entries.add(new cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry(1, party.partyId().toString(), Component.empty(), java.util.List.of(),
                    java.util.List.of(panelRow("owner", homeName), panelRow("group", PartyApi.partyName(player)
                            .map(Component::literal).orElseGet(() -> Component.translatable("skyblockmulti.panel.unnamed_group"))))));
            var pending = PartyHomeRegistry.pendingForDecisionMaker(player.getUUID());
            if (pending != null && party.owner() && pending.partyId().equals(party.partyId())
                    && pending.expiresAtMillis() > System.currentTimeMillis()) {
                entries.add(panelEntry(2, "home:" + pending, Component.translatable("skyblockmulti.panel.home_decision"), java.util.List.of(
                        panelAction("home_move", "skyblockmulti.panel.accept"), panelAction("home_keep", "skyblockmulti.panel.reject"))));
            } else if (!party.owner() && PartyHomeRegistry.pending(party.partyId()) == null
                    && scoreValue(server, player, "sb_party_pref") == -1 && homePlayer != null
                    && hasOwnIsland(server, homePlayer) && scoreValue(server, homePlayer, "sb3_state") == 2) {
                entries.add(panelEntry(2, "choice:" + party.partyId() + ":" + home,
                        Component.translatable(own ? "skyblockmulti.panel.island_decision" : "skyblockmulti.panel.island_decision_no_own", homeName),
                        java.util.List.of(panelAction("island_party", "skyblockmulti.panel.accept"), panelAction("island_own", "skyblockmulti.panel.reject"))));
            }
        } else if (PartyApi.hasExternalProvider()) entries.add(new cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry(1, "solo", Component.empty(), java.util.List.of(),
                java.util.List.of(panelRow("owner", own ? player.getDisplayName() : Component.translatable("skyblockmulti.panel.none")),
                        panelRow("group", Component.translatable("skyblockmulti.panel.no_group")))));
        cl.treecs.skyblockmulti.infernal.InfernalRewardLedger.appendPanelEntries(player, entries);
        if (entries.stream().noneMatch(entry -> entry.tab() == 2)) entries.add(panelEntry(2, "empty",
                Component.translatable("skyblockmulti.panel.no_notices"), java.util.List.of()));
        return java.util.List.copyOf(entries);
    }

    public static cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry panelEntry(int tab, String context, Component body,
            java.util.List<cl.treecs.skyblockmulti.network.IslandPanelPayload.Action> actions) {
        return new cl.treecs.skyblockmulti.network.IslandPanelPayload.Entry(tab, context, body, actions);
    }

    private static cl.treecs.skyblockmulti.network.IslandPanelPayload.Row panelRow(String field, Component value) {
        return new cl.treecs.skyblockmulti.network.IslandPanelPayload.Row(Component.translatable("skyblockmulti.panel.field." + field), value);
    }

    public static cl.treecs.skyblockmulti.network.IslandPanelPayload.Action panelAction(String id, String key) {
        return new cl.treecs.skyblockmulti.network.IslandPanelPayload.Action(id, Component.translatable(key));
    }

    private static int resolvePartyHomeDecision(CommandSourceStack source, boolean moveToNewLeader) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        var pending = PartyHomeRegistry.pendingForDecisionMaker(player.getUUID());
        if (pending == null || pending.expiresAtMillis() <= System.currentTimeMillis()) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.party_home.no_pending")
                    .withStyle(ChatFormatting.RED));
            return 0;
                }
        if (moveToNewLeader) {
            if (!hasOwnIsland(source.getServer(), player)) {
                PartyHomeRegistry.keepCurrentHome(pending.partyId(), player.getUUID());
                player.sendSystemMessage(Component.translatable("skyblockmulti.party_home.no_own")
                        .withStyle(ChatFormatting.YELLOW));
                return 1;
            }
            long remainingSeconds = partyHomeCooldownRemainingSeconds(
                    source.getServer(), pending.partyId(), System.currentTimeMillis());
            if (remainingSeconds > 0) {
                player.sendSystemMessage(Component.translatable(
                                "skyblockmulti.party_home.cooldown", formatCooldown(remainingSeconds))
                        .withStyle(ChatFormatting.RED));
                return 0;
            }
        }
        boolean resolved = moveToNewLeader
                ? PartyHomeRegistry.moveHomeToNewLeader(pending.partyId(), player.getUUID())
                : PartyHomeRegistry.keepCurrentHome(pending.partyId(), player.getUUID());
        if (!resolved) return 0;
        if (moveToNewLeader) {
            recordPartyHomeChange(source.getServer(), pending.partyId(), System.currentTimeMillis());
            returnNewOwnerToOwnIfPresent(source.getServer(), player);
        }

        Component result = Component.translatable(moveToNewLeader
                        ? "skyblockmulti.party_home.moved"
                        : "skyblockmulti.party_home.kept")
                .withStyle(moveToNewLeader ? ChatFormatting.GREEN : ChatFormatting.AQUA);
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            PartySnapshot snapshot = PartyApi.snapshot(online);
            if (snapshot.inParty() && pending.partyId().equals(snapshot.partyId())
                    && (moveToNewLeader || online.getUUID().equals(player.getUUID()))) {
                online.sendSystemMessage(result);
            }
        }
        // The leader chooses PARTY_HOME first. Only after that decision do the
        // remaining members receive their individual stay/move choice.
        if (moveToNewLeader) promptPartyMembersForHome(source.getServer(), pending.partyId());
        return 1;
    }

    private static void sendPartyHomeDecision(MinecraftServer server, UUID partyId,
                                              UUID previousOwner, UUID newOwner) {
        ServerPlayer player = server.getPlayerList().getPlayer(newOwner);
        if (player == null) return;
        if (!hasOwnIsland(server, player)) {
            PartyHomeRegistry.keepCurrentHome(partyId, newOwner);
            player.sendSystemMessage(Component.translatable("skyblockmulti.party_home.no_own")
                    .withStyle(ChatFormatting.YELLOW));
            PartySnapshot snapshot = PartyApi.snapshot(player);
            if (partyHomeIsAvailable(server, snapshot)) {
                setPartyIslandPreference(server, player, 1);
                reconcileActiveIsland(server, player, snapshot, true);
                resetRespawnPointAfterPartyMove(server, player, partyHomeOwnerName(server, snapshot));
            }
            return;
        }
        long remainingSeconds = partyHomeCooldownRemainingSeconds(
                server, partyId, System.currentTimeMillis());
        if (remainingSeconds > 0) {
            PartyHomeRegistry.keepCurrentHome(partyId, newOwner);
            player.sendSystemMessage(Component.translatable(
                            "skyblockmulti.party_home.transfer_cooldown", formatCooldown(remainingSeconds))
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        player.sendSystemMessage(Component.translatable("skyblockmulti.party_home.changed")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("skyblockmulti.party_home.changed_detail")
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(IslandPanelService.openLink());
    }

    private static void expirePartyHomeDecisions(MinecraftServer server) {
        for (var pending : PartyHomeRegistry.removeExpired(System.currentTimeMillis())) {
            ServerPlayer formerOwner = server.getPlayerList().getPlayer(pending.previousOwner());
            boolean remainsInParty = false;
            if (formerOwner != null) {
                PartySnapshot snapshot = PartyApi.snapshot(formerOwner);
                remainsInParty = snapshot.inParty() && pending.partyId().equals(snapshot.partyId());
            }
            ServerPlayer candidate = server.getPlayerList().getPlayer(pending.newOwner());
            remainsInParty = remainsInParty || candidate == null || !hasOwnIsland(server, candidate);
            UUID selectedHome = remainsInParty ? pending.previousOwner() : pending.newOwner();
            PartyHomeRegistry.setHome(pending.partyId(), selectedHome);
            if (!remainsInParty) {
                recordPartyHomeChange(server, pending.partyId(), System.currentTimeMillis());
                ServerPlayer newOwner = server.getPlayerList().getPlayer(pending.newOwner());
                if (newOwner != null) returnNewOwnerToOwnIfPresent(server, newOwner);
            }
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                PartySnapshot snapshot = PartyApi.snapshot(online);
                if (snapshot.inParty() && pending.partyId().equals(snapshot.partyId())
                        && (!remainsInParty || online.getUUID().equals(pending.newOwner()))) {
                    online.sendSystemMessage(Component.translatable(remainsInParty
                                    ? "skyblockmulti.party_home.timeout_keep"
                                    : "skyblockmulti.party_home.timeout_move")
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            if (!remainsInParty) promptPartyMembersForHome(server, pending.partyId());
        }
    }

    private static long partyHomeCooldownRemainingSeconds(MinecraftServer server, UUID partyId,
                                                           long nowMillis) {
        int cooldownHours = getConfiguredPartyHomeCooldownHours();
        if (cooldownHours <= 0 || partyId == null) return 0L;
        String holder = partyCooldownHolder(partyId);
        Integer changedAt = optionalScoreValue(
                server, ScoreHolder.forNameOnly(holder), "sb_party_cd");
        if (changedAt == null || changedAt <= 0) return 0L;
        long expiresAt = changedAt.longValue() + cooldownHours * 3_600L;
        return Math.max(0L, expiresAt - nowMillis / 1_000L);
    }

    private static void recordPartyHomeChange(MinecraftServer server, UUID partyId, long nowMillis) {
        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);
            executor.run("scoreboard objectives add sb_party_cd dummy");
            executor.run("scoreboard players set " + partyCooldownHolder(partyId)
                    + " sb_party_cd " + (nowMillis / 1_000L));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist the party-island cooldown", exception);
        }
    }

    private static String partyCooldownHolder(UUID partyId) {
        return "#pcd" + partyId.toString().replace("-", "");
    }

    private static String formatCooldown(long seconds) {
        long hours = seconds / 3_600L;
        long minutes = (seconds % 3_600L + 59L) / 60L;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }

    private static void checkPartyChange(MinecraftServer server, ServerPlayer player) {
        PartySnapshot partyInfo = PartyApi.snapshot(player);
        resolveDepartedPersonalHome(server, player, partyInfo);
        PartyState currentState = PartyState.from(PartyApi.activeProviderId(), partyInfo);
        PartyState previousState = PARTY_STATES.put(player.getUUID(), currentState);

        // La primera lectura se usa como estado base. JOIN ya hizo la reconciliación inicial.
        if (previousState == null) {
            return;
        }

        if (previousState.equals(currentState)) {
            // Estado de party estable: no tocar ACTIVE ni teletransportar.
            // La reconciliación se ejecuta solo en JOIN o ante un cambio real de party.
            return;
        }

        String playerName = player.getGameProfile().name();
        UUID playerUuid = player.getUUID();

        boolean wasMemberOfAnotherPlayer =
                previousState.inParty()
                        && previousState.ownerUuid() != null
                        && !playerUuid.equals(previousState.ownerUuid());

        boolean isMemberOfAnotherPlayer =
                currentState.inParty()
                        && !partyInfo.owner()
                        && currentState.ownerUuid() != null
                        && !playerUuid.equals(currentState.ownerUuid());

        boolean partyChanged = previousState.partyId() == null
                ? currentState.partyId() != null
                : !previousState.partyId().equals(currentState.partyId());

        if (currentState.inParty() && (!previousState.inParty() || partyChanged)) {
            sendPartyOwnershipNotice(player);
        }

        // Changing leadership within the same provider/party is not a join.
        // Preserve every member's ACTIVE and preference until a home move is approved.
        if (previousState.inParty() && currentState.inParty() && !partyChanged
                && java.util.Objects.equals(previousState.providerId(), currentState.providerId())) {
            return;
        }

        boolean joinedAnotherPlayersParty = isMemberOfAnotherPlayer
                && (!wasMemberOfAnotherPlayer || partyChanged);

        // Si antes compartía la isla de otro jugador y ahora ya no es miembro,
        // debe volver físicamente a su isla personal o, si nunca tuvo una, al HUB.
        boolean leftPartyAfterSharingIsland =
                previousState.inParty() && !currentState.inParty();

        boolean becamePartyOwner =
                wasMemberOfAnotherPlayer
                        && currentState.inParty()
                        && partyInfo.owner();

        System.out.println(
                "[Skyblock Multi] Cambio de party detectado para "
                        + playerName
                        + "."
        );

        if (leftPartyAfterSharingIsland) {
            returnPlayerAfterLeavingParty(server, player);
        } else if (joinedAnotherPlayersParty) {
            setPartyIslandPreference(server, player, -1);
            activateOwnIsland(server, player, false);
            sendPartyIslandChoice(server, player, partyInfo);
        } else if (becamePartyOwner) {
            // PARTY_HOME permanece en la isla anterior hasta que la decisión
            // explícita de liderazgo sea resuelta. OWN no se modifica aquí.
            System.out.println("[Skyblock Multi] Nuevo líder detectado; PARTY_HOME conservado para "
                    + playerName + ".");
        }
    }

    /**
     * Leadership transfer is not a party leave. Move an established new owner
     * back to OWN without ever invoking the destructive no-island reset path.
     */
    private static void returnNewOwnerToOwnIfPresent(MinecraftServer server, ServerPlayer player) {
        if (!hasOwnIsland(server, player)) return;
        activateOwnIsland(server, player, false);
        String playerName = player.getGameProfile().name();
        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);
            executor.run("scoreboard players set " + playerName + " sb_party_pref 0");
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 as " + playerName
                            + " run function skyblockmulti:player/home"
            );
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 as " + playerName
                            + " at @s run spawnpoint @s ~ ~ ~"
            );
        } catch (Exception exception) {
            System.err.println("[Skyblock Multi] No fue posible devolver al nuevo owner a OWN: "
                    + playerName + ": " + exception);
        }
    }

    private static void setPartyIslandPreference(MinecraftServer server, ServerPlayer player, int preference) {
        try {
            new ServerCommandExecutor(server).run("scoreboard players set "
                    + player.getGameProfile().name() + " sb_party_pref " + preference);
        } catch (Exception exception) {
            System.err.println("[Skyblock Multi] No fue posible guardar la preferencia de isla de "
                    + player.getGameProfile().name() + ": " + exception);
        }
    }

    private static boolean hasOwnIsland(MinecraftServer server, ServerPlayer player) {
        int slot = scoreValue(server, player, "sb3_slot");
        return slot >= 1 && slot <= MAX_CONFIGURED_SLOTS;
    }

    private static void activateOwnIsland(MinecraftServer server, ServerPlayer player, boolean teleport) {
        String playerName = player.getGameProfile().name();
        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);
            executor.run("scoreboard players reset " + playerName + " sb_active_x");
            executor.run("scoreboard players reset " + playerName + " sb_active_z");
            executor.run("execute if score " + playerName
                    + " sb3_slot matches 1..128 run scoreboard players operation "
                    + playerName + " sb_active_x = " + playerName + " sb3_x");
            executor.run("execute if score " + playerName
                    + " sb3_slot matches 1..128 run scoreboard players operation "
                    + playerName + " sb_active_z = " + playerName + " sb3_z");
            executor.run("tag " + playerName + " remove skyblock_party_guest");
            if (teleport) {
                executor.run("execute if score " + playerName
                        + " sb3_slot matches 1..128 as " + playerName
                        + " run function skyblockmulti:player/home");
            }
        } catch (Exception exception) {
            System.err.println("[Skyblock Multi] No fue posible activar OWN para "
                    + playerName + ": " + exception);
        }
    }

    private static boolean partyHomeIsAvailable(MinecraftServer server, PartySnapshot snapshot) {
        if (!snapshot.inParty()) return false;
        UUID homeOwnerUuid = PartyHomeRegistry.homeOwner(snapshot);
        ServerPlayer homeOwner = homeOwnerUuid == null
                ? null : server.getPlayerList().getPlayer(homeOwnerUuid);
        return homeOwner != null && hasOwnIsland(server, homeOwner)
                && scoreValue(server, homeOwner, "sb3_state") == 2;
    }

    private static void sendPartyOwnershipNotice(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable(
                "skyblockmulti:nexus_parties".equals(PartyApi.activeProviderId())
                        ? "skyblockmulti.party.ownership_nexus_parties"
                        : "skyblockmulti.party.ownership_personal")
                .withStyle(ChatFormatting.YELLOW));
    }

    /** Polling also repairs persisted ACTIVE on reconnect, without replaying a reset. */
    private static boolean resolveDepartedPersonalHome(MinecraftServer server, ServerPlayer player,
                                                       PartySnapshot party) {
        if (!party.inParty() || "skyblockmulti:nexus_parties".equals(PartyApi.activeProviderId())) return false;
        UUID home = PartyHomeRegistry.homeOwner(party);
        PartyHomeRegistry.refreshPersonalHome(party, PartyApi.activeProviderId(),
                PartyApi.isMember(server, party.partyId(), home));
        home = PartyHomeRegistry.homeOwner(party);
        if (!home.equals(party.ownerUuid()) || scoreValue(server, player, "sb_party_pref") != 1) return false;
        ScoreHolder leader = ScoreHolder.forNameOnly(party.ownerName());
        int slot = scoreValue(server, leader, "sb3_slot");
        boolean available = slot >= 1 && slot <= MAX_CONFIGURED_SLOTS
                && scoreValue(server, leader, "sb3_state") == 2;
        if (available && java.util.Objects.equals(optionalScoreValue(server, player, "sb_active_x"),
                optionalScoreValue(server, leader, "sb3_x"))
                && java.util.Objects.equals(optionalScoreValue(server, player, "sb_active_z"),
                optionalScoreValue(server, leader, "sb3_z"))) return false;
        switch (PartyHomeRegistry.personalHomeDestination(hasOwnIsland(server, player), available)) {
        case OWN -> {
            setPartyIslandPreference(server, player, 0);
            activateOwnIsland(server, player, false);
            returnPlayerAfterLeavingParty(server, player);
        }
        case LEADER -> {
            reconcileActiveIsland(server, player, party, true);
            resetRespawnPointAfterPartyMove(server, player, party.ownerName());
        }
        case HUB -> {
            returnPlayerAfterLeavingParty(server, player);
        }
        }
        player.sendSystemMessage(Component.translatable("skyblockmulti.party.personal_home_departed")
                .withStyle(ChatFormatting.YELLOW));
        return true;
    }

    private static void sendPartyIslandChoice(MinecraftServer server, ServerPlayer player,
                                              PartySnapshot snapshot) {
        if (!snapshot.inParty() || snapshot.owner()) return;
        if (PartyHomeRegistry.pending(snapshot.partyId()) != null) return;
        if (!partyHomeIsAvailable(server, snapshot)) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.party_island.pending")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        player.sendSystemMessage(Component.translatable("skyblockmulti.party_island.title")
                .append(" ").append(IslandPanelService.openLink()));
    }

    private static int choosePartyIsland(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        PartySnapshot snapshot = PartyApi.snapshot(player);
        if (!snapshot.inParty() || snapshot.owner()
                || PartyHomeRegistry.pending(snapshot.partyId()) != null
                || scoreValue(source.getServer(), player, "sb_party_pref") != -1) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.party_island.no_pending")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!partyHomeIsAvailable(source.getServer(), snapshot)) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.party_island.unavailable")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        setPartyIslandPreference(source.getServer(), player, 1);
        reconcileActiveIsland(source.getServer(), player, snapshot, true);
        resetRespawnPointAfterPartyMove(source.getServer(), player,
                partyHomeOwnerName(source.getServer(), snapshot));
        player.sendSystemMessage(Component.translatable("skyblockmulti.party_island.selected_party")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int choosePersonalIsland(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        PartySnapshot snapshot = PartyApi.snapshot(player);
        if (!snapshot.inParty() || snapshot.owner()
                || PartyHomeRegistry.pending(snapshot.partyId()) != null
                || scoreValue(source.getServer(), player, "sb_party_pref") != -1) {
            player.sendSystemMessage(Component.translatable("skyblockmulti.party_island.no_pending")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean ownsIsland = hasOwnIsland(source.getServer(), player);
        setPartyIslandPreference(source.getServer(), player, 0);
        activateOwnIsland(source.getServer(), player, false);
        if (!ownsIsland) {
            try {
                new ServerCommandExecutor(source.getServer()).run("execute in minecraft:overworld run teleport "
                        + player.getGameProfile().name() + " 0.5 161 0.5");
            } catch (Exception exception) {
                throw new IllegalStateException("Could not return player to Last Refuge", exception);
            }
        }
        player.sendSystemMessage(Component.translatable(ownsIsland
                        ? "skyblockmulti.party_island.selected_own"
                        : "skyblockmulti.party_island.selected_nexus")
                .withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static void notifyPartyHomeAvailable(MinecraftServer server, ServerPlayer homeOwner) {
        PartySnapshot ownerSnapshot = PartyApi.snapshot(homeOwner);
        UUID registeredHome = PartyHomeRegistry.homeOwner(ownerSnapshot);
        if (!ownerSnapshot.inParty() || registeredHome == null
                || !registeredHome.equals(homeOwner.getUUID())) return;
        promptPartyMembersForHome(server, ownerSnapshot.partyId());
    }

    private static void promptPartyMembersForHome(MinecraftServer server, UUID partyId) {
        if (PartyHomeRegistry.pending(partyId) != null) return;
        for (ServerPlayer member : server.getPlayerList().getPlayers()) {
            PartySnapshot snapshot = PartyApi.snapshot(member);
            if (!snapshot.inParty() || !partyId.equals(snapshot.partyId()) || snapshot.owner()) continue;
            setPartyIslandPreference(server, member, -1);
            activateOwnIsland(server, member, false);
            sendPartyIslandChoice(server, member, snapshot);
        }
    }

    private static void returnPlayerAfterLeavingParty(
            MinecraftServer server,
            ServerPlayer player
    ) {
        String playerName = player.getGameProfile().name();

        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);

            // IMPORTANTE: la decisión se hace dentro del propio comando de Minecraft.
            // No usamos el valor de retorno de ServerCommandExecutor para leer scoreboards,
            // porque ese retorno no es fiable como valor del score en runtime.
            //
            // Con slot personal 1..24: conservar todo y volver a OWN.
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 run tag "
                            + playerName + " remove skyblock_party_guest"
            );
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 run tag "
                            + playerName + " remove skyblock_party_reentry"
            );
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 as " + playerName
                            + " run function skyblockmulti:player/home"
            );
            // HOME ya terminó de mover al jugador a OWN. Sobrescribimos aquí
            // cualquier cama que hubiese fijado mientras estaba en la isla de
            // la party, para que una muerte posterior no pueda devolverlo allí.
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 as " + playerName
                            + " at @s run spawnpoint @s ~ ~ ~"
            );

            // Sin slot personal: recién aquí se considera jugador nuevo y se reinicia.
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 as " + playerName
                            + " run function skyblockmulti:player/reset_after_party_leave"
            );

            System.out.println(
                    "[Skyblock Multi] Retorno tras salir de party procesado para "
                            + playerName + "."
            );
        } catch (Exception e) {
            System.err.println(
                    "[Skyblock Multi] No fue posible devolver a "
                            + playerName
                            + " tras salir de la party: "
                            + e
            );
        }
    }

    private static void reconcileAllOnlinePlayers(MinecraftServer server, UUID newlyAvailableOwnerUuid) {
        reconcileAllOnlinePlayers(server, null, null, newlyAvailableOwnerUuid);
    }

    private static void reconcileAllOnlinePlayers(
            MinecraftServer server,
            UUID partyToMove,
            UUID playerToMove
    ) {
        reconcileAllOnlinePlayers(server, partyToMove, playerToMove, null);
    }

    private static void reconcileAllOnlinePlayers(
            MinecraftServer server,
            UUID partyToMove,
            UUID playerToMove,
            UUID newlyAvailableOwnerUuid
    ) {
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {

                PartySnapshot partyInfo = PartyApi.snapshot(player);

                boolean autoHome = (partyToMove != null
                        && partyInfo.inParty()
                        && partyToMove.equals(partyInfo.partyId()))
                        || (playerToMove != null && playerToMove.equals(player.getUUID()))
                        || (newlyAvailableOwnerUuid != null
                        && partyInfo.inParty()
                        && !partyInfo.owner()
                        && newlyAvailableOwnerUuid.equals(partyInfo.ownerUuid()));

                reconcileActiveIsland(server, player, partyInfo, autoHome);

                // Caso especial: el miembro ya estaba en la party pero el owner
                // acaba de obtener su primera isla. Ese primer traslado también
                // debe invalidar un spawn viejo de cama/ancla.
                if (autoHome && partyInfo.ownerUuid() != null) {
                    resetRespawnPointAfterPartyMove(
                            server,
                            player,
                            partyHomeOwnerName(server, partyInfo)
                    );
                }

                if (PartyApi.hasExternalProvider()) {
                    PARTY_STATES.put(
                            player.getUUID(),
                            PartyState.from(PartyApi.activeProviderId(), partyInfo)
                    );
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "[Skyblock Multi] No fue posible reconciliar las islas activas online: " + e
            );
        }
    }

    /**
     * Invalida el spawn vanilla anterior (cama/ancla) después de un traslado real
     * hacia la isla activa de una party. Se ejecuta solo si el owner ya tiene isla.
     * El comando se posiciona EN el jugador después de HOME, por lo que el nuevo
     * respawn queda en una posición segura de la isla compartida.
     */
    private static void resetRespawnPointAfterPartyMove(
            MinecraftServer server,
            ServerPlayer player,
            String ownerName
    ) {
        String playerName = player.getGameProfile().name();
        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);
            executor.run(
                    "execute if score " + ownerName
                            + " sb3_state matches 2 as " + playerName
                            + " at @s run spawnpoint @s ~ ~ ~"
            );
            System.out.println(
                    "[Skyblock Multi] Punto de reaparición actualizado para "
                            + playerName + " tras cambio de party."
            );
        } catch (Exception e) {
            System.err.println(
                    "[Skyblock Multi] No fue posible actualizar el punto de reaparición de "
                            + playerName + " tras cambio de party: " + e
            );
        }
    }

    private static void reconcileActiveIsland(
            MinecraftServer server,
            ServerPlayer player,
            PartySnapshot partyInfo,
            boolean autoHomeOnPartyTarget
    ) {
        String playerName = player.getGameProfile().name();

        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);

            UUID homeOwnerUuid = partyInfo.inParty() ? PartyHomeRegistry.homeOwner(partyInfo) : null;
            ServerPlayer onlineHomeOwner = homeOwnerUuid == null
                    ? null
                    : server.getPlayerList().getPlayer(homeOwnerUuid);
            String homeOwnerName = onlineHomeOwner != null ? onlineHomeOwner.getGameProfile().name()
                    : !"skyblockmulti:nexus_parties".equals(PartyApi.activeProviderId())
                    && homeOwnerUuid != null && homeOwnerUuid.equals(partyInfo.ownerUuid())
                    ? partyInfo.ownerName() : null;
            if (partyInfo.inParty() && homeOwnerName == null) {
                System.out.println("[Skyblock Multi] Propietario de PARTY_HOME offline; ACTIVE conservado para "
                        + playerName + " hasta poder reconciliar con " + homeOwnerUuid + ".");
                return;
            }
            // A guest may have state=2 without OWN. Never use their zeroed personal
            // coordinates as PARTY_HOME or pass them to the block-creating HOME.
            if (partyInfo.inParty()) {
                int homeSlot = scoreValue(server, ScoreHolder.forNameOnly(homeOwnerName), "sb3_slot");
                if (homeSlot < 1 || homeSlot > MAX_CONFIGURED_SLOTS) return;
            }

            // Limpiamos primero cualquier destino activo antiguo.
            executor.run("scoreboard players reset " + playerName + " sb_active_x");
            executor.run("scoreboard players reset " + playerName + " sb_active_z");

            // OWN solo existe realmente si hay un slot personal asignado.
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 run scoreboard players operation "
                            + playerName + " sb_active_x = "
                            + playerName + " sb3_x"
            );
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 run scoreboard players operation "
                            + playerName + " sb_active_z = "
                            + playerName + " sb3_z"
            );

            // Un slot 1..128 es la fuente de verdad para saber si OWN existe.
            executor.run(
                    "execute if score " + playerName
                            + " sb3_slot matches 1..128 run tag "
                            + playerName + " remove skyblock_party_guest"
            );

            if (!PartyApi.hasExternalProvider() || !partyInfo.inParty()) {
                return;
            }

            if (player.getUUID().equals(homeOwnerUuid)) {
                if (autoHomeOnPartyTarget) {
                    executor.run(
                            "execute if score " + playerName
                                    + " sb3_state matches 2 as " + playerName
                                    + " run function skyblockmulti:player/home"
                    );
                }
                return;
            }

            String ownerName = homeOwnerName;

            // Si el owner tiene una isla válida, esa pasa a ser la isla activa del miembro.
            executor.run(
                    "execute if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players operation "
                            + playerName + " sb_active_x = "
                            + ownerName + " sb3_x"
            );
            executor.run(
                    "execute if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players operation "
                            + playerName + " sb_active_z = "
                            + ownerName + " sb3_z"
            );

            // Miembro que nunca creó isla propia: deja el flujo de selección y pasa
            // a jugar directamente en la isla del owner sin consumir un slot.
            // Toda la decisión se hace con `execute unless score ...`, sin leer el
            // scoreboard desde el valor de retorno de Java.
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players set "
                            + playerName + " sb3_state 2"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players set "
                            + playerName + " sb_tree 0"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players set "
                            + playerName + " sb_difficulty 0"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players set "
                            + playerName + " sb_chest -1"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run scoreboard players set "
                            + playerName + " sb_menu 0"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run tag "
                            + playerName + " add skyblock_party_guest"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run tag "
                            + playerName + " remove skyblock_party_reentry"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 run tag "
                            + playerName + " remove skyblock_menu_shown_v1"
            );
            executor.run(
                    "execute unless score " + playerName
                            + " sb3_slot matches 1..128 if score " + ownerName
                            + " sb3_state matches 2 as " + playerName
                            + " run function skyblockmulti:player/unlock_selection"
            );

            System.out.println(
                    "[Skyblock Multi] Reconciliación de isla activa solicitada para "
                            + playerName
                            + " usando la isla de "
                            + ownerName
                            + "."
            );

            if (autoHomeOnPartyTarget) {
                // Solo una vez: al entrar/cambiar de party o cuando el owner obtiene
                // por primera vez una isla disponible. Nunca en cada polling.
                executor.run(
                        "execute if score " + ownerName
                                + " sb3_state matches 2 as " + playerName
                                + " run function skyblockmulti:player/home"
                );
            }
        } catch (Exception e) {
            System.err.println(
                    "[Skyblock Multi] No fue posible reconciliar la isla activa de "
                            + playerName
                            + ": "
                            + e
            );
        }
    }

    private static void registerServerStartedEvent() {
        try {
            Class<?> eventsClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents");
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted");
            Class<?> publicEventClass = Class.forName("net.fabricmc.fabric.api.event.Event");

            Object event = eventsClass.getField("SERVER_STARTED").get(null);
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "SkyblockMultiServerStartedCallback";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> args != null && args.length == 1 && proxy == args[0];
                                default -> null;
                            };
                        }
                        if (method.getName().equals("onServerStarted") && args != null && args.length == 1) {
                            activeServer = args[0];
                            worldGeometryLocked = false;
                            activeWorldRadius = -1;
                            activeWorldCapacity = -1;
                            MinecraftServer server = (MinecraftServer) args[0];
                            PartyHomeRegistry.open(server.getWorldPath(
                                    net.minecraft.world.level.storage.LevelResource.ROOT)
                                    .resolve("skyblockmulti_party_homes.properties"));
                            applyConfiguration(args[0]);
                            cl.treecs.skyblockmulti.api.party.IslandTransferApi.open(server);
                        }
                        return null;
                    }
            );

            Method register = publicEventClass.getMethod("register", Object.class);
            register.invoke(event, callback);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No fue posible registrar el evento de inicio con Fabric API.", e);
        }
    }

    private static void persistWorldGeometryLock(MinecraftServer server) {
        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);
            executor.run("scoreboard objectives add sb3_const dummy");
            executor.run("scoreboard objectives add sb3_cfg dummy");
            executor.run("scoreboard players set #geometry_locked sb3_cfg 1");
            executor.run("scoreboard players operation #world_capacity sb3_cfg = #capacity sb3_cfg");
            executor.run("scoreboard players operation #world_radius sb3_const = #distance sb3_const");
            executor.run("scoreboard players operation #world_ring_mask sb3_cfg = #ring_mask sb3_cfg");
            executor.run("scoreboard players operation #world_allocation sb3_cfg = #allocation_policy sb3_cfg");

            int capacity = scoreValue(
                    server,
                    ScoreHolder.forNameOnly("#capacity"),
                    "sb3_cfg"
            );
            int radius = scoreValue(
                    server,
                    ScoreHolder.forNameOnly("#distance"),
                    "sb3_const"
            );
            if (capacity >= 1 && capacity <= MAX_CONFIGURED_SLOTS && isExactRadiusOption(radius)) {
                activeWorldCapacity = capacity;
                activeWorldRadius = radius;
            }
            worldGeometryLocked = true;
        } catch (Exception e) {
            System.err.println("[Skyblock Multi] No fue posible persistir el bloqueo de geometría: " + e);
        }
    }

    private static String partyHomeOwnerName(MinecraftServer server, PartySnapshot partyInfo) {
        UUID homeOwnerUuid = PartyHomeRegistry.homeOwner(partyInfo);
        ServerPlayer homeOwner = homeOwnerUuid == null
                ? null
                : server.getPlayerList().getPlayer(homeOwnerUuid);
        return homeOwner == null ? partyInfo.ownerName() : homeOwner.getGameProfile().name();
    }

    private static void recoverAndPersistWorldGeometryLock(MinecraftServer server, ServerPlayer player) {
        // The assignment persisted the exact ring mask, capacity, radius and policy.
        // Never infer a different geometry from one island's coordinates.
        persistWorldGeometryLock(server);
    }

    private static boolean isExactRadiusOption(int radius) {
        for (int option : RADIUS_OPTIONS) {
            if (option == radius) return true;
        }
        return false;
    }

    private static int scoreValue(MinecraftServer server, ScoreHolder holder, String objectiveName) {
        Integer score = optionalScoreValue(server, holder, objectiveName);
        return score == null ? 0 : score;
    }

    private static Integer optionalScoreValue(MinecraftServer server, ScoreHolder holder,
                                              String objectiveName) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) return null;
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(holder, objective);
        return score == null ? null : score.value();
    }

    private static void applyConfiguration(Object server) {
        ConfigData config = loadConfig();
        int radius = config.radius();
        int capacity = config.capacity();
        IslandLayoutConfiguration effectiveLayout = config.layout();
        try {
            ServerCommandExecutor executor = new ServerCommandExecutor(server);

            executor.run("scoreboard objectives add sb3_const dummy");
            executor.run("scoreboard objectives add sb3_cfg dummy");
            executor.run("scoreboard objectives add sb3_used dummy");

            MinecraftServer minecraftServer = (MinecraftServer) server;
            boolean persistentLock = scoreValue(
                    minecraftServer,
                    ScoreHolder.forNameOnly("#geometry_locked"),
                    "sb3_cfg"
            ) == 1;

            int storedCapacity = persistentLock
                    ? scoreValue(
                            minecraftServer,
                            ScoreHolder.forNameOnly("#world_capacity"),
                            "sb3_cfg"
                    )
                    : -1;
            int storedRadius = persistentLock
                    ? scoreValue(
                            minecraftServer,
                            ScoreHolder.forNameOnly("#world_radius"),
                            "sb3_const"
                    )
                    : -1;
            int storedRingMask = persistentLock ? scoreValue(
                    minecraftServer, ScoreHolder.forNameOnly("#world_ring_mask"), "sb3_cfg") : 0;
            int storedAllocation = persistentLock ? scoreValue(
                    minecraftServer, ScoreHolder.forNameOnly("#world_allocation"), "sb3_cfg") : 0;

            if (persistentLock) {
                if (storedCapacity < 1 || storedCapacity > MAX_CONFIGURED_SLOTS
                        || !isExactRadiusOption(storedRadius)) {
                    throw new IllegalStateException("Invalid locked island geometry");
                }
                capacity = storedCapacity;
                radius = storedRadius;
                effectiveLayout = customLayoutFromScores(radius, capacity,
                        storedRingMask, storedAllocation);
                worldGeometryLocked = true;
            } else {
                persistentLock = false;
                worldGeometryLocked = false;
                capacity = config.capacity();
                radius = config.radius();
                effectiveLayout = config.layout();
            }

            activeWorldCapacity = capacity;
            activeWorldRadius = radius;
            activeWorldLayout = effectiveLayout;

            // Retirar tickets de la geometría anterior antes de reescribir los slots.
            executor.run("execute in minecraft:overworld if biome 0 64 0 minecraft:the_void run function skyblockmulti:slots/remove_forceload");
            executor.run("execute in minecraft:overworld if biome 0 64 0 minecraft:the_void run function skyblockmulti:slots/remove_free_anchors");
            executor.run("execute in minecraft:overworld run kill @e[type=minecraft:marker,tag=skyblock_slots_ready_v1]");
            executor.run("execute in minecraft:overworld run kill @e[type=minecraft:marker,tag=skyblock_slots_ready_v2]");
            executor.run("scoreboard players set #slots_built sb3_const 0");
            executor.run("scoreboard players set #slots_complete sb3_const 0");

            executor.run("data modify storage skyblockmulti:config island_radius set value " + radius);
            executor.run("data modify storage skyblockmulti:config island_distance set value " + radius);
            executor.run("scoreboard players set #distance sb3_const " + radius);
            executor.run("scoreboard players set #layout_version sb3_const " + LAYOUT_VERSION);
            executor.run("scoreboard players set #capacity sb3_cfg " + capacity);
            executor.run("scoreboard players set #max sb3_const " + capacity);
            executor.run("scoreboard players set #ring_mask sb3_cfg " + ringMask(effectiveLayout));
            executor.run("scoreboard players set #allocation_policy sb3_cfg "
                    + effectiveLayout.allocationPolicy().ordinal());
            executor.run("scoreboard players set #openpac sb3_cfg "
                    + (OpenPacCompat.isTerritorialIntegrationEnabled() ? 1 : 0));
            executor.run("scoreboard players set #enabled_count sb3_cfg " + countEnabled(config.trees()));
            executor.run("scoreboard players set #bonus_tier sb3_cfg " + config.bonusChestMode().scoreValue());
            executor.run("scoreboard players set #party_leave_tier sb3_cfg "
                    + config.partyLeaveDifficultyMode().scoreValue());

            if (persistentLock) {
                executor.run("scoreboard players set #geometry_locked sb3_cfg 1");
                executor.run("scoreboard players set #world_capacity sb3_cfg " + capacity);
                executor.run("scoreboard players set #world_radius sb3_const " + radius);
            }

            for (TreeOption tree : TreeOption.values()) {
                executor.run("scoreboard players set " + tree.scoreHolder() + " sb3_cfg "
                        + (Boolean.TRUE.equals(config.trees().get(tree.id())) ? 1 : 0));
            }

            // Borrar definiciones de slots previas y reiniciar ocupación lógica.
            // Las islas construidas se vuelven a detectar por su bedrock central.
            for (int i = 1; i <= MAX_CONFIGURED_SLOTS; i++) {
                String key = String.format(Locale.ROOT, "%02d", i);
                executor.run("data remove storage skyblockmulti:slots s" + key);
                executor.run("scoreboard players set #" + key + " sb3_used 0");
            }

            List<Slot> slots = calculateSlots(effectiveLayout);
            for (Slot slot : slots) {
                String key = String.format(Locale.ROOT, "%02d", slot.index());
                String snbt = String.format(Locale.ROOT,
                        "{x:%d,z:%d,slot:%d,key:\"%s\"}",
                        slot.x(), slot.z(), slot.index(), key);
                executor.run("data modify storage skyblockmulti:slots s" + key + " set value " + snbt);
            }

            executor.run("tag @a[scores={sb3_state=1}] remove skyblock_menu_shown_v1");
            executor.run("execute in minecraft:overworld if biome 0 64 0 minecraft:the_void run function skyblockmulti:slots/forceload");
            executor.run("execute in minecraft:overworld if biome 0 64 0 minecraft:the_void run scoreboard players set #slotgen sb3_const 60");
            System.out.println("[Skyblock Multi] Configuración aplicada: radio_hub=" + radius
                    + ", capacidad=" + capacity
                    + ", geometría_bloqueada=" + worldGeometryLocked
                    + ", árboles=" + countEnabled(config.trees())
                    + ", cofre=" + config.bonusChestMode().configKey()
                    + ", salida_party=" + config.partyLeaveDifficultyMode().configKey());
        } catch (Exception e) {
            System.err.println("[Skyblock Multi] No fue posible aplicar la configuración al servidor: " + e);
            e.printStackTrace(System.err);
            throw new IllegalStateException("Could not apply island geometry safely", e);
        }
    }

    private static List<Slot> calculateSlots(IslandLayoutConfiguration layout) {
        return layout.positions().stream()
                .map(position -> new Slot(position.slotId(), position.x(), position.z()))
                .toList();
    }

    private static int ringMask(IslandLayoutConfiguration layout) {
        int mask = 0;
        for (IslandRingLayout.Ring ring : layout.enabledRings()) {
            mask |= 1 << (ring.number() - 1);
        }
        return mask;
    }

    private static IslandLayoutConfiguration customLayoutFromScores(
            int radius, int capacity, int mask, int policyOrdinal) {
        EnumSet<IslandRingLayout.Ring> rings = EnumSet.noneOf(IslandRingLayout.Ring.class);
        for (IslandRingLayout.Ring ring : IslandRingLayout.Ring.values()) {
            if ((mask & (1 << (ring.number() - 1))) != 0) rings.add(ring);
        }
        if (rings.isEmpty()) {
            throw new IllegalStateException("custom island layout has no persisted rings");
        }
        var policies = IslandRingLayout.AllocationPolicy.values();
        var policy = policyOrdinal >= 0 && policyOrdinal < policies.length
                ? policies[policyOrdinal]
                : IslandRingLayout.AllocationPolicy.RANDOM_BALANCED;
        boolean technical = rings.contains(IslandRingLayout.Ring.DEEP_FRONTIER)
                || capacity > IslandRingLayout.ADVANCED_LIMIT;
        return IslandLayoutConfiguration.custom(radius, capacity, rings, policy, technical);
    }

    private record ConfigData(int radius, int capacity, Map<String, Boolean> trees,
                              BonusChestMode bonusChestMode,
                              BonusChestMode partyLeaveDifficultyMode,
                              IslandLayoutConfiguration layout) {}
    private record Slot(int index, int x, int z) {}

    private static final class ServerCommandExecutor {
        private final Object source;
        private final Object commands;
        private final Object dispatcher;
        private final Method commandMethod;
        private final Method dispatcherMethod;

        private ServerCommandExecutor(Object server) throws ReflectiveOperationException {
            Class<?> commandsClass = runtimeClass(
                    "net.minecraft.commands.Commands",
                    "net.minecraft.class_2170"
            );
            Class<?> sourceClass = runtimeClass(
                    "net.minecraft.commands.CommandSourceStack",
                    "net.minecraft.class_2168"
            );
            this.commands = invokeNoArgReturning(server, commandsClass);
            this.source = ((CommandSourceStack) invokeNoArgReturning(server, sourceClass))
                    .withSuppressedOutput();
            this.commandMethod = findCommandMethod(commands.getClass(), sourceClass);
            if (this.commandMethod == null) {
                this.dispatcher = findDispatcher(commands);
                this.dispatcherMethod = findExecuteMethod(dispatcher.getClass());
            } else {
                this.dispatcher = null;
                this.dispatcherMethod = null;
            }
        }

        private int run(String command) throws ReflectiveOperationException {
            String clean = command.startsWith("/") ? command.substring(1) : command;
            Object result;
            if (commandMethod != null) {
                Class<?>[] params = commandMethod.getParameterTypes();
                result = params[0] == String.class
                        ? commandMethod.invoke(commands, clean, source)
                        : commandMethod.invoke(commands, source, clean);
            } else {
                result = dispatcherMethod.invoke(dispatcher, clean, source);
            }
            return result instanceof Number number ? number.intValue() : 0;
        }

        private static Method findCommandMethod(Class<?> commandsClass, Class<?> sourceClass) {
            Method fallback = null;
            for (Method method : commandsClass.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2) continue;
                boolean sourceString = sourceClass.isAssignableFrom(params[0]) && params[1] == String.class;
                boolean stringSource = params[0] == String.class && sourceClass.isAssignableFrom(params[1]);
                if (!sourceString && !stringSource) continue;
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (name.contains("perform") || name.contains("prefixed") || name.contains("execute")) return method;
                fallback = method;
            }
            return fallback;
        }

        private static Object findDispatcher(Object commands) throws ReflectiveOperationException {
            for (Method method : commands.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType().getName().equals("com.mojang.brigadier.CommandDispatcher")) {
                    return method.invoke(commands);
                }
            }
            throw new NoSuchMethodException("Commands -> CommandDispatcher");
        }

        private static Method findExecuteMethod(Class<?> dispatcherClass) throws NoSuchMethodException {
            for (Method method : dispatcherClass.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals("execute") && params.length == 2 && params[0] == String.class) {
                    return method;
                }
            }
            throw new NoSuchMethodException("CommandDispatcher.execute(String, source)");
        }

        private static Object invokeNoArgReturning(Object owner, Class<?> returnType) throws ReflectiveOperationException {
            for (Method method : owner.getClass().getMethods()) {
                if (method.getParameterCount() == 0 && returnType.isAssignableFrom(method.getReturnType())) {
                    return method.invoke(owner);
                }
            }
            throw new NoSuchMethodException(owner.getClass().getName() + " -> " + returnType.getName());
        }

        private static Class<?> runtimeClass(String namedClass, String intermediaryClass) throws ClassNotFoundException {
            MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
            Collection<String> namespaces = resolver.getNamespaces();

            // Production Fabric uses intermediary names. Development environments may use named names.
            if (namespaces.contains("intermediary")) {
                try {
                    return Class.forName(resolver.mapClassName("intermediary", intermediaryClass));
                } catch (RuntimeException | ClassNotFoundException ignored) {
                }
            }
            if (namespaces.contains("named")) {
                try {
                    return Class.forName(resolver.mapClassName("named", namedClass));
                } catch (RuntimeException | ClassNotFoundException ignored) {
                }
            }

            // Direct fallbacks for unusual loaders or development launchers.
            try {
                return Class.forName(intermediaryClass);
            } catch (ClassNotFoundException ignored) {
                return Class.forName(namedClass);
            }
        }
    }
}
