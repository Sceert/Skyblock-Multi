# Changelog

## 0.2.4-v048-beta — Leaders without personal islands

- Preserves PARTY_HOME when a new leader has no OWN; explains the fallback.
- Requires a personal slot for party-home destinations; HOME rejects missing
  coordinates and central (0, 0) before placing blocks.
- Leaders leaving without OWN follow the normal HUB reset.
- Party-reset selection stays immobilized and protected at Last Refuge until
  island assignment; ordinary first-time selection remains free to move.
- Updates the no-island and reset messages in all 13 locales.

## 0.2.4-v047-beta — Leadership island decision correction

- Preserves member ACTIVE destinations during leadership transfer instead of
  treating the former leader as a newly joined member.
- Moving PARTY_HOME activates the new leader's OWN island, teleports them there
  and updates their respawn before offering the remaining members a choice.
- Keeping the existing home, including cooldown and timeout retention, does not
  send members a result or reopen their island choices.
- Blocks member island decisions while the leadership decision is pending.

## 0.2.4-v046-beta — Localized difficulty selectors

- Shows localized Extreme, Hard, Standard and Easy names in both Cloth Config
  difficulty selectors instead of the internal starter-chest enum names.
- Reuses existing translations and preserves configuration values and behavior.

## 0.2.4-v045-beta — Ring counter rendering fix

- Fixes the Cloth Config world-tab crash by attaching the live counter to its
  parent list and screen before rendering.
- Caches the counter text until its value changes instead of recreating it on
  every frame.

## 0.2.4-v044-beta — Live ring capacity and advanced settings

- Displays the live maximum island count and each ring's contribution.
- Groups Rings 4 and 5 in advanced settings with a resource-use warning.
- Enables mode-specific controls dynamically and blocks empty custom layouts.
- No technical-capacity switch is exposed in Mod Menu.

## 0.2.4-v042-beta — Generic addon reward providers

- Added public starter-inventory and personal Trial reward providers.
- Guarded starter initialization per island and persisted completion supplements
  for online/offline participant UUIDs in the Infernal Vault reward ledger.
- Retained legacy pending rewards; added completion deduplication, atomic writes
  and backups. Contract and persistence limits: `docs/datapacks/addon-rewards-v1.md`.

## 0.2.4-v043-beta — Guided world layout configuration

- Restores localized, user-facing names for layout modes and allocation rules in
  Cloth Config instead of displaying internal enum identifiers.
- Custom-ring capacity is now derived from the enabled rings, preventing a
  capacity greater than the available positions.
- Ring 5 now enables technical capacity implicitly; the redundant opt-in toggle
  was removed from Mod Menu.
- An empty custom selection safely falls back to Ring 1 when saving.

## 0.2.4-v042-beta — Cloth addon submenus

- Groups configurable addon capabilities into one expandable Cloth Config
  submenu per installed addon, matching the tree-category organization.

## 0.2.4-v041-beta — Starter-chest addon feature contract

- Added the generic `starter_chest` addon feature type so future integrations
  can expose optional initial-island contents in Cloth Config.
- Recorded separate switches for the planned Waystones starter chest and
  Infernal Trial loot; disabling one does not disable the other.

## 0.2.4-v040-beta — Cloth tree submenus

- Restores the Vanilla/addon separation inside the Cloth Config tree category.
- Places Vanilla trees in their own expandable submenu and creates one additional
  submenu for every installed mod or addon that contributes tree definitions.

## 0.2.4-v039-beta — Cloth Config interface

- Replaces the bespoke Mod Menu configuration screen with Cloth Config categories
  for world layout, difficulty, trees, addon switches and party cooldown.
- Keeps TOML as the authoritative dedicated-server format and preserves existing
  validation, migration and world-lock rules.
- Makes Cloth Config API 26.2.155 or newer a declared runtime dependency.

## 0.2.4-v038-beta — Party-home change cooldown

- Adds a persistent real-time cooldown for effective `PARTY_HOME` changes caused
  by leadership transfers.
- Defaults to 24 hours and can be changed or disabled with
  `[parties].island_change_cooldown_hours` in `general.toml`.
