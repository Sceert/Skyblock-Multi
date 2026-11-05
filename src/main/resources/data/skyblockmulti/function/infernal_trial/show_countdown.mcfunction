scoreboard players operation #display_seconds sb_infernal = #countdown sb_infernal
scoreboard players operation #display_seconds sb_infernal /= #twenty sb_infernal
execute in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] title {score:{name:"#display_seconds",objective:"sb_infernal"},color:"red",bold:true}
execute in minecraft:the_nether positioned 0 64 0 run playsound minecraft:block.note_block.hat master @a[distance=..64] ~ ~ ~ 0.8 1.0
