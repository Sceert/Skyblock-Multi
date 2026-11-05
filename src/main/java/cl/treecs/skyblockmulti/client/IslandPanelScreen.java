package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.IslandPanelPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

/** Two-column facts and paged decisions, sized to content within the user's limits. */
public final class IslandPanelScreen extends CompactIslandScreen {
    private final IslandPanelState state;
    private final List<Button> actions = new ArrayList<>();
    private record Line(FormattedCharSequence label, FormattedCharSequence value, int row, boolean note) {}

    public IslandPanelScreen(IslandPanelState state) {
        super(Component.translatable("skyblockmulti.panel.title"));
        this.state = state;
    }
    public void update() { rebuildWidgets(); }
    private List<IslandPanelPayload.Entry> entries() { return state.entries(); }
    private IslandPanelPayload.Entry entry() { return state.entry(); }
    private int step() { return font.lineHeight + 5; }
    private int labelWidth() { return (innerWidth() - 12) * 43 / 100; }
    private int actionHeight() { return entries().isEmpty() || entry().actions().isEmpty() ? 0 : 26; }
    private List<Line> lines() {
        if (entries().isEmpty()) return List.of();
        var result = new ArrayList<Line>();
        var theme = ClientPanelAppearance.get();
        int index = 0;
        for (var row : entry().rows()) {
            var labels = font.split(row.label(), labelWidth());
            boolean score = row.label().equals(Component.translatable("skyblockmulti.panel.field.score"));
            var values = font.split(row.value().copy().withStyle(style -> style.withBold(true)
                    .withColor(score ? theme.accent() : theme.value())), innerWidth() - labelWidth() - 12);
            for (int i = 0; i < Math.max(labels.size(), values.size()); i++) result.add(new Line(
                    i < labels.size() ? labels.get(i) : FormattedCharSequence.EMPTY,
                    i < values.size() ? values.get(i) : FormattedCharSequence.EMPTY, index, false));
            index++;
        }
        if (!entry().body().getString().isBlank()) {
            for (var line : font.split(entry().body(), innerWidth())) result.add(new Line(line, null, -1, !entry().rows().isEmpty()));
        }
        return result;
    }
    @Override protected int preferredHeight() {
        return Math.max(112, 80 + actionHeight() + lines().size() * step() + (entries().size() > 1 ? 22 : 0));
    }
    private boolean navigation() { return entries().size() > 1 || lines().size() * step() > panelHeight() - 80 - actionHeight(); }
    private int lineCount() { return Math.max(1, (panelHeight() - 80 - actionHeight() - (navigation() ? 22 : 0)) / step()); }
    private int textPages() { return Math.max(1, (lines().size() + lineCount() - 1) / lineCount()); }
    private int navY() { return bottom() - 40 - actionHeight(); }

    @Override protected void init() {
        if (state.refreshOnReturn) {
            state.refreshOnReturn = false;
            state.refresh();
        }
        actions.clear();
        addCloseButton();
        String[] tabs = {"island", "group", "notices"};
        var visibleTabs = state.snapshot().entries().stream().map(IslandPanelPayload.Entry::tab).distinct().sorted().toList();
        int w = (innerWidth() - (visibleTabs.size() - 1) * 4) / Math.max(1, visibleTabs.size());
        for (int i = 0; i < visibleTabs.size(); i++) {
            final int selected = visibleTabs.get(i);
            var label = Component.translatable("skyblockmulti.panel." + tabs[selected]);
            if (state.tab == selected) label.withStyle(style -> style.withBold(true).withColor(ClientPanelAppearance.get().accent()));
            Button button = addRenderableWidget(Button.builder(label, clicked -> {
                state.tab = selected; state.page = 0; state.textPage = 0; rebuildWidgets();
            }).bounds(left() + 10 + i * (w + 4), top() + 32, w, 20).build());
            button.active = state.tab != selected;
        }
        if (entries().isEmpty()) return;
        state.textPage = Math.min(state.textPage, textPages() - 1);
        var buttons = entry().actions();
        int buttonWidth = buttons.isEmpty() ? innerWidth() : (innerWidth() - (buttons.size() - 1) * 6) / buttons.size();
        for (int i = 0; i < buttons.size(); i++) {
            var action = buttons.get(i);
            Button button = addRenderableWidget(Button.builder(action.label(), clicked -> {
                if (state.pending()) return;
                actions.forEach(b -> b.active = false);
                state.act(action.id());
            }).bounds(left() + 10 + i * (buttonWidth + 6), bottom() - 42, buttonWidth, 20).build());
            button.active = !state.pending() && state.valid() && state.textPage == textPages() - 1;
            actions.add(button);
        }
        if (navigation()) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), clicked -> {
                if (state.textPage > 0) state.textPage--; else { state.page--; state.textPage = 0; }
                rebuildWidgets();
            }).bounds(left() + 10, navY(), 24, 18).build());
            previous.active = state.textPage > 0 || state.page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), clicked -> {
                if (state.textPage + 1 < textPages()) state.textPage++; else { state.page++; state.textPage = 0; }
                rebuildWidgets();
            }).bounds(left() + panelWidth() - 34, navY(), 24, 18).build());
            next.active = state.textPage + 1 < textPages() || state.page + 1 < entries().size();
        }
    }
    @Override public void tick() {}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (entries().isEmpty()) return;
        var theme = ClientPanelAppearance.get();
        var content = lines();
        int start = state.textPage * lineCount();
        for (int i = start; i < Math.min(content.size(), start + lineCount()); i++) {
            var line = content.get(i);
            int y = top() + 60 + (i - start) * step();
            if (line.value() != null) {
                if (line.row() % 2 == 0) graphics.fill(left() + 7, y - 2, left() + panelWidth() - 7, y + step() - 2,
                        0x35000000 | theme.header());
                graphics.text(font, line.label(), left() + 10, y, color(theme.label()));
                graphics.text(font, line.value(), left() + 22 + labelWidth(), y, color(theme.value()));
            } else graphics.text(font, line.label(), left() + 10, y, color(line.note() ? theme.label() : theme.value()));
        }
        if (navigation()) graphics.centeredText(font,
                Component.translatable("skyblockmulti.tree_screen.page", textPages() > 1 ? state.textPage + 1 : state.page + 1, textPages() > 1 ? textPages() : entries().size()),
                left() + panelWidth() / 2, navY() + 5, color(theme.label()));
        graphics.centeredText(font, Component.translatable("skyblockmulti.panel.footer"),
                left() + panelWidth() / 2, bottom() - 13, color(theme.label()));
    }
    @Override public void removed() {}
    @Override public void onClose() {
        ClientIslandPanel.closeLocal(false);
    }
    @Override protected void minimize() { ClientIslandPanel.closeLocal(true); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (ClientIslandPanel.isPanelKey(event)) { ClientIslandPanel.play(); return true; }
        return super.keyPressed(event);
    }
    @Override protected void openConfiguration() {
        state.refreshOnReturn = true;
        super.openConfiguration();
    }
}
