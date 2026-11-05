package cl.treecs.skyblockmulti.api.party;

import cl.treecs.skyblockmulti.island.IslandTransferJournal;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Focused executable tests of the SM protocol, with a durable-participant test double. */
public final class IslandOwnershipTransferVerification {
    private static final UUID FROM = UUID.randomUUID(), TO = UUID.randomUUID(), PARTY = UUID.randomUUID();
    private static final IslandTransferCoordinator.Island ISLAND =
            new IslandTransferCoordinator.Island(9, 400, -2016, 2);

    public static void main(String[] args) throws Exception {
        var w = new World(); var j = new MemoryJournal(); var p = new Participant(w, j);
        var c = new IslandTransferCoordinator(w, j);
        var offer = c.offer(FROM, TO, 0);
        require(offer != null && w.owner.equals(FROM) && w.own.get(FROM).equals(ISLAND),
                "Preview must not mutate leadership or ownership");
        require(c.cancel(offer.operation(), FROM) == IslandTransferCoordinator.Result.CANCELLED,
                "Cancellation must succeed");
        require(c.confirm(offer.operation(), FROM, 1, p) == IslandTransferCoordinator.Result.REJECTED
                && j.saved.isEmpty() && w.owner.equals(FROM), "Cancelled confirmation must do nothing");
        offer = c.offer(FROM, TO, 10);
        require(c.confirm(offer.operation(), FROM, 60_010, p) == IslandTransferCoordinator.Result.REJECTED,
                "Expired confirmation must not transfer");
        offer = c.offer(FROM, TO, 70_000);
        w.owner = TO;
        require(c.confirm(offer.operation(), FROM, 70_001, p) == IslandTransferCoordinator.Result.REJECTED,
                "Stale leader must be rejected");
        w.owner = FROM;
        offer = c.offer(FROM, TO, 80_000);
        w.own.put(TO, new IslandTransferCoordinator.Island(25, 0, -3072, 3));
        require(c.confirm(offer.operation(), FROM, 80_001, p) == IslandTransferCoordinator.Result.REJECTED,
                "A recipient acquiring OWN after preview must be preserved");
        require(c.offer(FROM, TO, 80_002) == null, "Existing OWN must not enter the exception");
        w.own.remove(TO);
        offer = c.offer(FROM, TO, 90_000); w.member = false;
        require(c.confirm(offer.operation(), FROM, 90_001, p) == IslandTransferCoordinator.Result.REJECTED,
                "Departed recipient must invalidate confirmation");
        w.member = true;
        offer = c.offer(FROM, TO, 100_000); j.fail = true;
        require(c.confirm(offer.operation(), FROM, 100_001, p) == IslandTransferCoordinator.Result.FAILED
                && w.owner.equals(FROM) && w.own.get(FROM).equals(ISLAND) && !w.own.containsKey(TO)
                && w.home.equals(FROM), "Pre-decision persistence failure must change nothing");
        j.fail = false;
        offer = c.offer(FROM, TO, 110_000); p.reject = true;
        require(c.confirm(offer.operation(), FROM, 110_001, p) == IslandTransferCoordinator.Result.REJECTED
                && j.saved.isEmpty(), "NP rejecting preparation must change nothing");
        p.reject = false;
        offer = c.offer(FROM, TO, 120_000); p.interrupt = true;
        require(c.confirm(offer.operation(), FROM, 120_001, p) == IslandTransferCoordinator.Result.RECOVERY_REQUIRED,
                "An interruption after the decision must request recovery, not report failure");
        require(c.confirm(offer.operation(), FROM, 120_002, p) == IslandTransferCoordinator.Result.RECOVERY_REQUIRED,
                "A pending operation must not be falsely acknowledged as completed");
        require(c.offer(FROM, TO, 120_003) == null, "Pending recovery blocks further offers");
        p.interrupt = false;
        c = new IslandTransferCoordinator(w, j);
        require(c.recover(p), "Restart must finish the durable decision");
        require(w.owner.equals(TO) && !w.own.containsKey(FROM) && w.own.get(TO).equals(ISLAND)
                && w.home.equals(TO) && w.active.get(FROM).equals(ISLAND),
                "Transfer must produce one permanent owner while preserving the former leader's ACTIVE");
        require(p.commits == 1 && w.blocks == 73 && w.slotOccupied,
                "Transfer must not rebuild blocks, release the slot or duplicate NP commit");
        require(c.confirm(offer.operation(), FROM, 120_004, p) == IslandTransferCoordinator.Result.COMMITTED
                && p.commits == 1 && j.saved.size() == 1, "Repeated confirmation must be a receipt lookup");
        require(c.confirm(offer.operation(), TO, 120_005, p) == IslandTransferCoordinator.Result.REJECTED,
                "Only the original confirmer may look up its receipt");
        w.leave(FROM);
        require(w.resetAtHub && w.own.get(TO).equals(ISLAND) && w.slotOccupied && w.blocks == 73,
                "Former leader leaving without OWN must reset at HUB without affecting the island");
        w.own.put(FROM, new IslandTransferCoordinator.Island(25, 0, -3072, 3));
        require(c.recover(p) && w.own.get(FROM).slot() == 25,
                "Replaying an applied decision must not erase a later personal island");

        var directory = Files.createTempDirectory("skyblockmulti-transfer-verification-");
        try {
            var journal = new IslandTransferJournal(directory.resolve("decisions.json"));
            journal.save(j.saved);
            require(journal.load().equals(j.saved), "Disk journal must preserve identities and coordinates");
            PartyHomeRegistry.open(directory.resolve("homes.properties"));
            PartyHomeRegistry.setHome(PARTY, TO);
            PartyHomeRegistry.clearRuntime();
            PartyHomeRegistry.open(directory.resolve("homes.properties"));
            require(PartyHomeRegistry.homeOwner(new PartySnapshot(true, PARTY, TO, "Target", true)).equals(TO),
                    "Transferred PARTY_HOME must survive restart");
            PartyHomeRegistry.clearRuntime();
            Files.writeString(directory.resolve("decisions.json"), "corrupt");
            try { journal.load(); throw new AssertionError("Corrupt journal must fail closed"); }
            catch (IOException expected) { }
        } finally {
            PartyHomeRegistry.clearRuntime();
            try (var files = Files.list(directory)) {
                for (var file : files.toList()) Files.delete(file);
            }
            Files.delete(directory);
        }
        System.out.println("Island ownership transfer verification passed (SM protocol; NP integration pending).");
    }

