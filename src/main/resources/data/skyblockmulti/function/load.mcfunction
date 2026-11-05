# Skyblock 26.2 Fabric v3.3 — objetivos principales.
# Los objetivos pueden existir en cualquier mundo; la lógica solo se activa en el preset Skyblock Multiplayer.
scoreboard objectives add sb3_state dummy
scoreboard objectives add sb3_slot dummy
scoreboard objectives add sb3_x dummy
scoreboard objectives add sb3_z dummy
scoreboard objectives add sb_active_x dummy
scoreboard objectives add sb_active_z dummy
scoreboard objectives add sb_party_pref dummy
scoreboard objectives add sb_party_cd dummy
scoreboard objectives add sb3_const dummy
scoreboard objectives add sb3_used dummy
scoreboard objectives add sb3_cfg dummy
scoreboard objectives add sb_deaths deathCount
scoreboard objectives add sb_last_deaths dummy
scoreboard objectives add sb_since_death minecraft.custom:minecraft.time_since_death
scoreboard objectives add sb_respawn_delay dummy
scoreboard objectives add sb_tree trigger
scoreboard objectives add sb_difficulty trigger
scoreboard objectives add sb_chest dummy
scoreboard objectives add sb_menu trigger
scoreboard objectives add sb_home trigger
scoreboard objectives add sb_info trigger
scoreboard objectives add sb_adv_timer dummy
scoreboard objectives add sb_nexus dummy
scoreboard objectives add sb_infernal dummy
scoreboard objectives add sb_infernal_run dummy
scoreboard objectives add sb_infernal_claim dummy
scoreboard objectives add sb_infernal_roll dummy
scoreboard objectives add sb_infernal_seen dummy
scoreboard objectives add sb_infernal_open minecraft.custom:minecraft.open_chest
scoreboard players set #twenty sb_infernal 20
execute unless score #bossbar_ready sb_infernal matches 1 run bossbar add skyblockmulti:infernal_trial {translate:"skyblockmulti.infernal_trial.wave_bar",color:"red"}
scoreboard players set #bossbar_ready sb_infernal 1
bossbar set skyblockmulti:infernal_trial color red
bossbar set skyblockmulti:infernal_trial style notched_10
bossbar set skyblockmulti:infernal_trial visible false
scoreboard objectives add sb_choice_time dummy
scoreboard objectives add sb_mobkills minecraft.custom:minecraft.mob_kills
scoreboard objectives add sb_k_zombie minecraft.killed:minecraft.zombie
scoreboard objectives add sb_k_skeleton minecraft.killed:minecraft.skeleton
scoreboard objectives add sb_k_creeper minecraft.killed:minecraft.creeper
scoreboard objectives add sb_k_spider minecraft.killed:minecraft.spider
scoreboard objectives add sb_k_witch minecraft.killed:minecraft.witch
scoreboard objectives add sb_k_slime minecraft.killed:minecraft.slime
scoreboard objectives add sb_k_enderman minecraft.killed:minecraft.enderman
scoreboard objectives add sb_k_blaze minecraft.killed:minecraft.blaze
scoreboard objectives add sb_k_ghast minecraft.killed:minecraft.ghast
scoreboard objectives add sb_k_dragon minecraft.killed:minecraft.ender_dragon
scoreboard players set #active sb3_const 0
execute in minecraft:overworld if biome 0 64 0 minecraft:the_void run scoreboard players set #active sb3_const 1
execute if score #active sb3_const matches 1 run function skyblockmulti:post_load
