# Registra la muerte sin modificar el punto vanilla de cama o ancla de reaparición.
scoreboard players operation @s sb_last_deaths = @s sb_deaths
tag @s remove skyblock_respawn_handled
tag @s add skyblock_respawn_pending
