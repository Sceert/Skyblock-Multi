# El Núcleo VI puede despertar niveles secretos; la dificultad del activador altera la tirada.
scoreboard players set #trial_level sb_infernal 6
execute store result score #secret_roll sb_infernal run random value 1..1000
execute if score #trial_difficulty sb_infernal matches 0 if score #secret_roll sb_infernal matches 1..40 run scoreboard players set #trial_level sb_infernal 10
execute if score #trial_difficulty sb_infernal matches 0 if score #secret_roll sb_infernal matches 41..120 run scoreboard players set #trial_level sb_infernal 9
execute if score #trial_difficulty sb_infernal matches 0 if score #secret_roll sb_infernal matches 121..250 run scoreboard players set #trial_level sb_infernal 8
execute if score #trial_difficulty sb_infernal matches 0 if score #secret_roll sb_infernal matches 251..450 run scoreboard players set #trial_level sb_infernal 7
execute if score #trial_difficulty sb_infernal matches 1 if score #secret_roll sb_infernal matches 1..10 run scoreboard players set #trial_level sb_infernal 10
execute if score #trial_difficulty sb_infernal matches 1 if score #secret_roll sb_infernal matches 11..50 run scoreboard players set #trial_level sb_infernal 9
execute if score #trial_difficulty sb_infernal matches 1 if score #secret_roll sb_infernal matches 51..130 run scoreboard players set #trial_level sb_infernal 8
execute if score #trial_difficulty sb_infernal matches 1 if score #secret_roll sb_infernal matches 131..300 run scoreboard players set #trial_level sb_infernal 7
execute if score #trial_difficulty sb_infernal matches 2 if score #secret_roll sb_infernal matches 1..5 run scoreboard players set #trial_level sb_infernal 10
execute if score #trial_difficulty sb_infernal matches 2 if score #secret_roll sb_infernal matches 6..20 run scoreboard players set #trial_level sb_infernal 9
execute if score #trial_difficulty sb_infernal matches 2 if score #secret_roll sb_infernal matches 21..70 run scoreboard players set #trial_level sb_infernal 8
execute if score #trial_difficulty sb_infernal matches 2 if score #secret_roll sb_infernal matches 71..200 run scoreboard players set #trial_level sb_infernal 7
execute if score #trial_difficulty sb_infernal matches 3 if score #secret_roll sb_infernal matches 1..2 run scoreboard players set #trial_level sb_infernal 10
execute if score #trial_difficulty sb_infernal matches 3 if score #secret_roll sb_infernal matches 3..10 run scoreboard players set #trial_level sb_infernal 9
execute if score #trial_difficulty sb_infernal matches 3 if score #secret_roll sb_infernal matches 11..30 run scoreboard players set #trial_level sb_infernal 8
execute if score #trial_difficulty sb_infernal matches 3 if score #secret_roll sb_infernal matches 31..100 run scoreboard players set #trial_level sb_infernal 7
