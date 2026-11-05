data remove block 18 16 -18 Items[{id:"skyblockmulti:offering_earth"}]
setblock 18 16 -18 minecraft:waxed_copper_grate
scoreboard players set #seal_earth sb_nexus 1
particle minecraft:happy_villager 18.5 17 -17.5 0.8 1.0 0.8 0.1 40 force
playsound minecraft:block.beacon.activate master @a 18 16 -18 0.8 1.2
