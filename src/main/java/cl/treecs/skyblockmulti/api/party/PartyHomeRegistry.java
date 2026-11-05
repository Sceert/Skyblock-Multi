package cl.treecs.skyblockmulti.api.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

/** Keeps PARTY_HOME independent from the mutable party leader. */
public final class PartyHomeRegistry {
    private static final Map<UUID, UUID> HOMES = new HashMap<>();
    private static final Map<UUID, PendingLeadershipChange> PENDING = new HashMap<>();
    private static java.nio.file.Path persistence;

    private PartyHomeRegistry() {}

    public static synchronized void open(java.nio.file.Path path) {
        clearRuntime();
        persistence = path;
        if (java.nio.file.Files.notExists(path)) return;
        try (var reader = java.nio.file.Files.newBufferedReader(path)) {
            var properties = new java.util.Properties();
            properties.load(reader);
            for (String party : properties.stringPropertyNames()) {
                HOMES.put(UUID.fromString(party), UUID.fromString(properties.getProperty(party)));
            }
        } catch (Exception invalid) {
            throw new IllegalStateException("Could not load persistent PARTY_HOME", invalid);
        }
    }

    private static void saveHomes() {
        if (persistence == null) return; // Standalone verification has no world binding.
        try {
            var properties = new java.util.Properties();
            HOMES.forEach((party, owner) -> properties.setProperty(party.toString(), owner.toString()));
            var text = new java.io.StringWriter();
            properties.store(text, "Skyblock Multi PARTY_HOME (UUIDs, no island blocks)");
            java.nio.file.Files.createDirectories(persistence.getParent());
            var temporary = persistence.resolveSibling(persistence.getFileName() + ".tmp");
            try (var channel = java.nio.channels.FileChannel.open(temporary,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            java.nio.file.Files.move(temporary, persistence, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Could not persist PARTY_HOME", failure);
        }
    }

    /** Read-only view for presentation; normal reconciliation owns initialization. */
    public static synchronized UUID peekHomeOwner(PartySnapshot party) {
        if (!party.inParty() || party.partyId() == null) return null;
        return HOMES.getOrDefault(party.partyId(), party.ownerUuid());
    }

    public static synchronized UUID homeOwner(PartySnapshot party) {
        if (!party.inParty() || party.partyId() == null) return null;
        UUID home = HOMES.get(party.partyId());
        if (home == null) {
            HOMES.put(party.partyId(), party.ownerUuid());
            saveHomes();
            home = party.ownerUuid();
        }
        return home;
    }

    public static synchronized void onEvent(PartyChangeEvent event, long nowMillis) {
        switch (event.type()) {
            case CREATED -> HOMES.putIfAbsent(event.partyId(), event.actorId());
            case OWNER_CHANGED -> {
                HOMES.putIfAbsent(event.partyId(), event.actorId());
                PENDING.put(event.partyId(), new PendingLeadershipChange(
                        event.partyId(), event.actorId(), event.subjectId(), nowMillis + 60_000L));
            }
            case DISBANDED -> {
                HOMES.remove(event.partyId());
                PENDING.remove(event.partyId());
            }
            default -> { }
        }
        saveHomes();
    }

    public static synchronized PendingLeadershipChange pending(UUID partyId) {
        return PENDING.get(partyId);
    }

    public static synchronized PendingLeadershipChange pendingForDecisionMaker(UUID playerId) {
        return PENDING.values().stream()
                .filter(pending -> pending.newOwner().equals(playerId))
                .findFirst().orElse(null);
    }

    public static synchronized List<PendingLeadershipChange> removeExpired(long nowMillis) {
        List<PendingLeadershipChange> expired = PENDING.values().stream()
                .filter(pending -> pending.expiresAtMillis() <= nowMillis)
                .toList();
        expired.forEach(pending -> PENDING.remove(pending.partyId()));
        return expired;
    }

    public static synchronized boolean keepCurrentHome(UUID partyId, UUID decisionMaker) {
        PendingLeadershipChange pending = PENDING.get(partyId);
        if (pending == null || !pending.newOwner().equals(decisionMaker)) return false;
        PENDING.remove(partyId);
        return true;
    }

    public static synchronized boolean moveHomeToNewLeader(UUID partyId, UUID decisionMaker) {
        PendingLeadershipChange pending = PENDING.get(partyId);
        if (pending == null || !pending.newOwner().equals(decisionMaker)) return false;
        HOMES.put(partyId, pending.newOwner());
        saveHomes();
        PENDING.remove(partyId);
        return true;
    }

    public static synchronized void setHome(UUID partyId, UUID ownerId) {
        if (partyId != null && ownerId != null) {
            HOMES.put(partyId, ownerId);
            saveHomes();
        }
    }

    /** Only an authoritative negative membership result revokes a fallback home. */
    public static synchronized boolean refreshPersonalHome(PartySnapshot party, String provider,
                                                            Boolean homeOwnerIsMember) {
        if (!party.inParty() || "skyblockmulti:nexus_parties".equals(provider)
                || !Boolean.FALSE.equals(homeOwnerIsMember)) return false;
        UUID previous = homeOwner(party);
        if (previous.equals(party.ownerUuid())) return false;
        HOMES.put(party.partyId(), party.ownerUuid());
        try {
            saveHomes();
        } catch (RuntimeException failure) {
            HOMES.put(party.partyId(), previous);
            throw failure;
        }
        PENDING.remove(party.partyId());
        return true;
    }

    public enum PersonalHomeDestination { OWN, LEADER, HUB }

    public static PersonalHomeDestination personalHomeDestination(boolean hasOwn, boolean leaderHasOwn) {
        return hasOwn ? PersonalHomeDestination.OWN
                : leaderHasOwn ? PersonalHomeDestination.LEADER : PersonalHomeDestination.HUB;
    }

    public static synchronized void clearRuntime() {
        HOMES.clear();
        PENDING.clear();
        persistence = null;
    }

    public record PendingLeadershipChange(UUID partyId, UUID previousOwner,
                                          UUID newOwner, long expiresAtMillis) {}
}
