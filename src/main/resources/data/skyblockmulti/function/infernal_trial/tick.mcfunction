# Recarga y avisos.
execute if score #cooldown_display_tick sb_infernal matches 0 if score #cooldown sb_infernal matches 1.. in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] actionbar {translate:"skyblockmulti.infernal_trial.cooldown",with:[{score:{name:"#cooldown_seconds",objective:"sb_infernal"}}],color:"gold"}
execute if score #cooldown sb_infernal matches 1 run function skyblockmulti:infernal_trial/ready
execute if score #cooldown sb_infernal matches 2.. run scoreboard players remove #cooldown sb_infernal 1
scoreboard players add #cooldown_display_tick sb_infernal 1
execute if score #cooldown_display_tick sb_infernal matches 20.. run scoreboard players set #cooldown_display_tick sb_infernal 0
execute if score #cooldown_display_tick sb_infernal matches 0 if score #cooldown sb_infernal matches 1.. run scoreboard players operation #cooldown_seconds sb_infernal = #cooldown sb_infernal
execute if score #cooldown_display_tick sb_infernal matches 0 if score #cooldown sb_infernal matches 1.. run scoreboard players add #cooldown_seconds sb_infernal 19
execute if score #cooldown_display_tick sb_infernal matches 0 if score #cooldown sb_infernal matches 1.. run scoreboard players operation #cooldown_seconds sb_infernal /= #twenty sb_infernal

# Cuenta regresiva inicial; Java conserva la selección de participantes elegibles.
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 100 run function skyblockmulti:infernal_trial/prepare_arena
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 20 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 40 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 60 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 80 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 100 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 120 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 140 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 160 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 180 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 200 run function skyblockmulti:infernal_trial/show_countdown
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 1 run function skyblockmulti:infernal_trial/begin_wave
execute if score #trial_state sb_infernal matches 1 if score #countdown sb_infernal matches 1.. run scoreboard players remove #countdown sb_infernal 1

# Bossbar y final de fase.
# La pertenencia de los hijos se hereda al dividirse; no se adopta fauna cercana.
execute if score #trial_state sb_infernal matches 2 in minecraft:the_nether store result score #wave_remaining sb_infernal run execute if entity @e[tag=skyblock_infernal_enemy]
# The Java reward ledger owns max/value while combat is active so the bar tracks
# combined mob health. Writing the enemy count here made both values alternate.
execute if score #trial_state sb_infernal matches 2 run function skyblockmulti:infernal_trial/update_bossbar
execute if score #trial_state sb_infernal matches 2 in minecraft:the_nether positioned 0 64 0 run bossbar set skyblockmulti:infernal_trial players @a[distance=..64]
execute if score #trial_state sb_infernal matches 2 if score #trial_level sb_infernal matches 1..6 in minecraft:the_nether unless entity @e[tag=skyblock_infernal_enemy] run function skyblockmulti:infernal_trial/complete
execute if score #trial_state sb_infernal matches 2 if score #trial_level sb_infernal matches 7..10 if score #phase sb_infernal matches 1 in minecraft:the_nether unless entity @e[tag=skyblock_infernal_enemy] run function skyblockmulti:infernal_trial/prepare_champion
execute if score #trial_state sb_infernal matches 2 if score #trial_level sb_infernal matches 7..9 if score #phase sb_infernal matches 2 in minecraft:the_nether unless entity @e[tag=skyblock_infernal_enemy] run function skyblockmulti:infernal_trial/complete
execute if score #trial_state sb_infernal matches 2 if score #trial_level sb_infernal matches 10 if score #phase sb_infernal matches 2 in minecraft:the_nether unless entity @e[tag=skyblock_infernal_enemy] run function skyblockmulti:infernal_trial/prepare_champion
execute if score #trial_state sb_infernal matches 2 if score #trial_level sb_infernal matches 10 if score #phase sb_infernal matches 3 in minecraft:the_nether unless entity @e[tag=skyblock_infernal_enemy] run function skyblockmulti:infernal_trial/complete

# Presentación de ocho segundos antes del campeón del nivel X.
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 20 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 40 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 60 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 80 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 100 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 120 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 140 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 160 run function skyblockmulti:infernal_trial/show_champion_countdown
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 1 run function skyblockmulti:infernal_trial/spawn_champion
execute if score #trial_state sb_infernal matches 3 if score #champion_countdown sb_infernal matches 1.. run scoreboard players remove #champion_countdown sb_infernal 1

# Abandono: quince minutos sin combatientes dentro del Coliseo.
execute if score #trial_state sb_infernal matches 1..3 in minecraft:the_nether positioned 0 64 0 unless entity @a[tag=skyblock_infernal_participant,gamemode=!spectator,distance=..64] run scoreboard players add #abandon sb_infernal 1
execute if score #trial_state sb_infernal matches 1..3 in minecraft:the_nether positioned 0 64 0 if entity @a[tag=skyblock_infernal_participant,gamemode=!spectator,distance=..64] run scoreboard players set #abandon sb_infernal 0
execute if score #abandon sb_infernal matches 1 in minecraft:the_nether positioned 0 64 0 run tellraw @a {translate:"skyblockmulti.infernal_trial.empty",color:"gold"}
execute if score #abandon sb_infernal matches 6000 in minecraft:the_nether positioned 0 64 0 run tellraw @a {translate:"skyblockmulti.infernal_trial.abandon_10",color:"red"}
execute if score #abandon sb_infernal matches 12000 in minecraft:the_nether positioned 0 64 0 run tellraw @a {translate:"skyblockmulti.infernal_trial.abandon_5",color:"red"}
execute if score #abandon sb_infernal matches 16800 in minecraft:the_nether positioned 0 64 0 run tellraw @a {translate:"skyblockmulti.infernal_trial.abandon_1",color:"red"}
execute if score #abandon sb_infernal matches 18000.. run function skyblockmulti:infernal_trial/abandon

