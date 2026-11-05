from pathlib import Path
from math import sqrt

ROOT = Path(__file__).resolve().parents[1]
FUNCTIONS = ROOT / "src/main/resources/data/skyblockmulti/function/hub"
NEXUS_FUNCTIONS = ROOT / "src/main/resources/data/skyblockmulti/function/nexus"

HINT_TITLES = {
    "leather": "The First Warden",
    "gold": "The Sunless King",
    "iron": "The Steadfast Watch",
    "diamond": "The Last Warden",
    "earth": "The Harvester's Verse",
    "trees": "The Forest's Promise",
    "metals": "The Forge's Memory",
    "war": "The Soldier's Lament",
}


def command_fill(x1, y1, z1, x2, y2, z2, block):
    return f"execute in minecraft:overworld run fill {x1} {y1} {z1} {x2} {y2} {z2} minecraft:{block}"


def command_set(x, y, z, block):
    return f"execute in minecraft:overworld run setblock {x} {y} {z} minecraft:{block}"


def nexus_hint_book(hint):
    """Return the block-entity NBT for a localized Nexus clue book."""
    title = HINT_TITLES[hint]
    return (
        "{Book:{id:'minecraft:written_book',count:1,components:{"
        "'minecraft:written_book_content':{"
        f"title:{{raw:\"{title}\"}},"
        "author:'The Nexus Chronicler',generation:0,resolved:true,"
        f"pages:[{{raw:{{translate:'skyblockmulti.nexus.hint.{hint}'}}}}]"
        "},"
        f"'minecraft:custom_name':{{translate:'skyblockmulti.nexus.hint_title.{hint}',color:'dark_aqua',italic:false}},"
        f"'minecraft:custom_data':{{skyblockmulti_nexus_hint:'{hint}'}}"
        "}},Page:0}"
    )


def disk(lines, y, radius, block, inner=-1):
    for z in range(-radius, radius + 1):
        outer_x = int(sqrt(max(0, radius * radius - z * z)))
        if inner < 0 or abs(z) > inner:
            lines.append(command_fill(-outer_x, y, z, outer_x, y, z, block))
            continue
        inner_x = int(sqrt(max(0, inner * inner - z * z)))
        if outer_x > inner_x:
            lines.append(command_fill(-outer_x, y, z, -inner_x - 1, y, z, block))
            lines.append(command_fill(inner_x + 1, y, z, outer_x, y, z, block))


def ring(lines, y, outer, inner, block):
    disk(lines, y, outer, block, inner)


def circle_shell(lines, y, radius, block, thickness=1):
    ring(lines, y, radius, max(0, radius - thickness), block)


def hemisphere_shell(lines, base_y, outer_radius, thickness, block):
    """Generate a continuous voxel hemisphere as outer sphere minus inner sphere."""
    inner_radius = outer_radius - thickness
    for dy in range(0, outer_radius + 1):
        y = base_y + dy
        outer_cross = int(sqrt(max(0, outer_radius * outer_radius - dy * dy)))
        inner_cross = -1
        if dy <= inner_radius:
            inner_cross = int(sqrt(max(0, inner_radius * inner_radius - dy * dy)))
        disk(lines, y, outer_cross, block, inner_cross)


def write(name, lines):
    (FUNCTIONS / name).write_text("\n".join(lines) + "\n", encoding="utf-8")


def ornamental_lamp(lines, x, z):
    """Build the reference lamp with its power source fully capped."""
    lines.append(command_set(x, 161, z, "chiseled_deepslate"))
    lines.append(command_set(x - 1, 161, z, "dark_oak_trapdoor[facing=west,half=bottom,open=true]"))
    lines.append(command_set(x + 1, 161, z, "dark_oak_trapdoor[facing=east,half=bottom,open=true]"))
    lines.append(command_set(x, 161, z - 1, "dark_oak_trapdoor[facing=north,half=bottom,open=true]"))
    lines.append(command_set(x, 161, z + 1, "dark_oak_trapdoor[facing=south,half=bottom,open=true]"))
    lines.append(command_set(x, 162, z, "polished_deepslate_wall"))
    lines.append(command_fill(x, 163, z, x, 164, z, "dark_oak_fence"))
    lines.append(command_set(x, 165, z, "hopper[facing=down]"))
    lines.append(command_set(x, 166, z, "redstone_lamp[lit=true]"))
    lines.append(command_set(x - 1, 166, z, "dark_oak_fence"))
    lines.append(command_set(x + 1, 166, z, "dark_oak_fence"))
    lines.append(command_set(x, 166, z - 1, "dark_oak_fence"))
    lines.append(command_set(x, 166, z + 1, "dark_oak_fence"))
    lines.append(command_set(x, 167, z, "redstone_block"))
    # Stair backs face inward, hiding the redstone; thin treads face outward.
    lines.append(command_set(x - 1, 167, z, "polished_deepslate_stairs[facing=east,half=bottom]"))
    lines.append(command_set(x + 1, 167, z, "polished_deepslate_stairs[facing=west,half=bottom]"))
    lines.append(command_set(x, 167, z - 1, "polished_deepslate_stairs[facing=south,half=bottom]"))
    lines.append(command_set(x, 167, z + 1, "polished_deepslate_stairs[facing=north,half=bottom]"))
    lines.append(command_set(x, 168, z, "polished_deepslate_slab[type=bottom]"))


