package cl.treecs.skyblockmulti.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.AbstractWidget;

/** Shared small, non-pausing surface. The world remains visible without a blur or dimmer. */
public abstract class CompactIslandScreen extends Screen {
    // Shared across the panel and selection steps; normalized so resizing cannot strand it off-screen.
    private static double horizontal = 1.0;
    private static double vertical = 0.5;
    private boolean moving;
    private double grabX;
    private double grabY;
    protected CompactIslandScreen(Component title) { super(title); }
    protected int panelWidth() { return Math.min(ClientPanelAppearance.get().width(), width - 16); }
    protected int preferredHeight() { return ClientPanelAppearance.get().height(); }
    protected int panelHeight() { return Math.min(preferredHeight(), Math.min(ClientPanelAppearance.get().height(), height - 16)); }
    protected int left() { return 8 + (int) Math.round(horizontal * Math.max(0, width - panelWidth() - 16)); }
    protected int top() { return 8 + (int) Math.round(vertical * Math.max(0, height - panelHeight() - 16)); }
    protected int bottom() { return top() + panelHeight(); }
    protected int innerWidth() { return panelWidth() - 20; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && event.x() >= left() && event.x() < left() + panelWidth() - 70
                && event.y() >= top() && event.y() < top() + 26) {
            moving = true;
            grabX = event.x() - left();
            grabY = event.y() - top();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!moving || event.button() != 0) return super.mouseDragged(event, dx, dy);
        int oldLeft = left(), oldTop = top();
        horizontal = fraction(event.x() - grabX - 8, width - panelWidth() - 16);
        vertical = fraction(event.y() - grabY - 8, height - panelHeight() - 16);
        for (var child : children()) if (child instanceof AbstractWidget widget) {
            widget.setX(widget.getX() + left() - oldLeft);
            widget.setY(widget.getY() + top() - oldTop);
        }
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (moving && event.button() == 0) { moving = false; return true; }
        return super.mouseReleased(event);
    }
    private static double fraction(double position, int available) {
        return available <= 0 ? 0 : Math.clamp(position / available, 0.0, 1.0);
    }
    protected void addCloseButton() {
        addRenderableWidget(Button.builder(Component.literal("C"), button -> openConfiguration())
                .bounds(left() + panelWidth() - 66, top() + 4, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("skyblockmulti.panel.settings"))).build());
        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            minimize();
        }).bounds(left() + panelWidth() - 44, top() + 4, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("skyblockmulti.panel.minimize"))).build());
        addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
                .bounds(left() + panelWidth() - 22, top() + 4, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("skyblockmulti.panel.close"))).build());
    }
    protected void openConfiguration() { minecraft.setScreenAndShow(ClothConfigScreenFactory.create(this, true)); }
    protected void minimize() { onClose(); ClientIslandPanel.markMinimized(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { ClientIslandPanel.clearMinimized(); super.onClose(); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        var theme = ClientPanelAppearance.get();
        graphics.fill(left() - 1, top() - 1, left() + panelWidth() + 1, bottom() + 1, color(theme.border()));
        graphics.fill(left(), top(), left() + panelWidth(), bottom(), color(theme.background()));
        graphics.fill(left(), top(), left() + panelWidth(), top() + 26, color(theme.header()));
        var lines = font.split(title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), panelWidth() - 78);
        if (!lines.isEmpty()) graphics.text(font, lines.getFirst(), left() + 8, top() + 10, color(theme.accent()));
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (!moving && mouseX >= left() && mouseX < left() + panelWidth() - 70
                && mouseY >= top() && mouseY < top() + 26) graphics.setTooltipForNextFrame(font,
                        Component.translatable("skyblockmulti.panel.drag"), mouseX, mouseY);
    }
    protected static int color(int rgb) { return cl.treecs.skyblockmulti.island.PanelAppearance.opaque(rgb); }
    protected int text(GuiGraphicsExtractor graphics, Component text, int y, int limit, int color) {
        for (var line : font.split(text, innerWidth())) {
            if (y + font.lineHeight > limit) break;
            graphics.text(font, line, left() + 10, y, color);
            y += font.lineHeight + 2;
        }
        return y;
    }
}
