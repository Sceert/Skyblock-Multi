scoreboard players operation @s sb_infernal_claim = #run_id sb_infernal
# Lote base personal.
give @s minecraft:quartz 8
give @s minecraft:gold_ingot 2
give @s minecraft:blaze_rod 1
execute if score #trial_level sb_infernal matches 4.. run give @s minecraft:nether_wart 2
execute if score #trial_level sb_infernal matches 5.. run give @s minecraft:magma_cream 2
execute if score #trial_level sb_infernal matches 6.. run give @s minecraft:emerald 2
# Solo VII–X escalan enemigos y recompensas por dificultad.
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 2 run give @s minecraft:emerald 2
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 1 run give @s minecraft:emerald 5
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 0 run give @s minecraft:emerald 8
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 0..1 run give @s minecraft:gold_ingot 3
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 0 run give @s minecraft:blaze_rod 3
# Probabilidad baja de Ancient Debris en IV–VI: 5%, 10% y 20%.
scoreboard players set #debris_chance sb_infernal 0
execute if score #trial_level sb_infernal matches 4 run scoreboard players set #debris_chance sb_infernal 50
execute if score #trial_level sb_infernal matches 5 run scoreboard players set #debris_chance sb_infernal 100
execute if score #trial_level sb_infernal matches 6 run scoreboard players set #debris_chance sb_infernal 200
execute if score #trial_level sb_infernal matches 4..6 store result score @s sb_infernal_roll run random value 1..1000
execute if score #trial_level sb_infernal matches 4..6 if score @s sb_infernal_roll <= #debris_chance sb_infernal run give @s minecraft:ancient_debris 1
# VII–X garantizan una cantidad moderada según dificultad y nivel.
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 2..3 run give @s minecraft:ancient_debris 1
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 1 run give @s minecraft:ancient_debris 2
execute if score #trial_level sb_infernal matches 7..8 if score #trial_difficulty sb_infernal matches 0 run give @s minecraft:ancient_debris 2
execute if score #trial_level sb_infernal matches 9..10 if score #trial_difficulty sb_infernal matches 0 run give @s minecraft:ancient_debris 3
# Probabilidad personal de Tótem según nivel y juramento.
scoreboard players set #totem_chance sb_infernal 0
execute if score #trial_level sb_infernal matches 6 run scoreboard players set #totem_chance sb_infernal 10
execute if score #trial_level sb_infernal matches 7 run scoreboard players set #totem_chance sb_infernal 20
execute if score #trial_level sb_infernal matches 8 run scoreboard players set #totem_chance sb_infernal 50
execute if score #trial_level sb_infernal matches 9 run scoreboard players set #totem_chance sb_infernal 100
execute if score #trial_level sb_infernal matches 10 run scoreboard players set #totem_chance sb_infernal 200
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 2 run scoreboard players add #totem_chance sb_infernal 20
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 1 run scoreboard players add #totem_chance sb_infernal 80
execute if score #trial_level sb_infernal matches 7.. if score #trial_difficulty sb_infernal matches 0 run scoreboard players add #totem_chance sb_infernal 160
execute store result score @s sb_infernal_roll run random value 1..1000
execute if score @s sb_infernal_roll <= #totem_chance sb_infernal run give @s minecraft:totem_of_undying 1
# Catalizador de lava: 2% en I-II, 4% en III-IV, 7% en V-VI y 10% en VII-X.
scoreboard players set #catalyst_chance sb_infernal 2
execute if score #trial_level sb_infernal matches 3..4 run scoreboard players set #catalyst_chance sb_infernal 4
execute if score #trial_level sb_infernal matches 5..6 run scoreboard players set #catalyst_chance sb_infernal 7
execute if score #trial_level sb_infernal matches 7.. run scoreboard players set #catalyst_chance sb_infernal 10
execute store result score @s sb_infernal_roll run random value 1..100
execute if score @s sb_infernal_roll <= #catalyst_chance sb_infernal run give @s skyblockmulti:lava_catalyst 1
# Trofeos personales de estructuras infernales. Cada reliquia usa una tirada
# independiente y exige una prueba acorde a su procedencia/progresión.
# Rib: ~7% desde VI (fortaleza del Nether).
execute if score #trial_level sb_infernal matches 6.. store result score @s sb_infernal_roll run random value 1..1000
execute if score #trial_level sb_infernal matches 6.. if score @s sb_infernal_roll matches ..70 run give @s minecraft:rib_armor_trim_smithing_template 1
# Snout: ~8% desde VII (bastión).
execute if score #trial_level sb_infernal matches 7.. store result score @s sb_infernal_roll run random value 1..1000
execute if score #trial_level sb_infernal matches 7.. if score @s sb_infernal_roll matches ..80 run give @s minecraft:snout_armor_trim_smithing_template 1
# Netherite Upgrade: 10% desde VIII; desbloquea la progresión vanilla sin crear una receta alternativa.
execute if score #trial_level sb_infernal matches 8.. store result score @s sb_infernal_roll run random value 1..1000
execute if score #trial_level sb_infernal matches 8.. if score @s sb_infernal_roll matches ..100 run give @s minecraft:netherite_upgrade_smithing_template 1
# Pigstep: ~5% desde IX, el trofeo cosmético más raro del conjunto.
execute if score #trial_level sb_infernal matches 9.. store result score @s sb_infernal_roll run random value 1..1000
execute if score #trial_level sb_infernal matches 9.. if score @s sb_infernal_roll matches ..50 run give @s minecraft:music_disc_pigstep 1
tellraw @s {translate:"skyblockmulti.infernal_trial.reward_claimed",color:"aqua"}
playsound minecraft:ui.toast.challenge_complete master @s ~ ~ ~ 0.8 1.2
