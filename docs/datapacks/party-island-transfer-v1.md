# Confirmed leadership and island transfer — public protocol v1

Status: **SM protocol available; normal NP party/island transfer confirmed by
the user; remaining manual scenarios pending.** NP documents its pre-transfer participant and single
authorization flow. The SM preview for a sender without OWN and a recipient with
OWN remains a dependency: it must distinguish a safe ordinary leadership transfer
from unavailable/ambiguous ownership state. Do not advertise the full integration
as verified until that dependency and the joint manual matrix are completed.

## Ownership and applicability

SM owns permanent island ownership, ACTIVE coordinates, PARTY_HOME and island
persistence. NP owns party membership, leadership and the user confirmation UI.
NP stays optional. Only the authoritative NP provider may use this exception;
OpenPAC and solo flows do not participate.

For providers without coordinated ownership transfer, PARTY_HOME is a reference
to a member's personal island. `PartyProvider.isMember(server, partyId, playerId)`
returns authoritative membership (including offline players when supported), or
null when unknown. The default checks online players; OpenPAC checks its public
party manager for offline players too. Queries use only the active provider.
An authoritative negative result replaces the departed owner's home reference
with the current leader. OWN is never changed. Members with OWN return to it;
members without OWN use a valid leader island or the normal HUB reset. Persisted
home references, ACTIVE coordinates and preferences prevent replay on reconnect.
Unknown membership preserves the existing home; external providers must override
this additive public method to support immediate offline removals.

The exception requires both players online in the same NP party, the sender still
its leader and sole owner of the current PARTY_HOME, and the recipient without
OWN. Transfer of a different personal island is rejected: it must not silently
replace an unrelated PARTY_HOME. If the recipient already has OWN, NP uses its
existing leadership flow and SM retains both players' ownership unchanged.

## NP integration required

Use only public SM classes `IslandTransferApi` and `IslandTransferCoordinator`.
No alpha version checks or internal NP classes are part of this contract.

1. During mod initialization, NP registers a `LeadershipParticipant` using
   `IslandTransferApi.registerLeadershipParticipant`. Its state is scoped by world
   and operation UUID. NP must be ready to recover before SM world startup completes.
2. Intercept the voluntary transfer **before** calling NP's existing `transfer`.
   Use the read-only `IslandTransferApi.inspect(sender, target)` snapshot to detect
   existing recipient OWN and `IslandTransferCoordinator.eligible(snapshot)` to
   identify this exact exception. For an eligible transfer, call `IslandTransferApi.offer(sender, target)`
   on the server thread. A null offer means do not execute the ownership exception.
   Do not silently fall through to leadership transfer when an eligible exception
   cannot be coordinated. Recipients already owning an island retain the old flow.
3. Show exactly one additional confirmation to the original leader. Show this
   literal warning, available as `IslandTransferCoordinator.WARNING` and the
   SM-owned key `skyblockmulti.party_transfer.warning` in all 13 locales:

   "Si el nuevo lider no tiene una isla configurada, el abandonar el grupo te teletransportara directo al domo y perderas el derecho a tu isla"

4. Confirmation passes the offer operation UUID and the authenticated original
   leader UUID to `IslandTransferApi.confirm`. The offer expires after 60 seconds;
   cancellation calls `cancel`. Disconnects, changed ownership, home, membership,
   leadership, names or recipient OWN invalidate the confirmation. Never fabricate
   a confirmation from `OwnerChanged`, a command retry or an administrative action.

## Consistency, failure and recovery

`LeadershipParticipant.prepare(decision)` must revalidate and durably reserve the
exact NP party/leader/recipient and operation UUID, without changing leadership.
While reserved, NP must serialize/block conflicting leave, kick, disband, transfer
and admin actions. `abort(operation)` is idempotent, releases a preparation and
never changes leadership. An error or rejection before the SM commit decision
leaves OWN, PARTY_HOME and leadership unchanged.

SM revalidates after preparation, then atomically writes the confirmed decision
to `<world>/skyblockmulti_island_transfers.json` with an operation UUID and revision.
The `.bak` is for explicit investigation/recovery, not an automatic rollback.
Corrupt/unsupported journals fail closed. Unconfirmed offers are ephemeral and
cannot survive a restart as an accepted confirmation.

`commit(decision)` applies and persists NP leadership idempotently, **without
publishing events or success messages**. After a successful preparation and a
durable SM decision, NP must not reject the decision on timeout or changed state:
it must finish it. SM then projects the same island slot/coordinates as recipient
OWN and removes sender OWN. The sender's ACTIVE stays at this PARTY_HOME with guest
preference. Construction, slot occupancy, rewards, claims and island initialization
markers are untouched. Existing island difficulty metadata follows that island.

`publish(decision)` releases the NP reservation and emits the buffered event/UI
success exactly once, after SM projection. Events must be synchronous on the server
thread; SM suppresses its ordinary home-selection prompt only during publication
of this exact coordinated operation. NP must retain durable operation receipts so
recovery/repeated publication never changes a later leader or repeats messages.

The durable decision is the point of no return. A later interruption returns
`RECOVERY_REQUIRED`, **not FAILED or CANCELLED**. NP must keep the reservation and
prevent players from using partially applied state until recovery completes.
Use `IslandTransferApi.recover()` after restoring NP's preparation/receipt state.
Every recorded decision is replayed in order: NP's commit/publication and SM's
revision-marked projection must be idempotent. Completed confirmation retries only
return the receipt; they do not transfer again. New offers are blocked while a
decision awaits recovery. Startup with decisions and no recovering participant is
rejected before players join rather than exposing inconsistent ownership.

SM persists PARTY_HOME separately in `skyblockmulti_party_homes.properties` and
uses `sb_own_rev` as the scoreboard projection revision. Do not clear the journal,
NP receipts or these revision markers independently. They prevent older decisions
from undoing later OWN assignments or home changes after restart.

## Former leader departure

While remaining in the party, the former owner may play at PARTY_HOME under normal
guest rules. A later departure, expulsion or dissolution uses the existing
`sb3_slot` ownership check: with another valid OWN, return there; without OWN,
perform the full existing reset and arrive at Last Refuge (Y=161) for normal
tree/difficulty selection. The transferred slot remains occupied and the recipient
and party retain its construction. No island generation or slot-release command
belongs in this protocol.

## Limits of current verification

`verifyIslandOwnershipTransfer` tests SM's state machine with a participant test
double, cancellation/expiry/stale participants, recipient OWN, write failure,
interrupted decisions, replay and disk persistence. It does not prove NP's durable
preparation, event buffering, confirmation UI or cross-mod startup behavior. Those
require joint verification through the manual checks in
`docs/testing/party-island-transfer-manual-test.md`.
