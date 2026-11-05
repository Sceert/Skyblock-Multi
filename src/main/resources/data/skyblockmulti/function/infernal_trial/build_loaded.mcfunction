# Esperar hasta que el centro del Nether esté realmente cargado.
execute in minecraft:the_nether if loaded 0 64 0 run function skyblockmulti:infernal_trial/build_structure
execute unless score #arena_built sb_infernal matches 1 run schedule function skyblockmulti:infernal_trial/build_loaded 20t replace
