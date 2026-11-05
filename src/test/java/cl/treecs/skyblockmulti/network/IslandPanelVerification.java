package cl.treecs.skyblockmulti.network;

import net.minecraft.network.chat.Component;
import java.util.List;

/** Protocol regression checks: only a currently advertised decision can run. */
public final class IslandPanelVerification {
    public static void main(String[] args) {
        var choice = entry("party-a:home-a");
        var shown = new IslandPanelPayload(41, true, List.of(choice));
        require(shown.permits(41, List.of(choice), "island_party"), "Current choice must work");
        require(!shown.permits(40, List.of(choice), "island_party"), "Previous session cannot answer");
        require(!shown.permits(41, List.of(entry("party-b:home-a")), "island_party"), "Changed membership invalidates choice");
        require(!shown.permits(41, List.of(entry("party-a:home-b")), "island_party"), "Changed home invalidates choice");
        require(!shown.permits(41, List.of(), "island_party"), "Expired decision cannot run");
        require(!shown.permits(41, List.of(choice), "abandon_yes"), "Another player's vote is not available");
        require(!shown.permits(41, List.of(choice), "op @s"), "Arbitrary commands are not actions");
        var acknowledged = new IslandPanelPayload(42, false, List.of(choice));
        require(!acknowledged.permits(41, List.of(choice), "island_party"), "Duplicate packet cannot replay after acknowledgement");
        var voted = new IslandPanelPayload.Entry(2, "vote:1", Component.literal("Recorded"), List.of());
        require(!new IslandPanelPayload(43, false, List.of(voted)).permits(43, List.of(voted), "abandon_yes"), "Recorded vote has no repeat button");
        try { new IslandPanelPayload.Entry(3, "invalid", Component.empty(), List.of()); throw new AssertionError("Invalid tab accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Island panel verification passed");
    }
    private static IslandPanelPayload.Entry entry(String context) {
        return new IslandPanelPayload.Entry(2, context, Component.literal("Choose island"), List.of(
                new IslandPanelPayload.Action("island_party", Component.literal("Accept")),
                new IslandPanelPayload.Action("island_own", Component.literal("Reject"))));
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
