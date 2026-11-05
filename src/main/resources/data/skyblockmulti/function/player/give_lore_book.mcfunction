# Crónica oficial del mundo. custom_data permite distinguirla de cualquier libro creado por el jugador.
execute if entity @s[tag=skyblock_lore_received] run return 0
tag @s add skyblock_lore_received
give @s minecraft:written_book[minecraft:custom_name={translate:'skyblockmulti.lore_book.title',color:'gold',italic:false},minecraft:written_book_content={title:{raw:'Chronicle of the Last Refuge'},author:'The Nexus Chronicler',generation:0,resolved:true,pages:[{raw:{translate:'skyblockmulti.lore_book.page.1'}},{raw:{translate:'skyblockmulti.lore_book.page.2'}},{raw:{translate:'skyblockmulti.lore_book.page.3'}},{raw:{translate:'skyblockmulti.lore_book.page.4'}},{raw:{translate:'skyblockmulti.lore_book.page.5'}}]},minecraft:custom_data={skyblockmulti_lore_book:1b}] 1
tellraw @s [{"text":"[Skyblock Multi] ","color":"aqua","bold":true},{"translate":"skyblockmulti.lore_book.received","color":"gold"}]
