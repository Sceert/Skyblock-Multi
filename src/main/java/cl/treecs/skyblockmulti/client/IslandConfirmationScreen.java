package cl.treecs.skyblockmulti.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact review; long localized warnings remain readable through pagination. */
public final class IslandConfirmationScreen extends CompactIslandScreen {
    private final Screen parent;
    private final Component summary;
    private final Runnable confirmAction;
    private int page;
    private boolean submitted;

    public IslandConfirmationScreen(Screen parent, Component tree, Component difficulty, int difficultyValue, Runnable confirmAction) {
        super(Component.translatable("skyblockmulti.selection.confirm.title"));
        this.parent = parent;
        this.confirmAction = confirmAction;
        var text = Component.translatable("skyblockmulti.selection.confirm.tree", tree).append("\n")
                .append(Component.translatable("skyblockmulti.selection.confirm.difficulty", difficulty)).append("\n\n")
                .append(Component.translatable("skyblockmulti.selection.confirm.prompt"));
        if (difficultyValue == 1) text.append("\n\n")
                .append(Component.translatable("skyblockmulti.difficulty.extreme.warning.title")).append("\n")
                .append(Component.translatable("skyblockmulti.selection.confirm.extreme.resources")).append("\n")
                .append(Component.translatable("skyblockmulti.selection.confirm.extreme.propagation"));
        summary = text;
    }
    private int linesPerPage() { return Math.max(1, (panelHeight() - 100) / (font.lineHeight + 2)); }
    private int pages() { return Math.max(1, (font.split(summary, innerWidth()).size() + linesPerPage() - 1) / linesPerPage()); }
    @Override protected void init() {
        page = Math.min(page, pages() - 1);
        int w = (innerWidth() - 6) / 2;
        Button accept = addRenderableWidget(Button.builder(Component.translatable("skyblockmulti.panel.accept"), button -> {
            if (submitted) return;
            submitted = true;
            button.active = false;
            confirmAction.run();
        }).bounds(left() + 10, bottom() - 30, w, 20).build());
        accept.active = !submitted && page == pages() - 1;
        addRenderableWidget(Button.builder(Component.translatable("skyblockmulti.selection.confirm.cancel"), button -> minecraft.setScreenAndShow(parent))
                .bounds(left() + 16 + w, bottom() - 30, w, 20).build());
        if (pages() > 1) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuildWidgets(); })
                    .bounds(left() + 10, bottom() - 56, 24, 18).build());
            previous.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuildWidgets(); })
                    .bounds(left() + panelWidth() - 34, bottom() - 56, 24, 18).build());
            next.active = page + 1 < pages();
        }
        addCloseButton();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        var lines = font.split(summary, innerWidth());
        int start = page * linesPerPage();
        for (int i = start; i < Math.min(lines.size(), start + linesPerPage()); i++)
            graphics.text(font, lines.get(i), left() + 10, top() + 35 + (i - start) * (font.lineHeight + 2), color(ClientPanelAppearance.get().value()));
        if (pages() > 1) graphics.centeredText(font, Component.translatable("skyblockmulti.tree_screen.page", page + 1, pages()),
                left() + panelWidth() / 2, bottom() - 51, color(ClientPanelAppearance.get().label()));
    }
}
