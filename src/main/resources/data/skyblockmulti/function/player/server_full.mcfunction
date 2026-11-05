scoreboard players set @s sb3_state 1
scoreboard players set @s sb_tree 0
scoreboard players set @s sb_menu 0
tag @s remove skyblock_menu_shown_v1
tellraw @s [{"text":"[Skyblock] ","color":"red","bold":true},{"translate":"skyblockmulti.menu.server_full","with":[{"score":{"name":"#capacity","objective":"sb3_cfg"}}],"color":"yellow"}]
