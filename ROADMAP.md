# SkyblockMulti Roadmap

Updated 2026-10-09 against v056, audit evidence and user confirmations.
Historical requests are preserved in internal revision notes; this is the current status.
For a short starting point and focused reading paths, see
[current-state](docs/development/current-state.md).

## Implemented

- Void Overworld/Nether, normal End, Last Refuge, protected Ascension Nexus,
  fortress and configurable End portal; persistent world geometry and structures.
- Configurable deterministic island rings; distinct OWN, ACTIVE and PARTY_HOME.
- Vanilla/external trees, enabled-only Random, difficulty, generic planting stock
  and addon switches. Extreme grants no vanilla starter chest or supplies.
- Native island panel, Cloth Config, optional Mod Menu.
- Nexus Parties priority, OpenPAC fallback and solo fallback; permanent Nexus
  claim integration without retired island reservation/assignment claims.
- Party-home decisions, ownership-transfer protocol and configurable cooldown.
- Lore book on arrival without duplicate assignment/reconnection; free movement
  during party reentry selection (v056).
- Infernal participant policy, health bars, abandonment voting, personal Vault,
  persistent rewards and offline addon supplements.
- Cube lineage and complete family cleanup; staggered datapack spawning.
- Vault persists intent before delivery: failed writes grant nothing; interrupted
  deliveries are retained for administrator review, not automatically replayed.

## User-confirmed tests

- End/progression/portal cases from previous pending item 14: already tested and
  OK, confirmed by the user on 2026-10-04. No outstanding general End test.
- Languages work correctly with no observed text exceeding the window. No current
  language/layout blocker; this does not assert exhaustive narrator accessibility.
- Addon recipes work. Basic crafting and recipe creation are not missing features.
  Additional edge checks below are not known defects.

## Island administration — commands still missing

Source review found no complete supported admin commands to assign an island,
transfer its owner, remove ownership/release a slot, or clean X chunks around it.
Player party decisions and internal mcfunctions are not this administration API.

Proposed scope:

- Assign an available slot to an exact player/UUID, respecting existing OWN and
  occupancy, with explicit tree/difficulty rules.
- Transfer an exact island to an exact owner, reconciling OWN, ACTIVE, PARTY_HOME,
  offline state, occupancy and pending party decisions.
- Separate ownership removal/slot release from physical block deletion.
- Inspect, clear or override island-switch cooldowns through admin commands.
- Cleanup must specify an island, dimension, vertical bounds and whether it
  removes blocks/entities or regenerates terrain. X chunks is a requested bound,
  never an unrestricted wipe.
- Compute allowed bounds from persisted coordinates of every neighboring ring,
  including reserved slots and the Nexus. Check the complete chunk-aligned
  footprint against neighboring regions with a safety margin. Centre distance
  alone cannot protect builds extending beyond island centres.
- Preview exact coordinates, volume and affected ownership references; reject
  overlap or stale state; preserve a recoverable snapshot before destruction.
  Confirmation must correspond to that exact preview.

These are proposed functions, not available command syntax. No island was
reassigned or cleaned during this review.

## Waystones — already implemented

Local project: ../SM Waystone Addon/. Its README identifies a Fabric JAR addon,
version 1.0.0, approved in-game by the user and with recorded automated checks.
The user calls the installed integration a datapack; the inspected local project
is a JAR addon. This review does not identify the installed artifact elsewhere.

- Independent starter and Infernal-loot switches already exist.
- Starter contribution is OFF by default. When enabled it provides one Waystone,
  including creating a chest where the base difficulty has none. This optional
  addon exception is separate from the vanilla Extreme rule.
- Trial rewards provide materials, not finished Waystones. Offline supplements
  remain in the personal Vault. Existing islands are not retroactively refilled.
- Core works without the integration. Do not list creating it as pending.
- Remaining unconfirmed edge cases from addon verification notes: four switch
  combinations through full selection; no refill after removing the item and
  reload/restart; a real Trial with one offline participant; full inventory;
  datapack Trial rewards with the current core.
- Special OWN-to-PARTY_HOME routing/free travel remains an optional future idea,
  not required for the existing addon to function.

## Recipes — working; optional extra coverage

Local project: ../SM Addon BOP/. Decorative recipes, including white/black sand,
already exist. Keep addon recipe IDs and behavior outside the core.

Further checks, if needed: ingredients attainable in a void world, duplication
or profitable conversion loops, feature/difficulty switches, dependency absent,
and recipe-book/JEI visibility after reload. These are not established failures.
Use the datapack or equivalent addon package, not both, as its documentation states.

## Remaining work

Coverage review and new boundary reproduction:
[2026-10-09 audit](docs/audits/2026-10-09-pending-coverage-review.md).
Historical v055/v056 passes remain scoped evidence, not missing tests to repeat.

- Confirmed protection bug: water placed outside radius 45 at Y=170 enters the
  protected Nexus and removes a valid flower. New boundary audit: 7 PASS, 1 FAIL.
  Outside-to-inside piston movement is correctly blocked in the tested case.

- Automatic Vault recovery after real process/OS crashes, including overflow.
  Inventory and ledger are not atomic together. Never blindly clear @inflight
  markers or restore a backup without reconciling delivery.
- Real NP/OpenPAC multiplayer transitions, offline expulsion, timeout, dissolution,
  OWN preservation, provider priority and permanent Nexus claim.
- Clean distributed dedicated-server installation remains pending. A separate
  development server JVM now starts, exposes all three dimensions and reloads
  the infernal example from disk successfully; no client connection was tested
  in that run. The existing harness already passes connection/generator checks.
- Full ring/overflow policy matrix and integral legacy-profile migration.
- Reload during combat, changed/invalid datapacks, remaining unused infernal
  options and offline original dynamic loot-table delivery. A valid example
  reload from disk now passes; persistent addon supplements are a separate path.
- Additional protection boundaries, lava and destructive entity interactions;
  water ingress is a reproduced defect, not merely unconfirmed coverage.
- Multiplayer encounter balance and reward rates.
- Administrative commands described above.

## Future development, not current bugs

- Lateral fortress access and additional progression/exploration content.
- Expensive player-built private challenge sites: define ownership, concurrency,
  persistence, protection and safe placement first.
- Flooded dimension and extra progression structures only when explicitly requested.
- Additional accessibility testing when requested or a concrete issue is reported.
