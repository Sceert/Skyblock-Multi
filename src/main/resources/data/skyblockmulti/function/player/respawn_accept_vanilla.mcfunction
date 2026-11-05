# La cama o ancla de reaparición funcionó; no se cambia ni teletransporta al jugador.
scoreboard players operation @s sb_last_deaths = @s sb_deaths
scoreboard players set @s sb_respawn_delay 0
tag @s remove skyblock_respawn_pending
tag @s add skyblock_respawn_handled