- Leadership may still transfer during cooldown, but the existing shared island
  is retained so repeated transfers cannot provide reusable island changes.

## 0.2.4-v037-beta — Ordered leadership transfer

- The new leader now makes the shared-island decision without receiving a second,
  overlapping personal-island prompt.
- Party members, including the former leader, receive their individual destination
  choice only after `PARTY_HOME` has been selected or the decision expires.
- Keeping the existing party island now also triggers the member destination flow.

## 0.2.4-v036-beta — Clear ring-list example

- Clarifies that `enabled_rings` accepts ring numbers, not their capacities.
- Adds `enabled_rings = [2, 3, 4]` as an explicit TOML and README example.

## 0.2.4-v035-beta — Documented TOML configuration

- Replaces editable `general.json` with a commented `general.toml` and automatic migration.
- Migrates per-addon feature switches and tree settings from JSON to commented TOML files.
- Adds a generated server configuration README and rejects impossible custom capacities.

## 0.2.4-v034-beta — Self-documented server configuration

- Completa los campos de anillos ausentes en configuraciones dedicadas antiguas.
- Añade ayuda válida dentro de `general.json`, incluidos radios, capacidades y valores permitidos.
- Aclara que en modo personalizado `islandRadius` corresponde al anillo 2.

## 0.2.4-v033-beta — Tree configuration isolation

- Separa la configuración de árboles de `general.json`.
- Guarda Vanilla en `config/skyblockmulti/trees/vanilla.json` y cada addon o datapack en un archivo por namespace.
- Migra automáticamente los estados existentes y conserva proveedores temporalmente ausentes.

## 0.2.4-v032-beta — Unified server configuration

- Consolidated the general and Nexus defaults in
  `config/skyblockmulti/general.json`, including the initial End Portal eyes.
- Added safe migration from the former `skyblockmulti.json` and
  `skyblockmulti_nexus.json` files without changing per-world portal state.
- Added a dedicated `config/skyblockmulti/addons/` directory so dedicated
  servers receive one editable feature-switch file per detected addon.
- Migrates the previous combined `skyblockmulti-addons.json` values into the
  corresponding per-addon files.

## 0.2.4-v031-beta — Single Infernal Vault interaction

- Restricted Infernal Vault reward claims to the player's main hand and ignored
  duplicate interaction callbacks received during the same server tick.
- Prevents one click from repeating every reward or empty-vault message and from
  consuming more than one pending personal reward.

## 0.2.4-v030-beta — Infernal arena recovery polish

- Returns fallen trial enemies to `0 65 -5`, opposite the Infernal Vault,
  instead of placing them on the central fire.
- Replaced the fixed magma-cube cleanup with six delayed recursive passes so
  every subdivision is removed before combat starts or after abandonment.
- Added the Infernal Vault item translation alongside its block translation in
  every supported locale.
- Updated the Infernal Coliseum template to the author's latest WorldEdit
  revision and recorded it internally as arena version 7.
- Added a reusable Sponge schematic v3 to vanilla structure converter for future
  visual revisions.

## 0.2.4-v029-beta — Phase-wide Infernal health baseline

- Preserves the combined maximum health captured during each Infernal phase.
  Defeated enemies now reduce the bar permanently instead of disappearing from
  both its current and maximum values and making survivors appear at 100%.
- Applies the stable baseline to built-in and data-driven trials, while allowing
  it to grow if delayed reinforcements appear.

## 0.2.4-v028-beta — Stable Infernal health bar

- Removed the competing per-tick enemy-count write to the built-in Infernal
  bossbar. Its progress now has one owner and consistently represents combined
  remaining enemy health; the enemy count remains visible in the bar title.

## 0.2.4-v027-beta — Infernal Vault startup fix

- Fixed a startup crash caused by copying the vanilla Vault's state-dependent
  light properties into the plain Infernal Vault block.
- The Infernal Vault now uses independent metal block properties while retaining
  its ominous-vault model and Skyblock Multi reward behavior.

## 0.2.4-v026-beta — Code and integration audit

- Removed the retired OpenPAC island-reservation, assignment and initial-claim
  backend. OpenPAC now contains only the permanent Nexus Server Claim and its
  optional party-provider adapter, matching the active integration policy.
