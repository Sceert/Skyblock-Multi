# Macro: la bedrock oculta en Y=63 distingue una isla asignada de una ancla libre.
$execute in minecraft:overworld if block $(x) 64 $(z) minecraft:bedrock unless block $(x) 63 $(z) minecraft:bedrock run scoreboard players set #$(key) sb3_used 0
$execute in minecraft:overworld if block $(x) 64 $(z) minecraft:bedrock if block $(x) 63 $(z) minecraft:bedrock run scoreboard players set #$(key) sb3_used 1
$execute in minecraft:overworld unless block $(x) 64 $(z) minecraft:bedrock run scoreboard players set #$(key) sb3_used 1
$execute if score #$(key) sb3_used matches 1 in minecraft:overworld run forceload remove $(x) $(z)
