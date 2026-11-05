# Historical AGENTS reference — SkyblockMulti

> Archived on 2026-09-11. This file is historical context, not active agent
> instructions. Current rules are in `E:/GitHub/AGENTS.md`.

## 1. Project identity

Repository: `Sceert/SkyblockMulti`

Primary development branch:
`feature/world-foundation`

Current development version:
`0.2.3-beta`

Published checkpoint/release:
`0.2.3-beta`

Mod ID:
`skyblockmulti`

License:
MIT

Author / project owner:
Treecs / Sceert

## 2. Technical baseline

Target stack:

- Minecraft Java: 26.2
- Fabric Loader: >= 0.19.3
- Fabric API: 0.156.0+26.2
- Java: 25
- Gradle used in GitHub Actions: 9.5.0
- Fabric Loom: 1.17.x
- Mod Menu: optional
- Open Parties and Claims / OpenPAC: optional integration
- Xaero minimap/world map may be present during testing

The build must remain compatible with Minecraft 26.2 mappings currently used by the repository.

Before changing APIs, compile and verify against the current branch. Do not assume older mappings or method names from 1.21.x.

## 3. Workflow rules for Codex

When starting work:

1. Inspect the current branch and repository state.
2. Read this file fully.
3. Run the build before making changes if practical:
   `gradle build --stacktrace`
4. Do not rewrite unrelated systems.
5. Prefer small, isolated changes.
6. After every meaningful change:
   - run the build;
   - inspect compiler errors carefully;
   - fix only the actual incompatibility;
   - avoid speculative refactors.
7. Do not create or publish a GitHub Release/tag for `0.2.0-beta` unless explicitly instructed.
8. Do not merge branches unless explicitly instructed.
9. Do not modify published release history.
10. Keep optional integrations optional. SkyblockMulti must still function without OpenPAC.

If an API call does not compile under Minecraft 26.2, inspect available mapped methods/classes in the current dependency set instead of guessing.

## 4. Version handling

The authoritative version source is:

`gradle.properties`

Expected:
`mod_version=0.2.3-beta`

`build.gradle` derives:
`version = project.mod_version`

`fabric.mod.json` uses:
`"version": "${version}"`

Never hardcode the mod version in chat messages or translations.

Known stale issue:
older locale strings still contain:
`SkyblockMulti 0.1.1-beta cargado.` / `loaded.`

The preferred direction is to derive visible runtime version text from Fabric metadata.

Current intended behavior:
startup console should derive the version with FabricLoader metadata.

If a dedicated join message exists, it should also derive the version dynamically.

## 5. Current high-level architecture

SkyblockMulti is a configurable multiplayer Skyblock mod.

World architecture:

1. Overworld:
   - void world
   - central protected area: The Ascension Nexus
   - player islands arranged in circular rings
2. Nether:
   - void world
3. Future dimension:
   - flooded ancient world
4. End:
   - vanilla normal End, not void

Do not convert the End to void.

## 6. Central area naming

Canonical internal/concept name:
`The Ascension Nexus`

Spanish display:
`El Nexo del Ascenso`

The entire central protected region is The Ascension Nexus.

Inside it:

- `Last Refuge` / `Último Refugio`
  = arrival HUB/dome
- lower vertical core
- future dungeon approaches
- fortress
- End portal chamber

Do not use "Last Refuge" as the name for the entire central protected region.

## 7. Ascension Nexus current geometry

Current OpenPAC claim geometry:

- circular discrete chunk claim
- centered at chunk 0,0
- radius: 15 chunks
- maximum diameter: 31 chunks
- approximately 496 blocks across
- approximately 709 claimed chunks

This size is provisional and may be adjusted later.

The protection logic should be decoupled from the visual HUB design.

Preferred principle:
protect by logical region, not by exact block palette or exact current walls.

The visual structure will change.

## 8. Ascension Nexus current visual prototype

The current prototype was intentionally built as a rough structural mockup, not final art.

Current intended heights:

- HUB playable floor: Y=160
- player initial spawn: Y=161
- dome top: approximately Y=198
- fortress roof: approximately Y=35
- fortress interior: approximately Y=12..35
- End portal chamber: approximately Y=4..13
- End portal frames: around Y=6

The large vertical gap is intentional.

