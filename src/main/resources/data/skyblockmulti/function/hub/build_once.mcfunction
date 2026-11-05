# ============================================================
# THE ASCENSION NEXUS - WORLD FOUNDATION v20
# Centro jugable HUB: Y=160 / spawn Y=161
# Fortaleza: Y=12..36
# ============================================================

# Mantener cargado el centro durante la construcción.
execute in minecraft:overworld run forceload add -3 -3 3 3

# Limpiar únicamente restos del HUB técnico anterior.
execute in minecraft:overworld run fill -12 90 -12 12 110 12 minecraft:air
execute in minecraft:overworld run fill -8 -64 0 8 -50 18 minecraft:air

# El HUB diseñado por el jugador se coloca como una estructura nativa.
function skyblockmulti:hub/build_structure

# La fortaleza sigue siendo un módulo independiente bajo el HUB.
function skyblockmulti:hub/build_fortress

# Spawn definitivo del Último Refugio.
execute in minecraft:overworld run setworldspawn 0 161 0
execute in minecraft:overworld positioned 0.5 161 0.5 run summon minecraft:marker ~ ~-1 ~ {Tags:["skyblock_nexus_built_v20"]}

# Bloqueo persistente: ninguna carga, reinicio o cambio de dimensión vuelve a colocar la estructura.
scoreboard players set #nexus_built sb3_const 1
scoreboard players set #nexus_functional sb3_const 1

# El centro técnico permanece cargado (3x3 chunks) para lógica futura.
execute in minecraft:overworld run forceload add -1 -1 1 1
