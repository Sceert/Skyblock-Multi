execute store result score #starter_new sb3_const run skyblockmulti prepare_starter
execute unless score #starter_new sb3_const matches 1 run return 0
function skyblockmulti:island/starter_chest/contents
skyblockmulti initialize_starter
