package cl.treecs.skyblockmulti.panels;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import java.util.Properties;

/** Fabric's isolated dedicated-server harness, never an existing server directory. */
final class DedicatedAudit {
    static void run(ClientGameTestContext context) {
        Properties properties = new Properties();
        properties.setProperty("level-type", "skyblockmulti:skyblockmulti");
        properties.setProperty("online-mode", "false");
        properties.setProperty("server-ip", "127.0.0.1");
        properties.setProperty("view-distance", "2");
        properties.setProperty("simulation-distance", "2");
        try (var server = context.worldBuilder().adjustSettings(settings -> settings.setWorldType(
                settings.getNormalPresetList().stream().filter(entry -> entry.preset().unwrapKey()
                        .map(key -> key.identifier().toString().equals("skyblockmulti:skyblockmulti"))
                        .orElse(false)).findFirst().orElseThrow())).createServer(properties)) {
            server.runOnServer(mc -> {
                require(mc.overworld().getChunkSource().getGenerator() instanceof FlatLevelSource, "dedicated Overworld uses Skyblock preset");
                require(mc.getLevel(Level.NETHER).getChunkSource().getGenerator() instanceof FlatLevelSource, "dedicated Nether is void");
                require(mc.getLevel(Level.END).getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator, "dedicated End remains normal");
            });
            try (var connection = server.connect()) {
                context.waitFor(mc -> mc.player != null);
                server.waitFor(mc -> !mc.getPlayerList().getPlayers().isEmpty());
                server.runOnServer(mc -> require(mc.getPlayerList().getPlayers().getFirst().entityTags().contains("skyblock_registered_v1"),
                        "dedicated connection completes Skyblock first join"));
            }
        }
    }
    private static void require(boolean passed, String name) {
        System.out.println("[DEDICATED-AUDIT] " + (passed ? "PASS " : "FAIL ") + name);
        if (!passed) throw new AssertionError(name);
    }
}