def biome_sector(lines, x_sign, z_sign, block):
    """Paint an organic annular quadrant while preserving cardinal paths."""
    for z_abs in range(5, 40):
        spans = []
        start = None
        for x_abs in range(5, 40):
            radius_sq = x_abs * x_abs + z_abs * z_abs
            irregularity = ((x_abs * 7 + z_abs * 11) % 5) - 2
            inside = (19 + irregularity) ** 2 <= radius_sq <= (40 + irregularity) ** 2
            if inside and start is None:
                start = x_abs
            elif not inside and start is not None:
                spans.append((start, x_abs - 1))
                start = None
        if start is not None:
            spans.append((start, 39))
        z = z_sign * z_abs
        for x1_abs, x2_abs in spans:
            x1, x2 = x_sign * x1_abs, x_sign * x2_abs
            lines.append(command_fill(min(x1, x2), 160, z, max(x1, x2), 160, z, block))


def terrain():
    lines = [
        "# The Last Refuge v2 - circular garden platform.",
        "# Playable floor Y=160; sealed observation oculus at the centre.",
    ]
    for y, radius, block in [
        (151, 20, "deepslate"), (152, 24, "deepslate"),
        (153, 28, "stone"), (154, 32, "stone"),
        (155, 35, "tuff"), (156, 37, "tuff"),
        (157, 39, "stone"), (158, 40, "dirt"),
        (159, 41, "dirt"),
    ]:
        disk(lines, y, radius, block)
    disk(lines, 160, 41, "grass_block")

    # Open the full island thickness beneath the observation oculus. The
    # vertical space below Y=151 is already void in the Skyblock preset.
    for y in range(151, 160):
        disk(lines, y, 12, "air")

    # Warm structural rim inspired by the reference image.
    circle_shell(lines, 159, 44, "deepslate_bricks", 3)
    circle_shell(lines, 160, 44, "dark_oak_planks", 3)
    circle_shell(lines, 161, 44, "dark_oak_slab[type=bottom]", 2)

    # Central sealed observation pool: clear glass over the deep open view.
    # Lighting rings belong inside the vertical shaft, not on this window.
    disk(lines, 160, 12, "glass")
    ring(lines, 160, 15, 12, "deepslate_tiles")
    circle_shell(lines, 161, 15, "dark_oak_slab[type=bottom]", 1)

    # A single stratified floating-island mass, tapering organically toward
    # the bottom while preserving the radius-12 observation shaft.
    for y, radius, block in [
        (150,38,"stone"), (149,37,"stone"), (148,36,"tuff"),
        (147,35,"tuff"), (146,34,"deepslate"), (145,33,"deepslate"),
        (144,31,"deepslate"), (143,30,"deepslate_tiles"),
        (142,28,"deepslate"), (141,26,"deepslate"),
        (140,24,"deepslate_tiles"), (139,22,"deepslate"),
        (138,20,"deepslate"), (137,18,"deepslate_tiles"),
        (136,16,"deepslate"), (135,14,"deepslate"),
    ]:
        disk(lines, y, radius, block, 12)

    # Short uneven roots break the cone silhouette without becoming columns.
    for x, z, bottom in [(-18,-16,131),(-21,9,133),(-8,23,132),(16,18,130),(23,-7,134),(10,-22,132)]:
        lines.append(command_fill(x - 1, bottom + 3, z - 1, x + 1, 138, z + 1, "deepslate"))
        lines.append(command_fill(x, bottom, z, x, bottom + 2, z, "deepslate_tiles"))

    # Four paths stop at the observation rim and never descend.
    lines += [
        command_fill(-2, 160, -40, 2, 160, -16, "stone_bricks"),
        command_fill(-2, 160, 16, 2, 160, 40, "stone_bricks"),
        command_fill(-40, 160, -2, -16, 160, 2, "stone_bricks"),
        command_fill(16, 160, -2, 40, 160, 2, "stone_bricks"),
    ]

    # Warm, stepped central plaza adds relief.
    ring(lines, 160, 19, 15, "stone_bricks")
    ring(lines, 161, 18, 16, "smooth_stone_slab[type=bottom]")

    # Four broad organic sectors integrated into the circular HUB floor.
    biome_sector(lines, -1, -1, "packed_ice")
    biome_sector(lines, 1, -1, "podzol")
    biome_sector(lines, 1, 1, "moss_block")
    biome_sector(lines, -1, 1, "grass_block")

    # Four ornamental redstone lamps around the central plaza. Their stacked
    # silhouette follows the reference: trapped base, stone collar, timber
    # shaft, hopper, framed lamp and a stair-covered hidden power block.
    # Two lamps on each cardinal path and one centred in each biome.
    for x, z in [
        (0,23),(0,34),(0,-23),(0,-34),
        (23,0),(34,0),(-23,0),(-34,0),
        (27,27),(-27,27),(27,-27),(-27,-27),
    ]:
        ornamental_lamp(lines, x, z)

    # Northwest: compact snowy tundra forest, safely inside the glass curve.
    for x, z in [(-23,-23),(-28,-20),(-20,-29),(-29,-28)]:
        lines.append(command_fill(x, 161, z, x, 164, z, "spruce_log"))
        lines.append(command_fill(x - 2, 164, z - 2, x + 2, 165, z + 2, "spruce_leaves[persistent=true]"))
        lines.append(command_fill(x - 1, 166, z - 1, x + 1, 167, z + 1, "spruce_leaves[persistent=true]"))
    for x, z in [(-24,-30),(-31,-22),(-20,-20),(-27,-26)]:
        lines.append(command_set(x, 161, z, "snow"))

    # Northeast: dark forest, podzol floor and a dense dark-oak canopy.
    for x, z in [(22,-22),(28,-20),(20,-29),(29,-27)]:
        lines.append(command_fill(x, 161, z, x, 164, z, "dark_oak_log"))
        lines.append(command_fill(x - 2, 164, z - 2, x + 2, 166, z + 2, "dark_oak_leaves[persistent=true]"))
    lines.append(command_set(26, 161, -25, "red_mushroom"))
    lines.append(command_set(22, 161, -31, "brown_mushroom"))

    # Southeast: rolling meadow with several small stepped summits.
    for x, z, radius, height in [(23,24,6,2),(31,24,5,3),(25,32,5,2),(33,31,4,3)]:
        for dy in range(1, height + 1):
            disk_radius = max(1, radius - (dy - 1) * 2)
            for row_z in range(z - disk_radius, z + disk_radius + 1):
                span = int(sqrt(max(0, disk_radius * disk_radius - (row_z - z) ** 2)))
                lines.append(command_fill(x - span, 160 + dy, row_z, x + span, 160 + dy, row_z, "moss_block"))
    for x, y, z, flower in [(23,163,24,"dandelion"),(31,164,24,"poppy"),(25,163,32,"oxeye_daisy"),(33,164,31,"azure_bluet")]:
        lines.append(command_set(x, y, z, flower))

    # Southwest: cherry grove with pink petals and flowering undergrowth.
    for x, z in [(-22,22),(-28,20),(-20,29),(-29,28)]:
        lines.append(command_fill(x, 161, z, x, 164, z, "cherry_log"))
        lines.append(command_fill(x - 2, 164, z - 2, x + 2, 166, z + 2, "cherry_leaves[persistent=true]"))
    for x, z in [(-21,25),(-25,34),(-34,24),(-35,32),(-24,37)]:
        lines.append(command_set(x, 161, z, "pink_petals[flower_amount=4]"))
    lines.append(command_set(-24, 161, 26, "flowering_azalea"))
    write("build_terrain.mcfunction", lines)


