package cl.treecs.skyblockmulti.client;

import cl.treecs.skyblockmulti.network.IslandPanelPayload;
import cl.treecs.skyblockmulti.network.IslandPanelRequestPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

public final class ClientIslandPanel {
    private static IslandPanelState state;
    public static IslandPanelState createSession() {
        if (state != null) state.close(false);
        state = new IslandPanelState(request -> {
            if (!ClientPlayNetworking.canSend(IslandPanelRequestPayload.TYPE)) throw new IllegalStateException("Panel protocol unavailable");
            ClientPlayNetworking.send(request);
        });
        return state;
    }
    public static void closeSession(IslandPanelState session, boolean disconnect) {
        session.close(disconnect);
        if (state == session) state = null;
    }
    private static boolean minimized;
    private static IslandPanelScreen panel;
    private static KeyMapping panelKey;
    private ClientIslandPanel() {}
    public static void markMinimized() { minimized = true; }
    public static void clearMinimized() { minimized = false; }
    public static boolean retained(IslandPanelScreen screen) { return panel == screen; }
    public static void dismiss() {
        if (state != null) closeSession(state, false);
        panel = null; minimized = false;
    }
    public static boolean isPanelKey(net.minecraft.client.input.KeyEvent event) { return panelKey != null && panelKey.matches(event); }
    public static void play() {
        if (panel == null) return;
        transitionLocal(() -> {
            panel.clearFocus();
            net.minecraft.client.Minecraft.getInstance().setScreenAndShow(null);
        });
    }
    public static void closeLocal(boolean minimize) {
        transitionLocal(() -> {
            if (minimize) { minimized = true; panel.clearFocus(); }
            else dismiss();
            net.minecraft.client.Minecraft.getInstance().setScreenAndShow(null);
        });
    }
    private static void transitionLocal(Runnable commit) {
        if (state == null) return;
        var expected = state;
        var client = net.minecraft.client.Minecraft.getInstance();
        var expectedScreen = client.gui.screen();
        var preparation = !state.valid() && !state.pending() ? state.refresh() : state.prepareBackground();
        preparation.thenAccept(result -> client.execute(() -> {
            if (state != expected || client.player == null || client.gui.screen() != expectedScreen) return;
            if (result.accepted()) commit.run();
            else client.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.translatable(result.key()));
        }));
    }
    private static void openLocal() {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (panel != null) {
            minimized = false;
            if (!state.valid() && !state.pending()) state.refresh();
            client.setScreenAndShow(panel);
        } else if (client.player != null && ClientPlayNetworking.canSend(IslandPanelRequestPayload.TYPE))
            ClientPlayNetworking.send(new IslandPanelRequestPayload(0, "open"));
    }
    private static int position(int available, int size, int percent) {
        return 8 + Math.max(0, available - size - 16) * percent / 100;
    }
    public static void register() {
        ClientPanelAppearance.load();
        registerLocal();
        ClientPlayNetworking.registerGlobalReceiver(IslandPanelPayload.TYPE, (payload, context) -> {
            var connection = context.client().getConnection();
            context.client().execute(() -> {
                if (connection != context.client().getConnection() || context.client().player == null) return;
                if (state != null && state.update(payload)) {
                    if (panel != null) panel.update();
                    if (payload.open() && payload.request() == 0) {
                        minimized = false;
                    }
                    return;
                }
                // Only unsolicited command/link opens may start a new session. Late receipts cannot.
                if (payload.open() && payload.request() == 0 && payload.session() == 0) {
                    if (state == null) {
                        minimized = false;
                        panel = new IslandPanelScreen(createSession());
                        panel.init(context.client().getWindow().getGuiScaledWidth(), context.client().getWindow().getGuiScaledHeight());
                        panel.clearFocus();
                    }
                    state.refresh();
                }
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (state != null) state.tick();
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (state != null) closeSession(state, true);
            panel = null; minimized = false;
        });
    }
    private static void registerLocal() {
        var key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skyblockmulti.panel", InputConstants.KEY_I, KeyMapping.Category.MULTIPLAYER));
        panelKey = key;
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!minimized || !(screen instanceof net.minecraft.client.gui.screens.ChatScreen)) return;
            var theme = ClientPanelAppearance.get();
            var label = net.minecraft.network.chat.Component.translatable("skyblockmulti.panel.title")
                    .append(" · ").append(key.getTranslatedKeyMessage());
            int w = Math.min(width - 16, client.font.width(label) + 16);
            net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(
                    net.minecraft.client.gui.components.Button.builder(label, button -> {
                        openLocal();
                    }).bounds(position(width, w, theme.hudX()), position(height - 24, 22, theme.hudY()), w, 22).build());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.gui.screen() instanceof IslandPanelScreen) play();
                else if (client.gui.screen() instanceof CompactIslandScreen screen) screen.onClose();
                else if (client.gui.screen() == null) openLocal();
            }
        });
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
                net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.HOTBAR,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("skyblockmulti", "island_panel_hint"), (graphics, delta) -> {
                    var client = net.minecraft.client.Minecraft.getInstance();
                    if (panel != null && !minimized && client.player != null && client.gui.screen() == null) {
                        if (panel.width != graphics.guiWidth() || panel.height != graphics.guiHeight())
                            panel.resize(graphics.guiWidth(), graphics.guiHeight());
                        panel.clearFocus();
                        panel.extractRenderState(graphics, -1000, -1000, 0);
                        return;
                    }
                    if (!minimized || client.player == null || client.gui.screen() != null) return;
                    var label = net.minecraft.network.chat.Component.translatable("skyblockmulti.panel.title")
                            .append(" · ").append(key.getTranslatedKeyMessage());
                    var theme = ClientPanelAppearance.get();
                    int w = Math.min(graphics.guiWidth() - 16, client.font.width(label) + 16);
                    int x = position(graphics.guiWidth(), w, theme.hudX());
                    int y = position(graphics.guiHeight() - 24, 22, theme.hudY());
                    graphics.fill(x, y, x + w, y + 22, cl.treecs.skyblockmulti.island.PanelAppearance.opaque(theme.header()));
                    graphics.text(client.font, label, x + 8, y + 7, cl.treecs.skyblockmulti.island.PanelAppearance.opaque(theme.accent()));
                });
    }
}
