# Infernal trials data API — schema 1

Status: loader, validation, immutable reload registry and dynamic trial execution
implemented. Built-in trials retain their proven mcfunctions, while external
definitions run through the same arena lock, countdown, cleanup, phase-delay,
victory, cooldown and spawn-restoration guarantees.

## Champion presentation

Infernal champions use a default visual scale of `1.4` so they are immediately
recognisable as the mini-boss of the final phase. Scale remains configurable per
mob entry and is bounded by schema validation for arena safety.

## Design goals

- Add or replace trial levels without editing Skyblock Multi's internal functions.
- Keep arena coordinates separate from encounter composition.
- Allow multiple phases, champions and difficulty/participant scaling.
- Validate datapacks on reload and reject unsafe definitions with useful logs.
- Keep all spawned enemies identifiable so cleanup and victory detection remain reliable.
- Reference Minecraft loot tables instead of embedding long reward command lists.

## Resource layout

```text
data/<namespace>/skyblockmulti/infernal_arenas/<arena>.json
data/<namespace>/skyblockmulti/infernal_trials/<trial>.json
data/<namespace>/loot_table/infernal_trials/<reward>.json
```

Registered infernal cores would point to a trial ID. The MAX core may select one
of several trials through a weighted escalation table.

Datapacks cannot register a brand-new item ID by themselves. A custom nucleus can
therefore use any registered item (vanilla, Skyblock Multi, or another installed
mod) plus a unique `custom_data` object. Its visible name and model may be supplied
by item components/resource-pack assets. The API compares both the item and the
declared custom data, so authors remain free to name and present their nucleus.

## Arena definition

The arena owns named floors and safe spawn points. Trial files refer to these
names instead of hard-coded coordinates.

```json
{
  "schema_version": 1,
  "dimension": "minecraft:the_nether",
  "origin": [0, 64, 0],
  "cleanup_radius": 64,
  "floors": {
    "base": [[-18, 0, -8], [18, 0, 8], [0, 0, 18]],
    "floor_1": [[-20, 8, 8], [20, 8, -8]],
    "floor_2": [[-18, 12, -18], [18, 12, 18]],
    "floor_3": [[0, 20, -18], [12, 20, 12]],
    "champion": [[0, 12, -18]]
  }
}
```

Every point must be checked for a solid floor, sufficient headroom and arena
bounds before an entity is summoned. `random` means a random valid point from
the requested floor set, not an arbitrary coordinate.

## Trial definition

```json
{
  "schema_version": 1,
  "arena": "skyblockmulti:infernal_colosseum",
  "display": {"translate": "skyblockmulti.infernal_trial.level_7"},
  "activations": [
    {
      "item": "minecraft:blaze_powder",
      "custom_data": {"example:infernal_trial": "level_7"},
      "consume": true
    }
  ],
  "countdown_seconds": 5,
  "cooldown_seconds": 60,
  "cleanup_mobs": true,
  "cleanup_items": true,
  "spawn_release_seconds": 30,
  "phases": [
    {
      "delay_after_previous_seconds": 3,
      "mobs": [
        {
          "entity": "minecraft:blaze",
          "count": 6,
          "floor": ["base", "floor_1", "floor_2"],
          "placement": "random",
          "health_multiplier": 1.25,
          "scale": 1.0,
          "name": {"translate": "skyblockmulti.infernal_trial.mob.ember_guard"},
          "effects": [
            {"id": "minecraft:fire_resistance", "amplifier": 0}
          ],
          "equipment": {},
          "drop_policy": "vanilla",
          "player_targeting_only": true
        }
      ]
    },
    {
      "mobs": [
        {
          "entity": "minecraft:piglin_brute",
          "count": 1,
          "floor": "champion",
          "placement": "fixed",
          "champion": true,
          "health_multiplier": 2.5,
          "damage_multiplier": 1.35,
          "knockback_resistance": 0.8,
          "scale": 1.4,
          "name": {"text": "Ashen Champion", "color": "dark_red", "bold": true},
          "drop_policy": "trial_only",
          "player_targeting_only": true
        }
      ]
    }
  ],
  "scaling": {
    "participants": {
      "extra_mobs_per_player": 2,
      "health_per_extra_player": 0.15,
      "maximum_participants_counted": 8
    },
    "island_difficulty": {
      "easy": {"count_multiplier": 0.75, "health_multiplier": 0.85},
      "standard": {"count_multiplier": 1.0, "health_multiplier": 1.0},
      "hard": {"count_multiplier": 1.2, "health_multiplier": 1.15},
      "extreme": {"count_multiplier": 1.4, "health_multiplier": 1.3}
    }
  },
  "rewards": {
    "completion": "skyblockmulti:infernal_trials/level_7",
    "per_participant": true,
    "phase_bonus": "skyblockmulti:infernal_trials/phase_bonus"
  }
}
```

