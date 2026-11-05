tellraw @s {"translate":"skyblockmulti.admin.diagnose.title","color":"aqua","bold":true}
execute if score #slots_built sb3_const matches 1 run tellraw @s {"translate":"skyblockmulti.admin.diagnose.anchors_ready","color":"green"}
execute unless score #slots_built sb3_const matches 1 run tellraw @s {"translate":"skyblockmulti.admin.diagnose.anchors_preparing","color":"yellow"}
function skyblockmulti:slots/sync_occupancy
scoreboard players set #used_count sb3_const 0
execute if score #01 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #02 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #03 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #04 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #05 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #06 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #07 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #08 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #09 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #10 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #11 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #12 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #13 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #14 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #15 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #16 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #17 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #18 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #19 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #20 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #21 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #22 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #23 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
execute if score #24 sb3_used matches 1 run scoreboard players add #used_count sb3_const 1
scoreboard players operation #free_count sb3_const = #max sb3_const
scoreboard players operation #free_count sb3_const -= #used_count sb3_const
tellraw @s {"translate":"skyblockmulti.admin.diagnose.occupancy","with":[{"score":{"name":"#used_count","objective":"sb3_const"}},{"score":{"name":"#free_count","objective":"sb3_const"}}],"color":"gray"}
tellraw @s {"translate":"skyblockmulti.admin.diagnose.player","with":[{"score":{"name":"@s","objective":"sb3_state"}},{"score":{"name":"@s","objective":"sb3_slot"}}],"color":"gray"}
tellraw @s {"translate":"skyblockmulti.admin.diagnose.distance","with":[{"score":{"name":"#distance","objective":"sb3_const"}}],"color":"gray"}
