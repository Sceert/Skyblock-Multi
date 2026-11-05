# Reinicio completo para un miembro de party que nunca tuvo una isla personal.
# Se considera un nuevo ciclo de jugador, pero conserva sus advancements históricos.

# Inventario normal, armadura y manos.
clear @s
item replace entity @s weapon.offhand with minecraft:air
item replace entity @s armor.head with minecraft:air
item replace entity @s armor.chest with minecraft:air
item replace entity @s armor.legs with minecraft:air
item replace entity @s armor.feet with minecraft:air
# Cursor y crafting personal.
item replace entity @s player.cursor with minecraft:air
item replace entity @s player.crafting.0 with minecraft:air
item replace entity @s player.crafting.1 with minecraft:air
item replace entity @s player.crafting.2 with minecraft:air
item replace entity @s player.crafting.3 with minecraft:air
# Ender Chest completo.
item replace entity @s enderchest.0 with minecraft:air
item replace entity @s enderchest.1 with minecraft:air
item replace entity @s enderchest.2 with minecraft:air
item replace entity @s enderchest.3 with minecraft:air
item replace entity @s enderchest.4 with minecraft:air
item replace entity @s enderchest.5 with minecraft:air
item replace entity @s enderchest.6 with minecraft:air
item replace entity @s enderchest.7 with minecraft:air
item replace entity @s enderchest.8 with minecraft:air
item replace entity @s enderchest.9 with minecraft:air
item replace entity @s enderchest.10 with minecraft:air
item replace entity @s enderchest.11 with minecraft:air
item replace entity @s enderchest.12 with minecraft:air
item replace entity @s enderchest.13 with minecraft:air
item replace entity @s enderchest.14 with minecraft:air
item replace entity @s enderchest.15 with minecraft:air
item replace entity @s enderchest.16 with minecraft:air
item replace entity @s enderchest.17 with minecraft:air
item replace entity @s enderchest.18 with minecraft:air
item replace entity @s enderchest.19 with minecraft:air
item replace entity @s enderchest.20 with minecraft:air
item replace entity @s enderchest.21 with minecraft:air
item replace entity @s enderchest.22 with minecraft:air
item replace entity @s enderchest.23 with minecraft:air
item replace entity @s enderchest.24 with minecraft:air
item replace entity @s enderchest.25 with minecraft:air
item replace entity @s enderchest.26 with minecraft:air

# Experiencia y efectos previos.
experience set @s 0 points
experience set @s 0 levels
effect clear @s

# Borrar cualquier asignación personal/activa anterior.
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
scoreboard players set @s sb_party_pref 0
scoreboard players set @s sb_choice_time 0
scoreboard players set @s sb_respawn_delay 0
scoreboard players add @s sb_deaths 0
scoreboard players operation @s sb_last_deaths = @s sb_deaths

tag @s remove skyblock_party_guest
tag @s add skyblock_party_reentry
tag @s remove skyblock_respawn_pending
tag @s remove skyblock_respawn_v2
tag @s remove skyblock_respawn_handled
tag @s remove skyblock_menu_shown_v1

# Única excepción que permite regresar al Último Refugio tras haberlo abandonado.
execute in minecraft:overworld run teleport @s 0.5 161 0.5
execute in minecraft:overworld run spawnpoint @s 0 161 0
tag @s remove skyblock_lore_received
function skyblockmulti:player/give_lore_book
tellraw @s [{"text":"[Skyblock Multi] ","color":"aqua","bold":true},{"translate":"skyblockmulti.party.reset_no_personal_island","color":"yellow"}]
function skyblockmulti:player/freeze_selection
