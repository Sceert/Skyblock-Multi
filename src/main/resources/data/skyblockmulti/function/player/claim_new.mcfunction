# Assign an active stable slot from the configured rings.
scoreboard players set #claim sb3_const 0
scoreboard players set #random_slot sb3_const 1
function skyblockmulti:player/claim_custom

# La primera asignación fija la geometría de ESTE mundo de forma persistente.
execute if score #claim sb3_const matches 1 run scoreboard players set #geometry_locked sb3_cfg 1
execute if score #claim sb3_const matches 1 run scoreboard players operation #world_capacity sb3_cfg = #capacity sb3_cfg
execute if score #claim sb3_const matches 1 run scoreboard players operation #world_radius sb3_const = #distance sb3_const
execute if score #claim sb3_const matches 1 run scoreboard players operation #world_ring_mask sb3_cfg = #ring_mask sb3_cfg
execute if score #claim sb3_const matches 1 run scoreboard players operation #world_allocation sb3_cfg = #allocation_policy sb3_cfg

execute if score #claim sb3_const matches 0 run function skyblockmulti:player/server_full
