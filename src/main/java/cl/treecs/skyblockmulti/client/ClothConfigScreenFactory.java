package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.NexusFoundation;
import cl.treecs.skyblockmulti.VanillaRecipeConfig;
import cl.treecs.skyblockmulti.SkyblockMultiMod;
import cl.treecs.skyblockmulti.SkyblockMultiMod.BonusChestMode;
import cl.treecs.skyblockmulti.addon.AddonFeatureConfig;
import cl.treecs.skyblockmulti.addon.AddonFeatureDefinition;
import cl.treecs.skyblockmulti.addon.AddonFeatureRegistry;
import cl.treecs.skyblockmulti.island.IslandLayoutConfiguration;
import cl.treecs.skyblockmulti.island.IslandRingLayout;
import cl.treecs.skyblockmulti.network.TreeCatalogPayload;
import cl.treecs.skyblockmulti.tree.TreeCatalog;
import cl.treecs.skyblockmulti.tree.DataTreeDefinition;
import cl.treecs.skyblockmulti.tree.TreeDefinition;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** Standard Cloth Config front end; TOML remains the authoritative storage. */
public final class ClothConfigScreenFactory {
    private ClothConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        return create(parent, false);
    }

    public static Screen create(Screen parent, boolean focusPanel) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("skyblockmulti.config.title"))
                .setDoesConfirmSave(true)
                .alwaysShowTabs();
        ConfigEntryBuilder entries = builder.entryBuilder();
        boolean worldOpen = Minecraft.getInstance().level != null;
        boolean geometryLocked = SkyblockMultiMod.isWorldGeometryLocked();

        IslandLayoutConfiguration currentLayout = SkyblockMultiMod.getConfiguredIslandLayout();
        AtomicInteger radius = new AtomicInteger(currentLayout.baseRadius());
        EnumSet<IslandRingLayout.Ring> rings = currentLayout.enabledRings().isEmpty()
                ? EnumSet.of(IslandRingLayout.Ring.INNER, IslandRingLayout.Ring.OUTER)
                : EnumSet.copyOf(currentLayout.enabledRings());
        AtomicReference<IslandRingLayout.AllocationPolicy> policy =
                new AtomicReference<>(currentLayout.allocationPolicy());
        ConfigCategory world = builder.getOrCreateCategory(
                Component.translatable("skyblockmulti.config.tab.world"));
        world.addEntry(entries.startSelector(
                        Component.translatable("skyblockmulti.config.radius.button", ""),
                        new Integer[]{2048, 4096, 8192, 16384}, radius.get())
                .setDefaultValue(SkyblockMultiMod.DEFAULT_RADIUS)
                .setRequirement(() -> !geometryLocked)
                .setSaveConsumer(radius::set).build());
        Map<IslandRingLayout.Ring, BooleanListEntry> ringEntries = new LinkedHashMap<>();
        List<AbstractConfigListEntry> advancedRings = new ArrayList<>();
        advancedRings.add(entries.startTextDescription(Component.translatable(
                "skyblockmulti.config.layout.advanced_warning")).build());
        for (IslandRingLayout.Ring ring : IslandRingLayout.Ring.values()) {
            var ringEntry = entries.startBooleanToggle(
                            Component.translatable("skyblockmulti.config.layout.ring_capacity",
                                    ring.number(), ring.positions()), rings.contains(ring))
                    .setDefaultValue(ring == IslandRingLayout.Ring.INNER
                            || ring == IslandRingLayout.Ring.OUTER)
                    .setRequirement(() -> !geometryLocked)
                    .setErrorSupplier(enabled -> ringEntries.values().stream().noneMatch(BooleanListEntry::getValue)
                            ? java.util.Optional.of(Component.translatable(
                                    "skyblockmulti.config.layout.no_rings"))
                            : java.util.Optional.empty())
                    .setSaveConsumer(enabled -> {
                        if (enabled) rings.add(ring);
                        else rings.remove(ring);
                    }).build();
            ringEntries.put(ring, ringEntry);
            if (ring.number() >= 4) advancedRings.add(ringEntry);
            else world.addEntry(ringEntry);
        }
        world.addEntry(entries.startSubCategory(Component.translatable(
                "skyblockmulti.config.layout.advanced"), advancedRings).build());
        world.addEntry(new TextListEntry(Component.empty(), Component.empty(), -1,
                java.util.Optional::empty) {
            private int displayedMaximum = -1;
            private TextListEntry counter;

            @Override
            public int getItemHeight() { return 24; }

            @Override
            public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y,
                    int x, int width, int height, int mouseX, int mouseY,
                    boolean hovered, float delta) {
                int maximum = ringEntries.entrySet().stream()
                                .filter(entry -> entry.getValue().getValue())
                                .mapToInt(entry -> entry.getKey().positions()).sum();
                if (counter == null || displayedMaximum != maximum) {
                    counter = new TextListEntry(Component.empty(), Component.translatable(
                            "skyblockmulti.config.layout.maximum", maximum), -1,
                            java.util.Optional::empty);
                    displayedMaximum = maximum;
                }
                counter.setParent(getParent());
                counter.setScreen(getConfigScreen());
                counter.extractRenderState(graphics, index, y, x, width, height,
                        mouseX, mouseY, hovered, delta);
            }
        });
        world.addEntry(entries.startEnumSelector(
                        Component.translatable("skyblockmulti.config.layout.policy.button", ""),
                        IslandRingLayout.AllocationPolicy.class, policy.get())
                .setEnumNameProvider(value -> Component.translatable(
                        "skyblockmulti.config.layout.policy."
                                + value.name().toLowerCase(java.util.Locale.ROOT)))
                .setDefaultValue(IslandRingLayout.AllocationPolicy.DIFFICULTY)
                .setRequirement(() -> !geometryLocked)
                .setSaveConsumer(policy::set).build());

        AtomicReference<BonusChestMode> chest =
                new AtomicReference<>(SkyblockMultiMod.getConfiguredBonusChestMode());
        AtomicReference<BonusChestMode> partyLeave =
                new AtomicReference<>(SkyblockMultiMod.getConfiguredPartyLeaveDifficultyMode());
        AtomicInteger endEyes = new AtomicInteger(NexusFoundation.getConfiguredEndPortalEyesPercent());
        AtomicInteger partyCooldown =
                new AtomicInteger(SkyblockMultiMod.getConfiguredPartyHomeCooldownHours());
        ConfigCategory difficulty = builder.getOrCreateCategory(
                Component.translatable("skyblockmulti.config.tab.difficulty"));
        java.util.concurrent.atomic.AtomicBoolean vanillaRecipes =
                new java.util.concurrent.atomic.AtomicBoolean(VanillaRecipeConfig.enabled());
        difficulty.addEntry(entries.startSubCategory(Component.translatable(
                        "skyblockmulti.config.crafting.title"), List.of(
                entries.startBooleanToggle(Component.translatable(
                                "skyblockmulti.config.crafting.vanilla"), vanillaRecipes.get())
                        .setDefaultValue(true).setRequirement(() -> !worldOpen)
                        .setTooltip(Component.translatable("skyblockmulti.config.crafting.tooltip"))
                        .setSaveConsumer(vanillaRecipes::set).build())).build());
        difficulty.addEntry(entries.startEnumSelector(
                        Component.translatable("skyblockmulti.config.bonus_chest.mode.button", ""),
                        BonusChestMode.class, chest.get())
                .setEnumNameProvider(mode -> Component.translatable(
                        "skyblockmulti.config.bonus_chest.mode." + ((BonusChestMode) mode).configKey()))
                .setDefaultValue(BonusChestMode.BEGINNER)
                .setRequirement(() -> !worldOpen)
                .setSaveConsumer(chest::set).build());
        difficulty.addEntry(entries.startEnumSelector(
                        Component.translatable("skyblockmulti.config.party_leave.mode.button", ""),
                        BonusChestMode.class, partyLeave.get())
                .setEnumNameProvider(mode -> Component.translatable(
                        "skyblockmulti.config.bonus_chest.mode." + ((BonusChestMode) mode).configKey()))
                .setDefaultValue(BonusChestMode.BEGINNER)
                .setRequirement(() -> !worldOpen)
                .setSaveConsumer(partyLeave::set).build());
        difficulty.addEntry(entries.startSelector(
                        Component.translatable("skyblockmulti.config.end_portal.eyes.button", ""),
                        new Integer[]{0, 25, 50, 75}, endEyes.get())
                .setDefaultValue(NexusFoundation.DEFAULT_END_EYES)
                .setRequirement(() -> !worldOpen
                        && !NexusFoundation.isEndPortalConfigurationLocked())
                .setSaveConsumer(endEyes::set).build());
        difficulty.addEntry(entries.startIntField(
                        Component.translatable("skyblockmulti.config.party_home.cooldown"),
                        partyCooldown.get())
                .setDefaultValue(24).setMin(0).setMax(8760)
                .setRequirement(() -> !worldOpen)
                .setTooltip(Component.translatable("skyblockmulti.config.party_home.cooldown.tooltip"))
                .setSaveConsumer(partyCooldown::set).build());

        Map<String, Boolean> treeStates = SkyblockMultiMod.getConfiguredTreeStatesById();
        for (TreeCatalogPayload.Entry entry : ClientTreeCatalog.entries()) {
            treeStates.put(entry.id(), entry.enabled());
        }
        ConfigCategory treesCategory = builder.getOrCreateCategory(
                Component.translatable("skyblockmulti.config.tab.trees"));
        Map<String, List<AbstractConfigListEntry>> treeGroups = new LinkedHashMap<>();
        Map<String, Component> treeGroupNames = new LinkedHashMap<>();
        for (ConfigTree tree : catalogTrees(treeStates)) {
            AbstractConfigListEntry toggle = entries.startBooleanToggle(tree.name(),
                            treeStates.getOrDefault(tree.id(), tree.defaultEnabled()))
                    .setDefaultValue(tree.defaultEnabled())
                    .setRequirement(() -> !worldOpen)
                    .setSaveConsumer(value -> treeStates.put(tree.id(), value)).build();
            String groupId = tree.vanilla() ? "minecraft" : tree.sourceModId();
            treeGroups.computeIfAbsent(groupId, ignored -> new ArrayList<>()).add(toggle);
            treeGroupNames.putIfAbsent(groupId, tree.vanilla()
                    ? Component.translatable("skyblockmulti.config.trees.vanilla")
                    : Component.translatable("skyblockmulti.config.trees.integration",
                            tree.sourceModName()));
        }
        for (Map.Entry<String, List<AbstractConfigListEntry>> group : treeGroups.entrySet()) {
            treesCategory.addEntry(entries.startSubCategory(
                    treeGroupNames.get(group.getKey()), group.getValue()).build());
        }

        Map<String, Boolean> addonStates = AddonFeatureConfig.configuredStates();
        Runnable saveAppearance = PanelAppearanceConfig.add(builder);
        if (focusPanel) builder.setFallbackCategory(builder.getOrCreateCategory(
                Component.translatable("skyblockmulti.panel.config.title")));
        ConfigCategory addons = builder.getOrCreateCategory(
                Component.translatable("skyblockmulti.config.tab.addons"));
        Map<String, List<AbstractConfigListEntry>> addonGroups = new LinkedHashMap<>();
        Map<String, Component> addonGroupNames = new LinkedHashMap<>();
        for (AddonFeatureDefinition feature : AddonFeatureRegistry.features()) {
            Component name = feature.nameTranslationKey() == null
                    ? Component.literal(feature.literalName() == null ? feature.id() : feature.literalName())
                    : Component.translatable(feature.nameTranslationKey());
            AbstractConfigListEntry toggle = entries.startBooleanToggle(name,
                            addonStates.getOrDefault(feature.id(), feature.enabledByDefault()))
                    .setDefaultValue(feature.enabledByDefault())
                    .setRequirement(() -> !worldOpen)
                    .setSaveConsumer(value -> addonStates.put(feature.id(), value)).build();
            addonGroups.computeIfAbsent(feature.addonModId(), ignored -> new ArrayList<>()).add(toggle);
            addonGroupNames.putIfAbsent(feature.addonModId(), Component.literal(feature.addonName()));
        }
        for (Map.Entry<String, List<AbstractConfigListEntry>> group : addonGroups.entrySet()) {
            addons.addEntry(entries.startSubCategory(
                    addonGroupNames.get(group.getKey()), group.getValue()).build());
        }

        builder.setSavingRunnable(() -> {
            saveAppearance.run();
            if (treeStates.values().stream().noneMatch(Boolean.TRUE::equals)) return;
            try {
                if (rings.isEmpty()) rings.add(IslandRingLayout.Ring.INNER);
                int selectedCapacity = rings.stream().mapToInt(IslandRingLayout.Ring::positions).sum();
                boolean technicalCapacity = rings.contains(IslandRingLayout.Ring.DEEP_FRONTIER);
                IslandLayoutConfiguration selected = IslandLayoutConfiguration.custom(
                        SkyblockMultiMod.normalizeRadius(radius.get()), selectedCapacity,
                        rings, policy.get(), technicalCapacity);
                selected.positions();
                SkyblockMultiMod.saveConfigurationByTreeId(
                        selected.baseRadius(), selected.capacity(), treeStates,
                        chest.get(), partyLeave.get(), selected);
                NexusFoundation.saveConfiguredEndPortalEyesPercent(endEyes.get());
                SkyblockMultiMod.savePartyHomeCooldownHours(partyCooldown.get());
                VanillaRecipeConfig.save(vanillaRecipes.get());
                AddonFeatureConfig.save(addonStates);
            } catch (IllegalArgumentException error) {
                System.err.println("[Skyblock Multi] Cloth Config rejected invalid settings: "
                        + error.getMessage());
            }
        });
        return builder.build();
    }

    private static List<ConfigTree> catalogTrees(Map<String, Boolean> states) {
        Set<String> vanilla = TreeCatalog.builtIns().stream()
                .map(TreeDefinition::id).collect(Collectors.toSet());
        List<TreeCatalogPayload.Entry> synchronizedEntries = ClientTreeCatalog.entries();
        if (synchronizedEntries.isEmpty()) {
            synchronizedEntries = new ArrayList<>(ClientTreeCatalog.discoverBundled(states));
        }
        Map<String, ConfigTree> result = new LinkedHashMap<>();
        for (DataTreeDefinition tree : TreeCatalog.activeDefinitions().values()) {
            Component name = tree.name().translationKey() == null
                    ? Component.literal(tree.name().literal()) : Component.translatable(tree.name().translationKey());
            result.put(tree.id(), new ConfigTree(
                    tree.id(), name, tree.enabledByDefault(), true, "minecraft", "Minecraft"));
        }
        for (TreeCatalogPayload.Entry tree : synchronizedEntries) {
            Component name = tree.nameTranslationKey() == null
                    ? Component.literal(tree.literalName() == null ? tree.id() : tree.literalName())
                    : Component.translatable(tree.nameTranslationKey());
            result.put(tree.id(), new ConfigTree(
                    tree.id(), name, tree.enabled(), vanilla.contains(tree.id()),
                    tree.sourceModId(), tree.sourceModName()));
            states.putIfAbsent(tree.id(), tree.enabled());
        }
        return result.values().stream().sorted(Comparator
                .comparing((ConfigTree tree) -> !vanilla.contains(tree.id()))
                .thenComparing(ConfigTree::id)).toList();
    }

    private record ConfigTree(String id, Component name, boolean defaultEnabled, boolean vanilla,
                              String sourceModId, String sourceModName) {
    }
}
