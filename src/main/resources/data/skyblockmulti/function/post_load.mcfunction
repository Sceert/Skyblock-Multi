execute in minecraft:overworld run kill @e[type=minecraft:text_display,tag=skyblock_island_label]
# Configuración general y respaldo de posiciones.
# Valores seguros para el menú. Java aplica general.toml y los anillos persistidos.
execute unless score #capacity sb3_cfg matches 1..128 run scoreboard players set #capacity sb3_cfg 24
execute unless score #enabled_count sb3_cfg matches 1.. run scoreboard players set #enabled_count sb3_cfg 11
execute unless score #distance sb3_const matches 1.. run scoreboard players set #distance sb3_const 2048
execute unless score #bonus_tier sb3_cfg matches 0..3 run scoreboard players set #bonus_tier sb3_cfg 3
execute unless score #party_leave_tier sb3_cfg matches 0..3 run scoreboard players set #party_leave_tier sb3_cfg 3
execute unless score #adv_timer sb_adv_timer matches 0.. run scoreboard players set #adv_timer sb_adv_timer 0
execute unless score #adv_phase sb_adv_timer matches 0..1 run scoreboard players set #adv_phase sb_adv_timer 0
execute unless score #tree_oak sb3_cfg matches 0..1 run scoreboard players set #tree_oak sb3_cfg 1
execute unless score #tree_spruce sb3_cfg matches 0..1 run scoreboard players set #tree_spruce sb3_cfg 1
execute unless score #tree_birch sb3_cfg matches 0..1 run scoreboard players set #tree_birch sb3_cfg 1
execute unless score #tree_jungle sb3_cfg matches 0..1 run scoreboard players set #tree_jungle sb3_cfg 1
execute unless score #tree_acacia sb3_cfg matches 0..1 run scoreboard players set #tree_acacia sb3_cfg 1
execute unless score #tree_cherry sb3_cfg matches 0..1 run scoreboard players set #tree_cherry sb3_cfg 1
execute unless score #tree_mangrove sb3_cfg matches 0..1 run scoreboard players set #tree_mangrove sb3_cfg 1
execute unless score #tree_dark_oak sb3_cfg matches 0..1 run scoreboard players set #tree_dark_oak sb3_cfg 1
execute unless score #tree_pale_oak sb3_cfg matches 0..1 run scoreboard players set #tree_pale_oak sb3_cfg 1
execute unless score #tree_azalea sb3_cfg matches 0..1 run scoreboard players set #tree_azalea sb3_cfg 1
execute unless score #tree_flowering_azalea sb3_cfg matches 0..1 run scoreboard players set #tree_flowering_azalea sb3_cfg 1
tag @a[scores={sb3_state=1}] remove skyblock_menu_shown_v1
scoreboard players operation #max sb3_const = #capacity sb3_cfg
scoreboard players set #claim sb3_const 0
# Java supplies the active ring slots; no independent fallback geometry.
# Migración no destructiva del mundo de desarrollo actual: el marcador existente confirma la generación.
execute if entity @e[type=minecraft:marker,tag=skyblock_slots_ready_v2,limit=1] run scoreboard players set #slots_built sb3_const 1
execute if entity @e[type=minecraft:marker,tag=skyblock_slots_ready_v2,limit=1] run scoreboard players set #slots_complete sb3_const 1
execute unless score #slots_complete sb3_const matches 1 run function skyblockmulti:slots/forceload
execute unless score #slots_complete sb3_const matches 1 unless score #slotgen sb3_const matches 1.. run scoreboard players set #slotgen sb3_const 100

# The Ascension Nexus: spawn principal.
execute in minecraft:overworld run setworldspawn 0 161 0
execute in minecraft:overworld run gamerule respawn_radius 0

# Coliseo Infernal: generar solo una vez por mundo; una versión nueva nunca sobrescribe bloques existentes.
execute unless score #arena_built sb_infernal matches 1 unless score #arena_build_pending sb_infernal matches 1 run function skyblockmulti:infernal_trial/build

# El Nexus no se vuelve a colocar durante cargas o cambios de dimensión.
# #nexus_built y #nexus_functional son estado persistente del mundo.

# Marcador técnico reposicionado al nivel del nuevo HUB.
execute in minecraft:overworld unless entity @e[type=minecraft:marker,tag=skyblock_system_v1,limit=1] positioned 0 160 0 run summon minecraft:marker ~ ~ ~ {Tags:["skyblock_system_v1"]}

execute if score #slots_complete sb3_const matches 1 run function skyblockmulti:slots/forceload
execute if score #slots_complete sb3_const matches 1 run function skyblockmulti:slots/sync_occupancy
execute if score #slots_complete sb3_const matches 1 run function skyblockmulti:slots/remove_forceload

# El mensaje de versión ahora lo envía VersionJoinMessage.java usando la metadata real del mod.

# Limpiar bloqueos residuales de versiones anteriores.
execute as @a[scores={sb3_state=2},tag=skyblock_selection_locked] run function skyblockmulti:player/unlock_selection
