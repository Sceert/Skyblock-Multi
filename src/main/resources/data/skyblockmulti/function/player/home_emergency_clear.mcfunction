# Despeja solo los dos bloques sobre la bedrock, pero no cambia el punto de reaparición vanilla.
$execute in minecraft:overworld run setblock $(x) 65 $(z) minecraft:air destroy
$execute in minecraft:overworld run setblock $(x) 66 $(z) minecraft:air destroy
$execute in minecraft:overworld positioned $(x) 65 $(z) run teleport @s ~0.5 ~ ~0.5
execute in minecraft:overworld run effect give @s minecraft:resistance 3 4 true
execute in minecraft:overworld run effect give @s minecraft:slow_falling 3 0 true
