# Logical Regions API v1

Skyblock Multi identifies its fixed central structures by dimension and coordinates.
This does not rebuild blocks, alter biomes or depend on OpenPAC.

## Built-in regions and player tags

| Region ID | Entity tag |
|---|---|
| `skyblockmulti:ascension_nexus` | `skyblockmulti_region_ascension_nexus` |
| `skyblockmulti:last_refuge` | `skyblockmulti_region_last_refuge` |
| `skyblockmulti:nexus_fortress` | `skyblockmulti_region_nexus_fortress` |
| `skyblockmulti:end_portal_chamber` | `skyblockmulti_region_end_portal_chamber` |
| `skyblockmulti:infernal_coliseum` | `skyblockmulti_region_infernal_coliseum` |

Tags synchronize every 10 server ticks. Nested regions keep every applicable tag;
for example, the End portal chamber also belongs to the Ascension Nexus.

```mcfunction
execute as @a[tag=skyblockmulti_region_nexus_fortress] run say Estoy en la fortaleza
execute as @a[tag=skyblockmulti_region_infernal_coliseum] run function example:arena/tick
```

Use `/skyblockmulti region` to show the most specific region at the player's current
position, or `/skyblockmulti region check <id>` to test a particular region.

Java integrations can query `LogicalRegionApi.regionsAt(...)` or register an additional
`LogicalRegion`. `LogicalRegionApi.API_VERSION` is `1`.
