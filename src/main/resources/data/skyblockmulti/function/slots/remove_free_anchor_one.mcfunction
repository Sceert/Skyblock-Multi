# Elimina únicamente una ancla libre. Nunca toca una isla ya construida (Y=63 bedrock).
$execute in minecraft:overworld if block $(x) 64 $(z) minecraft:bedrock unless block $(x) 63 $(z) minecraft:bedrock run setblock $(x) 64 $(z) minecraft:air
