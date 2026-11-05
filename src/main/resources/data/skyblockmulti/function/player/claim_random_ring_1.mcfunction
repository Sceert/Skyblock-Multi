# Search active slots in configured ring 1.
execute store result score #random_slot sb3_const run random value 1..8
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..1 if data storage skyblockmulti:slots s01.x if score #01 sb3_used matches 0 run function skyblockmulti:slots/claim/01
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..2 if data storage skyblockmulti:slots s02.x if score #02 sb3_used matches 0 run function skyblockmulti:slots/claim/02
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..3 if data storage skyblockmulti:slots s03.x if score #03 sb3_used matches 0 run function skyblockmulti:slots/claim/03
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..4 if data storage skyblockmulti:slots s04.x if score #04 sb3_used matches 0 run function skyblockmulti:slots/claim/04
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..5 if data storage skyblockmulti:slots s05.x if score #05 sb3_used matches 0 run function skyblockmulti:slots/claim/05
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..6 if data storage skyblockmulti:slots s06.x if score #06 sb3_used matches 0 run function skyblockmulti:slots/claim/06
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..7 if data storage skyblockmulti:slots s07.x if score #07 sb3_used matches 0 run function skyblockmulti:slots/claim/07
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..8 if data storage skyblockmulti:slots s08.x if score #08 sb3_used matches 0 run function skyblockmulti:slots/claim/08
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 2.. if data storage skyblockmulti:slots s01.x if score #01 sb3_used matches 0 run function skyblockmulti:slots/claim/01
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 3.. if data storage skyblockmulti:slots s02.x if score #02 sb3_used matches 0 run function skyblockmulti:slots/claim/02
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 4.. if data storage skyblockmulti:slots s03.x if score #03 sb3_used matches 0 run function skyblockmulti:slots/claim/03
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 5.. if data storage skyblockmulti:slots s04.x if score #04 sb3_used matches 0 run function skyblockmulti:slots/claim/04
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 6.. if data storage skyblockmulti:slots s05.x if score #05 sb3_used matches 0 run function skyblockmulti:slots/claim/05
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 7.. if data storage skyblockmulti:slots s06.x if score #06 sb3_used matches 0 run function skyblockmulti:slots/claim/06
execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches 8.. if data storage skyblockmulti:slots s07.x if score #07 sb3_used matches 0 run function skyblockmulti:slots/claim/07