Design principle:

HUB / Last Refuge
↓ large vertical void
Fortress
↓
End portal

The player should be able to see the fortress/portal area from above through glass, but there must be no direct staircase or shaft access from the HUB.

Future access should be lateral through progression/dungeon content.

The current terrain and fortress palette/layout are temporary and expected to change.

## 9. HUB generation persistence

The physical Nexus/HUB should generate once per world.

Current marker concept:
`skyblock_nexus_built_v1`

Do not rebuild the HUB every server restart.

The generated structure should remain editable during development without being overwritten on reload.

If a future generation version changes materially, prefer a new explicit versioned marker/migration strategy rather than silently overwriting existing worlds.

## 10. Player initial flow

Intended first-time flow:

1. player arrives in Last Refuge
2. lore book is given
3. short intro delay / selection flow
4. tree selection
5. difficulty selection
6. island assignment
7. teleport to island

Players should be able to walk around the HUB during selection.

Do not repeatedly teleport/freeze them in place during selection.

The old behavior that constantly teleported the player to the HUB center is incorrect.

## 11. Lore behavior

Lore item:
`Crónica del Último Refugio`

English concept:
Chronicle of the Last Refuge

The book is given on first arrival.

A player who legitimately resets after leaving a party with no personal island receives the intro/lore again.

An established player who simply loses the book does not automatically receive a replacement.

There is a hidden advancement related to reaching the End while carrying the official lore book.

Do not remove that system.

## 12. Island geometry

Current configurable capacities:
8, 16, 24

Radius options:

For 8:
512, 1024, 2048, 4096, 8192

For 16:
1024, 2048, 4096, 8192, 16384

For 24:
2048, 4096, 8192, 16384

24-player layout:

- inner ring: slots 01..08
- radius = outer radius / 2
- starts north, clockwise
- outer ring: slots 09..24
- 16 positions
- half-step angular offset

At outer radius 2048 the intended slot centers are approximately:

Inner:
01 (0,-1024)
02 (720,-720)
03 (1024,0)
04 (720,720)
05 (0,1024)
06 (-720,720)
07 (-1024,0)
08 (-720,-720)

Outer:
09 (400,-2016)
10 (1136,-1696)
11 (1696,-1136)
12 (2016,-400)
13 (2016,400)
14 (1696,1136)
15 (1136,1696)
16 (400,2016)
17 (-400,2016)
18 (-1136,1696)
19 (-1696,1136)
20 (-2016,400)
21 (-2016,-400)
22 (-1696,-1136)
23 (-1136,-1696)
24 (-400,-2016)

Keep slot geometry circular/radial.

## 13. Difficulty placement policy

Difficulty priority:

- Easy prefers inner ring
- Extreme / Hard / Standard prefer outer ring
- overflow may use the other ring

The mapping of difficulty score values currently follows:

- Extreme = 0
- Hard = 1
- Standard = 2
- Easy = 3

Default easiest permitted mode:
Easy / Beginner

Do not invert this without auditing existing score logic and UI.

## 14. Tree selection

Selectable trees:

- Oak
- Spruce
- Birch
- Jungle
- Acacia
- Cherry
- Mangrove
- Dark Oak
- Pale Oak
- Azalea
- Flowering Azalea
- Random

Random must only select enabled trees.

The visual menu intentionally limits layout density.

Do not remove locale-aware display.

## 15. Localization

SkyblockMulti currently maintains 13 locale files.

English:
- en_us
- en_gb
- en_au
- en_ca
- en_nz

Spanish:
- es_es
- es_419
- es_cl
- es_mx
- es_ar
- es_ec
- es_uy
- es_ve

When adding a user-visible SkyblockMulti translation key, update all 13 locale files.

English locales may share identical text.
Spanish locales may share identical text unless there is a reason to localize further.

OpenPAC claim labels are a separate system and may not support per-client localization.

## 16. OpenPAC integration — rules

OpenPAC is optional.

Mod ID:
`openpartiesandclaims`

SkyblockMulti must remain playable without it.

OpenPAC responsibilities are secondary:
- party discovery
- player personal claims
- reserved future slot claims
- central Server Claim as an additional protection layer

SkyblockMulti native protection must remain the primary required protection for the Nexus.

