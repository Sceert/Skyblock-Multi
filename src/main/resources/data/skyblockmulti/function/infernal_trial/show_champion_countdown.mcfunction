scoreboard players operation #display_seconds sb_infernal = #champion_countdown sb_infernal
scoreboard players operation #display_seconds sb_infernal /= #twenty sb_infernal
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] title {score:{name:"#display_seconds",objective:"sb_infernal"},color:"dark_red",bold:true}
execute in minecraft:the_nether positioned 0 68 0 run particle minecraft:soul_fire_flame ~ ~ ~ 1.5 2 1.5 0.03 30 force
