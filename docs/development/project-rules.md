# AGENTS.md — SkyblockMulti

## Scope

These instructions apply to E:/GitHub/SkyblockMulti.

- Repository: Sceert/SkyblockMulti
- Development branch: feature/world-foundation
- Published checkpoint: 0.2.3-beta
- Development version: use mod_version from gradle.properties; never hardcode it
- Mod ID: skyblockmulti
- License: MIT

Historical design notes are archived in
docs/development/legacy-agents-reference.md. Consult them only
when old behavior is not explained by current code, README.md, ROADMAP.md, or
focused documentation.

## Technical baseline

- Minecraft Java 26.3
- Fabric Loader >= 0.20.1
- Fabric API 0.165.2+26.3
- Java 25
- Gradle 9.7.0 in CI
- Fabric Loom 1.17.x
- Mod Menu, Nexus Parties, OpenPAC, JEI and Xaero mods are optional

Compile against current mappings. Inspect available mapped APIs instead of
copying method names from Minecraft 1.21.x or older examples.

## Working rules

1. Inspect the branch and worktree before changing code.
2. Preserve unrelated user changes in the dirty worktree.
3. Prefer small, isolated changes; do not rewrite unrelated systems.
4. Use apply_patch for source and documentation edits.
5. After a meaningful implementation batch, run the relevant checks once.
6. Do not repeat successful broad checks unless another change affects them.
7. Do not merge, tag, publish, push, or alter release history unless requested.
8. Keep optional integrations optional; the core must work without them.
9. Diagnose from current code, mappings and logs before speculative fixes.

On this Windows host, use the already approved external Gradle invocation when
the restricted environment cannot load native-platform.dll:

    & 'C:\Gradle\bin\gradle.bat' build --stacktrace

The JOML sun.misc.Unsafe warning comes from an external dependency and is not a
missing project component. Do not retry a successful build because of it.

## Version and changelog policy

- Increment `mod_version` for every new implementation revision delivered for testing,
  including UI/config/resource changes. With an internal `vNNN` version, advance
  to the next unused revision; do not rebuild different changes under a previously
  delivered version. Read the current value first. Retries within the same revision
  do not require another increment. This rule applies across all chats.
- gradle.properties is authoritative for the development version.
- build.gradle and fabric.mod.json derive the version dynamically.
- Runtime version messages use Fabric metadata.
- CHANGELOG.md contains only concise changes for published versions.
- Internal vXXX revisions belong in docs/development/internal-revisions-*.md.
- Do not create a public release for an internal test revision.

## World invariants

- Overworld: void, central Ascension Nexus, radial player islands.
- Nether: void with controlled progression content.
- End: normal vanilla End; never convert it to void.
- The future flooded dimension is not implemented unless explicitly requested.
- The physical Nexus generates once per world and does not rebuild on restart.
- Use explicit versioned markers for intentional structure migrations.
- Never silently overwrite an existing Nexus, island, claim or player build.

Canonical names:

- Entire central region: The Ascension Nexus / El Nexo del Ascenso.
- Arrival dome inside it: Last Refuge / Último Refugio.

The HUB floor is Y=160 and initial arrival is Y=161. The fortress and End portal
remain below it with no direct HUB staircase or shaft. Protection follows
logical regions rather than the current visual palette.

## Initial player flow and persistence

Expected first-time sequence:

1. Arrive in Last Refuge and receive the official lore book.
2. Move freely while selecting a tree and difficulty.
3. Receive an island assignment and teleport there.

Do not freeze or repeatedly teleport players during selection. Established
players respawn at their valid island, not the HUB. A party-leave reset returns
to the HUB only when the player has no personal island, following normal reset
rules. Preserve the hidden End advancement tied to the official lore book.

World geometry and structural portal choices lock through per-world state after
initialization. Global configuration supplies defaults for future worlds and
must never replace established world values silently.

## Islands and parties

- Keep island positions radial and deterministic through IslandRingLayout.
- Current limits and ring geometry are authoritative in code and tests.
- Difficulty values remain Extreme=0, Hard=1, Standard=2, Easy=3.
- Easy prefers inner positions; other difficulties prefer outer positions, with
  controlled overflow.

Never merge these concepts:

- OWN: the player's permanent personal island.
- ACTIVE: the island currently chosen for play.
- PARTY_HOME: the island selected for the party.

