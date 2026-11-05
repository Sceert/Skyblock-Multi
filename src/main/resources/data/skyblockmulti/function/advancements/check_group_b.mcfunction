# Combate, Nether, End y secretos; omite avances ya completados.
execute unless entity @s[advancements={skyblockmulti:combat/bones=true}] if items entity @s inventory.* minecraft:bone run advancement grant @s only skyblockmulti:combat/bones
execute unless entity @s[advancements={skyblockmulti:combat/gunpowder=true}] if items entity @s inventory.* minecraft:gunpowder run advancement grant @s only skyblockmulti:combat/gunpowder
execute unless entity @s[advancements={skyblockmulti:combat/ender_pearl=true}] if items entity @s inventory.* minecraft:ender_pearl run advancement grant @s only skyblockmulti:combat/ender_pearl
execute unless entity @s[advancements={skyblockmulti:nether/blaze_rod=true}] if items entity @s inventory.* minecraft:blaze_rod run advancement grant @s only skyblockmulti:nether/blaze_rod
execute unless entity @s[advancements={skyblockmulti:nether/brewing=true}] if items entity @s inventory.* minecraft:brewing_stand run advancement grant @s only skyblockmulti:nether/brewing
execute unless entity @s[advancements={skyblockmulti:end/elytra=true}] if items entity @s inventory.* minecraft:elytra run advancement grant @s only skyblockmulti:end/elytra
execute unless entity @s[advancements={skyblockmulti:end/shulker=true}] if items entity @s inventory.* minecraft:shulker_box run advancement grant @s only skyblockmulti:end/shulker
execute unless entity @s[advancements={skyblockmulti:secret/obsidian_accident=true}] if items entity @s inventory.* minecraft:obsidian run advancement grant @s only skyblockmulti:secret/obsidian_accident
execute unless entity @s[advancements={skyblockmulti:secret/five_deaths=true}] if score @s sb_deaths matches 5.. run advancement grant @s only skyblockmulti:secret/five_deaths
execute unless entity @s[advancements={skyblockmulti:secret/indecision=true}] if score @s sb_choice_time matches 6000.. run advancement grant @s only skyblockmulti:secret/indecision
execute unless entity @s[advancements={skyblockmulti:nether/quartz=true}] if items entity @s inventory.* minecraft:quartz run advancement grant @s only skyblockmulti:nether/quartz
execute unless entity @s[advancements={skyblockmulti:nether/nether_wart=true}] if items entity @s inventory.* minecraft:nether_wart run advancement grant @s only skyblockmulti:nether/nether_wart
execute unless entity @s[advancements={skyblockmulti:nether/potion=true}] if items entity @s inventory.* minecraft:potion run advancement grant @s only skyblockmulti:nether/potion
execute unless entity @s[advancements={skyblockmulti:nether/soul_sand=true}] if items entity @s inventory.* minecraft:soul_sand run advancement grant @s only skyblockmulti:nether/soul_sand
execute unless entity @s[advancements={skyblockmulti:end/shulker_shell=true}] if items entity @s inventory.* minecraft:shulker_shell run advancement grant @s only skyblockmulti:end/shulker_shell
execute unless entity @s[advancements={skyblockmulti:end/dragon_breath=true}] if items entity @s inventory.* minecraft:dragon_breath run advancement grant @s only skyblockmulti:end/dragon_breath
execute unless entity @s[advancements={skyblockmulti:end/end_crystal=true}] if items entity @s inventory.* minecraft:end_crystal run advancement grant @s only skyblockmulti:end/end_crystal
execute unless entity @s[advancements={skyblockmulti:combat/string=true}] if items entity @s inventory.* minecraft:string run advancement grant @s only skyblockmulti:combat/string
execute unless entity @s[advancements={skyblockmulti:combat/rotten_flesh=true}] if items entity @s inventory.* minecraft:rotten_flesh run advancement grant @s only skyblockmulti:combat/rotten_flesh
execute unless entity @s[advancements={skyblockmulti:combat/spider_eye=true}] if items entity @s inventory.* minecraft:spider_eye run advancement grant @s only skyblockmulti:combat/spider_eye
execute unless entity @s[advancements={skyblockmulti:combat/slimeball=true}] if items entity @s inventory.* minecraft:slime_ball run advancement grant @s only skyblockmulti:combat/slimeball
execute unless entity @s[advancements={skyblockmulti:combat/arrow=true}] if items entity @s inventory.* minecraft:arrow run advancement grant @s only skyblockmulti:combat/arrow
execute unless entity @s[advancements={skyblockmulti:combat/blaze_powder=true}] if items entity @s inventory.* minecraft:blaze_powder run advancement grant @s only skyblockmulti:combat/blaze_powder
execute unless entity @s[advancements={skyblockmulti:combat/ghast_tear=true}] if items entity @s inventory.* minecraft:ghast_tear run advancement grant @s only skyblockmulti:combat/ghast_tear
execute unless entity @s[advancements={skyblockmulti:combat/redstone=true}] if items entity @s inventory.* minecraft:redstone run advancement grant @s only skyblockmulti:combat/redstone
execute unless entity @s[advancements={skyblockmulti:combat/basic_farm=true}] if items entity @s inventory.* minecraft:rotten_flesh if items entity @s inventory.* minecraft:bone if items entity @s inventory.* minecraft:string if items entity @s inventory.* minecraft:gunpowder run advancement grant @s only skyblockmulti:combat/basic_farm
execute unless entity @s[advancements={skyblockmulti:combat/advanced_loot=true}] if items entity @s inventory.* minecraft:spider_eye if items entity @s inventory.* minecraft:slime_ball if items entity @s inventory.* minecraft:redstone if items entity @s inventory.* minecraft:ender_pearl run advancement grant @s only skyblockmulti:combat/advanced_loot
execute unless entity @s[advancements={skyblockmulti:combat/nether_hunter=true}] if items entity @s inventory.* minecraft:blaze_rod if items entity @s inventory.* minecraft:blaze_powder if items entity @s inventory.* minecraft:ghast_tear run advancement grant @s only skyblockmulti:combat/nether_hunter
