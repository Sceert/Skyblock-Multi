# Respeta cama o ancla vanilla. Solo recupera la isla si Minecraft envió al jugador al spawn mundial.
execute in minecraft:overworld positioned 0.5 101 0.5 if entity @s[distance=..96] run function skyblockmulti:player/respawn_restore
execute in minecraft:overworld positioned 0.5 101 0.5 unless entity @s[distance=..96] run function skyblockmulti:player/respawn_accept_vanilla
