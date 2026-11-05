# Restaura siempre al jugador en su isla.
# Mantiene la bedrock central visible como marcador permanente del respawn.
$execute in minecraft:overworld run setblock $(x) 63 $(z) minecraft:bedrock
$execute in minecraft:overworld run setblock $(x) 64 $(z) minecraft:bedrock
$execute in minecraft:overworld run spawnpoint @s $(x) 65 $(z)
scoreboard players set #respawn_safe sb3_const 0
function skyblockmulti:player/respawn_find_safe with storage skyblockmulti:runtime respawn
execute if score #respawn_safe sb3_const matches 0 run function skyblockmulti:player/respawn_emergency_clear with storage skyblockmulti:runtime respawn