Do not make core Nexus protection depend on OpenPAC.

## 17. OpenPAC claim ownership model

Two concepts must remain distinct:

OWN:
player's personal island

ACTIVE:
island currently used by the player

Rules:

- solo player:
  ACTIVE = OWN
- party owner:
  ACTIVE = OWN
- party member:
  ACTIVE = owner's island
  OWN is preserved

When leaving a party:
- if player already has OWN:
  return to OWN
  preserve inventory/progress
- if player has no OWN:
  full SkyblockMulti reset
  return to HUB
  select again

Never destroy a valid personal island just because a player joins another party.

## 18. OpenPAC reservation policy

Future unused island slot:
5x5 chunks
centered on slot

Assigned island:
3x3 personal claim

When assigning a reserved slot:

- center 3x3 transfers to player
- outer 16 chunks return to wilderness
- never leave the outer ring server-owned
- never overwrite third-party claims

Structural slot usage is controlled by SkyblockMulti world data, not by whether an OpenPAC claim exists.

A generated/used slot is structurally occupied forever unless there is an explicit future reset system.

Manual unclaim must NOT make a used slot reusable.

## 19. OpenPAC one-time world claim initialization

Desired behavior:

- future slot Server Claims are created once per world
- do not recreate them on every restart
- central Ascension Nexus Server Claim is permanent
- central claim must not be transferred to players

If OpenPAC is installed later in an existing world, current behavior may initialize missing OpenPAC world claims. Avoid silently changing this policy without discussion.

## 20. OpenPAC visual identity

Current intended visual identity:

- The Ascension Nexus:
  light blue / cyan
- reserved future island slots:
  green
- personal player claims:
  use player's own OpenPAC visual configuration

Current prototype color intent:
Nexus approximately `#55D6FF`
reservations approximately `#55CC66`

The Ascension Nexus should use a named server sub-claim rather than renaming every Server Claim.

Canonical OpenPAC name:
`The Ascension Nexus`

Do not rename all reserved island claims to The Ascension Nexus.

## 21. Native Nexus protection — required behavior

Goal:
The Ascension Nexus must remain protected with or without OpenPAC.

Protection should cover the logical circular region centered on 0,0.

Current target behavior for normal players:

deny:
- breaking blocks
- placing blocks
- placing/removing fluids
- fire placement
- TNT/explosion destruction
- destructive piston behavior
- destructive entity grief where relevant
- End Crystal placement if destructive

allow:
- walking
- non-destructive buttons
- doors
- chests
- intended future altar/receptacle interactions

Development bypass:
do NOT rely on OP status.

Reason:
in integrated singleplayer, the host is effectively privileged and would bypass protection during testing.

Preferred current bypass:
Creative mode can edit.
Survival/Adventure remain protected even if OP.

If a better explicit permission system is implemented later, keep it intentional and easy to test.

## 22. Known protection implementation history

A first prototype used:

`PlayerBlockBreakEvents.BEFORE`
and
`UseBlockCallback.EVENT`

The first build failed because old mapping assumptions were used:
- `player.getServer()`
- `isOp(player.getGameProfile())`

Minecraft 26.2 expected newer APIs.

A second bypass attempt used:
`player.getTags()`
which also did not compile with the current mappings.

Do not reintroduce those exact APIs without verifying.

Current preferred simple bypass:
`player.isCreative()`

Before changing this, compile against the current branch.

## 23. Explosion protection

Explosion/TNT protection is still a required future layer.

Do not assume block-break events alone protect against explosions.

If Fabric API does not expose an appropriate cancellable regional explosion callback for this version, a focused mixin is acceptable.

Mixin rules:

- keep it small
- target only explosion block destruction behavior needed by the Nexus
- do not globally disable explosions
- outside the protected Nexus, vanilla behavior should remain unchanged
- inside Nexus, prevent block destruction
- ideally preserve visuals/damage unless design explicitly says otherwise
- test TNT, creepers and End Crystals separately

## 24. End portal initial eyes

The End portal is standard 12-frame.

Allowed initial configurations:
- 0% = exactly 0 eyes
- 25% = exactly 3 eyes
- 75% = exactly 9 eyes
- never 100%

Default:
0%

Positions should be randomized but the quantity must be exact.