- Removed a duplicate Nexus interaction synchronization pass and dead dynamic
  trial snapshot state.
- Consolidated data-driven Infernal health scaling around the public
  `health_per_extra_player` setting instead of maintaining a conflicting fixed
  multiplier in the runner.
- Fixed data-driven difficulty health and damage multipliers, which were parsed
  but not applied to spawned enemies.
- Added regression checks for the default 10% regular and 17.5% champion health
  increases with two participants, and documented their composition rules.
- Audited JSON resources, locale parity, duplicate Java/mcfunction content and
  stale references to the removed OpenPAC helpers.

## 0.2.4-v025-beta — Repeated Infernal abandonment access

- Moved the abandonment shortcut from the initial trial countdown to the exact
  start of every combat phase, after its counter reaches zero.
- Repeats the clickable abandonment shortcut once when a participant leaves the
  arena radius. Returning rearms the boundary warning, with an individual
  one-minute cooldown to prevent repeated crossings from flooding chat.
- Applied phase and distance reminders to built-in and datapack-defined trials.

## 0.2.4-v024-beta — Infernal abandonment votes and addon planting stock

- Added a clickable voluntary-abandonment vote to built-in and datapack-defined
  Infernal Trials. The electorate is the trial's participant snapshot, the vote
  lasts one minute, and abandonment requires a strict majority; ties continue.
- Preserved configured reinforcement counts while scaling maximum health by each
  additional participant: regular enemies gain 10% and champions gain 17.5%,
  counting at most eight participants.
- Extended tree option schema 1 with optional `planting_stock`, allowing addons to
  declare an initial reproductive item and quantity from 1 to 64 for the starter
  chest without hardcoded mod knowledge.
- Updated the public tree-option documentation and automated verification for the
  new addon contract.

## 0.2.4-v023-beta — Personal Infernal Vault and combat scaling

- Replaced automatic built-in Infernal Trial rewards with a persistent personal
  reward ledger. Completed rewards survive disconnects, restarts and later trials
  and are consumed one at a time only when their player clicks the vault.
- Added the SkyblockMulti-owned Infernal Vault block, visually based on the
  ominous-vault concept but without changing vanilla vaults or requiring keys.
- Limited Infernal Trial participation and scaling to the activating player's
  party when an external party provider is active; nearby-player behavior remains
  the fallback when no party provider is available.
- Changed built-in and data-driven encounter bars to track combined enemy health,
  while retaining the remaining-enemy count in the data-driven bar label.
- Enlarged the final champion and raised its maximum health to 160.
- Added a level-scaled 2–10% chance of receiving a Lava Catalyst from personal
  built-in Infernal Trial rewards.
- Restyled vanilla tree settings to match the compact two-column addon-tree list.
- Locked tree, difficulty and addon switches while a world is open so these
  future-world settings cannot be changed during an active session.

## 0.2.4-v019–v022-beta — Addon controls and tree presentation

- Grouped modded tree settings by their declared required mod and changed the
  layout to two named trees per row with compact ON/OFF buttons.
- Extended the addon feature contract with runtime switches for optional Wandering
  Trader offers, challenge loot and future addon-owned systems.
- Redesigned the ADDONS page as grouped integration lists with descriptive labels
  on the left and compact ON/OFF controls in a consistent right-hand column.
- Added a dynamic ADDONS tab to Mod Menu and a generic `fabric.mod.json`
  capability contract for enabling or disabling recipe groups supplied by
  optional addons. Recipe changes take effect after a data reload or restart.

## 0.2.4-v018-beta — Crafting and OpenPAC scope corrections

- Converted material recipes without a meaningful layout, including mycelium,
  to shapeless recipes while preserving their ingredient costs and output waste.
- Added a balanced shapeless Soul Sand recipe using sand, rotten flesh and a
  ghast tear, yielding eight blocks from nine ingredients.
- Corrected the no-`OWN` party-leave reset to arrive safely at Y=161 in the Last Refuge.
- OpenPAC now always maintains the permanent Ascension Nexus Server Claim even
  when Nexus Parties is authoritative, while Skyblock Multi no longer creates,
  reserves or transfers OpenPAC island claims.
