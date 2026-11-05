# Never resolve a missing ACTIVE destination as the storage default (0, 0).
execute unless score @s sb_active_x = @s sb_active_x run return 0
execute unless score @s sb_active_z = @s sb_active_z run return 0
execute if score @s sb_active_x matches 0 if score @s sb_active_z matches 0 run return 0
scoreboard players set @s sb_home 0
execute store result storage skyblockmulti:runtime respawn.x int 1 run scoreboard players get @s sb_active_x
execute store result storage skyblockmulti:runtime respawn.z int 1 run scoreboard players get @s sb_active_z
function skyblockmulti:player/home_position with storage skyblockmulti:runtime respawn