def dome():
    lines = [
        "# The Last Refuge v12 - contained forests over a natural floating island.",
        "# Radius 45, three-block spherical shell, base Y=160, crown Y=205.",
    ]
    hemisphere_shell(lines, 160, 45, 3, "light_blue_stained_glass")

    # Continuous lower skirt: no flying mob or player-sized entity can pass
    # through the stepped junction between the platform and hemisphere.
    for y in range(160, 165):
        circle_shell(lines, y, 45, "light_blue_stained_glass", 3)

    # A continuous timber sill receives all four diagonal ribs at floor level.
    circle_shell(lines, 160, 45, "dark_oak_planks", 4)
    circle_shell(lines, 161, 45, "dark_oak_planks", 3)
    circle_shell(lines, 162, 45, "dark_oak_slab[type=bottom]", 2)

    # Four diagonal ribs form a three-block-wide X when viewed from above.
    # No horizontal timber hoops: the glass remains visually open.
    for dy in range(0, 46):
        radius = round(sqrt(max(0, 45 * 45 - dy * dy)))
        y = 160 + dy
        diagonal = round(radius / sqrt(2))
        for x, z in [
            (diagonal, diagonal), (-diagonal, diagonal),
            (diagonal, -diagonal), (-diagonal, -diagonal),
        ]:
            lines.append(command_fill(x - 1, y, z - 1, x + 1, y, z + 1, "dark_oak_planks"))

    # Four cardinal storm lanterns. The redstone block is hidden directly
    # below each lamp; the deepslate cap carries a lightning rod.
    for x, z in [(45,0),(-45,0),(0,45),(0,-45)]:
        lines.append(command_fill(x - 1, 159, z - 1, x + 1, 161, z + 1, "dark_oak_planks"))
        lines.append(command_set(x, 162, z, "redstone_block"))
        lines.append(command_set(x - 1, 162, z, "dark_oak_planks"))
        lines.append(command_set(x + 1, 162, z, "dark_oak_planks"))
        lines.append(command_set(x, 162, z - 1, "dark_oak_planks"))
        lines.append(command_set(x, 162, z + 1, "dark_oak_planks"))
        lines.append(command_set(x, 163, z, "redstone_lamp[lit=true]"))
        lines.append(command_set(x, 164, z, "chiseled_deepslate"))
        lines.append(command_set(x, 165, z, "lightning_rod[facing=up]"))

    # Short diagonal bridges close the final one-block gaps left by voxel
    # rounding and physically join every rib to the low central boss.
    for sx, sz in [(1,1),(-1,1),(1,-1),(-1,-1)]:
        for distance in range(5, 10):
            x, z = sx * distance, sz * distance
            lines.append(command_fill(x - 1, 203, z - 1, x + 1, 203, z + 1, "dark_oak_planks"))
        for distance in range(2, 7):
            x, z = sx * distance, sz * distance
            lines.append(command_fill(x - 1, 204, z - 1, x + 1, 204, z + 1, "dark_oak_planks"))

    # Rounded stepped wooden knob joining the four ribs at the crown.
    lines += [
        command_fill(-3, 204, -3, 3, 204, 3, "dark_oak_slab[type=top]"),
        command_fill(-2, 205, -2, 2, 205, 2, "dark_oak_planks"),
        command_fill(-2, 206, -2, 2, 206, 2, "stripped_dark_oak_log"),
        command_fill(-1, 207, -1, 1, 208, 1, "dark_oak_planks"),
        command_set(0, 209, 0, "dark_oak_slab[type=bottom]"),
    ]
    write("build_dome.mcfunction", lines)


