package cl.treecs.skyblockmulti.api.party;

import java.util.UUID;

/** Provider-neutral party lifecycle event. */
public record PartyChangeEvent(Type type, UUID partyId, UUID actorId, UUID subjectId) {
    public enum Type { CREATED, DISBANDED, MEMBER_JOINED, MEMBER_LEFT, OWNER_CHANGED }
}
