scoreboard players set #cooldown sb_infernal 0
scoreboard players set #cooldown_display_tick sb_infernal 0
execute in minecraft:the_nether run setblock 0 65 0 minecraft:fire
execute in minecraft:the_nether positioned 0 64 0 run particle minecraft:soul_fire_flame 0 65 0 1.5 0.6 1.5 0.02 35 force @a[distance=..48]
execute in minecraft:the_nether positioned 0 64 0 run playsound minecraft:block.beacon.activate master @a[distance=..48] ~ ~ ~ 0.8 0.8
execute in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..48] {translate:"skyblockmulti.infernal_trial.ready",color:"aqua"}
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..48] actionbar {translate:"skyblockmulti.infernal_trial.ready",color:"aqua",bold:true}
execute in minecraft:the_nether unless entity @e[type=minecraft:text_display,tag=skyblock_infernal_label] run summon minecraft:text_display 0.5 66.5 0.5 {Tags:["skyblock_infernal_label"],billboard:"center",text:{translate:"skyblockmulti.infernal_trial.altar",color:"gold",bold:true},background:0,shadow:true}
