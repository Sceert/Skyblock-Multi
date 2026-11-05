# Elige uniformemente una especie entre las habilitadas mediante muestreo con rechazo.
scoreboard players set #random_allowed sb3_const 0
execute store result score @s sb_tree run random value 1..11
execute if score @s sb_tree matches 1 if score #tree_oak sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 2 if score #tree_spruce sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 3 if score #tree_birch sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 4 if score #tree_jungle sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 5 if score #tree_acacia sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 6 if score #tree_cherry sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 7 if score #tree_mangrove sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 8 if score #tree_dark_oak sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 9 if score #tree_pale_oak sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 10 if score #tree_azalea sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score @s sb_tree matches 11 if score #tree_flowering_azalea sb3_cfg matches 1 run scoreboard players set #random_allowed sb3_const 1
execute if score #random_allowed sb3_const matches 0 run function skyblockmulti:player/random_tree
