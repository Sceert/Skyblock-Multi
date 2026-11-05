
# Construir una isla circular de radio 4 con superficie en Y=64.
# El centro conserva una bedrock visible y otra oculta para respawn y control de ocupación.
fill ~-8 ~-6 ~-8 ~8 ~10 ~8 minecraft:air
# Superficie circular de radio 4 (57 bloques).
fill ~-1 ~0 ~-4 ~1 ~0 ~-4 minecraft:grass_block
fill ~-2 ~0 ~-3 ~2 ~0 ~-3 minecraft:grass_block
fill ~-3 ~0 ~-2 ~3 ~0 ~-2 minecraft:grass_block
fill ~-4 ~0 ~-1 ~4 ~0 ~1 minecraft:grass_block
fill ~-3 ~0 ~2 ~3 ~0 ~2 minecraft:grass_block
fill ~-2 ~0 ~3 ~2 ~0 ~3 minecraft:grass_block
fill ~-1 ~0 ~4 ~1 ~0 ~4 minecraft:grass_block
# Segunda capa de tierra.
fill ~-1 ~-1 ~-4 ~1 ~-1 ~-4 minecraft:dirt
fill ~-2 ~-1 ~-3 ~2 ~-1 ~-3 minecraft:dirt
fill ~-3 ~-1 ~-2 ~3 ~-1 ~-2 minecraft:dirt
fill ~-4 ~-1 ~-1 ~4 ~-1 ~1 minecraft:dirt
fill ~-3 ~-1 ~2 ~3 ~-1 ~2 minecraft:dirt
fill ~-2 ~-1 ~3 ~2 ~-1 ~3 minecraft:dirt
fill ~-1 ~-1 ~4 ~1 ~-1 ~4 minecraft:dirt
# Capas inferiores cónicas.
fill ~-1 ~-2 ~-3 ~1 ~-2 ~-3 minecraft:dirt
fill ~-2 ~-2 ~-2 ~2 ~-2 ~-2 minecraft:dirt
fill ~-3 ~-2 ~-1 ~3 ~-2 ~1 minecraft:dirt
fill ~-2 ~-2 ~2 ~2 ~-2 ~2 minecraft:dirt
fill ~-1 ~-2 ~3 ~1 ~-2 ~3 minecraft:dirt
fill ~-1 ~-3 ~-2 ~1 ~-3 ~-2 minecraft:stone
fill ~-2 ~-3 ~-1 ~2 ~-3 ~1 minecraft:stone
fill ~-1 ~-3 ~2 ~1 ~-3 ~2 minecraft:stone
fill ~-1 ~-4 ~-1 ~1 ~-4 ~1 minecraft:stone
setblock ~0 ~-5 ~0 minecraft:stone

# Ancla permanente de respawn y marcador oculto de isla asignada.
setblock ~0 ~0 ~0 minecraft:bedrock
setblock ~0 ~-1 ~0 minecraft:bedrock
