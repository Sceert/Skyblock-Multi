package cl.treecs.skyblockmulti.api.party;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable verification for deterministic optional-provider selection. */
public final class PartyApiVerification {
    private PartyApiVerification() {
    }

    public static void main(String[] args) {
        UUID partyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        PartySnapshot openPacSnapshot = new PartySnapshot(true, UUID.randomUUID(), ownerId, "OpenPacOwner", false);
        PartySnapshot nexusSnapshot = new PartySnapshot(true, partyId, ownerId, "NexusOwner", false);

        PartyApi.clearProvidersForTesting();
        PartyApi.registerProvider(provider("skyblockmulti:openpac", 100, true, openPacSnapshot));
        PartyApi.registerProvider(provider("skyblockmulti:nexus_parties", 200, true, nexusSnapshot));
        require("skyblockmulti:nexus_parties".equals(PartyApi.activeProviderId()),
                "Nexus Parties must have priority over OpenPAC");
        require(PartyApi.snapshot(null).partyId().equals(partyId),
                "Only the authoritative Nexus Parties snapshot may be returned");

        AtomicInteger fallbackQueries = new AtomicInteger();
        PartyApi.clearProvidersForTesting();
        PartyApi.registerProvider(countingProvider(
                "skyblockmulti:openpac", 100, fallbackQueries, openPacSnapshot
        ));
        PartyApi.registerProvider(new PartyProvider() {
            @Override public String id() { return "skyblockmulti:nexus_parties"; }
            @Override public int priority() { return 200; }
            @Override public boolean isAvailable() { return true; }
            @Override public PartySnapshot snapshot(ServerPlayer player) {
                throw new IllegalStateException("simulated authoritative failure");
            }
        });
        require(PartyApi.snapshot(null) == PartySnapshot.solo(),
                "An authoritative failure must degrade to solo, not a second party authority");
        require(fallbackQueries.get() == 0,
                "OpenPAC must not be queried while Nexus Parties is authoritative");

        PartyApi.clearProvidersForTesting();
        PartyApi.registerProvider(provider("skyblockmulti:openpac", 100, true, openPacSnapshot));
        PartyApi.registerProvider(provider("skyblockmulti:nexus_parties", 200, false, nexusSnapshot));
        require("skyblockmulti:openpac".equals(PartyApi.activeProviderId()),
                "OpenPAC must remain the fallback when Nexus Parties is unavailable");

        PartyApi.clearProvidersForTesting();
        require(PartyApi.snapshot(null) == PartySnapshot.solo(),
                "No provider must resolve to solo mode");

        PartyHomeRegistry.clearRuntime();
        UUID oldOwner = UUID.randomUUID();
        UUID newOwner = UUID.randomUUID();
        PartySnapshot originalParty = new PartySnapshot(true, partyId, oldOwner, "OldOwner", true);
        require(PartyHomeRegistry.homeOwner(originalParty).equals(oldOwner),
                "A new party home must start at its original owner island");
        PartyHomeRegistry.onEvent(new PartyChangeEvent(
                PartyChangeEvent.Type.OWNER_CHANGED, partyId, oldOwner, newOwner), 1_000L);
        PartySnapshot transferredLeadership = new PartySnapshot(true, partyId, newOwner, "NewOwner", true);
        require(PartyHomeRegistry.homeOwner(transferredLeadership).equals(oldOwner),
                "Leadership transfer must not implicitly move PARTY_HOME");
        require(PartyHomeRegistry.pendingForDecisionMaker(newOwner) != null,
                "The new owner must receive the PARTY_HOME decision");
        require(PartyHomeRegistry.pendingForDecisionMaker(oldOwner) == null,
                "The previous owner must not receive the leadership decision");
        require(PartyHomeRegistry.moveHomeToNewLeader(partyId, newOwner),
                "The new owner must be able to choose the new PARTY_HOME");
        require(PartyHomeRegistry.homeOwner(transferredLeadership).equals(newOwner),
                "Approved move must point PARTY_HOME at the new leader");
        PartyHomeRegistry.onEvent(new PartyChangeEvent(
                PartyChangeEvent.Type.DISBANDED, partyId, newOwner, null), 2_000L);
        require(PartyHomeRegistry.pending(partyId) == null,
                "Disbanding must remove pending leadership decisions");
        PartyHomeRegistry.setHome(partyId, oldOwner);
        require(!PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:openpac", null),
                "Unknown membership must preserve the existing personal home");
        require(!PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:openpac", true),
                "Offline owners who remain members must retain their home");
        require(!PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:nexus_parties", false),
                "NP must keep its coordinated transfer rules");
        require(PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:openpac", false),
                "A departed personal owner must relinquish the party home reference");
        require(PartyHomeRegistry.homeOwner(transferredLeadership).equals(newOwner),
                "Fallback home must reference the current leader without transferring OWN");
        require(!PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:openpac", false),
                "Repeated observations must not replay the fallback");
        require(PartyHomeRegistry.personalHomeDestination(true, true)
                        == PartyHomeRegistry.PersonalHomeDestination.OWN,
                "Members with OWN must prefer their own island");
        require(PartyHomeRegistry.personalHomeDestination(true, false)
                        == PartyHomeRegistry.PersonalHomeDestination.OWN,
                "OWN must survive even when the leader has no island");
        require(PartyHomeRegistry.personalHomeDestination(false, true)
                        == PartyHomeRegistry.PersonalHomeDestination.LEADER,
                "Members without OWN must use the available leader island");
        require(PartyHomeRegistry.personalHomeDestination(false, false)
                        == PartyHomeRegistry.PersonalHomeDestination.HUB,
                "Members without an available island must follow the HUB reset");
        try {
            var directory = java.nio.file.Files.createTempDirectory("sm-personal-home-");
            var path = directory.resolve("homes.properties");
            PartyHomeRegistry.open(path);
            PartyHomeRegistry.setHome(partyId, oldOwner);
            PartyHomeRegistry.clearRuntime();
            PartyHomeRegistry.open(path);
            require(PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:openpac", false),
                    "A departed owner must be detected after reopening the world");
            PartyHomeRegistry.clearRuntime();
            PartyHomeRegistry.open(path);
            require(PartyHomeRegistry.homeOwner(transferredLeadership).equals(newOwner),
                    "The replacement home must survive a restart");
            require(!PartyHomeRegistry.refreshPersonalHome(transferredLeadership, "skyblockmulti:openpac", false),
                    "Restart must not replay an already persisted home replacement");
            PartyHomeRegistry.clearRuntime();
            java.nio.file.Files.delete(path);
            java.nio.file.Files.delete(directory);
        } catch (java.io.IOException failure) {
            throw new AssertionError("Personal PARTY_HOME persistence verification failed", failure);
        }
        System.out.println("Party API provider verification passed.");
    }

    private static PartyProvider provider(String id, int priority, boolean available,
                                          PartySnapshot snapshot) {
        return new PartyProvider() {
            @Override public String id() { return id; }
            @Override public int priority() { return priority; }
            @Override public boolean isAvailable() { return available; }
            @Override public PartySnapshot snapshot(ServerPlayer player) { return snapshot; }
        };
    }

    private static PartyProvider countingProvider(String id, int priority, AtomicInteger queries,
                                                  PartySnapshot snapshot) {
        return new PartyProvider() {
            @Override public String id() { return id; }
            @Override public int priority() { return priority; }
            @Override public boolean isAvailable() { return true; }
            @Override public PartySnapshot snapshot(ServerPlayer player) {
                queries.incrementAndGet();
                return snapshot;
            }
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
