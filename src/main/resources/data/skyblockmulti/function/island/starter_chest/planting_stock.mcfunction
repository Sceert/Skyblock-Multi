# Solo los árboles cuya propagación inicial no queda razonablemente asegurada
# por una única planta reciben este conjunto excepcional. El último espacio lo
# mantiene separado del contenido que distingue las dificultades.
execute if score @s sb_tree matches 7 run item replace block ~2 ~1 ~0 container.26 with minecraft:mangrove_propagule 1
execute if score @s sb_tree matches 8 run item replace block ~2 ~1 ~0 container.26 with minecraft:dark_oak_sapling 4
execute if score @s sb_tree matches 9 run item replace block ~2 ~1 ~0 container.26 with minecraft:pale_oak_sapling 4
