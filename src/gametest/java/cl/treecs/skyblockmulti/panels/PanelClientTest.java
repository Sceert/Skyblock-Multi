package cl.treecs.skyblockmulti.panels;

import cl.treecs.skyblockmulti.client.IslandPanelScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

/** Exercises the native panel in an isolated client world. */
public final class PanelClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (Boolean.getBoolean("skyblockmulti.boundaryAudit")) {
            ProtectionBoundaryAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.v056Regression")) {
            PendingAudit.run(context);
            CoreWorldAudit.run(context);
            VaultAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.v055Regression")) {
            cl.treecs.skyblockmulti.infernal.DynamicContractAudit.run(context);
            cl.treecs.skyblockmulti.infernal.DynamicTrialAudit.run(context);
            TrialsAudit.run(context);
            TreeMatrixAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.dynamicContractAudit")) {
            cl.treecs.skyblockmulti.infernal.DynamicContractAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.dynamicAudit")) {
            cl.treecs.skyblockmulti.infernal.DynamicTrialAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.nativePanelAudit")) {
            NativePanelAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.treesAudit")) {
            TreeMatrixAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.vaultAudit")) {
            VaultAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.protectionAudit")) {
            ProtectionAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.dedicatedAudit")) {
            DedicatedAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.trialsAudit")) {
            TrialsAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.coreAudit")) {
            CoreWorldAudit.run(context);
            return;
        }
        if (Boolean.getBoolean("skyblockmulti.audit")) {
            PendingAudit.run(context);
            return;
        }
        try (var world = context.worldBuilder().create()) {
            context.waitFor(mc -> mc.player != null);
            context.waitTicks(25);
            context.runOnClient(mc -> mc.setScreenAndShow(null));
            context.waitTicks(3);
            context.runOnClient(mc -> require(KeyMapping.get("key.skyblockmulti.panel") != null, "Native key installed"));
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitTicks(20);
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitForScreen(IslandPanelScreen.class);
            context.runOnClient(mc -> require(!mc.gui.screen().isPauseScreen(), "Native screen does not pause"));
            context.takeScreenshot("skyblock-panel-native");
            context.getInput().pressKey(InputConstants.KEY_I);
            context.waitFor(mc -> mc.gui.screen() == null);
        }
        context.waitTicks(3);
    }
    static void require(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
