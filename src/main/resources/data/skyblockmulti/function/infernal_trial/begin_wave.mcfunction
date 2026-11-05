scoreboard players set #trial_state sb_infernal 2
scoreboard players set #phase sb_infernal 1
execute in minecraft:the_nether run tag @e[tag=skyblock_infernal_enemy] add skyblockmulti_cleanup
execute in minecraft:the_nether run kill @e[tag=skyblock_infernal_enemy]
execute in minecraft:the_nether positioned 0 64 0 store result score #participant_count sb_infernal run execute if entity @a[tag=skyblock_infernal_participant,distance=..64]
# VI–VII representan a la Fortaleza. Retirar Piglins naturales del recinto
# evita guerras de facciones que ignoran al jugador y falsean el contador.
execute if score #trial_level sb_infernal matches 6..7 in minecraft:the_nether positioned 0 64 0 run kill @e[type=minecraft:piglin,distance=..64,tag=!skyblock_infernal_enemy]
execute if score #trial_level sb_infernal matches 6..7 in minecraft:the_nether positioned 0 64 0 run kill @e[type=minecraft:piglin_brute,distance=..64,tag=!skyblock_infernal_enemy]
execute if score #trial_level sb_infernal matches 6..7 in minecraft:the_nether positioned 0 64 0 run kill @e[type=minecraft:zombified_piglin,distance=..64,tag=!skyblock_infernal_enemy]
# I–VI conservan la composición original.
execute if score #trial_level sb_infernal matches 1..6 in minecraft:the_nether run summon minecraft:blaze -6 65 -6 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 1..6 in minecraft:the_nether run summon minecraft:blaze 6 65 6 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 2..6 in minecraft:the_nether run summon minecraft:blaze 0 70 -16 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 2..6 in minecraft:the_nether run summon minecraft:blaze 0 70 16 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 3..6 in minecraft:the_nether run summon minecraft:blaze -15 76 -14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 3..6 in minecraft:the_nether run summon minecraft:blaze 15 76 14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 4..6 in minecraft:the_nether run summon minecraft:wither_skeleton -5 64 -5 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 4..6 in minecraft:the_nether run summon minecraft:wither_skeleton 5 64 5 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 5..6 in minecraft:the_nether run summon minecraft:blaze -16 70 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 5..6 in minecraft:the_nether run summon minecraft:blaze 16 70 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 5..6 in minecraft:the_nether run summon minecraft:wither_skeleton 5 64 -5 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 5..6 in minecraft:the_nether run summon minecraft:wither_skeleton -5 64 5 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 6 in minecraft:the_nether run summon minecraft:blaze 0 84 -24 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 6 in minecraft:the_nether run summon minecraft:blaze 0 84 24 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 6 in minecraft:the_nether run summon minecraft:wither_skeleton 0 64 -7 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 6 in minecraft:the_nether run summon minecraft:wither_skeleton 0 64 7 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
# Refuerzo progresivo I–VI, repartido entre suelo y galerías.
execute if score #trial_level sb_infernal matches 1..6 in minecraft:the_nether run summon minecraft:blaze 0 68 10 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 2..6 in minecraft:the_nether run summon minecraft:blaze 10 72 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 3..6 in minecraft:the_nether run summon minecraft:blaze -10 76 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 4..6 in minecraft:the_nether run summon minecraft:wither_skeleton 0 76 -14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 5..6 in minecraft:the_nether run summon minecraft:blaze 14 84 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 6 in minecraft:the_nether run summon minecraft:wither_skeleton -14 76 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
# VII–X: asedio distribuido por puntos cardinales e intercardinales.
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin -18 64 -18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin 18 64 -18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin -18 64 18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin 18 64 18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin 0 64 -20 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin 0 64 20 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
# VII: Asedio de la Fortaleza, exclusivamente Wither Skeletons y Blazes.
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton 0 64 -20 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton 20 64 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton 0 64 20 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton -20 64 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton -12 64 -12 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton 12 64 12 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton -8 64 8 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton 8 64 -8 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin_brute 0 64 -14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin_brute 14 64 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:magma_cube -12 64 12 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,Size:3}
execute if score #trial_level sb_infernal matches 9..10 in minecraft:the_nether run summon minecraft:piglin_brute -14 64 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 10 in minecraft:the_nether run summon minecraft:piglin_brute 10 64 10 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 10 in minecraft:the_nether run summon minecraft:magma_cube -10 64 -10 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,Size:3}
execute if score #trial_level sb_infernal matches 7..10 in minecraft:the_nether run summon minecraft:blaze -14 72 -14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7..10 in minecraft:the_nether run summon minecraft:blaze 14 72 14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7..10 in minecraft:the_nether run summon minecraft:blaze -14 72 14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7..10 in minecraft:the_nether run summon minecraft:blaze 14 72 -14 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 9..10 in minecraft:the_nether run summon minecraft:blaze 0 76 18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:blaze 0 72 -12 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:blaze 0 72 12 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
# Refuerzos adicionales de la primera fase en niveles superiores.
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton -18 76 -8 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:wither_skeleton 18 84 8 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:blaze -8 86 18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 7 in minecraft:the_nether run summon minecraft:blaze 8 86 -18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin -18 76 -8 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:piglin 18 84 8 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 8..10 in minecraft:the_nether run summon minecraft:blaze -8 86 18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b}
execute if score #trial_level sb_infernal matches 9..10 in minecraft:the_nether run summon minecraft:piglin_brute 8 76 -18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 9..10 in minecraft:the_nether run summon minecraft:piglin_brute -8 84 18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,IsImmuneToZombification:1b}
execute if score #trial_level sb_infernal matches 10 in minecraft:the_nether run summon minecraft:magma_cube 18 64 0 {Tags:["skyblock_infernal_enemy","skyblock_infernal_regular"],PersistenceRequired:1b,Size:3}
execute if score #trial_level sb_infernal matches 7..10 run function skyblockmulti:infernal_trial/spawn_scaling
function skyblockmulti:infernal_trial/equip_enemies
execute in minecraft:the_nether run effect give @e[tag=skyblock_infernal_enemy] minecraft:slow_falling 5 0 true
execute in minecraft:the_nether store result score #wave_total sb_infernal run execute if entity @e[tag=skyblock_infernal_enemy]
execute store result bossbar skyblockmulti:infernal_trial max run scoreboard players get #wave_total sb_infernal
execute store result bossbar skyblockmulti:infernal_trial value run scoreboard players get #wave_total sb_infernal
bossbar set skyblockmulti:infernal_trial visible true
execute in minecraft:the_nether positioned 0 64 0 run particle minecraft:flame 0 65 0 4 1 4 0.08 120 force
execute in minecraft:the_nether positioned 0 64 0 run playsound minecraft:entity.blaze.ambient master @a[distance=..64] ~ ~ ~ 1.5 0.7
function skyblockmulti:infernal_trial/announce_name
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] title {translate:"skyblockmulti.infernal_trial.combat_started",color:"gold",bold:true}
