package cl.treecs.skyblockmulti.compatibility.nexusparties;

import cl.treecs.nexusparties.NexusParties;
import cl.treecs.skyblockmulti.api.party.PartyProvider;
import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import cl.treecs.skyblockmulti.api.party.PartyChangeEvent;
import cl.treecs.nexusparties.api.event.PartyEvent;
import cl.treecs.nexusparties.api.event.PartyEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/** Authoritative party source when Nexus Parties is installed and ready. */
public final class NexusPartiesPartyProvider implements PartyProvider {
    public static final String ID = "skyblockmulti:nexus_parties";
    public static final int PRIORITY = 200;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return isCompatible()
                && NexusParties.apiIfReady().isPresent();
    }

    public static boolean isCompatible() {
        return FabricLoader.getInstance().isModLoaded("nexus_parties") && ApiCompatibility.SUPPORTED;
    }

    private static final class ApiCompatibility {
        private static final boolean SUPPORTED = readInstalledVersion();

        private static boolean readInstalledVersion() {
            try {
                // A direct reference to a static final int is inlined by javac and
                // would test the build dependency, not the installed provider.
                return NexusParties.class.getField("API_VERSION").getInt(null) == 1;
            } catch (ReflectiveOperationException | LinkageError error) {
                System.err.println("[Skyblock Multi] Unsupported Nexus Parties API: " + error);
                return false;
            }
        }
    }

    @Override
    public PartySnapshot snapshot(ServerPlayer player) {
        var api = NexusParties.apiIfReady().orElse(null);
        if (api == null) return PartySnapshot.solo();
        var party = api.getParty(player.getUUID()).orElse(null);
        if (party == null) return PartySnapshot.solo();

        var server = player.level().getServer();
        var onlineOwner = server == null ? null : server.getPlayerList().getPlayer(party.owner());
        String ownerName = onlineOwner == null
                ? party.owner().toString()
                : onlineOwner.getName().getString();

        return new PartySnapshot(
                true,
                party.id(),
                party.owner(),
                ownerName,
                party.owner().equals(player.getUUID())
        );
    }

    public static AutoCloseable registerEvents(java.util.function.Consumer<PartyChangeEvent> listener) {
        return PartyEvents.register(event -> {
            PartyChangeEvent normalized = switch (event) {
                case PartyEvent.Created created -> new PartyChangeEvent(
                        PartyChangeEvent.Type.CREATED, created.partyId(), created.ownerId(), created.ownerId());
                case PartyEvent.Disbanded disbanded -> new PartyChangeEvent(
                        PartyChangeEvent.Type.DISBANDED, disbanded.partyId(), disbanded.formerOwnerId(), null);
                case PartyEvent.MemberJoined joined -> new PartyChangeEvent(
                        PartyChangeEvent.Type.MEMBER_JOINED, joined.partyId(), joined.playerId(), joined.playerId());
                case PartyEvent.MemberLeft left -> new PartyChangeEvent(
                        PartyChangeEvent.Type.MEMBER_LEFT, left.partyId(), left.playerId(), left.playerId());
                case PartyEvent.MemberKicked kicked -> new PartyChangeEvent(
                        PartyChangeEvent.Type.MEMBER_LEFT, kicked.partyId(), kicked.actorId(), kicked.playerId());
                case PartyEvent.OwnerChanged changed -> new PartyChangeEvent(
                        PartyChangeEvent.Type.OWNER_CHANGED, changed.partyId(),
                        changed.previousOwnerId(), changed.newOwnerId());
                default -> null; // Ignore additive events outside the home/membership contract.
            };
            if (normalized != null) listener.accept(normalized);
        });
    }

    @Override public String partyName(ServerPlayer player) {
        return NexusParties.apiIfReady().flatMap(api -> api.getParty(player.getUUID()))
                .map(party -> party.name()).orElse(null);
    }
}