    private static final class World implements IslandTransferCoordinator.WorldAccess {
        UUID owner = FROM, home = FROM;
        boolean member = true, slotOccupied = true, resetAtHub;
        int blocks = 73;
        final Map<UUID, IslandTransferCoordinator.Island> own = new HashMap<>(Map.of(FROM, ISLAND));
        final Map<UUID, IslandTransferCoordinator.Island> active = new HashMap<>(Map.of(FROM, ISLAND, TO, ISLAND));
        final Set<UUID> applied = new HashSet<>();
        public IslandTransferCoordinator.Snapshot inspect(UUID sender, UUID recipient) {
            if (!member) return null;
            return new IslandTransferCoordinator.Snapshot(IslandTransferCoordinator.PROVIDER,
                    PARTY, owner, home, sender, "Former", own.get(sender), recipient, "Target", own.get(recipient));
        }
        public void project(IslandTransferCoordinator.Decision decision) {
            if (!applied.add(decision.operation())) return;
            own.remove(decision.expected().sender());
            own.put(decision.expected().recipient(), decision.expected().own());
            home = decision.expected().recipient();
        }
        void leave(UUID player) {
            resetAtHub = !own.containsKey(player);
            active.remove(player);
        }
    }
    private static final class MemoryJournal implements IslandTransferCoordinator.Journal {
        List<IslandTransferCoordinator.Decision> saved = List.of(); boolean fail;
        public List<IslandTransferCoordinator.Decision> load() { return saved; }
        public void save(List<IslandTransferCoordinator.Decision> value) throws IOException {
            if (fail) throw new IOException("simulated failure");
            saved = value;
        }
    }
    private static final class Participant implements IslandTransferCoordinator.LeadershipParticipant {
        final World world; final MemoryJournal journal; final Set<UUID> committed = new HashSet<>();
        boolean interrupt, reject; int commits;
        Participant(World world, MemoryJournal journal) { this.world = world; this.journal = journal; }
        public boolean prepare(IslandTransferCoordinator.Decision d) { return !reject; }
        public void abort(UUID operation) { }
        public void commit(IslandTransferCoordinator.Decision d) {
            if (interrupt) throw new IllegalStateException("simulated disconnect/restart");
            require(journal.saved.contains(d), "NP must never commit before the durable decision");
            if (committed.add(d.operation())) { world.owner = d.expected().recipient(); commits++; }
        }
        public void publish(IslandTransferCoordinator.Decision d) { }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
