# Ejecutado como el Núcleo Infernal arrojado sobre el altar.
kill @s
scoreboard players set #trial_active sb_infernal 1
scoreboard players set #trial_state sb_infernal 1
scoreboard players set #countdown sb_infernal 200
scoreboard players set #abandon sb_infernal 0
scoreboard players set #champion_spawned sb_infernal 0
scoreboard players set #phase sb_infernal 0
scoreboard players set #spawn_lock sb_infernal 0
scoreboard players add #run_id sb_infernal 1
execute unless score #run_id sb_infernal matches 1.. run scoreboard players set #run_id sb_infernal 1
execute in minecraft:the_nether run kill @e[type=minecraft:text_display,tag=skyblock_infernal_label]
execute in minecraft:the_nether run tag @e[tag=skyblock_infernal_enemy] add skyblockmulti_cleanup
execute in minecraft:the_nether run kill @e[tag=skyblock_infernal_enemy]
execute in minecraft:the_nether run setblock 0 64 5 minecraft:air
tag @a remove skyblock_infernal_activator
execute in minecraft:the_nether positioned 0 64 0 run tag @p[distance=..8,limit=1,sort=nearest] add skyblock_infernal_activator
scoreboard players set #trial_difficulty sb_infernal 2
execute as @a[tag=skyblock_infernal_activator,limit=1] run scoreboard players operation #trial_difficulty sb_infernal = @s sb_chest
# Java captures eligible participants and assigns their run IDs once.
execute in minecraft:the_nether positioned 0 64 0 store result score #participant_count sb_infernal run execute if entity @a[distance=..64]
execute if score #trial_level sb_infernal matches 6 run function skyblockmulti:infernal_trial/select_secret_level
execute in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] [{translate:"skyblockmulti.infernal_trial.countdown_started",color:"gold"},{text:" "},{score:{name:"#trial_level",objective:"sb_infernal"},color:"red",bold:true}]
execute in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] {translate:"skyblockmulti.infernal_trial.cleanup_warning",color:"yellow",italic:true}
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] times 0 25 0
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] subtitle {translate:"skyblockmulti.infernal_trial.reposition",color:"gold"}
execute in minecraft:the_nether positioned 0 64 0 run playsound minecraft:block.trial_spawner.ominous_activate master @a[distance=..64] ~ ~ ~ 1.2 0.8
