package cl.treecs.skyblockmulti.infernal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.util.Map;

/** Lightweight verification executable; the project intentionally has no JUnit dependency. */
public final class InfernalDataApiVerification {
    private InfernalDataApiVerification() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var activator = java.util.UUID.randomUUID();
        var outsider = java.util.UUID.randomUUID();
        var partyId = java.util.UUID.randomUUID();
        var solo = cl.treecs.skyblockmulti.api.party.PartySnapshot.solo();
        var party = new cl.treecs.skyblockmulti.api.party.PartySnapshot(
                true, partyId, activator, "Activator", true);
        require(InfernalParticipantPolicy.eligible(false, activator, solo, outsider, solo),
                "No provider must retain the nearby-player fallback");
        require(InfernalParticipantPolicy.eligible(true, activator, solo, activator, solo),
                "A solo activator remains eligible");
        require(!InfernalParticipantPolicy.eligible(true, activator, solo, outsider, solo),
                "A provider must not enroll unrelated solo players");
        require(InfernalParticipantPolicy.eligible(true, activator, party, outsider, party),
                "Members of the activating party remain eligible");
        require(!InfernalParticipantPolicy.eligible(true, activator, party, outsider, solo),
                "Unrelated players must not scale encounters or earn rewards");
        InfernalDefinitions.Arena arena = InfernalDefinitionReloadListener.parseArena(
                "example:colosseum", object("""
                        {
                          "schema_version": 1,
                          "dimension": "minecraft:the_nether",
                          "origin": [0, 64, 0],
                          "cleanup_radius": 64,
                          "floors": {
                            "base": [[0, 0, 8]],
                            "gallery": [[12, 8, 0]],
                            "champion": [[0, 8, -12]]
                          }
                        }
                        """));

        InfernalDefinitions.Trial trial = InfernalDefinitionReloadListener.parseTrial(
                "example:ember_oath", object("""
                        {
                          "schema_version": 1,
                          "arena": "example:colosseum",
                          "display": {"text": "Ember Oath"},
                          "activations": [
                            {"item": "minecraft:blaze_powder", "custom_data": {"example:trial": "ember_oath"}}
                          ],
                          "phases": [{
                            "id": "opening",
                            "display": {"text": "The Galleries Awaken"},
                            "mobs": [{
                              "entity": "minecraft:blaze",
                              "count": 4,
                              "floor": ["base", "gallery"],
                              "placement": "round_robin"
                            }]
                          }, {
                            "id": "champion",
                            "mobs": [{
                              "entity": "minecraft:piglin_brute",
                              "count": 1,
                              "floor": "champion",
                              "placement": "fixed",
                              "champion": true,
                              "scale": 1.4,
                              "health_multiplier": 2.5
                            }]
                          }],
                          "scaling": {"maximum_count_multiplier": 2.0}
                        }
                        """), Map.of(arena.id(), arena));

        require(trial.phases().size() == 2, "Expected two phases");
        require(trial.activations().size() == 1, "Expected one activation item");
        require(trial.phases().get(1).mobs().getFirst().champion(), "Champion flag was not preserved");
        require(trial.phases().get(1).mobs().getFirst().scale() == 1.4, "Champion scale was not preserved");
        require(trial.scaling().healthPerExtraPlayer() == 0.10,
                "Default participant health scaling must be 10%");
        require(InfernalTrialRunner.participantHealthMultiplier(false, 2, trial.scaling()) == 1.10,
                "Two participants must give regular enemies 10% more health");
        require(InfernalTrialRunner.participantHealthMultiplier(true, 2, trial.scaling()) == 1.175,
                "Two participants must give champions 17.5% more health");

        expectInvalid("unknown floor", () -> InfernalDefinitionReloadListener.parseTrial(
                "example:bad_floor", object("""
                        {"schema_version":1,"arena":"example:colosseum",
                         "activations":[{"item":"minecraft:blaze_powder"}],
                         "phases":[{"mobs":[{"entity":"minecraft:blaze","count":1,"floor":"roof"}]}]}
                        """), Map.of(arena.id(), arena)));
        expectInvalid("unsafe scale", () -> InfernalDefinitionReloadListener.parseTrial(
                "example:bad_scale", object("""
                        {"schema_version":1,"arena":"example:colosseum",
                         "activations":[{"item":"minecraft:blaze_powder"}],
                         "phases":[{"mobs":[{"entity":"minecraft:blaze","count":1,"floor":"base","scale":4.0}]}]}
                        """), Map.of(arena.id(), arena)));
        expectInvalid("mob cap", () -> InfernalDefinitionReloadListener.parseTrial(
                "example:bad_count", object("""
                        {"schema_version":1,"arena":"example:colosseum",
                         "activations":[{"item":"minecraft:blaze_powder"}],
                         "phases":[{"mobs":[{"entity":"minecraft:blaze","count":65,"floor":"base"}]}]}
                        """), Map.of(arena.id(), arena)));

        System.out.println("Infernal Trial datapack schema verification passed.");
    }

    private static JsonObject object(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static void expectInvalid(String label, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected invalid definition: " + label);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