# Tras completarse, el bloqueo natural permanece treinta segundos.
execute if score #trial_state sb_infernal matches 4 if score #spawn_lock sb_infernal matches 1 in minecraft:the_nether positioned 0 64 0 run tellraw @a[distance=..64] {translate:"skyblockmulti.infernal_trial.arena_released",color:"dark_purple",italic:true}
execute if score #trial_state sb_infernal matches 4 if score #spawn_lock sb_infernal matches 1 in minecraft:the_nether positioned 0 64 0 run title @a[distance=..64] actionbar {translate:"skyblockmulti.infernal_trial.arena_released",color:"light_purple"}
execute if score #trial_state sb_infernal matches 4 if score #spawn_lock sb_infernal matches 1.. run scoreboard players remove #spawn_lock sb_infernal 1

# Cada cinco segundos devuelve al centro únicamente enemigos invocados que escaparon.
scoreboard players add #watchdog sb_infernal 1
execute if score #watchdog sb_infernal matches 100.. run scoreboard players set #watchdog sb_infernal 0
execute if score #trial_state sb_infernal matches 2 if score #watchdog sb_infernal matches 0 in minecraft:the_nether positioned 0 64 0 run tp @e[tag=skyblock_infernal_enemy,distance=40..] 0 65 -5

# Relicario personal: abrir el cofre detecta una solicitud de recompensa.
execute as @a at @s if dimension minecraft:the_nether positioned 0 64 5 if entity @s[distance=..3] if score @s sb_infernal_open > @s sb_infernal_seen run function skyblockmulti:infernal_trial/claim_reward
execute as @a run scoreboard players operation @s sb_infernal_seen = @s sb_infernal_open

# Activación del altar, solo cuando no existe una prueba o cuenta regresiva activa.
execute unless score #dynamic_trial_lock sb_infernal matches 1 unless score #trial_state sb_infernal matches 1..3 if score #cooldown sb_infernal matches 0 in minecraft:the_nether positioned 0.5 64.2 0.5 if entity @a[distance=..4,gamemode=!spectator] as @e[type=minecraft:item,distance=..1.5,nbt={Item:{id:"skyblockmulti:infernal_core"}},limit=1,sort=nearest] run function skyblockmulti:infernal_trial/start_1
execute unless score #dynamic_trial_lock sb_infernal matches 1 unless score #trial_state sb_infernal matches 1..3 if score #cooldown sb_infernal matches 0 in minecraft:the_nether positioned 0.5 64.2 0.5 if entity @a[distance=..4,gamemode=!spectator] as @e[type=minecraft:item,distance=..1.5,nbt={Item:{id:"skyblockmulti:infernal_core_2"}},limit=1,sort=nearest] run function skyblockmulti:infernal_trial/start_2
execute unless score #dynamic_trial_lock sb_infernal matches 1 unless score #trial_state sb_infernal matches 1..3 if score #cooldown sb_infernal matches 0 in minecraft:the_nether positioned 0.5 64.2 0.5 if entity @a[distance=..4,gamemode=!spectator] as @e[type=minecraft:item,distance=..1.5,nbt={Item:{id:"skyblockmulti:infernal_core_3"}},limit=1,sort=nearest] run function skyblockmulti:infernal_trial/start_3
execute unless score #dynamic_trial_lock sb_infernal matches 1 unless score #trial_state sb_infernal matches 1..3 if score #cooldown sb_infernal matches 0 in minecraft:the_nether positioned 0.5 64.2 0.5 if entity @a[distance=..4,gamemode=!spectator] as @e[type=minecraft:item,distance=..1.5,nbt={Item:{id:"skyblockmulti:infernal_core_4"}},limit=1,sort=nearest] run function skyblockmulti:infernal_trial/start_4
execute unless score #dynamic_trial_lock sb_infernal matches 1 unless score #trial_state sb_infernal matches 1..3 if score #cooldown sb_infernal matches 0 in minecraft:the_nether positioned 0.5 64.2 0.5 if entity @a[distance=..4,gamemode=!spectator] as @e[type=minecraft:item,distance=..1.5,nbt={Item:{id:"skyblockmulti:infernal_core_5"}},limit=1,sort=nearest] run function skyblockmulti:infernal_trial/start_5
execute unless score #dynamic_trial_lock sb_infernal matches 1 unless score #trial_state sb_infernal matches 1..3 if score #cooldown sb_infernal matches 0 in minecraft:the_nether positioned 0.5 64.2 0.5 if entity @a[distance=..4,gamemode=!spectator] as @e[type=minecraft:item,distance=..1.5,nbt={Item:{id:"skyblockmulti:infernal_core_6"}},limit=1,sort=nearest] run function skyblockmulti:infernal_trial/start_6
