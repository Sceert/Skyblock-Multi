scoreboard players set #trial_state sb_infernal 3
scoreboard players set #champion_countdown sb_infernal 160
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] times 0 25 0
function skyblockmulti:infernal_trial/show_next_phase
execute as @a if score @s sb_infernal_run = #run_id sb_infernal run function skyblockmulti:infernal_trial/reward/phase_bonus
execute if score #trial_level sb_infernal matches 7..9 in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] [{translate:"skyblockmulti.infernal_trial.phase_complete",color:"gold",bold:true},{text:" 1/2"}]
execute if score #trial_level sb_infernal matches 10 if score #phase sb_infernal matches 1 in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] [{translate:"skyblockmulti.infernal_trial.phase_complete",color:"gold",bold:true},{text:" 1/3"}]
execute if score #trial_level sb_infernal matches 10 if score #phase sb_infernal matches 2 in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] [{translate:"skyblockmulti.infernal_trial.phase_complete",color:"gold",bold:true},{text:" 2/3"}]
execute if score #trial_level sb_infernal matches 10 if score #phase sb_infernal matches 2 in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] {translate:"skyblockmulti.infernal_trial.champion_warning",color:"dark_red",bold:true}
execute in minecraft:the_nether positioned 0 64 0 run playsound minecraft:entity.warden.sonic_charge master @a[distance=..64] ~ ~ ~ 1.0 0.7
