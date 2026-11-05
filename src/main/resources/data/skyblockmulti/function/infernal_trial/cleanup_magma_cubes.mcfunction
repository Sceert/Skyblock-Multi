# Solo repetir sobre la limpieza ya seleccionada; no capturar nuevos encuentros.
execute in minecraft:the_nether run kill @e[tag=skyblockmulti_cleanup]
scoreboard players remove #magma_cleanup_passes sb_infernal 1
execute if score #magma_cleanup_passes sb_infernal matches 1.. run schedule function skyblockmulti:infernal_trial/cleanup_magma_cubes 2t replace
