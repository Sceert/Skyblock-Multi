# Reserva el centro como ancla permanente sin modificar los bloques superiores.
$execute in minecraft:overworld run setblock $(x) 63 $(z) minecraft:bedrock
$execute in minecraft:overworld run setblock $(x) 64 $(z) minecraft:bedrock
$execute in minecraft:overworld run spawnpoint @s $(x) 65 $(z)
