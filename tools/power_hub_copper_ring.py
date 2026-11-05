#!/usr/bin/env python3
"""Power the HUB copper ring and generate its post-template redstone pulse."""

from pathlib import Path

from convert_sponge_schem import Tag, read_nbt, write_nbt


ROOT = Path(__file__).resolve().parents[1]
STRUCTURE = ROOT / "src/main/resources/data/skyblockmulti/structure/nexus_hub_v13.nbt"
POWER_FUNCTION = ROOT / "src/main/resources/data/skyblockmulti/function/hub/power_copper_ring.mcfunction"


def main() -> None:
    _, root = read_nbt(STRUCTURE)
    palette = root.value["palette"].value
    blocks = root.value["blocks"].value
    names = [entry.value["Name"].value for entry in palette]
    by_position = {
        tuple(value.value for value in block.value["pos"].value): block
        for block in blocks
    }

    redstone_state = next(
        (index for index, name in enumerate(names) if name == "minecraft:redstone_block"),
        None,
    )
    if redstone_state is None:
        redstone_state = len(palette)
        palette.append(Tag(10, {"Name": Tag(8, "minecraft:redstone_block")}))

    changed = 0
    ring_positions: list[tuple[int, int, int, str]] = []
    for position, block in by_position.items():
        # The imported circular ring is the 88-block layer at local Y=29.
        # Other redstone-powered bulbs elsewhere in the authored HUB are not
        # part of this fix and must retain their original behaviour.
        if position[1] != 29:
            continue
        state = block.value["state"].value
        if names[state] not in {"minecraft:daylight_detector", "minecraft:redstone_block"}:
            continue
        above = by_position.get((position[0], position[1] + 1, position[2]))
        if above is None:
            continue
        above_name = names[above.value["state"].value]
        if above_name != "minecraft:waxed_exposed_copper_bulb":
            continue
        if names[state] == "minecraft:daylight_detector":
            block.value["state"] = Tag(3, redstone_state)
        changed += 1
        ring_positions.append((position[0] - 46, position[1] + 130, position[2] - 46, above_name))

    if changed != 88:
        raise RuntimeError(f"Expected 88 powered ring lamps, found {changed}")
    write_nbt(STRUCTURE, root)
    commands = [
        "# Pulse the 88 copper bulbs only after the full HUB template is placed.",
        "# Copper bulbs toggle on a rising redstone edge; forcing only their saved",
        "# block state is unreliable while a structure resolves neighbour updates.",
    ]
    for x, y, z, bulb_name in sorted(ring_positions):
        commands.append(f"setblock {x} {y} {z} minecraft:air")
        commands.append(
            f"setblock {x} {y + 1} {z} {bulb_name}[lit=false,powered=false]"
        )
        commands.append(f"setblock {x} {y} {z} minecraft:redstone_block")
    POWER_FUNCTION.write_text("\n".join(commands) + "\n", encoding="utf-8")
    print(f"Powered {changed} HUB copper-ring lamps and generated their pulse function.")


if __name__ == "__main__":
    main()
