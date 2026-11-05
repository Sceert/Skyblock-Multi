# Compatibilidad administrativa: ejecuta ambas mitades bajo demanda.
function skyblockmulti:advancements/check_group_a
function skyblockmulti:advancements/check_group_b

# Logros de combate mediante estadísticas vanilla; evita predicados incompatibles.
execute unless entity @s[advancements={skyblockmulti:combat/kill_hostile=true}] if score @s sb_mobkills matches 1.. run advancement grant @s only skyblockmulti:combat/kill_hostile
execute unless entity @s[advancements={skyblockmulti:combat/zombie_kill=true}] if score @s sb_k_zombie matches 1.. run advancement grant @s only skyblockmulti:combat/zombie_kill
execute unless entity @s[advancements={skyblockmulti:combat/skeleton_kill=true}] if score @s sb_k_skeleton matches 1.. run advancement grant @s only skyblockmulti:combat/skeleton_kill
execute unless entity @s[advancements={skyblockmulti:combat/creeper_kill=true}] if score @s sb_k_creeper matches 1.. run advancement grant @s only skyblockmulti:combat/creeper_kill
execute unless entity @s[advancements={skyblockmulti:combat/spider_kill=true}] if score @s sb_k_spider matches 1.. run advancement grant @s only skyblockmulti:combat/spider_kill
execute unless entity @s[advancements={skyblockmulti:combat/witch_kill=true}] if score @s sb_k_witch matches 1.. run advancement grant @s only skyblockmulti:combat/witch_kill
execute unless entity @s[advancements={skyblockmulti:combat/slime_kill=true}] if score @s sb_k_slime matches 1.. run advancement grant @s only skyblockmulti:combat/slime_kill
execute unless entity @s[advancements={skyblockmulti:combat/enderman_kill=true}] if score @s sb_k_enderman matches 1.. run advancement grant @s only skyblockmulti:combat/enderman_kill
execute unless entity @s[advancements={skyblockmulti:combat/blaze_kill=true}] if score @s sb_k_blaze matches 1.. run advancement grant @s only skyblockmulti:combat/blaze_kill
execute unless entity @s[advancements={skyblockmulti:combat/ghast_kill=true}] if score @s sb_k_ghast matches 1.. run advancement grant @s only skyblockmulti:combat/ghast_kill
execute unless entity @s[advancements={skyblockmulti:end/dragon_kill=true}] if score @s sb_k_dragon matches 1.. run advancement grant @s only skyblockmulti:end/dragon_kill
