# Infernal cores and datapacks

Skyblock Multi registers six infernal core items:

| Display tier | Item ID | Default trial level |
| --- | --- | --- |
| I | `skyblockmulti:infernal_core` | 1 |
| II | `skyblockmulti:infernal_core_2` | 2 |
| III | `skyblockmulti:infernal_core_3` | 3 |
| IV | `skyblockmulti:infernal_core_4` | 4 |
| V | `skyblockmulti:infernal_core_5` | 5 |
| MAX | `skyblockmulti:infernal_core_6` | 6, with a chance to escalate to 7–10 |

## Safe datapack customization

The crafting recipes are regular server data and may be replaced by a datapack
using the same resource IDs:

```text
data/skyblockmulti/recipe/infernal_core_1.json
data/skyblockmulti/recipe/infernal_core_2.json
data/skyblockmulti/recipe/infernal_core_3.json
data/skyblockmulti/recipe/infernal_core_4.json
data/skyblockmulti/recipe/infernal_core_5.json
data/skyblockmulti/recipe/infernal_core_6.json
```

Changing a recipe affects how its registered core is crafted. It does not alter
the level selected when that core is offered at the infernal altar.

## Encounter implementation

The six built-in cores retain their proven `skyblockmulti:infernal_trial/*`
functions. External datapacks can add independent trials through the validated
Infernal Trial schema without replacing those internal functions. A custom
catalyst may use any registered item plus unique `custom_data`, while definitions
control phases, mobs, equipment, scaling, timing, loot and arena hooks.

## Current visual direction

The production cores use six distinct transparent 2D artifact sprites. Their
colour and intensity progress from a dim ember through increasingly dangerous
orange, crimson and magenta energy, while remaining readable in the inventory,
hand, ground and item frames. Earlier cuboid experiments remain only as design
history and are not referenced by the current item models.
