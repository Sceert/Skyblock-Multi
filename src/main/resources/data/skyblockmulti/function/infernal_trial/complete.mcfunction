scoreboard players set #trial_active sb_infernal 0
scoreboard players set #trial_state sb_infernal 4
scoreboard players set #spawn_lock sb_infernal 600
scoreboard players set #cooldown sb_infernal 1200
scoreboard players set #cooldown_display_tick sb_infernal 0
scoreboard players set #wave_remaining sb_infernal 0
bossbar set skyblockmulti:infernal_trial value 0
bossbar set skyblockmulti:infernal_trial visible false
bossbar set skyblockmulti:infernal_trial players
execute in minecraft:the_nether run setblock 0 65 0 minecraft:fire
execute in minecraft:the_nether run particle minecraft:soul_fire_flame 0 65 0 3 1 3 0.05 80 force
execute in minecraft:the_nether run playsound minecraft:ui.toast.challenge_complete master @a[distance=..48] 0 64 0 1.0 1.0
execute in minecraft:the_nether run tellraw @a[distance=..48] {translate:"skyblockmulti.infernal_trial.completed",color:"aqua"}
execute in minecraft:the_nether run setblock 0 64 5 skyblockmulti:infernal_vault
execute in minecraft:the_nether positioned 0 64 5 run particle minecraft:trial_spawner_detection ~0.5 ~1 ~0.5 1 1 1 0.02 80 force
execute in minecraft:the_nether positioned 0 64 5 run tellraw @a[distance=..64] {translate:"skyblockmulti.infernal_trial.reliquary_ready",color:"gold",bold:true}
# La recompensa queda registrada por UUID y se entrega exclusivamente al abrir
# la Bóveda Infernal. No se concede automáticamente al completar la prueba.
