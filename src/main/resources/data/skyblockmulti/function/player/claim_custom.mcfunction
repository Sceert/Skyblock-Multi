# Allocation policy: BALANCED and RANDOM_BALANCED use all active slots.
execute if score #allocation_policy sb3_cfg matches 0 run function skyblockmulti:player/claim_random_active
execute if score #allocation_policy sb3_cfg matches 4 run function skyblockmulti:player/claim_random_active

# INNER_FIRST.
execute if score #allocation_policy sb3_cfg matches 1 run function skyblockmulti:player/claim_random_ring_1
execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_2
execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_3
execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_4
execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_5

# OUTER_FIRST.
execute if score #allocation_policy sb3_cfg matches 2 run function skyblockmulti:player/claim_random_ring_5
execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_4
execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_3
execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_2
execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_1

# DIFFICULTY: Easy prefers inner rings; other difficulties prefer outer rings.
execute if score #allocation_policy sb3_cfg matches 3 if score @s sb_chest matches 3 run function skyblockmulti:player/claim_custom_inner
execute if score #allocation_policy sb3_cfg matches 3 unless score @s sb_chest matches 3 run function skyblockmulti:player/claim_custom_outer