def fortress():
    lines = [
        "# Ascension Nexus v3 - circular lower fortress and future dungeon hub.",
        "# Roof Y=35; ceremonial floor Y=14; portal chamber Y=4..13.",
    ]
    disk(lines, 12, 38, "reinforced_deepslate")
    disk(lines, 13, 37, "polished_blackstone_bricks")
    disk(lines, 14, 34, "polished_deepslate")
    circle_shell(lines, 15, 38, "polished_blackstone_bricks", 3)
    for y in range(16, 35):
        wall_block = "deepslate_tiles" if y % 4 else "polished_blackstone_bricks"
        circle_shell(lines, y, 38, wall_block, 2)

    # Dark glass roof connects directly to the observation cylinder. It keeps
    # the fortress enclosed while retaining the dramatic vertical sightline.
    ring(lines, 35, 38, 27, "polished_blackstone_bricks")
    disk(lines, 35, 27, "tinted_glass")
    circle_shell(lines, 36, 38, "chiseled_polished_blackstone", 2)
    circle_shell(lines, 34, 25, "crying_obsidian", 1)

    # Inner ceremonial rotunda and purple-lit seal ring.
    circle_shell(lines, 15, 19, "stone_bricks", 2)
    for y in range(16, 28):
        circle_shell(lines, y, 19, "cracked_stone_bricks", 1)
    circle_shell(lines, 15, 14, "crying_obsidian", 2)
    # Keep all four cardinal axes clear for the future four-way descent.
    for x, z in [(3,-8),(8,-3),(8,3),(3,8),(-3,8),(-8,3),(-8,-3),(-3,-8)]:
        lines.append(command_fill(x-1, 15, z-1, x+1, 15, z+1, "chiseled_stone_bricks"))
        lines.append(command_set(x, 16, z, "polished_blackstone"))

    # Eight tall inner pylons create a stronger vertical silhouette.
    for x, z in [(0,-25),(18,-18),(25,0),(18,18),(0,25),(-18,18),(-25,0),(-18,-18)]:
        lines.append(command_fill(x - 2, 15, z - 2, x + 2, 16, z + 2, "polished_blackstone_bricks"))
        lines.append(command_fill(x - 1, 17, z - 1, x + 1, 27, z + 1, "deepslate_bricks"))
        lines.append(command_set(x, 28, z, "glowstone"))
        lines.append(command_fill(x - 1, 29, z - 1, x + 1, 29, z + 1, "chiseled_polished_blackstone"))

    # Contained lava channels and soul-fire braziers restore the hostile,
    # ancient stronghold atmosphere without touching the central portal axis.
    for x1, z1, x2, z2 in [
        (-28,-23,-22,-21), (22,-23,28,-21),
        (-28,21,-22,23), (22,21,28,23),
        (-23,-28,-21,-22), (21,-28,23,-22),
        (-23,22,-21,28), (21,22,23,28),
    ]:
        lines.append(command_fill(x1, 14, z1, x2, 14, z2, "polished_blackstone_bricks"))
        lines.append(command_fill(x1, 15, z1, x2, 15, z2, "lava"))
        lines.append(command_fill(x1, 16, z1, x2, 16, z2, "orange_stained_glass"))
    for x, z in [(0,-22),(16,-16),(22,0),(16,16),(0,22),(-16,16),(-22,0),(-16,-16)]:
        lines.append(command_set(x, 15, z, "soul_soil"))
        lines.append(command_set(x, 16, z, "soul_fire"))

    # Four contained lavafalls descend from the upper wall into catch basins.
    for x, z in [(0,-34),(34,0),(0,34),(-34,0)]:
        lines.append(command_fill(x - 2, 14, z - 2, x + 2, 14, z + 2, "polished_blackstone_bricks"))
        lines.append(command_set(x, 15, z, "lava"))
        lines.append(command_set(x, 30, z, "lava"))
    # Transparent shields keep these dramatic falls decorative: buckets hit
    # the glass before reaching the four source blocks in the upper wall.
    lines.append(command_set(0, 30, -33, "orange_stained_glass"))
    lines.append(command_set(33, 30, 0, "orange_stained_glass"))
    lines.append(command_set(0, 30, 33, "orange_stained_glass"))
    lines.append(command_set(-33, 30, 0, "orange_stained_glass"))
    # Additional warm fire contrasts with the soul-fire seal circle.
    for x, z in [(11,-27),(27,-11),(27,11),(11,27),(-11,27),(-27,11),(-27,-11),(-11,-27)]:
        lines.append(command_set(x, 15, z, "netherrack"))
        lines.append(command_set(x, 16, z, "fire"))

    # Eight open dungeon arms, matching the eight global offering seals.
    arms = [
        (-4,-38,4,-20), (-4,20,4,38), (-38,-4,-20,4), (20,-4,38,4),
        (-31,-31,-17,-17), (17,17,31,31), (-31,17,-17,31), (17,-31,31,-17),
    ]
    for x1,z1,x2,z2 in arms:
        lines.append(command_fill(x1, 14, z1, x2, 20, z2, "air"))

    # Four cardinal corridors and chambers continue beyond the rotunda.
    cardinal_rooms = [
        (-11, -72, 11, -52),
        (-11, 52, 11, 72),
        (-72, -11, -52, 11),
        (52, -11, 72, 11),
    ]
    cardinal_corridors = [
        (-4, -54, 4, -34),
        (-4, 34, 4, 54),
        (-54, -4, -34, 4),
        (34, -4, 54, 4),
    ]
    for x1, z1, x2, z2 in cardinal_rooms + cardinal_corridors:
        lines.append(command_fill(x1, 13, z1, x2, 14, z2, "deepslate_tiles"))
        lines.append(command_fill(x1, 15, z1, x2, 23, z2, "deepslate_bricks"))
        lines.append(command_fill(x1 + 1, 15, z1 + 1, x2 - 1, 22, z2 - 1, "air"))
        lines.append(command_fill(x1, 23, z1, x2, 24, z2, "polished_blackstone_bricks"))

    # Diagonal arms use overlapping vaulted cells so they read as ruined,
    # irregular passages rather than square tunnels.
    diagonal_signs = [(1, 1), (1, -1), (-1, 1), (-1, -1)]
    for sx, sz in diagonal_signs:
        for distance in range(27, 54, 3):
            x = sx * distance
            z = sz * distance
            lines.append(command_fill(x - 4, 13, z - 4, x + 4, 14, z + 4, "deepslate_tiles"))
            lines.append(command_fill(x - 4, 15, z - 4, x + 4, 22, z + 4, "deepslate_bricks"))
            lines.append(command_fill(x - 3, 15, z - 3, x + 3, 21, z + 3, "air"))
            lines.append(command_fill(x - 4, 22, z - 4, x + 4, 23, z + 4, "polished_blackstone_bricks"))

    # Structural pylons for all eight wings. Fragile hanging decorations are
    # supplied only by the final schematic so they cannot break into items
    # when that schematic replaces this prototype layer.
    room_centres = [(0,-62),(44,-44),(62,0),(44,44),(0,62),(-44,44),(-62,0),(-44,-44)]
    for x, z in room_centres:
        lines.append(command_fill(x - 1, 15, z - 1, x + 1, 18, z + 1, "chiseled_deepslate"))
        for dx, dz in [(-7,-7),(7,-7),(-7,7),(7,7)]:
            lines.append(command_fill(x + dx, 15, z + dz, x + dx, 20, z + dz, "polished_blackstone_brick_wall"))

    # Four renewable lava wells. Each source is restored only when its nearby
    # donation chest receives one crafted Lava Catalyst.
    wells = [
        (0,-66,0,-62,"north"),
        (66,0,62,0,"east"),
        (0,66,0,62,"south"),
        (-66,0,-62,0,"west"),
    ]
    for x, z, chest_x, chest_z, facing in wells:
        lines.append(command_fill(x - 2, 14, z - 2, x + 2, 14, z + 2, "polished_blackstone_bricks"))
        lines.append(command_fill(x - 1, 15, z - 1, x + 1, 15, z + 1, "polished_blackstone"))
        lines.append(command_set(x, 15, z, "lava[level=0]"))

    # The four cardinal requirements are displayed on real armor stands.
    cardinal_stands = [
        (0,-25,"leather",180.0),(25,0,"gold",-90.0),
        (0,25,"iron",0.0),(-25,0,"diamond",90.0),
    ]

    # The four intercardinal chests accept only their crafted Offering item.
    receptacles = [
        (18,-18,"north"),(18,18,"south"),(-18,18,"south"),(-18,-18,"north"),
    ]

    # Eight readable clue books. Their pages use translation components, so
    # every client receives the hint in its own configured language.
    hints = [
        # Cardinal lecterns sit one block lower and directly outward from
        # their armor stand, facing the same cardinal direction.
        (0,15,-26,"north","leather"), (26,15,0,"east","gold"),
        (0,15,26,"south","iron"), (-26,15,0,"west","diamond"),
        # Intercardinal clues sit one block lower beside their offering chests.
        (15,15,-18,"west","earth"), (15,15,18,"west","trees"),
        (-15,15,18,"east","metals"), (-15,15,-18,"east","war"),
    ]

    # Re-open the four inner corridor mouths after their structural end walls
    # have been generated. Width seven keeps the lava wings visibly accessible.
    lines += [
        command_fill(-3, 15, -36, 3, 21, -32, "air"),
        command_fill(-3, 15, 32, 3, 21, 36, "air"),
        command_fill(32, 15, -3, 36, 21, 3, "air"),
        command_fill(-36, 15, -3, -32, 21, 3, "air"),
    ]

    # Sealed portal chamber; coordinates remain compatible with persistence.
    disk(lines, 4, 11, "reinforced_deepslate")
    for y in range(5, 14):
        circle_shell(lines, y, 11, "deepslate_bricks", 2)
    # Stronghold-inspired dark portal dais: a visible lava moat surrounds the
    # raised frame, while four stair flights approach it without a spawner.
    lines += [
        command_fill(-6, 5, -6, 6, 5, 6, "polished_deepslate"),
        command_fill(-5, 5, -5, 5, 5, -4, "lava"),
        command_fill(-5, 5, 4, 5, 5, 5, "lava"),
        command_fill(-5, 5, -3, -4, 5, 3, "lava"),
        command_fill(4, 5, -3, 5, 5, 3, "lava"),
        command_fill(-3, 5, -3, 3, 5, 3, "polished_blackstone_bricks"),
        command_fill(-2, 6, -3, 2, 6, -3, "polished_blackstone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]"),
        command_fill(-2, 6, 3, 2, 6, 3, "polished_blackstone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"),
        command_fill(-3, 6, -2, -3, 6, 2, "polished_blackstone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"),
        command_fill(3, 6, -2, 3, 6, 2, "polished_blackstone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]"),
    ]
    lines += [
        command_fill(-1, 6, -2, 1, 6, -2, "end_portal_frame[facing=south,eye=false]"),
        command_fill(-1, 6, 2, 1, 6, 2, "end_portal_frame[facing=north,eye=false]"),
        command_fill(-2, 6, -1, -2, 6, 1, "end_portal_frame[facing=east,eye=false]"),
        command_fill(2, 6, -1, 2, 6, 1, "end_portal_frame[facing=west,eye=false]"),
        command_fill(-1, 6, -1, 1, 6, 1, "air"),
    ]
    # Transparent sealed axis between the HUB and fortress. It is an
    # observation feature, never a traversable shaft.
    for y in range(36, 160):
        circle_shell(lines, y, 12, "black_stained_glass", 1)

    # Widely spaced luminous hoops create depth when looking down through the
    # oculus. Every centre stays open and no access route is introduced.
    for y in [148, 132, 116, 100, 84, 68, 52, 38]:
        circle_shell(lines, y, 11, "pearlescent_froglight", 1)

    # The player-authored fortress is the final visual layer for Y=0..40.
    # It deliberately includes air, allowing manual removals and openings to
    # replace the prototype while the functional entities created above and
    # the observation cylinder above Y=40 remain managed by the mod.
    lines.append(
        "execute in minecraft:overworld run place template "
        "skyblockmulti:nexus_fortress_v2 -74 0 -74"
    )

    # Small visual repairs found during the v2 fortress walk-through. Keeping
    # these as post-template corrections avoids requiring a new schematic for
    # three isolated authoring slips.
    lines.append(command_set(-62, 19, 0, "soul_lantern"))
    lines.append(command_set(-64, 23, 2, "polished_blackstone_bricks"))
    lines.append(command_set(-66, 23, 4, "polished_blackstone_bricks"))

    # Match the south lava-fall cornice to the deepslate-tile trim used by the
    # other cardinal entrances without replacing the surrounding brick walls.
    lines.append(command_fill(-4, 21, 34, -4, 22, 35, "deepslate_tiles"))
    lines.append(command_fill(4, 21, 34, 4, 22, 35, "deepslate_tiles"))
    lines.append(command_fill(-3, 22, 34, 3, 22, 34, "deepslate_tiles"))

    # Portal chamber finish: the outer square becomes an outward-facing
    # deepslate-tile stair frame. Remove the three obsolete decorative stair
    # clusters and cover the visible blackstone/reinforced transition with a
    # single deepslate-brick palette.
    lines.append(command_fill(-6, 5, -6, 6, 5, -6, "deepslate_tile_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]"))
    lines.append(command_fill(-6, 5, 6, 6, 5, 6, "deepslate_tile_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"))
    lines.append(command_fill(-6, 5, -5, -6, 5, 5, "deepslate_tile_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"))
    lines.append(command_fill(6, 5, -5, 6, 5, 5, "deepslate_tile_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]"))
    lines.append(command_fill(-7, 5, -1, -7, 5, 1, "air"))
    lines.append(command_fill(7, 5, -1, 7, 5, 1, "air"))
    lines.append(command_fill(-1, 5, 7, 1, 5, 7, "air"))
    lines.append(command_fill(-3, 5, -3, 3, 5, 3, "deepslate_bricks"))
    lines.append(command_fill(-6, 4, -6, 6, 4, -6, "deepslate_bricks"))
    lines.append(command_fill(-6, 4, 6, 6, 4, 6, "deepslate_bricks"))
    lines.append(command_fill(-6, 4, -5, -6, 4, 5, "deepslate_bricks"))
    lines.append(command_fill(6, 4, -5, 6, 4, 5, "deepslate_bricks"))

    # The schematic still contains the former prototype stations at distance
    # 69. Remove those copies, then restore the four functional catalyst
    # chests at distance 62 on top of the player-authored architecture.
    for old_x, old_z in [(0, -69), (69, 0), (0, 69), (-69, 0)]:
        lines.append(
            f"execute in minecraft:overworld if block {old_x} 16 {old_z} "
            f"minecraft:oxidized_copper_chest run setblock {old_x} 16 {old_z} minecraft:air"
        )
        lines.append(
            f"execute in minecraft:overworld if block {old_x} 17 {old_z} "
            f"minecraft:copper_bulb run setblock {old_x} 17 {old_z} minecraft:air"
        )
    for _, _, chest_x, chest_z, facing in wells:
        lines.append(command_set(
            chest_x, 16, chest_z,
            f"oxidized_copper_chest[facing={facing},type=single,waterlogged=false]"
        ))
        lines.append(
            f'''execute in minecraft:overworld run data merge block {chest_x} 16 {chest_z} '''
            '''{CustomName:{translate:"skyblockmulti.nexus.lava_well",color:"gold",italic:false}}'''
        )

    # Functional stations are applied only now, after the visual schematic,
    # so no duplicate lecterns, books, receptacles or armor stands are made.
    for x, z, seal, yaw in cardinal_stands:
        lines.append(command_fill(x - 1, 15, z - 1, x + 1, 15, z + 1, "chiseled_deepslate"))
        lines.append(command_set(x, 16, z, "air"))
        lines.append(
            f'execute in minecraft:overworld run summon minecraft:armor_stand '
            f'{x + 0.5} 16 {z + 0.5} '
            f'{{Tags:["skyblock_nexus_seal_{seal}"],NoGravity:1b,Invulnerable:1b,'
            f'PersistenceRequired:1b,ShowArms:1b,Rotation:[{yaw}f,0.0f]}}'
        )

    for x, z, facing in receptacles:
        lines.append(command_fill(x - 1, 15, z - 1, x + 1, 15, z + 1, "chiseled_deepslate"))
        lines.append(command_set(x, 16, z, f"oxidized_copper_chest[facing={facing},type=single,waterlogged=false]"))

    for x, y, z, facing, hint in hints:
        # Remove only the obsolete raised intercardinal copy from the schematic.
        if hint in {"earth", "trees", "metals", "war"}:
            lines.append(
                f"execute in minecraft:overworld if block {x} 16 {z} "
                f"minecraft:lectern run setblock {x} 16 {z} minecraft:air"
            )
        lines.append(command_set(x, y - 1, z, "chiseled_deepslate"))
        lines.append(command_set(x, y, z, f"lectern[facing={facing},has_book=true,powered=false]"))
        lines.append(
            f'''execute in minecraft:overworld run data merge block {x} {y} {z} '''
            + nexus_hint_book(hint)
        )

    # Repair the two missing dark-glass floor tiles in the upper rooms.
    lines.append(command_set(-30, 36, 15, "black_stained_glass"))
    lines.append(command_set(-16, 36, -22, "black_stained_glass"))

    write("build_fortress.mcfunction", lines)

    # Keep the eight official volumes recoverable without duplicating any
    # other block entity from the fortress schematic.
    restore_lines = [
        "# Restore an official clue volume after it is removed from its lectern."
    ]
    for x, y, z, facing, hint in hints:
        restore_lines.append(
            f"execute in minecraft:overworld if block {x} {y} {z} "
            f"minecraft:lectern[has_book=false] run setblock {x} {y} {z} "
            f"minecraft:lectern[facing={facing},has_book=true,powered=false]"
        )
        restore_lines.append(
            f"execute in minecraft:overworld if block {x} {y} {z} "
            f"minecraft:lectern[has_book=true] unless data block {x} {y} {z} Book "
            f"run data merge block {x} {y} {z} {nexus_hint_book(hint)}"
        )
    NEXUS_FUNCTIONS.mkdir(parents=True, exist_ok=True)
    (NEXUS_FUNCTIONS / "restore_hints.mcfunction").write_text(
        "\n".join(restore_lines) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    FUNCTIONS.mkdir(parents=True, exist_ok=True)
    terrain()
    dome()
    fortress()
    print("Generated Nexus v20 terrain, dome and dungeon fortress functions.")