- Recorded the Biomes O' Plenty block recipe audit as future addon-only work.

## 0.2.4-v017-beta — Per-player party island choice

- Party members now choose independently between their preserved personal island
  and the party home instead of being moved automatically.
- Accepting an available party home performs one immediate transfer; the choice
  command cannot be reused as free travel afterward.
- Members waiting in the Ascension Nexus are notified when the leader creates the
  party's first island, including after reconnecting.
- Leadership home changes now prompt each member instead of teleporting the whole
  party, while `OWN` remains separate from `ACTIVE`.

## 0.2.4-v012-beta — Logical structure regions

- Added stable logical regions for the Ascension Nexus, Last Refuge, Nexus fortress,
  End portal chamber and Infernal Coliseum without rebuilding or converting structures.
- Exposed synchronized player tags and diagnostic commands for datapacks.
- Added a public Java query/registration API and automated geometry verification.

## 0.2.4-v011-beta — Fortress glass correction

- Corrected the two actual missing black stained-glass floor tiles in the upper fortress rooms.
- Removed the inaccurate provisional repair coordinate from v010.

## 0.2.4-v010-beta — Nexus visual polish

- Reframed the mod icon with a closer and more centered view of the Ascension Nexus.
- Restored the missing black stained-glass floor tile in the western fortress room.

## 0.2.4-v009-beta — Ascension Nexus identity

- Replaced the mod icon with a centered view of the complete Ascension Nexus.
- Added a matching, clearly labelled icon to the Biomes O' Plenty tree addon.

## 0.2.4-v008-beta — Adaptive tree selection grid

- Expanded the in-game tree selector to four columns on wide interfaces.
- Removed the artificial four-row limit and now uses the available vertical space.
- Updated the Biomes O' Plenty compatibility module to own its localized tree names;
  the core remains independent and continues reading generic datapack/JAR metadata.

## 0.2.4-v007-beta — Pre-world modded tree discovery

- Added client-side discovery of tree definitions bundled in installed mods, allowing
  the Mod Menu page to list them before entering a world.
- Preserved server synchronization as the authority for world-specific datapacks.
- Moved the tree summary above the grid so it no longer overlaps the first row.

## 0.2.4-v006-beta — Synced modded tree settings

- Fixed the Mod Menu tree page so it uses the server-synchronized tree catalog.
- External trees supplied by mods or datapacks, including the Biomes O' Plenty
  compatibility module, now appear under the modded-tree tab while connected.
- Kept the built-in catalog as a safe fallback when no world/server catalog is available.

## 0.2.4-v005-beta — Unified island controls

- Replaced the duplicated legacy/custom island-capacity controls with one contextual control.
- Kept the compatibility mode internally so existing 8/16/24 geometry remains unchanged.
- Removed redundant headings that overlapped the world-layout buttons at GUI scale 2.
- Corrected custom-ring radius cycling so it consistently uses the advanced radius options.

## 0.2.4-v004-beta — Mod Menu organization

- Consolidated island geometry and ring controls into the World tab.
- Split the tree page between vanilla and mod/datapack definitions.
- Grouped minimum difficulty, party-leave difficulty and initial End eyes in a
  dedicated Difficulty tab.
- Removed the OpenPAC-specific wording from party-leave settings so it applies to
  whichever party provider is active.

## 0.2.4-v003-beta — Configurable island rings

- Added a Mod Menu page for configuring island rings in new worlds.
- Preserved the existing 8, 16 and 24-island layouts without coordinate changes.
- Added four normal rings (up to 80 positions) and an explicit optional technical
  fifth ring (up to 128 positions).
- Added balanced, inner-first, outer-first, difficulty and random-balanced allocation.
- Custom geometry is persisted and locked when the first island is assigned.
- Stable slot identifiers allow sparse ring layouts without making old islands reusable.

## 0.2.4-v002-beta — Nexus Parties interoperability

This beta adds an optional party-provider bridge while keeping island ownership and
territorial protection independent from the selected party mod.

### Added

