package cl.treecs.skyblockmulti.api.party;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

/**
 * Server-side extension point for party mods.
 *
 * <p>Providers should return {@link PartySnapshot#solo()} when the player is
 * not in a party. Higher priority providers win when several are available.</p>
 */
public interface PartyProvider {
    String id();

    int priority();

    boolean isAvailable();

    PartySnapshot snapshot(ServerPlayer player);

    /** Optional display metadata. Must refer to this provider's group only. */
    default String partyName(ServerPlayer player) { return null; }

    /** Authoritative membership, including offline players; null means unknown. */
    default Boolean isMember(MinecraftServer server, UUID partyId, UUID playerId) {
        if (server == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return null;
        PartySnapshot party = snapshot(player);
        return party == null ? null : party.inParty() && partyId.equals(party.partyId());
    }
}
