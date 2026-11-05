$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$functions = Join-Path $root 'src/main/resources/data/skyblockmulti/function'
$slotRoot = Join-Path $functions 'slots'
$claimRoot = Join-Path $slotRoot 'claim'
$playerRoot = Join-Path $functions 'player'
$maximum = 128

New-Item -ItemType Directory -Path $claimRoot -Force | Out-Null

function Key([int]$slot) { return $slot.ToString('00') }

$generate = @(
    '# Create and verify anchors for every active slot definition.',
    'scoreboard players set #anchors sb3_const 0'
)
$forceload = @('# Temporarily load every active slot definition.')
$removeForceload = @('# Remove temporary tickets from every stored slot definition.')
$sync = @('# Synchronize occupancy for every active slot definition.')
$removeFree = @('# Remove only free anchors from the previous configured geometry.')
$forward = @('# Search active stable slot IDs from the random starting point to 128.')
$wrapped = @('', '# Wrap to active stable slot IDs before the random starting point.')

for ($slot = 1; $slot -le $maximum; $slot++) {
    $key = Key $slot
    $storage = "skyblockmulti:slots s$key"
    $generate += "execute if data storage skyblockmulti:slots s$key.x run function skyblockmulti:slots/create_anchor with storage $storage"
    $forceload += "execute if data storage skyblockmulti:slots s$key.x run function skyblockmulti:slots/forceload_one with storage $storage"
    $removeForceload += "execute if data storage skyblockmulti:slots s$key.x run function skyblockmulti:slots/remove_forceload_one with storage $storage"
    $sync += "execute if data storage skyblockmulti:slots s$key.x run function skyblockmulti:slots/sync_one with storage $storage"
    $removeFree += "execute if data storage skyblockmulti:slots s$key.x run function skyblockmulti:slots/remove_free_anchor_one with storage $storage"
    $forward += "execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..$slot if data storage skyblockmulti:slots s$key.x if score #$key sb3_used matches 0 run function skyblockmulti:slots/claim/$key"
    if ($slot -lt $maximum) {
        $wrapped += "execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches $($slot + 1).. if data storage skyblockmulti:slots s$key.x if score #$key sb3_used matches 0 run function skyblockmulti:slots/claim/$key"
    }
    @(
        "# Configurable island slot $slot.",
        "function skyblockmulti:slots/forceload_one with storage $storage",
        "function skyblockmulti:slots/claim_generic with storage $storage"
    ) | Set-Content -LiteralPath (Join-Path $claimRoot "$key.mcfunction") -Encoding utf8NoBOM
}

$generate += @(
    '',
    'execute if score #anchors sb3_const = #capacity sb3_cfg in minecraft:overworld positioned 0 100 0 run summon minecraft:marker ~ ~ ~ {Tags:["skyblock_slots_ready_v2"]}',
    '# El primer lote listo desbloquea el menú; el resto continúa preparándose en segundo plano.',
    'execute if score #anchors sb3_const matches 1.. run scoreboard players set #slots_built sb3_const 1',
    'execute if score #anchors sb3_const = #capacity sb3_cfg run scoreboard players set #slots_complete sb3_const 1',
    'execute if score #anchors sb3_const = #capacity sb3_cfg run function skyblockmulti:slots/sync_occupancy',
    'execute if score #anchors sb3_const = #capacity sb3_cfg run function skyblockmulti:slots/remove_forceload',
    'execute if score #anchors sb3_const = #capacity sb3_cfg run scoreboard players set #slotgen sb3_const -1',
    'execute unless score #anchors sb3_const = #capacity sb3_cfg run scoreboard players set #slotgen sb3_const 40'
)