- Optional direct integration with Nexus Parties API v1 (`>=0.1.0-alpha.7`).
- Deterministic party-provider priority: Nexus Parties, OpenPAC, then solo mode.
- Automated verification for provider priority, fallback and single-authority behavior.
- Immediate Nexus party lifecycle event handling with periodic reconciliation retained
  only as a recovery mechanism.
- The first non-destructive `PARTY_HOME` state, independent from party leadership.
- Localized clickable decisions for keeping the current shared island or moving to
  the new leader's island, with a 60-second response window.
- Safe timeout behavior: preserve the current home while the former leader remains,
  or move to the new leader after the former leader leaves.

### Improved

- OpenPAC continues managing reservations, chunks and claims even when Nexus Parties
  is the authoritative group provider.
- Party reconciliation now tracks the active provider as well as party identity.
- A leadership change preserves the current shared island until an explicit decision
  moves or transfers it.
- Offline Nexus party owners preserve each member's last valid ACTIVE island until
  the owner can be resolved again.

### Fixed

- Leadership transfer no longer follows the party-leave reset path.
- Joining, leaving, transferring leadership or disbanding a party never removes a
  player's personal OWN island.
- A failure in the authoritative provider degrades that query to solo mode instead
  of mixing membership data from two party systems.

## 0.2.3-beta — Infernal Trials and Datapack API

This beta checkpoint adds the first shared combat activity to Skyblock Multi while preserving the existing Ascension Nexus and island progression.

### Added

- A persistent Infernal Coliseum in the void Nether.
- Six craftable Infernal Cores and secret advanced tiers VII–X.
- Progressive waves that scale with trial tier, selected island difficulty and nearby participants.
- Two phases for tiers VII–IX, with a dedicated third champion phase at tier X.
- Raid-style remaining-enemy display, start countdowns, cooldown feedback and arena lore.
- Personal reward delivery with a reliquary fallback for returning participants.
- Controlled Ancient Debris rewards, golden apple bonuses and renewable Nether materials.
- Temporary natural-spawn suppression inside the coliseum during active trials.
- A validated datapack API for custom Infernal Trials, phases, enemies, equipment,
  scaling, timing, arena hooks and custom catalyst items.
- Distinct 2D artifact sprites for the four Nexus offerings and six Infernal Cores.

### Improved

- Infernal enemies are distributed between the arena floor and upper galleries.
- Third-floor spawn positions now align with the walkable floor.
- Trial cleanup removes unrelated mobs, dropped items and every Magma Cube division.
- Split Magma Cubes remain part of the active wave and enemy counter.
- Infernal Cores are fire resistant and require a nearby living player to activate the altar.
- The arena automatically resets abandoned encounters and delays natural-spawn restoration after victory.
- The coliseum template includes the author's missing second-level floor segment.

### Fixed

- Prevented cores from being consumed by the altar while it is on cooldown.
- Prevented death-dropped cores from starting a trial without a nearby living player.
- Removed the countdown's redundant zero before the trial-start announcement.
- Corrected upper-floor mobs taking fall damage immediately after spawning.
- Removed obsolete procedural coliseum builders now replaced by the maintained NBT template.

### Known beta scope

- Additional Infernal Trial definitions and temporary arena transformations remain
  extensibility features for datapack authors rather than built-in progression.
# 0.2.4-v016-beta

- Hace mutuamente excluyentes las integraciones de parties: Nexus Parties tiene prioridad y OpenPAC queda como alternativa cuando Nexus Parties no está instalado.
- Desactiva la creación, transferencia y reserva de claims de OpenPAC cuando Nexus Parties está activo.
- Elimina la réplica de miembros entre Nexus Parties y OpenPAC para evitar privilegios territoriales residuales.
- Corrige el retorno inmediato a `OWN` —o el reinicio al Nexo si no existe— al abandonar, ser expulsado o disolverse una party de Nexus.

# 0.2.4-v015-beta

- Sincroniza la composición de Nexus Parties hacia OpenPAC para que los miembros compartan permisos territoriales.
- Teletransporta inmediatamente al integrante que entra a una party hacia `PARTY_HOME`.
- Tras resolver un cambio de liderazgo, mueve a todos los integrantes online a la isla activa elegida sin modificar sus islas `OWN`.
- Amplía a 128 el reconocimiento de slots personales en la reconciliación de parties.
