# Recuperación posterior al respawn, incluso si Minecraft recurrió temporalmente al spawn mundial.
execute store result storage skyblockmulti:runtime respawn.x int 1 run scoreboard players get @s sb_active_x
execute store result storage skyblockmulti:runtime respawn.z int 1 run scoreboard players get @s sb_active_z
function skyblockmulti:player/respawn_restore_position with storage skyblockmulti:runtime respawn
scoreboard players set @s sb_respawn_delay 0
tag @s remove skyblock_respawn_pending
tag @s add skyblock_respawn_v2
tag @s add skyblock_respawn_handled
