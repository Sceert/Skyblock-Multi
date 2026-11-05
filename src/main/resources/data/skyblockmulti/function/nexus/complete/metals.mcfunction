data remove block -18 16 18 Items[{id:"skyblockmulti:offering_metals"}]
setblock -18 16 18 minecraft:waxed_copper_grate
scoreboard players set #seal_metals sb_nexus 1
particle minecraft:happy_villager -17.5 17 18.5 0.8 1.0 0.8 0.1 40 force
playsound minecraft:block.beacon.activate master @a -18 16 18 0.8 1.4
