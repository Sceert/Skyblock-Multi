# Selección inicial dentro del Último Refugio.
# El jugador puede caminar normalmente por el HUB mientras elige árbol/dificultad.
# La etiqueta se conserva solo como estado de flujo para compatibilidad.
tag @s add skyblock_selection_locked
# Protección temporal del jugador mientras el menú inicial permanece abierto.
# Se renueva únicamente en los estados de selección y desaparece al asignar la isla.
effect give @s minecraft:water_breathing 2 0 true
effect give @s minecraft:saturation 2 0 true
# Reingreso: conserva protección temporal sin impedir caminar durante la selección.
execute if entity @s[tag=skyblock_party_reentry] run effect give @s minecraft:resistance 2 255 true
