scoreboard players set #trial_state sb_infernal 2
scoreboard players set #champion_spawned sb_infernal 1
scoreboard players set #phase sb_infernal 3
execute in minecraft:the_nether run summon minecraft:piglin_brute 0 76 -18 {Tags:["skyblock_infernal_enemy","skyblock_infernal_champion"],PersistenceRequired:1b,IsImmuneToZombification:1b}
function skyblockmulti:infernal_trial/equip_enemies
execute in minecraft:the_nether as @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] run data merge entity @s {CustomName:{translate:"skyblockmulti.infernal_trial.champion_name",color:"dark_red",bold:true},CustomNameVisible:1b}
execute in minecraft:the_nether run attribute @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] minecraft:scale base set 1.55
execute in minecraft:the_nether run attribute @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] minecraft:max_health base set 160
execute in minecraft:the_nether run data merge entity @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] {Health:160.0f}
execute in minecraft:the_nether run item replace entity @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] weapon.mainhand with minecraft:netherite_axe
execute in minecraft:the_nether run item replace entity @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] armor.head with minecraft:netherite_helmet
execute in minecraft:the_nether run effect give @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] minecraft:resistance 4 4 true
execute in minecraft:the_nether run effect give @e[type=minecraft:piglin_brute,tag=skyblock_infernal_champion,limit=1] minecraft:slow_falling 4 0 true
execute in minecraft:the_nether positioned 0 76 -18 run particle minecraft:soul_fire_flame ~ ~ ~ 2 2 2 0.08 160 force
execute in minecraft:the_nether positioned 0 64 0 run playsound minecraft:entity.piglin_brute.angry master @a[distance=..64] ~ ~ ~ 2.0 0.6
execute in minecraft:the_nether store result score #wave_total sb_infernal run execute if entity @e[tag=skyblock_infernal_enemy]
execute store result bossbar skyblockmulti:infernal_trial max run scoreboard players get #wave_total sb_infernal
function skyblockmulti:infernal_trial/announce_phase_3
