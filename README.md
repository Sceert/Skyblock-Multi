# Skyblock Multi

Desde **0.2.4-v054-beta**, Skyblock Multi utiliza siempre su interfaz nativa.
Nexus Parties y los demás proveedores conservan su compatibilidad funcional.

Configurable multiplayer Skyblock for **Minecraft Java 26.3** on **Fabric**, focused on multiplayer-safe island allocation, player-selectable difficulty, custom progression, optional party integration, and a growing world/lore layer.

> **Minecraft 26.3 release:** `0.2.4-beta` (see `gradle.properties` for the authoritative version)
> [Modrinth](https://modrinth.com/mod/skyblock-multi) · [GitHub releases](https://github.com/Sceert/Skyblock-Multi/releases)

## Status

Skyblock Multi is currently in beta development.

`v0.1.1-beta` closed the multiplayer/OpenPAC foundation phase. `0.2.3-beta` expands the world foundation with the protected Ascension Nexus, its Last Refuge HUB, fortress progression, the Infernal Coliseum and a validated datapack API for custom trials.

## Requirements

- Minecraft Java **26.3**
- Fabric Loader **0.20.1+**
- Fabric API for Minecraft 26.3
- Cloth Config API **26.3.159+**
- Java **25**
- Mod Menu — optional, recommended for in-game configuration
- Nexus Parties — optional; preferred party provider when installed and API-compatible
- Open Parties and Claims — optional, for shared-party island integration
- Just Enough Items (JEI) — optional, for browsing Skyblock Multi recipes and custom offering variants

OpenPAC integration was tested previously on Minecraft 26.2; 26.3 compiles against **Open Parties and Claims 0.33.0**, with live multiplayer verification pending.
JEI integration has been tested with **JEI 31.17.0.108 for Fabric/Minecraft 26.3**.

## Main features

- Configurable radial island rings with stable slot IDs and up to 128 positions
- Rings 1–4 provide 8, 16, 24 and 32 positions; optional Ring 5 provides 48
- Configurable total capacity within enabled ring positions, including 8, 16 or 24
- Balanced, random-balanced, inner-first, outer-first and difficulty allocation
- Easy mode prefers inner rings; other difficulties prefer outer rings, with overflow
- 11 selectable tree starts plus a Random option
- Four player difficulty modes:
  - Extreme
  - Hard
  - Standard
  - Easy
- Configurable minimum difficulty
- Custom starting chests tied to difficulty
- Custom recipes and Skyblock-oriented progression
- Custom advancement tree, including secret advancements
- Initial lore book
- Secret End advancement for preserving the original lore book
- Optional OpenPAC party integration
- English and Spanish client localization
- The protected **Ascension Nexus**, with a custom glass-dome HUB and fortress
- Eight world-global armor/offering seals that unlock the End portal chamber
- Renewable fortress lava wells using a craftable Lava Catalyst
- A dedicated creative inventory tab for the mod's recipes and progression items

## Multiplayer and OpenPAC

### Compact island panel

Press **I** (configurable in Controls) or use `/skyblockmulti panel` to see your
personal/active island, group home and pending decisions or Infernal rewards.
The compact panel keeps the world visible; minimizing never accepts or rejects
a decision. Tree and difficulty selection use the same compact surface.
See [panel behavior and focused checks](docs/development/island-panel.md).
Facts appear in two columns with highlighted values.
The panel can remain visible while playing. Its configured key switches between
gameplay and interacting with the panel; incoming updates never take mouse focus.
**Close** dismisses the panel;
**Minimize** retains a small reminder (clickable with chat open, or reopen by key).
Mod Menu's **Island panel** category customizes size, colors and reminder position;
local preferences are saved in `config/skyblockmulti/client.toml`.

Provider priority is Nexus Parties, then OpenPAC, then solo; membership is never mixed.

The optional confirmed leadership/island-transfer protocol is documented in
[Party island transfer API](docs/datapacks/party-island-transfer-v1.md). The current
NP implementation is documented and its normal party/island transfer has been
confirmed in game by the user; failure/recovery manual verification and the SM preview
for a leader without OWN remain pending.
SM must not revoke OWN in response to an ordinary post-leadership event.

Without NP, a shared island remains personal property. If its owner leaves or
is removed, the group loses that home: members with personal islands return to
them, and other members use the current leader's available island or follow the
normal HUB reset. No island ownership, blocks or slot allocation are transferred.
Group formation/join messages explain this rule or the coordinated NP transfer.

Skyblock Multi can run without Open Parties and Claims.

When OpenPAC is installed:

- Party members can share the party owner's active island.
- A member's personal island is preserved while playing with another party.
- Leaving or being removed from a party returns the player to their own island if they already have one.
- A player who joined a party before creating a personal island is treated as a new player if they later leave that party:
  - inventory is reset
  - armor/offhand are reset
  - XP is reset
  - Ender Chest is reset
  - the player returns to the HUB
  - the initial island-selection cycle begins again
- Bed/respawn points are reset when party ownership context changes, preventing players from respawning on islands they no longer belong to.

## Island geometry

Configurable rings are the only distribution system. The default enables Rings 1
and 2 with 24 islands and a base radius of 2048 blocks.

| Ring | Positions | Stable slot IDs | Radius |
|---|---:|---|---|
| 1 | 8 | 1–8 | 50% of base |
| 2 | 16 | 9–24 | 100% of base |
| 3 | 24 | 25–48 | 150% of base |
| 4 | 32 | 49–80 | 200% of base |
| 5 (technical) | 48 | 81–128 | 250% of base |

Base radius presets are 2048, 4096, 8192 and 16384 blocks. Ring geometry,
angular offsets and stable IDs remain deterministic even with disabled rings.
Capacity cannot exceed enabled positions or the 80-slot normal / 128-slot
technical limit. Capacity 8, 16 or 24 is configured through these same rings.

The first island assignment locks the exact ring mask, capacity, radius and
allocation policy in the world save. Global defaults apply to future worlds;
there is no old-mode migration, island relocation or world reconstruction.

## Tree starts

Available initial tree types:

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

Random only selects from tree types enabled in the world configuration.

## Difficulty

Each player chooses their own starting difficulty after selecting a tree.

| Difficulty | Starting resources |
|---|---|
| Extreme | No starter chest or supplies; saplings are not guaranteed |
| Hard | Water + lava |
| Standard | Standard Skyblock starter resources |
| Easy | Additional starter resources and bed |

The server validates the selection. Difficulty options that are below the configured minimum remain visible but unavailable.

When OpenPAC is installed, a separate minimum difficulty can be configured for players who restart after leaving a party without ever owning an island.

## Lore and progression

New players receive the **Chronicle of the Last Refuge**, an initial written book that introduces the world and its background.

The book is intentionally worth preserving: reaching the End while still carrying the original Skyblock Multi lore book unlocks a secret advancement.

The custom advancement tree is designed both as progression tracking and as a guide for Skyblock gameplay.

## Current world structure

### Overworld

- Void world
- Shared central HUB
- Skyblock islands distributed around the HUB

### Nether

- Void-based Skyblock progression
- A central **Infernal Coliseum** generated once in new worlds
- Six craftable Infernal Cores with secret advanced trial tiers
- Difficulty- and participant-aware combat encounters
- Two-phase trials at tiers VII–IX and a dedicated third champion phase at tier X
- Raid-style enemy tracking, abandonment recovery and temporary spawn control
- Personal rewards with renewable Nether resources and controlled Ancient Debris progression

### End

- Vanilla End generation remains enabled
- Central island, dragon fight, gateways, outer islands, End Cities and ships remain part of progression

## 0.2.x-beta — World Foundation

The `0.2.x-beta` milestone establishes the mod's central world identity and its first shared progression systems. The current revision is `0.2.3-beta`:

- The **Last Refuge**, a walkable glass-dome introductory HUB inside the Ascension Nexus
- Native central protection for blocks, fluids and explosions, independent of optional OpenPAC integration
- A transparent vertical connection to the protected fortress below
- A deepslate, stronghold-inspired End portal chamber
- Configurable initial End Portal Eyes:
  - `0%` = 0/12 eyes
  - `25%` = 3/12 eyes
  - `50%` = 6/12 eyes
  - `75%` = 9/12 eyes
  - never 100%
- Eight permanent world-global seals: leather, gold, iron and diamond armor plus Earth, Forest, Forge and War offerings
- Craftable offering items, clue lecterns, colored beacon signals and a four-way portal reveal
- Renewable lava wells supplied through a craftable catalyst
- The **Infernal Coliseum**, a protected ritual arena in the void Nether
- Six visible trial tiers and secret tiers VII–X selected from the highest catalyst
- Progressive waves distributed throughout the arena galleries
- Multiplayer scaling, difficulty-aware rewards and personal reward recovery
- Renewable Nether progression resources, with Ancient Debris controlled by trial tier and difficulty
- A validated datapack API for custom catalysts, phases, mobs, equipment, scaling, timing, loot and temporary arena hooks

## Configuration

Skyblock Multi stores its configuration in:

```text
config/skyblockmulti/general.toml
```

The in-game configuration screen can control, depending on installed optional mods:

- enabled island rings and allocation policy
- total capacity derived from enabled rings in Cloth Config (TOML can set a lower total)
- base island radius
- enabled tree starts
- minimum starting difficulty
- added vanilla-material recipes, under **Difficulty → Crafting** (enabled by default)
- minimum difficulty after leaving an OpenPAC party without a personal island

Ring layout, capacity, radius and allocation become locked per world after the first island is assigned.

The crafting switch controls all 23 vanilla-material recipes added by Skyblock Multi;
vanilla Minecraft recipes, addon recipes and Nexus progression recipes remain independent.
Change it before opening a world. On dedicated servers, stop the server and edit
`general.toml`, then start it again:

```toml
[difficulty.crafting]
vanilla_recipes_enabled = false
```

Set it to `true` to restore the recipes. The server filters them when recipe data
loads; a multiplayer client's local setting cannot change server recipes.

## Localization

Skyblock Multi includes localized UI files for:

English:
- `en_us`
- `en_gb`
- `en_au`
- `en_ca`
- `en_nz`

Spanish:
- `es_es`
- `es_419`
- `es_cl`
- `es_mx`
- `es_ar`
- `es_ec`
- `es_uy`
- `es_ve`

Each client can display translated menus in its own language on the same multiplayer server.

## Installation

1. Install Minecraft Java 26.3.
2. Install Fabric Loader 0.20.1 or newer.
3. Install Fabric API.
4. Install Cloth Config API 26.3.159 or newer.
5. Place the Skyblock Multi `.jar` in the Minecraft `mods` folder.
6. Optionally install Mod Menu.
7. Optionally install Open Parties and Claims for party integration.
8. Create a world using the Skyblock Multi world preset.

For multiplayer, installing Skyblock Multi on both server/host and clients is recommended so players receive the intended translated UI.

## Development branches

- `main` — published/checkpoint code
- `feature/world-foundation` — current `0.2.3-beta` development
- older feature branches may remain temporarily for historical/reference purposes

## License

Skyblock Multi is distributed under the **MIT License**. See [`LICENSE`](LICENSE).

## Author

**Treecs**
