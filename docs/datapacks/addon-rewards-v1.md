# Addon rewards API v1

The feature manifest API exposes switches, but v041 did not expose starter
inventory or personal Trial reward hooks. AddonRewardApi fills this gap without
mixins. Addons register named providers during initialization. Providers return
plain registered item IDs and counts, never mutate worlds or deliver inventory.
They must be side-effect-free and consult AddonFeatureApi for their own switches.

Starter providers run only during first chest initialization. A per-world marker
records the decision even when disabled. Existing chests are left untouched;
no scanning or retroactive filling occurs. An empty starter selection gets a
chest only if a provider contributes items. Only empty slots are used.

Trial providers run at completion for each captured participant UUID, including
offline participants. The returned items are frozen in the personal Vault reward;
claiming does not reroll or consult switches again. Built-in levels are 1–3.
Datapack Trials currently have no numeric level: their API context uses level 1,
with the definition ID available for providers that opt into explicit balancing.
Datapack supplement rewards use the same Vault, without changing existing
datapack phase/completion loot behavior.

Pending legacy level,difficulty records remain readable. New records append
plain item IDs/counts. Completion IDs suppress repeated callbacks. Writes use
atomic replacement and retain a backup; invalid data fails closed. Do not
downgrade a save with new records to an older core. Back up worlds first.

Normal disconnect, clean restart and repeated callbacks are covered. Inventory
claims now persist an `@inflight` UUID marker before granting anything. If that
write fails, no items are delivered and the queue remains claimable. A marker
found on reload blocks further claims for that player and retains the original
reward for administrator review. Do not clear it or restore a backup blindly:
delivery may already have occurred. Preserve the ledger, its backup and player
data before reconciling inventory and pending rewards. Successful delivery saves
player data before clearing the marker. This reduces the failure window but does
not make the two files transactional.

Inventory
and ledger files are not a transactional database: an OS/process crash between
delivery and disk saves cannot promise exactly-once delivery. This is also a
limitation of the existing Vault; no addon should claim crash-atomic guarantees.
