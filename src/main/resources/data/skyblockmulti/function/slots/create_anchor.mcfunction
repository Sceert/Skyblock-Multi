# Macro: crea anclas libres y migra islas antiguas al centro de bedrock permanente.
# La presencia de superficie adyacente identifica una isla ya construida.
$execute in minecraft:overworld positioned $(x) 64 $(z) if block ~1 ~ ~ minecraft:grass_block unless block ~ ~ ~ minecraft:bedrock run setblock ~ ~ ~ minecraft:bedrock
$execute in minecraft:overworld positioned $(x) 64 $(z) if block ~ ~ ~ minecraft:bedrock if block ~1 ~ ~ minecraft:grass_block unless block ~ ~-1 ~ minecraft:bedrock run setblock ~ ~-1 ~ minecraft:bedrock
# Una posición totalmente vacía recibe solamente la ancla superior.
$execute in minecraft:overworld positioned $(x) 64 $(z) if block ~ ~ ~ minecraft:air if block ~1 ~ ~ minecraft:air run setblock ~ ~ ~ minecraft:bedrock
# Clasificar y contar la posición.
$execute in minecraft:overworld if block $(x) 64 $(z) minecraft:bedrock if block $(x) 63 $(z) minecraft:bedrock run scoreboard players set #$(key) sb3_used 1
$execute in minecraft:overworld if block $(x) 64 $(z) minecraft:bedrock unless block $(x) 63 $(z) minecraft:bedrock run scoreboard players set #$(key) sb3_used 0
$execute in minecraft:overworld if block $(x) 64 $(z) minecraft:bedrock run scoreboard players add #anchors sb3_const 1
$execute in minecraft:overworld unless block $(x) 64 $(z) minecraft:bedrock run scoreboard players set #$(key) sb3_used 1
