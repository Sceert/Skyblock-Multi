# Preparar los chunks del Coliseo Infernal antes de colocar bloques.
scoreboard players set #arena_build_pending sb_infernal 1
execute in minecraft:the_nether run forceload add -32 -32 32 32
schedule function skyblockmulti:infernal_trial/build_loaded 40t replace
