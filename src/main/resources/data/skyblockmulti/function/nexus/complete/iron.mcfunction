# Preserve the completed armor on a Steve mannequin inside a glass reliquary.
summon minecraft:mannequin 0.5 16 25.5 {Tags:["skyblock_nexus_guardian_iron"],Invulnerable:1b,PersistenceRequired:1b,Silent:1b,immovable:1b,hide_description:1b,profile:{texture:"minecraft:entity/player/wide/steve",model:"wide"},Rotation:[0.0f,0.0f]}
item replace entity @e[type=minecraft:mannequin,tag=skyblock_nexus_guardian_iron,limit=1] armor.head from entity @s armor.head
item replace entity @e[type=minecraft:mannequin,tag=skyblock_nexus_guardian_iron,limit=1] armor.chest from entity @s armor.chest
item replace entity @e[type=minecraft:mannequin,tag=skyblock_nexus_guardian_iron,limit=1] armor.legs from entity @s armor.legs
item replace entity @e[type=minecraft:mannequin,tag=skyblock_nexus_guardian_iron,limit=1] armor.feet from entity @s armor.feet
item replace entity @s armor.head with minecraft:air
item replace entity @s armor.chest with minecraft:air
item replace entity @s armor.legs with minecraft:air
item replace entity @s armor.feet with minecraft:air
kill @s
fill -1 16 24 1 20 26 minecraft:glass_pane
fill 0 16 25 0 20 25 minecraft:air
scoreboard players set #seal_iron sb_nexus 1
particle minecraft:happy_villager 0.5 17 25.5 0.8 1.0 0.8 0.1 40 force
playsound minecraft:block.beacon.activate master @a 0 16 25 0.8 1.0
