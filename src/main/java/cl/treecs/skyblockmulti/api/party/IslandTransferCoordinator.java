package cl.treecs.skyblockmulti.api.party;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** SM-owned half of the optional, pre-leadership island transfer protocol. */
public final class IslandTransferCoordinator {
    public static final int API_VERSION = 1;
    public static final String PROVIDER = "skyblockmulti:nexus_parties";
    public static final String WARNING_KEY = "skyblockmulti.party_transfer.warning";
    public static final String WARNING = "Si el nuevo lider no tiene una isla configurada, el abandonar el grupo te teletransportara directo al domo y perderas el derecho a tu isla";
    private final WorldAccess world;
    private final Journal journal;
    private final Map<UUID, Offer> offers = new HashMap<>();
    private final List<Decision> decisions;
    private final java.util.Set<UUID> completed = new java.util.HashSet<>();
    private Decision publishing;

    public record Island(int slot, int x, int z, int difficulty) {
        public Island {
            if (slot < 1 || slot > 128 || (x == 0 && z == 0)) {
                throw new IllegalArgumentException("Invalid personal island");
            }
        }
    }

    /** Both players must be online, in this party, with the sender still its leader. */
    public record Snapshot(String provider, UUID party, UUID leader, UUID homeOwner,
                           UUID sender, String senderName, Island own,
                           UUID recipient, String recipientName, Island recipientOwn) { }
    public record Offer(UUID operation, Snapshot expected, long expiresAtMillis) { }
    public record Decision(UUID operation, Snapshot expected, int revision) { }
    public enum Result { COMMITTED, CANCELLED, REJECTED, FAILED, RECOVERY_REQUIRED }

    public interface WorldAccess {
        Snapshot inspect(UUID sender, UUID recipient);
        /** Idempotent metadata projection, never generation, slot release or block edits. */
        void project(Decision decision);
    }

    public interface Journal {
        List<Decision> load() throws IOException;
        /** Durable atomic replacement; a failed write must leave the old journal intact. */
        void save(List<Decision> decisions) throws IOException;
    }

    /**
     * NP owns confirmation and leadership. prepare must durably reserve/revalidate
     * the party without changing its leader. commit is idempotent and applies a
     * durable SM decision without publishing events. publish runs after projection.
     * A prepared operation must be recovered from SM's journal, never timed out
     * independently after a commit decision. See docs/datapacks/party-island-transfer-v1.md.
     */
    public interface LeadershipParticipant {
        boolean prepare(Decision decision);
        void abort(UUID operation);
        void commit(Decision decision);
        void publish(Decision decision);
    }

    public IslandTransferCoordinator(WorldAccess world, Journal journal) throws IOException {
        this.world = Objects.requireNonNull(world);
        this.journal = Objects.requireNonNull(journal);
        this.decisions = new ArrayList<>(journal.load());
        for (int i = 0; i < decisions.size(); i++) {
            Decision decision = decisions.get(i);
            if (decision.revision() != i + 1 || !eligible(decision.expected())
                    || decisions.subList(0, i).stream().anyMatch(d -> d.operation().equals(decision.operation()))) {
                throw new IOException("Invalid island transfer journal");
            }
        }
    }

    public synchronized Offer offer(UUID sender, UUID recipient, long nowMillis) {
        if (completed.size() != decisions.size()) return null;
        Snapshot snapshot = world.inspect(sender, recipient);
        if (!eligible(snapshot)) return null;
        offers.values().removeIf(o -> o.expiresAtMillis() <= nowMillis
                || o.expected().party().equals(snapshot.party()));
        Offer offer = new Offer(UUID.randomUUID(), snapshot, nowMillis + 60_000L);
        offers.put(offer.operation(), offer);
        return offer;
    }

    public synchronized Result cancel(UUID operation, UUID sender) {
        Offer offer = offers.get(operation);
        if (offer == null || !offer.expected().sender().equals(sender)) return Result.REJECTED;
        offers.remove(operation);
        return Result.CANCELLED;
    }

    /** Invoke only after NP's one additional confirmation by the original leader. */
    public synchronized Result confirm(UUID operation, UUID sender, long nowMillis,
                                       LeadershipParticipant participant) {
        Objects.requireNonNull(participant);
        for (Decision decision : decisions) {
            if (decision.operation().equals(operation)) {
                // Repeated confirmation is a receipt lookup, never another transfer.
                if (!decision.expected().sender().equals(sender)) return Result.REJECTED;
                return completed.contains(operation) ? Result.COMMITTED : Result.RECOVERY_REQUIRED;
            }
        }
        Offer offer = offers.get(operation);
        if (offer == null || !offer.expected().sender().equals(sender)) return Result.REJECTED;
        if (completed.size() != decisions.size()) return Result.RECOVERY_REQUIRED;
        offers.remove(operation);
        Snapshot current = world.inspect(offer.expected().sender(), offer.expected().recipient());
        if (offer.expiresAtMillis() <= nowMillis || !eligible(current)
                || !offer.expected().equals(current)) return Result.REJECTED;
        Decision decision = new Decision(operation, current, decisions.size() + 1);
        try {
            if (!participant.prepare(decision)) {
                participant.abort(operation);
                return Result.REJECTED;
            }
            // Revalidate after NP has reserved the party, before recording any decision.
            if (!current.equals(world.inspect(current.sender(), current.recipient()))) {
                participant.abort(operation);
                return Result.REJECTED;
            }
            List<Decision> next = new ArrayList<>(decisions);
            next.add(decision);
            journal.save(List.copyOf(next));
            decisions.add(decision);
        } catch (IOException | RuntimeException failure) {
            participant.abort(operation);
            return Result.FAILED;
        }
        return finish(decision, participant);
    }

    /** After a durable decision, interruption requires recovery, never rollback. */
    private Result finish(Decision decision, LeadershipParticipant participant) {
        try {
            participant.commit(decision);
            world.project(decision);
            publishing = decision;
            try { participant.publish(decision); } finally { publishing = null; }
            completed.add(decision.operation());
            return Result.COMMITTED;
        } catch (RuntimeException interrupted) {
            return Result.RECOVERY_REQUIRED;
        }
    }

    public synchronized boolean recover(LeadershipParticipant participant) {
        for (Decision decision : decisions) {
            if (finish(decision, participant) != Result.COMMITTED) return false;
        }
        return true;
    }

    public synchronized List<Decision> decisions() { return List.copyOf(decisions); }
    public synchronized Decision publishing() { return publishing; }

    public static boolean eligible(Snapshot s) {
        return s != null && PROVIDER.equals(s.provider()) && s.party() != null
                && s.sender() != null && s.recipient() != null
                && !s.sender().equals(s.recipient()) && s.sender().equals(s.leader())
                && s.sender().equals(s.homeOwner()) && s.own() != null && s.recipientOwn() == null
                && s.senderName() != null && !s.senderName().isBlank()
                && s.recipientName() != null && !s.recipientName().isBlank();
    }
}
