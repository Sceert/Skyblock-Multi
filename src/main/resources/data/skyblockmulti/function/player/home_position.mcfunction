# Teletransporte a casa sin alterar el punto de cama o ancla vanilla.
$execute in minecraft:overworld run setblock $(x) 63 $(z) minecraft:bedrock
$execute in minecraft:overworld run setblock $(x) 64 $(z) minecraft:bedrock
scoreboard players set #respawn_safe sb3_const 0
function skyblockmulti:player/respawn_find_safe with storage skyblockmulti:runtime respawn
execute if score #respawn_safe sb3_const matches 0 run function skyblockmulti:player/home_emergency_clear with storage skyblockmulti:runtime respawn
