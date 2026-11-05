scoreboard players set @s sb_info 0
tellraw @s {"translate":"skyblockmulti.info.title","color":"aqua","bold":true}
tellraw @s {"translate":"skyblockmulti.info.state","with":[{"score":{"name":"@s","objective":"sb3_state"}},{"score":{"name":"@s","objective":"sb3_slot"}}],"color":"gray"}
tellraw @s {"translate":"skyblockmulti.info.center","with":[{"score":{"name":"@s","objective":"sb3_x"}},{"score":{"name":"@s","objective":"sb3_z"}}],"color":"gray"}
tellraw @s {"translate":"skyblockmulti.info.distance","with":[{"score":{"name":"#distance","objective":"sb3_const"}}],"color":"gray"}
tellraw @s {"translate":"skyblockmulti.info.capacity","with":[{"score":{"name":"#capacity","objective":"sb3_cfg"}},{"score":{"name":"#enabled_count","objective":"sb3_cfg"}}],"color":"gray"}
tellraw @s {"translate":"skyblockmulti.info.commands","color":"gray"}
