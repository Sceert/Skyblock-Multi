package cl.treecs.skyblockmulti.compatibility.openpac;

import cl.treecs.skyblockmulti.api.party.PartyProvider;
import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import net.minecraft.server.level.ServerPlayer;

/** Direct fallback integration when Nexus Parties is not installed. */
public final class OpenPacPartyProvider implements PartyProvider {
    public static final String ID = "skyblockmulti:openpac";
    public static final int PRIORITY = 100;

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
        return OpenPacCompat.isTerritorialIntegrationEnabled();
    }

    @Override
    public PartySnapshot snapshot(ServerPlayer player) {
        OpenPacCompat.PartyInfo info = OpenPacCompat.getPartyInfo(player);
        return new PartySnapshot(
                info.inParty(),
                info.partyId(),
                info.ownerUuid(),
                info.ownerName(),
                info.owner()
        );
    }

    @Override
    public Boolean isMember(net.minecraft.server.MinecraftServer server,
                            java.util.UUID partyId, java.util.UUID playerId) {
        var party = xaero.pac.common.server.api.OpenPACServerAPI.get(server)
                .getPartyManager().getPartyByMember(playerId);
        return party != null && partyId.equals(party.getId());
    }

    @Override public String partyName(ServerPlayer player) {
        var party = xaero.pac.common.server.api.OpenPACServerAPI.get(player.level().getServer())
                .getPartyManager().getPartyByMember(player.getUUID());
        return party == null ? null : party.getDefaultName();
    }
}
