package cl.treecs.skyblockmulti.island;

import java.nio.file.Files;
import java.util.UUID;

public final class IslandDetailsVerification {
    public static void main(String[] args) throws Exception {
        for (var ring : IslandRingLayout.Ring.values()) {
            require(IslandRingLayout.ringForSlot(ring.firstSlot()).orElseThrow() == ring, "Ring start");
            require(IslandRingLayout.ringForSlot(ring.firstSlot() + ring.positions() - 1).orElseThrow() == ring, "Ring end");
        }
        require(IslandRingLayout.ringForSlot(0).isEmpty() && IslandRingLayout.ringForSlot(129).isEmpty(), "No invented ring");
        require(IslandProgressScore.countsAsMilestone("skyblockmulti", true, true, false), "Visible milestone counts");
        require(!IslandProgressScore.countsAsMilestone("skyblockmulti", true, true, true), "Secrets excluded");
        require(!IslandProgressScore.countsAsMilestone("skyblockmulti", false, true, false), "Root excluded");
        require(!IslandProgressScore.countsAsMilestone("skyblockmulti", true, false, false), "Technical advances excluded");
        require(!IslandProgressScore.countsAsMilestone("minecraft", true, true, false), "Vanilla excluded");
        require(IslandProgressScore.points(3) == 30, "Documented score weights");
        verifyAppearance();

        var directory = Files.createTempDirectory("skyblock-island-details-");
        var file = directory.resolve("trees.properties");
        UUID owner = UUID.randomUUID(), nextOwner = UUID.randomUUID();
        try {
            var history = new IslandTreeHistory(file);
            require(history.tree(9) == null, "Old island must remain unknown");
            history.remember(owner, "minecraft:oak");
            history.remember(owner, "example:custom_tree");
            history = new IslandTreeHistory(file);
            require("example:custom_tree".equals(history.pending(owner)), "Selection survives restart");
            history.commit(owner, 9);
            history = new IslandTreeHistory(file);
            require("example:custom_tree".equals(history.tree(9)), "Actual selected ID persists");
            require(history.pending(owner) == null, "Committed pending state cleared");
            history.remember(nextOwner, "minecraft:birch");
            history.commit(nextOwner, 9);
            require("example:custom_tree".equals(history.tree(9)), "Ownership change never rewrites starter tree");
            require(Files.exists(file.resolveSibling("trees.properties.bak")), "Recoverable backup kept");
            Files.writeString(file, "schema=99\n");
            try { new IslandTreeHistory(file); throw new AssertionError("Unknown schema accepted"); }
            catch (IllegalStateException expected) { }
            require(Files.readString(file).equals("schema=99\n"), "Invalid data preserved");
        } finally {
            Files.deleteIfExists(file.resolveSibling("trees.properties.tmp"));
            Files.deleteIfExists(file.resolveSibling("trees.properties.bak"));
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        System.out.println("Island details verification passed");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void verifyAppearance() throws Exception {
        var directory = Files.createTempDirectory("skyblock-panel-config-");
        var file = directory.resolve("client.toml");
        try {
            require(PanelAppearance.load(file).equals(PanelAppearance.DEFAULT), "Absent client config uses defaults");
            Files.writeString(file, "[other]\nkeep = 42\n\n[island_panel]\nwidth = 9999\nheight = invalid\nhud_x = 150\nbackground = 0x123456\ncustom = 7\n\n[next]\nkeep = 99\n");
            var appearance = PanelAppearance.load(file);
            require(appearance.width() == 420 && appearance.height() == 240 && appearance.hudX() == 100, "Unsafe and invalid settings bounded");
            require(appearance.background() == 0x123456, "Hex colors load");
            appearance.save(file);
            require(PanelAppearance.load(file).equals(appearance), "Preferences round trip");
            String saved = Files.readString(file);
            require(saved.contains("[other]\nkeep = 42") && saved.contains("[next]\nkeep = 99") && saved.contains("custom = 7"), "Unrelated settings preserved");
            require(Files.exists(directory.resolve("client.toml.bak")), "Client config backup retained");
        } finally {
            Files.deleteIfExists(directory.resolve("client.toml.tmp"));
            Files.deleteIfExists(directory.resolve("client.toml.bak"));
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
