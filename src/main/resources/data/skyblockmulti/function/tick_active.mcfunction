# Respaldo de inicialización.
execute in minecraft:overworld unless entity @e[type=minecraft:marker,tag=skyblock_system_v1,limit=1] run function skyblockmulti:bootstrap
# Generar The Ascension Nexus solo si este mundo nunca fue inicializado.
execute unless score #nexus_built sb3_const matches 1 run function skyblockmulti:hub/build
# Preparar las anclas por lotes. El menú se habilita al existir al menos una posición,
# mientras las restantes continúan cargándose sin bloquear al jugador.
execute unless score #slots_complete sb3_const matches 1 if score #slotgen sb3_const matches 1.. run scoreboard players remove #slotgen sb3_const 1
execute unless score #slots_complete sb3_const matches 1 if score #slotgen sb3_const matches 0 run function skyblockmulti:slots/generate_all
# Registrar jugadores.
execute as @a[tag=!skyblock_registered_v1] run function skyblockmulti:player/first_join
# Recuperar una selección interrumpida (estado 3 no debe persistir entre ticks).
execute as @a[scores={sb3_state=3}] run function skyblockmulti:player/recover_claim
# Corregir automáticamente el punto de reaparición de jugadores asignados por versiones anteriores.
execute as @a[scores={sb3_state=2},tag=!skyblock_respawn_v2] run function skyblockmulti:player/migrate_respawn
# Restauración robusta después de morir.
execute as @a[scores={sb3_state=2}] unless score @s sb_deaths = @s sb_last_deaths run function skyblockmulti:player/death_detected
execute as @a[tag=skyblock_respawn_pending,scores={sb3_state=2,sb_since_death=1..}] run function skyblockmulti:player/respawn_decide

# Prueba infernal comunitaria y renovable del Nether.
execute as @a[tag=!skyblock_infernal_chronicle_v1] at @s if dimension minecraft:the_nether run function skyblockmulti:player/give_infernal_chronicle
function skyblockmulti:infernal_trial/tick

# Progresión global de la fortaleza y fuentes renovables controladas (v15+).
execute if score #nexus_functional sb3_const matches 1 in minecraft:overworld run function skyblockmulti:nexus/tick

# Mantener estado de selección SIN inmovilizar al jugador.
execute as @a[scores={sb3_state=1}] run function skyblockmulti:player/freeze_selection
execute as @a[scores={sb3_state=3}] run function skyblockmulti:player/freeze_selection
execute as @a[scores={sb3_state=4}] run function skyblockmulti:player/freeze_selection

# Habilitar únicamente triggers que forman parte del flujo de selección.
scoreboard players enable @a[scores={sb3_state=1,sb_tree=0}] sb_tree
scoreboard players enable @a[scores={sb3_state=1,sb_menu=0}] sb_menu
scoreboard players enable @a[scores={sb3_state=4,sb_difficulty=0}] sb_difficulty
# Mostrar selector solo cuando las anclas estén listas.
execute if score #slots_built sb3_const matches 1 as @a[scores={sb3_state=1},tag=!skyblock_menu_shown_v1] run function skyblockmulti:player/menu
# Procesar exclusivamente botones del flujo de selección.
execute as @a[scores={sb_menu=1..,sb3_state=1}] run function skyblockmulti:player/menu
execute as @a[scores={sb_tree=1..12,sb3_state=1}] run function skyblockmulti:player/select
execute as @a[scores={sb_difficulty=1..4,sb3_state=4}] run function skyblockmulti:player/difficulty_select
# Limpiar valores inválidos.
scoreboard players set @a[scores={sb_tree=13..}] sb_tree 0
scoreboard players set @a[scores={sb_difficulty=5..}] sb_difficulty 0

# Secreto del libro.
execute as @a at @s if dimension minecraft:the_end unless entity @s[advancements={skyblockmulti:secret/remember_where_you_came_from=true}] run function skyblockmulti:advancements/check_lore_book_end

# Progreso de logros.
scoreboard players add #adv_timer sb_adv_timer 1
execute if score #adv_timer sb_adv_timer matches 100.. if score #adv_phase sb_adv_timer matches 0 run function skyblockmulti:advancements/check_group_a
execute if score #adv_timer sb_adv_timer matches 100.. if score #adv_phase sb_adv_timer matches 1 run function skyblockmulti:advancements/check_group_b
execute if score #adv_timer sb_adv_timer matches 100.. run scoreboard players add #adv_phase sb_adv_timer 1
execute if score #adv_phase sb_adv_timer matches 2.. run scoreboard players set #adv_phase sb_adv_timer 0
execute if score #adv_timer sb_adv_timer matches 100.. run scoreboard players set #adv_timer sb_adv_timer 0
scoreboard players add @a[scores={sb3_state=1}] sb_choice_time 1
scoreboard players add @a[scores={sb3_state=4}] sb_choice_time 1
scoreboard players set @a[scores={sb3_state=2}] sb_choice_time 0
