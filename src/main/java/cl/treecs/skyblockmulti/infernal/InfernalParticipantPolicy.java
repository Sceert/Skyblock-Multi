package cl.treecs.skyblockmulti.infernal;

import cl.treecs.skyblockmulti.api.party.PartySnapshot;
import java.util.UUID;

/** Nearby fallback is allowed only when there is no active party provider. */
final class InfernalParticipantPolicy {
    private InfernalParticipantPolicy() {}

    static boolean eligible(boolean providerPresent, UUID activator, PartySnapshot party,
                            UUID candidate, PartySnapshot candidateParty) {
        if (!providerPresent) return true;
        if (!party.inParty()) return activator.equals(candidate);
        return candidateParty.inParty() && party.partyId().equals(candidateParty.partyId());
    }
}
