# Logro secreto: llegar al End llevando la Crónica oficial del Último Refugio.
# El inventario del jugador separa hotbar e inventory, por lo que se revisan ambos rangos.
execute if items entity @s hotbar.* minecraft:written_book[minecraft:custom_data~{skyblockmulti_lore_book:1b}] run advancement grant @s only skyblockmulti:secret/remember_where_you_came_from
execute unless entity @s[advancements={skyblockmulti:secret/remember_where_you_came_from=true}] if items entity @s inventory.* minecraft:written_book[minecraft:custom_data~{skyblockmulti_lore_book:1b}] run advancement grant @s only skyblockmulti:secret/remember_where_you_came_from
execute unless entity @s[advancements={skyblockmulti:secret/remember_where_you_came_from=true}] if items entity @s weapon.offhand minecraft:written_book[minecraft:custom_data~{skyblockmulti_lore_book:1b}] run advancement grant @s only skyblockmulti:secret/remember_where_you_came_from
