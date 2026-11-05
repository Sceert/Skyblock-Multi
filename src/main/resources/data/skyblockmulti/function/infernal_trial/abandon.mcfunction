execute in minecraft:the_nether run tag @e[tag=skyblock_infernal_enemy] add skyblockmulti_cleanup
execute in minecraft:the_nether positioned 0 64 0 run tag @e[type=minecraft:magma_cube,distance=..64] add skyblockmulti_cleanup
execute in minecraft:the_nether positioned 0 64 0 run tag @e[type=minecraft:slime,distance=..64] add skyblockmulti_cleanup
execute in minecraft:the_nether run kill @e[tag=skyblock_infernal_enemy]
execute in minecraft:the_nether positioned 0 64 0 run kill @e[type=minecraft:slime,distance=..64]
execute in minecraft:the_nether positioned 0 64 0 run kill @e[type=minecraft:magma_cube,distance=..64]
scoreboard players set #magma_cleanup_passes sb_infernal 6
schedule function skyblockmulti:infernal_trial/cleanup_magma_cubes 1t replace
scoreboard players set #trial_active sb_infernal 0
scoreboard players set #trial_state sb_infernal 0
scoreboard players set #spawn_lock sb_infernal 0
scoreboard players set #cooldown sb_infernal 2400
scoreboard players set #abandon sb_infernal 0
bossbar set skyblockmulti:infernal_trial visible false
bossbar set skyblockmulti:infernal_trial players
execute in minecraft:the_nether positioned 0 64 0 run tellraw @a {translate:"skyblockmulti.infernal_trial.abandoned",color:"dark_gray",italic:true}
execute in minecraft:the_nether run setblock 0 65 0 minecraft:soul_fire
execute in minecraft:the_nether unless entity @e[type=minecraft:text_display,tag=skyblock_infernal_label] run summon minecraft:text_display 0.5 66.5 0.5 {Tags:["skyblock_infernal_label"],billboard:"center",text:{translate:"skyblockmulti.infernal_trial.altar",color:"gold",bold:true},background:0,shadow:true}
