# Inicialización de respaldo exclusiva del preset Skyblock Multiplayer.
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
scoreboard players set #active sb3_const 1
execute in minecraft:overworld if biome 0 64 0 minecraft:the_void run function skyblockmulti:post_load

scoreboard objectives add sb_adv_timer dummy
scoreboard objectives add sb_choice_time dummy