Joining, leaving, leadership transfer, expulsion or dissolution must never
delete a valid OWN. A player leaving with OWN returns to it; a player without
OWN follows the full HUB reset. Party membership must not create reusable free
teleports outside the explicit decision flow.

Party-provider priority is Nexus Parties, then OpenPAC, then solo fallback.
Never combine membership from multiple providers. Nexus Parties compatibility
uses its public API contract instead of targeting individual alpha versions.

OpenPAC remains optional. Its maintained responsibilities are the permanent
Ascension Nexus Server Claim and its optional fallback party provider. Do not
restore retired island reservation, assignment or transfer claims. Native Nexus
protection remains authoritative with or without OpenPAC.

## Protection

Inside the logical Nexus, normal Survival and Adventure players cannot perform
destructive actions such as breaking, placement, fluids, fire, explosions,
pistons or destructive entity interactions. Intended buttons, doors, containers
and progression receptacles remain usable.

Creative mode is the explicit development bypass. OP status alone does not
bypass protection. Outside protected regions, preserve vanilla behavior.
Explosion protection remains regional and does not globally disable TNT,
creepers or End Crystals.

## Trees, addons and configuration

- Vanilla selectable trees and Random remain locale-aware.
- Random selects only enabled definitions.
- External trees come from datapack/JAR metadata; do not hardcode BOP
  definitions or translations in the core.
- Addons may declare starter reproductive material through planting_stock.
- Generic feature switches are documented in
  docs/datapacks/addon-features-v1.md.

Global configuration layout:

    config/skyblockmulti/general.toml
    config/skyblockmulti/addons/<addon_mod_id>.toml
    config/skyblockmulti/trees/<namespace>.toml

JSON files are legacy migration inputs. Preserve their values and recoverable
backups when migrating. Vanilla-material recipe availability is controlled by
`[difficulty.crafting].vanilla_recipes_enabled`; progression recipes stay enabled.

Dedicated servers create editable addon files without Mod Menu. Keep legacy
migration safe and preserve values. Per-world state remains in the world save
and is not global configuration.

## Infernal Trials

- The Infernal Vault uses persistent, personal, exactly-once rewards.
- Pending rewards survive disconnects, restarts and later trials.
- One click must not consume multiple rewards or duplicate messages.
- With a party provider, participant snapshots include only eligible party
  members; otherwise use the nearby-player fallback.
- Unrelated players and spectators do not scale encounters or earn rewards.
- Health bars track combined current health against a stable phase maximum.
- Cleanup includes split or transformed encounter mobs without removing
  unrelated Nether entities.
- Built-in and datapack-defined trials follow the same public scaling and
  abandonment contracts where applicable.

API and format documentation lives under docs/datapacks/. Keep public APIs
generic so third-party addons can provide trees, trials, rewards, trades and
future party providers without addon-specific core code.

## Localization

Update all 13 locale files for every user-visible Skyblock Multi key:

- English: en_us, en_gb, en_au, en_ca, en_nz
- Spanish: es_es, es_419, es_cl, es_mx, es_ar, es_ec, es_uy, es_ve

Addon-owned names and translations belong to the addon. OpenPAC claim labels
are separate and may not support client-specific localization.

## Build and verification

Run checks proportional to the change:

- Java/config/API changes: one Gradle build.
- Resource-only edits: validate affected resources, then build once.
- Generated island slots: update the generator and regenerate.
- Structure imports: validate dimensions, palette, blocks and block entities.
- Runtime bugs: inspect the profile log and run a focused manual test.

Custom verification tasks declare outputs and should be UP-TO-DATE when inputs
have not changed. Do not routinely run a second clean build.

Before handing off a beta:

1. Run git diff --check without dumping unrelated diffs.
2. Complete one successful Gradle build.
3. Verify the expected versioned JAR and SHA-256 when installing it.
4. Remove the previous Skyblock Multi JAR from the test profile.
5. Confirm only one JAR with mod ID skyblockmulti is installed.
6. Report only the manual tests introduced by that revision.

## Safety

Before deleting blocks, releasing slots, resetting scoreboards, reassigning
islands or claims, rebuilding structures or changing world-state formats,
inspect existing persistence first. Require exact targets and preserve
recoverability. Never use broad destructive commands or reset user work.

When behavior is ambiguous, preserve current behavior, make the smallest safe
change, leave a focused TODO if necessary, and report the unresolved choice.