The portal should not be automatically activated by initialization.

The initial eye choice is structural world configuration.

Once the world is initialized, this choice should be locked for that world.

Do not reset/re-randomize eyes on server restart.

Do not remove eyes inserted later by players.

## 25. Current portal persistence direction

Current prototype uses a per-world persistent state file concept:
`skyblockmulti_nexus.properties`

Purpose:
record that the portal initial-eye configuration was already applied.

This avoids reapplying eye layout after restart.

The design goal matters more than the exact storage mechanism.

If refactoring, preserve:

- initialize once
- exact count
- persistent
- no re-randomization
- future player-inserted eyes remain untouched

## 26. Mod Menu — future portal option

The End portal initial-eye percentage should eventually be exposed in Mod Menu.

Desired UI concept:

Spanish:
`Portal del End`
`Ojos iniciales: 0%`
`Ojos iniciales: 25%`
`Ojos iniciales: 75%`

English:
`End Portal`
`Initial Eyes: 0%`
`Initial Eyes: 25%`
`Initial Eyes: 75%`

This must eventually use localization keys in all 13 locales.

If the current world has already initialized structural portal settings, the control should be disabled/locked for that world, similar to geometry locking.

Do not silently change an existing world's portal setting.

## 27. End access progression

The End portal must not be reachable immediately.

The fortress may be visible from the HUB, but physical access should be sealed.

Future progression path:

ISLANDS
→ progression
→ discover lateral access
→ dungeon
→ puzzles/combat/resources
→ fortress
→ offering chamber
→ End portal

Do not add a direct staircase from HUB to fortress.

## 28. Eight-seal offering chamber

Planned world-global progression before the portal:

8 seals total.

4 cardinal armor requirements:
- full leather
- full gold
- full iron
- full diamond

4 intercardinal custom offerings:
- Earth / Harvest
- Trees / Forest
- Metals / Forge
- War / Hunt

Planned custom offering identity:
`custom_data`
with concept:
`skyblockmulti:offering=earth|trees|metals|war`

Each correct offering:
- is consumed
- permanently completes its seal

8/8:
- effects/sound
- permanently opens physical barrier to portal area

This progression is world-global, not per player or party.

Do not implement giant item stacks as requirements.

Use a small number of meaningful ingredients.

## 29. Resource audit rules

Before making progression requirements, classify resources as:

- renewable
- finite
- unavailable
- future source

Important constraints:

- loose diamonds are not intended as a direct mandatory sink
- diamond tools/armor via villagers/trading are acceptable
- Ancient Debris is not obtained from piglin bartering
- current void Nether does not naturally supply Ancient Debris
- do not require Ancient Debris for End access at this stage

A future controlled Ancient Debris / Netherite Scrap source is planned.

## 30. Death / respawn behavior

Normal death must not return established players to the HUB.

Assigned players should return to their own/current valid island respawn logic.

Party transitions must respect beds/anchors carefully.

Leaving party with OWN:
return OWN

Leaving party without OWN:
reset and return HUB

Do not regress this behavior while working on Nexus code.

## 31. Existing world geometry persistence

World geometry should lock after the first real island assignment.

Runtime values:
- active world radius
- active world capacity

World persistence uses scoreboard/config logic already present.

Do not replace existing world radius/capacity for an established world just because global defaults changed.

Mod Menu defaults are intended for future/new worlds when no active locked world overrides them.

## 32. Commands / old public triggers

Old public normal-play triggers such as:
- sb_hub
- sb_home
- sb_info

have been removed/restricted from normal intended gameplay.

Do not reintroduce an unrestricted normal `/trigger sb_hub` that allows established players to return to the HUB.

The HUB should normally be one-time only, except the legitimate reset case.

## 33. Debugging

There is a SkyblockMulti debug command/snapshot system.

Keep `/skyblockmulti debug` functional.

When diagnosing:

- prefer log evidence
- prefer actual current mappings
- prefer GitHub Actions compiler output
- avoid speculative API assumptions

## 34. Build and CI

GitHub Actions is the primary trusted compilation path.

The user may not have a local Gradle installation.

Do not require the user to install Gradle.

The repository now includes/uses a valid wrapper in CI context, but practical testing may still happen through Actions.

