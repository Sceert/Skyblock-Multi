# /summon no ejecuta toda la inicialización del spawn natural. Aplicamos aquí
# el equipo vanilla esperado de forma explícita para cada oleada.
execute as @e[type=minecraft:wither_skeleton,tag=skyblock_infernal_enemy] run item replace entity @s weapon.mainhand with minecraft:stone_sword
execute as @e[type=minecraft:piglin,tag=skyblock_infernal_enemy] run item replace entity @s weapon.mainhand with minecraft:golden_sword
execute as @e[type=minecraft:piglin,tag=skyblock_infernal_enemy] if predicate skyblockmulti:infernal_trial/ranged_piglin run item replace entity @s weapon.mainhand with minecraft:crossbow
execute as @e[type=minecraft:piglin,tag=skyblock_infernal_enemy] if predicate skyblockmulti:infernal_trial/gold_armor run item replace entity @s armor.head with minecraft:golden_helmet
execute as @e[type=minecraft:piglin,tag=skyblock_infernal_enemy] if predicate skyblockmulti:infernal_trial/gold_armor run item replace entity @s armor.chest with minecraft:golden_chestplate
execute as @e[type=minecraft:piglin_brute,tag=skyblock_infernal_enemy] run item replace entity @s weapon.mainhand with minecraft:golden_axe
