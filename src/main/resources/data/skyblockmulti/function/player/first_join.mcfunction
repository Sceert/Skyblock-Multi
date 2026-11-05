tag @s add skyblock_registered_v1
tag @s remove skyblock_party_guest
tag @s remove skyblock_party_reentry
scoreboard players set @s sb3_state 1
scoreboard players set @s sb3_slot 0
scoreboard players set @s sb3_x 0
scoreboard players set @s sb3_z 0
scoreboard players reset @s sb_active_x
scoreboard players reset @s sb_active_z
scoreboard players set @s sb_tree 0
scoreboard players set @s sb_difficulty 0
scoreboard players set @s sb_chest -1
scoreboard players set @s sb_menu 0
scoreboard players set @s sb_home 0
scoreboard players set @s sb_info 0
scoreboard players add @s sb_deaths 0
scoreboard players add @s sb_since_death 0
scoreboard players set @s sb_choice_time 0
scoreboard players operation @s sb_last_deaths = @s sb_deaths
scoreboard players set @s sb_respawn_delay 0
tag @s remove skyblock_respawn_pending
tag @s remove skyblock_respawn_v2
tag @s remove skyblock_respawn_handled
tag @s remove skyblock_menu_shown_v1

# Llegada al Último Refugio, dentro de The Ascension Nexus.
execute in minecraft:overworld run teleport @s 0.5 161 0.5
execute in minecraft:overworld run spawnpoint @s 0 161 0
execute unless entity @s[tag=skyblock_lore_received] run function skyblockmulti:player/give_lore_book
tellraw @s [{"text":"[Skyblock] ","color":"aqua","bold":true},{"translate":"skyblockmulti.welcome","color":"yellow"}]
