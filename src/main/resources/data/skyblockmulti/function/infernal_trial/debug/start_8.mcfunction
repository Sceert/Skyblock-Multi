# Depuración: inicia VIII sin consumir un núcleo. Ejecutar cerca del altar.
scoreboard players set #trial_level sb_infernal 8
execute in minecraft:the_nether run summon minecraft:marker 0 64 0 {Tags:["skyblock_infernal_debug_start"]}
execute in minecraft:the_nether as @e[type=minecraft:marker,tag=skyblock_infernal_debug_start,limit=1] run function skyblockmulti:infernal_trial/start
