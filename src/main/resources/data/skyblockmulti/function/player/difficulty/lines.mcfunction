# Seleccionar el límite correcto según el origen del ciclo.
execute if entity @s[tag=skyblock_party_reentry] run function skyblockmulti:player/difficulty/lines_party_leave
execute unless entity @s[tag=skyblock_party_reentry] run function skyblockmulti:player/difficulty/lines_global
