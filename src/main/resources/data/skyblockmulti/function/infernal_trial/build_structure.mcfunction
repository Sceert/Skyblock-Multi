# Coliseo Infernal v7: estructura editada por el jugador en WorldEdit.
# La plantilla ocupa exactamente -32,59,-32 .. 32,96,32.
execute in minecraft:the_nether run kill @e[type=minecraft:text_display,tag=skyblock_infernal_label]
execute in minecraft:the_nether run tag @e[tag=skyblock_infernal_enemy] add skyblockmulti_cleanup
execute in minecraft:the_nether run kill @e[tag=skyblock_infernal_enemy]
# Limpiar primero permite que los espacios de aire del schematic también se respeten.
execute in minecraft:the_nether run fill -32 59 -32 32 65 32 minecraft:air
execute in minecraft:the_nether run fill -32 66 -32 32 72 32 minecraft:air
execute in minecraft:the_nether run fill -32 73 -32 32 79 32 minecraft:air
execute in minecraft:the_nether run fill -32 80 -32 32 86 32 minecraft:air
execute in minecraft:the_nether run fill -32 87 -32 32 93 32 minecraft:air
execute in minecraft:the_nether run fill -32 94 -32 32 96 32 minecraft:air
execute in minecraft:the_nether run place template skyblockmulti:infernal_colosseum_v1 -32 59 -32
# Corrección puntual del autor: completar el tramo ausente del segundo nivel.
execute in minecraft:the_nether run fill 9 68 11 16 68 11 minecraft:polished_blackstone_bricks
# Los displays no forman parte del schematic; se restauran como capa funcional.
execute in minecraft:the_nether run summon minecraft:text_display 0.5 66.5 0.5 {Tags:["skyblock_infernal_label"],billboard:"center",text:{translate:"skyblockmulti.infernal_trial.altar",color:"gold",bold:true},background:0,shadow:true}
scoreboard players set #arena_built sb_infernal 1
scoreboard players set #arena_version sb_infernal 7
scoreboard players set #arena_build_pending sb_infernal 0
scoreboard players set #trial_active sb_infernal 0
scoreboard players set #trial_state sb_infernal 0
scoreboard players set #trial_level sb_infernal 0
scoreboard players set #cooldown sb_infernal 0
execute in minecraft:the_nether run forceload remove -32 -32 32 32
