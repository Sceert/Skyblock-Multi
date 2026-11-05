package cl.treecs.skyblockmulti.api.party;

import cl.treecs.skyblockmulti.island.IslandTransferJournal;
import cl.treecs.skyblockmulti.island.IslandTransferWorldAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.util.UUID;

/** Public optional contract. NP must opt in; post-change events never imply consent. */
public final class IslandTransferApi {
    public static final int API_VERSION = 1;
    private static IslandTransferCoordinator coordinator;
    private static IslandTransferCoordinator.LeadershipParticipant participant;
    private static MinecraftServer server;
    private IslandTransferApi() { }

    /** NP registers its durable participant during mod initialization, before world startup. */
    public static void registerLeadershipParticipant(IslandTransferCoordinator.LeadershipParticipant value) {
        if (participant != null) throw new IllegalStateException("Leadership participant already registered");
        participant = java.util.Objects.requireNonNull(value);
    }

    public static void open(MinecraftServer currentServer) {
        server = currentServer;
        try {
            coordinator = new IslandTransferCoordinator(new IslandTransferWorldAccess(server),
                    new IslandTransferJournal(server.getWorldPath(LevelResource.ROOT)
                            .resolve("skyblockmulti_island_transfers.json")));
            if (!coordinator.decisions().isEmpty() && !recover()) {
                throw new IllegalStateException("NP island-transfer recovery is required before players join");
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not load island ownership transfers", error);
        }
    }

    public static void close() { coordinator = null; server = null; }

    /** Read-only preview distinguishes existing recipient OWN from missing coordination. */
    public static IslandTransferCoordinator.Snapshot inspect(UUID sender, UUID recipient) {
        checkThread();
        return new IslandTransferWorldAccess(server).inspect(sender, recipient);
    }

    public static IslandTransferCoordinator.Offer offer(UUID sender, UUID recipient) {
        checkThread();
        return participant == null ? null : coordinator.offer(sender, recipient, System.currentTimeMillis());
    }

    public static IslandTransferCoordinator.Result confirm(UUID operation, UUID sender) {
        checkThread();
        if (participant == null) return IslandTransferCoordinator.Result.REJECTED;
        return coordinator.confirm(operation, sender, System.currentTimeMillis(), participant);
    }

    public static IslandTransferCoordinator.Result cancel(UUID operation, UUID sender) {
        checkThread();
        return coordinator.cancel(operation, sender);
    }

    public static boolean recover() {
        checkThread();
        return participant != null && coordinator.recover(participant);
    }

    /** Suppress the ordinary post-leadership home prompt for this confirmed exception only. */
    public static boolean coordinated(PartyChangeEvent event) {
        if (coordinator == null || event.type() != PartyChangeEvent.Type.OWNER_CHANGED) return false;
        // Only operations publishing right now qualify. Historical receipts must not
        // suppress a later, unrelated leadership transfer between the same players.
        var publishing = coordinator.publishing();
        return publishing != null && publishing.expected().party().equals(event.partyId())
                && publishing.expected().sender().equals(event.actorId())
                && publishing.expected().recipient().equals(event.subjectId());
    }

    private static void checkThread() {
        if (server == null || coordinator == null || !server.isSameThread()) {
            throw new IllegalStateException("Island transfers require the active server thread");
        }
    }
}