When a build fails:
- inspect the exact compiler error
- fix only the reported incompatibility first
- rerun CI

Do not perform broad rewrites in response to a single mapping failure.

## 35. Current known good facts from recent runtime

Recent test log confirmed:

- Minecraft 26.2
- Fabric Loader 0.19.3
- SkyblockMulti loads as `0.2.0-beta`
- startup console says `Mod 0.2.0-beta inicializado`
- `Nexus Foundation inicializado`
- initial End portal backend initialized 0% = 0/12 eyes
- OpenPAC can be absent and SkyblockMulti still starts

A stale chat line still showed 0.1.1-beta in an older iteration because of locale/datapack text, not because the JAR version was wrong.

## 36. Current immediate development priorities

Proceed in roughly this order unless explicitly redirected:

1. Make native Nexus protection reliably work in Survival without OpenPAC.
2. Verify Creative development bypass.
3. Add regional explosion/TNT protection.
4. Add Mod Menu control for End portal initial eyes:
   0 / 25 / 75
5. Update all 13 locales.
6. Verify portal persistence:
   no re-randomization after restart.
7. Keep fortress/portal structurally protected.
8. Later:
   pistons
   fire spread
   entity grief
9. Then:
   offering/seal system
10. Then:
   dungeon access/progression
11. Then:
   visual redesign and polish of HUB/fortress

## 37. Visual design policy

The current HUB and fortress are prototypes.

Do not overfit code to the current block layout.

Expected future visual changes may include:
- different terrain shape
- larger/smaller dome
- different glass pattern
- vegetation
- ruins
- improved paths
- stronger stronghold atmosphere
- portal chamber redesign
- better lighting
- different fortress silhouette

Protection and progression code should survive these visual changes.

## 38. Fortress style

Desired general atmosphere:

- vanilla stronghold inspired
- deepslate
- stone brick variants
- obsidian/reinforced accents where appropriate
- iron bars
- controlled lava
- ancient/ruined atmosphere
- no silverfish spawner

Do not add a silverfish spawner.

The fortress is not meant to be a normal vanilla-generated stronghold.

It is a custom central progression structure.

## 39. Future flooded dimension

Planned concept:
`skyblockmulti:flooded_world`

Theme:
ancient drowned civilization beneath a high sea.

Possible future content:
- monuments
- trial chambers
- ancient-city-like content
- procedural ruins
- controlled progression resources

This dimension is not implemented yet.

Do not implement it unless explicitly asked.

## 40. Code quality guidance

Prefer:

- small focused classes
- clear comments only where behavior is non-obvious
- explicit versioned migration markers
- deterministic world-state checks
- graceful optional-mod detection
- no hard dependency on OpenPAC
- no hardcoded user names
- no hardcoded mod version strings
- no hidden behavior tied only to client-side mods

Avoid:

- giant one-off method rewrites
- unnecessary reflection
- reintroducing deprecated/old mapped APIs
- world-destructive migrations
- resetting player progress unexpectedly
- regenerating persistent structures on every load

## 41. Safety for existing worlds

Before any code that:
- deletes blocks
- resets scoreboards
- rewrites islands
- reassigns claims
- changes structural world settings
- rebuilds the Nexus

stop and inspect current persistence logic first.

Existing worlds are important test assets.

Do not silently destroy or overwrite them.

## 42. When uncertain

If a design decision is ambiguous:

- preserve current behavior
- make the smallest safe change
- leave a concise TODO
- report the ambiguity

Do not invent a major gameplay rule without explicit direction.

If a technical API is uncertain:
- inspect the current mappings/decompiled sources or compile against the branch
- do not guess from older Minecraft versions.

## 43. Suggested Codex task format

For each task, use this pattern internally:

1. Goal
2. Files likely involved
3. Current behavior
4. Intended behavior
5. Compatibility constraints
6. Minimal implementation
7. Build
8. Test notes
9. Summary of changed files

Example current task:

Goal:
make native Ascension Nexus protection work for Survival players without OpenPAC.

Constraints:
- Minecraft 26.2
- Fabric API current branch
- Creative can bypass for development
- OP alone must not bypass
- protection only inside circular Nexus
- outside Nexus vanilla behavior
- OpenPAC optional
- do not alter island logic

Build before and after.
