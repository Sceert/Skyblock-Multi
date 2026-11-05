# Migra contadores sin alterar una cama o ancla de reaparición ya asignada.
scoreboard players add @s sb_deaths 0
scoreboard players operation @s sb_last_deaths = @s sb_deaths
scoreboard players set @s sb_respawn_delay 0
tag @s remove skyblock_respawn_pending
tag @s add skyblock_respawn_v2
tag @s add skyblock_respawn_handled
