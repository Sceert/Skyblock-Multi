package cl.treecs.skyblockmulti.panels;

import cl.treecs.skyblockmulti.client.ClientIslandPanel;
import cl.treecs.skyblockmulti.client.IslandPanelScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import com.mojang.blaze3d.platform.InputConstants;

final class NativePanelAudit {
    static void run(ClientGameTestContext context) {
        IslandPanelScreen[] previous = new IslandPanelScreen[1];
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null);
            context.waitTicks(25);
            context.runOnClient(mc -> mc.setScreenAndShow(null));
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitTicks(20);
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitForScreen(IslandPanelScreen.class);
            previous[0] = context.computeOnClient(mc -> (IslandPanelScreen) mc.gui.screen());
            context.clickScreenButton("-");
            context.waitFor(mc -> mc.gui.screen() == null);
            context.runOnClient(mc -> check(ClientIslandPanel.retained(previous[0]), "minimize retains panel instance"));
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitForScreen(IslandPanelScreen.class);
            context.runOnClient(mc -> check(mc.gui.screen() == previous[0], "restore reuses minimized panel"));
            context.clickScreenButton("C");
            context.waitFor(mc -> mc.gui.screen() != null && !(mc.gui.screen() instanceof IslandPanelScreen));
            context.runOnClient(mc -> check(ClientIslandPanel.retained(previous[0]), "configuration retains panel session"));
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(IslandPanelScreen.class);
            context.runOnClient(mc -> check(mc.gui.screen() == previous[0], "configuration Escape returns to same panel"));
            context.waitTicks(10);
            context.runOnClient(mc -> {
                var panel = (IslandPanelScreen) mc.gui.screen();
                for (int[] size : new int[][] {{854,480},{426,240},{320,180}}) {
                    panel.resize(size[0],size[1]);
                    boolean visible = panel.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                            .filter(widget -> widget.visible).allMatch(widget -> widget.getX() >= 0 && widget.getY() >= 0
                                    && widget.getX() + widget.getWidth() <= size[0] && widget.getY() + widget.getHeight() <= size[1]);
                    check(visible, "all panel controls fit " + size[0] + "x" + size[1]);
                }
                panel.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            });
            context.takeScreenshot("audit-native-panel-restored");
            context.clickScreenButton("X");
            context.waitFor(mc -> mc.gui.screen() == null);
            context.runOnClient(mc -> check(!ClientIslandPanel.retained(previous[0]), "X destroys retained panel"));
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitTicks(20);
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitForScreen(IslandPanelScreen.class);
            context.runOnClient(mc -> check(mc.gui.screen() != previous[0], "reopen creates fresh panel instance"));
            previous[0] = context.computeOnClient(mc -> (IslandPanelScreen) mc.gui.screen());
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(mc -> mc.gui.screen() == null);
            context.runOnClient(mc -> check(!ClientIslandPanel.retained(previous[0]), "Escape destroys retained panel"));
        }
        context.runOnClient(mc -> check(!ClientIslandPanel.retained(previous[0]), "disconnect leaves no retained panel"));
    }
    private static void check(boolean passed, String name) {
        System.out.println("[NATIVE-PANEL-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) throw new AssertionError(name);
    }
}
