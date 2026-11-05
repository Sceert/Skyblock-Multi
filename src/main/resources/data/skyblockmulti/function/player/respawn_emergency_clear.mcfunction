# Garantía final de respawn.
# Solo despeja los dos bloques directamente sobre la bedrock central.
# "destroy" conserva los bloques y contenidos como drops en vez de borrarlos silenciosamente.
$execute in minecraft:overworld run setblock $(x) 65 $(z) minecraft:air destroy
$execute in minecraft:overworld run setblock $(x) 66 $(z) minecraft:air destroy
$execute in minecraft:overworld run spawnpoint @s $(x) 65 $(z)
$execute in minecraft:overworld positioned $(x) 65 $(z) run teleport @s ~0.5 ~ ~0.5
execute in minecraft:overworld run effect give @s minecraft:resistance 3 4 true
execute in minecraft:overworld run effect give @s minecraft:slow_falling 3 0 true
tellraw @s {"translate":"skyblockmulti.respawn.cleared","color":"yellow"}
