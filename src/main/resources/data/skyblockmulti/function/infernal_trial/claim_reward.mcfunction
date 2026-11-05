# El cofre es solo una interfaz visual; cada participante recibe su propio lote una vez.
execute unless score @s sb_infernal_run = #run_id sb_infernal run tellraw @s {translate:"skyblockmulti.infernal_trial.not_participant",color:"red"}
execute if score @s sb_infernal_run = #run_id sb_infernal if score @s sb_infernal_claim = #run_id sb_infernal run tellraw @s {translate:"skyblockmulti.infernal_trial.already_claimed",color:"gray"}
execute if score @s sb_infernal_run = #run_id sb_infernal unless score @s sb_infernal_claim = #run_id sb_infernal run function skyblockmulti:infernal_trial/reward/grant
