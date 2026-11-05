package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.TreeCatalogPayload;
import cl.treecs.skyblockmulti.network.SelectionMenuClosedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TreeSelectionScreen extends CompactIslandScreen {
    private final List<Button> dynamicButtons = new ArrayList<>();
    private final List<VisibleCard> visibleCards = new ArrayList<>();
    private EditBox searchBox;
    private Button previousButton;
    private Button nextButton;
    private int page;
    private int pageCount = 1;
    private String query = "";
    private boolean selectionCompleted;

    public TreeSelectionScreen() {
        super(Component.translatable("skyblockmulti.tree_screen.title"));
    }

    @Override
    protected void init() {
        int centerX = left() + panelWidth() / 2;
        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font,
                left() + 10,
                top() + 32,
                innerWidth(),
                20,
                Component.translatable("skyblockmulti.tree_screen.search")
        ));
        this.searchBox.setHint(Component.translatable("skyblockmulti.tree_screen.search"));
        this.searchBox.setMaxLength(80);
        this.searchBox.setValue(query);
        this.searchBox.setResponder(value -> {
            query = value;
            page = 0;
            rebuildCards();
        });

        this.previousButton = this.addRenderableWidget(Button.builder(
                Component.literal("<"), button -> {
                    if (page > 0) {
                        page--;
                        rebuildCards();
                    }
                }).bounds(left() + 10, bottom() - 30, 30, 20).build());
        this.nextButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"), button -> {
                    if (page + 1 < pageCount) {
                        page++;
                        rebuildCards();
                    }
                }).bounds(left() + panelWidth() - 40, bottom() - 30, 30, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("skyblockmulti.tree_screen.random"),
                button -> {
                    if (ClientTreeSelection.requestRandom()) {
                        ClientIslandSelectionDraft.selectRandom();
                        closeAfterSelection();
                    }
                }).bounds(centerX - 55, bottom() - 30, 110, 20)
                .tooltip(Tooltip.create(Component.translatable(
                        "skyblockmulti.tree_screen.random.description"
                )))
                .build());

        addCloseButton();

        rebuildCards();
        this.setInitialFocus(this.searchBox);
    }

    private void rebuildCards() {
        for (Button button : dynamicButtons) this.removeWidget(button);
        dynamicButtons.clear();
        visibleCards.clear();

        List<TreeCatalogPayload.Entry> filtered = filteredEntries();
        int columns = 1;
        int rows = Math.max(1, (panelHeight() - 112) / 24);
        int pageSize = columns * rows;
        pageCount = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        page = Math.min(page, pageCount - 1);

        int cardWidth = innerWidth();
        int totalWidth = columns * cardWidth + (columns - 1) * 6;
        int startX = left() + 10;
        int startY = top() + 60;
        int from = page * pageSize;
        int to = Math.min(filtered.size(), from + pageSize);
        for (int index = from; index < to; index++) {
            TreeCatalogPayload.Entry entry = filtered.get(index);
            int local = index - from;
            int x = startX + (local % columns) * (cardWidth + 6);
            int y = startY + (local / columns) * 24;
            Component label = Component.literal("   ").append(displayName(entry));
            Button button = Button.builder(label, clicked -> {
                        if (ClientTreeSelection.request(entry.id())) {
                            ClientIslandSelectionDraft.selectTree(displayName(entry));
                            closeAfterSelection();
                        }
                    })
                    .bounds(x, y, cardWidth, 22)
                    .tooltip(Tooltip.create(tooltip(entry)))
                    .build();
            button.active = entry.enabled() && entry.selectable();
            this.addRenderableWidget(button);
            dynamicButtons.add(button);
            visibleCards.add(new VisibleCard(entry, button));
        }

        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page + 1 < pageCount;
    }

    private List<TreeCatalogPayload.Entry> filteredEntries() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return ClientTreeCatalog.entries().stream()
                .filter(entry -> needle.isEmpty()
                        || entry.id().toLowerCase(Locale.ROOT).contains(needle)
                        || displayName(entry).getString().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private static Component displayName(TreeCatalogPayload.Entry entry) {
        if (entry.nameTranslationKey() != null) return Component.translatable(entry.nameTranslationKey());
        return Component.literal(entry.literalName() == null ? entry.id() : entry.literalName());
    }

    private static Component tooltip(TreeCatalogPayload.Entry entry) {
        Component description;
        if (!entry.selectable()) {
            description = Component.translatable("skyblockmulti.tree_screen.addon_pending");
        } else if (!entry.enabled()) {
            description = Component.translatable("skyblockmulti.menu.tree.disabled.hover");
        } else if (entry.descriptionTranslationKey() != null) {
            description = Component.translatable(entry.descriptionTranslationKey());
        } else if (entry.literalDescription() != null) {
            description = Component.literal(entry.literalDescription());
        } else {
            description = Component.literal(entry.id());
        }
        return description;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int centerX = left() + panelWidth() / 2;
        graphics.centeredText(
                this.font,
                Component.translatable("skyblockmulti.tree_screen.page", page + 1, pageCount),
                centerX,
                bottom() - 44,
                color(ClientPanelAppearance.get().label())
        );
        if (visibleCards.isEmpty()) {
            graphics.centeredText(
                    this.font,
                    Component.translatable("skyblockmulti.tree_screen.no_results"),
                    centerX,
                    top() + 68,
                    0xFFFFAA00
            );
        }
        for (VisibleCard card : visibleCards) {
            Identifier iconId = Identifier.tryParse(card.entry.iconItem());
            if (iconId != null) {
                BuiltInRegistries.ITEM.getOptional(iconId)
                        .ifPresent(item -> graphics.item(
                                new ItemStack(item),
                                card.button.getX() + 4,
                                card.button.getY() + 3
                        ));
            }
        }
    }

    @Override
    public void onClose() {
        if (!selectionCompleted && ClientPlayNetworking.canSend(SelectionMenuClosedPayload.TYPE)) {
            ClientPlayNetworking.send(SelectionMenuClosedPayload.INSTANCE);
        }
        super.onClose();
    }

    private void closeAfterSelection() {
        selectionCompleted = true;
        onClose();
    }

    private record VisibleCard(TreeCatalogPayload.Entry entry, Button button) {
    }
}
