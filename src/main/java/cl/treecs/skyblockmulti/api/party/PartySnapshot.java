package cl.treecs.skyblockmulti.api.party;

import java.util.UUID;

/**
 * Immutable view of the party relationship relevant to SkyblockMulti.
 * The party owner is the owner of the shared ACTIVE island.
 */
public record PartySnapshot(
        boolean inParty,
        UUID partyId,
        UUID ownerUuid,
        String ownerName,
        boolean owner
) {
    private static final PartySnapshot SOLO = new PartySnapshot(false, null, null, null, false);

    public static PartySnapshot solo() {
        return SOLO;
    }
}
