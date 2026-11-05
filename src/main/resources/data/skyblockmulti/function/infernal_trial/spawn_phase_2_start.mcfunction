scoreboard players set #trial_state sb_infernal 2
scoreboard players set #champion_spawned sb_infernal 0
scoreboard players set #phase sb_infernal 2
function skyblockmulti:infernal_trial/spawn_phase_2
function skyblockmulti:infernal_trial/equip_enemies
execute in minecraft:the_nether run effect give @e[tag=skyblock_infernal_enemy] minecraft:slow_falling 5 0 true
execute in minecraft:the_nether store result score #wave_total sb_infernal run execute if entity @e[tag=skyblock_infernal_enemy]
execute store result bossbar skyblockmulti:infernal_trial max run scoreboard players get #wave_total sb_infernal
function skyblockmulti:infernal_trial/announce_phase_2