The engine always provides the countdown, cleanup and cooldown lifecycle. These
fields are optional: omitted values default to a 10-second visible countdown,
hostile-mob and dropped-item cleanup enabled, a 60-second reuse cooldown, and a
30-second ambient-spawn release delay. Set either cleanup flag to `false` only
for an encounter that intentionally preserves that category inside the arena.
Every phase after the first also receives an eight-second visible intermission by
default. Use `delay_after_previous_seconds` on that phase to choose another
duration, including `0` when an immediate transition is intentional.

Participant health scaling defaults to `0.10` (10% per additional participant,
up to `maximum_participants_counted`). A declared value replaces that default.
Champions use 1.75 times the declared per-player health increase, so the default
for a second participant is 17.5%. Difficulty health and damage multipliers are
combined with the mob and participant multipliers.

## Recommended mob fields

Required:

- `entity`: registered entity ID.
- `count`: base number before participant/difficulty scaling.
- `floor`: one floor, a list of floors, or `random` across allowed floors.

Optional presentation and combat:

- `name`: text component, translatable or literal.
- `scale`: visual size and hitbox; recommended champion default `1.4`.
- `health_multiplier` and `damage_multiplier`.
- `knockback_resistance`, movement speed and follow range.
- `effects`: potion effects with amplifier and optional particles.
- `equipment`: item IDs/components for each equipment slot.
- `champion`: marks a mini-boss and enables its presentation.
- `spawn_particles` and `spawn_sound`.

Optional behaviour and lifecycle:

- `player_targeting_only`: prevents trial enemies fighting ambient mobs.
- `alliance`: places compatible enemies on the trial team.
- `drop_policy`: `vanilla`, `none`, or `trial_only`.
- `persistent`: defaults to true during the trial.
- `counts_for_victory`: defaults to true.
- `children_count_for_victory`: defaults to true, so magma-cube and slime divisions remain part of the trial; set false only when their children should be ignored.
- `fire_immune`: only when explicitly required by the encounter.
- `despawn_on_abandon`: defaults to true.

Optional spawning:

- `placement`: `fixed`, `random`, `spread`, or `round_robin`.
- `minimum_player_distance` and `maximum_player_distance`.
- `spawn_delay_ticks`: interval between mobs within one entry. The first appears immediately;
  mob index N appears at N times this interval. Zero keeps simultaneous spawning.
  Pending spawns prevent phase completion and are cancelled on abandonment.
- `chance`: permits rare variants inside a phase.
- `conditions`: difficulty, participant range, phase result, or another scoreboard-safe condition.

## MAX core escalation

Escalation should be data driven rather than hard-coded:

```json
{
  "core": "skyblockmulti:infernal_core_6",
  "fallback": "skyblockmulti:level_6",
  "by_difficulty": {
    "easy": [
      {"trial": "skyblockmulti:level_6", "weight": 900},
      {"trial": "skyblockmulti:level_7", "weight": 100}
    ],
    "extreme": [
      {"trial": "skyblockmulti:level_6", "weight": 550},
      {"trial": "skyblockmulti:level_7", "weight": 250},
      {"trial": "skyblockmulti:level_8", "weight": 130},
      {"trial": "skyblockmulti:level_9", "weight": 50},
      {"trial": "skyblockmulti:level_10", "weight": 20}
    ]
  }
}
```

Weights need not add to 100; the loader normalises them.

## Validation and safety limits

The loader should reject or clamp definitions that exceed configurable limits:

- Unknown entity, item, effect, sound, arena, floor or loot-table IDs.
- Negative counts, multipliers or delays.
- Excessive entity count per phase or trial.
- Scale outside a safe range, initially `0.5–2.0`.
- Spawn points outside the protected arena.
- Champions without a valid fixed/safe spawn point.
- Empty trials, duplicate IDs or escalation tables without a fallback.

On `/reload`, valid definitions should be swapped atomically. An active trial
continues using the validated snapshot with which it started; new data applies
to the next trial.

A complete copyable example is available under
`docs/datapacks/examples/infernal-api-v1`. Phase-specific arena functions may make
temporary changes, while `arena_restore_function`, the arena-level restore
function, and the baseline structure provide the restoration contract. The
dynamic runner will enforce restoration at phase end, completion and abandonment.