$generate | Set-Content -LiteralPath (Join-Path $slotRoot 'generate_content.mcfunction') -Encoding utf8NoBOM
$forceload | Set-Content -LiteralPath (Join-Path $slotRoot 'forceload.mcfunction') -Encoding utf8NoBOM
$removeForceload | Set-Content -LiteralPath (Join-Path $slotRoot 'remove_forceload.mcfunction') -Encoding utf8NoBOM
$sync | Set-Content -LiteralPath (Join-Path $slotRoot 'sync_occupancy.mcfunction') -Encoding utf8NoBOM
$removeFree | Set-Content -LiteralPath (Join-Path $slotRoot 'remove_free_anchors.mcfunction') -Encoding utf8NoBOM
($forward + $wrapped) | Set-Content -LiteralPath (Join-Path $playerRoot 'claim_random_active.mcfunction') -Encoding utf8NoBOM

$ringRanges = @(@(1, 8), @(9, 24), @(25, 48), @(49, 80), @(81, 128))
for ($ring = 1; $ring -le $ringRanges.Count; $ring++) {
    $start = $ringRanges[$ring - 1][0]
    $end = $ringRanges[$ring - 1][1]
    $lines = @("# Search active slots in configured ring $ring.")
    $lines += "execute store result score #random_slot sb3_const run random value $start..$end"
    for ($slot = $start; $slot -le $end; $slot++) {
        $key = Key $slot
        $lines += "execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches ..$slot if data storage skyblockmulti:slots s$key.x if score #$key sb3_used matches 0 run function skyblockmulti:slots/claim/$key"
    }
    for ($slot = $start; $slot -lt $end; $slot++) {
        $key = Key $slot
        $lines += "execute if score #claim sb3_const matches 0 if score #random_slot sb3_const matches $($slot + 1).. if data storage skyblockmulti:slots s$key.x if score #$key sb3_used matches 0 run function skyblockmulti:slots/claim/$key"
    }
    $lines | Set-Content -LiteralPath (Join-Path $playerRoot "claim_random_ring_$ring.mcfunction") -Encoding utf8NoBOM
}

$custom = @(
    '# Allocation policy: BALANCED and RANDOM_BALANCED use all active slots.',
    'execute if score #allocation_policy sb3_cfg matches 0 run function skyblockmulti:player/claim_random_active',
    'execute if score #allocation_policy sb3_cfg matches 4 run function skyblockmulti:player/claim_random_active',
    '',
    '# INNER_FIRST.',
    'execute if score #allocation_policy sb3_cfg matches 1 run function skyblockmulti:player/claim_random_ring_1',
    'execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_2',
    'execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_3',
    'execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_4',
    'execute if score #allocation_policy sb3_cfg matches 1 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_5',
    '',
    '# OUTER_FIRST.',
    'execute if score #allocation_policy sb3_cfg matches 2 run function skyblockmulti:player/claim_random_ring_5',
    'execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_4',
    'execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_3',
    'execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_2',
    'execute if score #allocation_policy sb3_cfg matches 2 if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_1',
    '',
    '# DIFFICULTY: Easy prefers inner rings; other difficulties prefer outer rings.',
    'execute if score #allocation_policy sb3_cfg matches 3 if score @s sb_chest matches 3 run function skyblockmulti:player/claim_custom_inner',
    'execute if score #allocation_policy sb3_cfg matches 3 unless score @s sb_chest matches 3 run function skyblockmulti:player/claim_custom_outer'
)
$custom | Set-Content -LiteralPath (Join-Path $playerRoot 'claim_custom.mcfunction') -Encoding utf8NoBOM
@(
    'function skyblockmulti:player/claim_random_ring_1',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_2',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_3',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_4',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_5'
) | Set-Content -LiteralPath (Join-Path $playerRoot 'claim_custom_inner.mcfunction') -Encoding utf8NoBOM
@(
    'function skyblockmulti:player/claim_random_ring_5',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_4',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_3',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_2',
    'execute if score #claim sb3_const matches 0 run function skyblockmulti:player/claim_random_ring_1'
) | Set-Content -LiteralPath (Join-Path $playerRoot 'claim_custom_outer.mcfunction') -Encoding utf8NoBOM
