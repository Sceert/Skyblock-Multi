#!/usr/bin/env python3
"""Convert a WorldEdit Sponge schematic v3 into a vanilla structure NBT."""

from __future__ import annotations

import argparse
from pathlib import Path

import nbtlib
from nbtlib import ByteArray, Compound, File, Int, List, String


def decode_varints(data: ByteArray, expected: int) -> list[int]:
    values: list[int] = []
    value = 0
    shift = 0
    for signed_byte in data:
        byte = int(signed_byte) & 0xFF
        value |= (byte & 0x7F) << shift
        if byte & 0x80:
            shift += 7
            if shift > 35:
                raise ValueError("Invalid block-state varint in schematic")
            continue
        values.append(value)
        value = 0
        shift = 0
    if shift != 0 or len(values) != expected:
        raise ValueError(f"Expected {expected} block states, decoded {len(values)}")
    return values


def block_state(value: str) -> Compound:
    if "[" not in value:
        return Compound({"Name": String(value)})
    name, raw_properties = value[:-1].split("[", 1)
    properties = Compound()
    for entry in raw_properties.split(","):
        key, property_value = entry.split("=", 1)
        properties[key] = String(property_value)
    return Compound({"Name": String(name), "Properties": properties})


def convert(source: Path, destination: Path) -> tuple[list[int], int, int]:
    source_file = nbtlib.load(source)
    schematic = source_file["Schematic"]
    if int(schematic["Version"]) != 3:
        raise ValueError("Only Sponge schematic version 3 is supported")

    width = int(schematic["Width"])
    height = int(schematic["Height"])
    length = int(schematic["Length"])
    volume = width * height * length
    sponge_blocks = schematic["Blocks"]
    states = decode_varints(sponge_blocks["Data"], volume)

    indexed_palette = sorted(
        ((int(index), name) for name, index in sponge_blocks["Palette"].items()),
        key=lambda entry: entry[0],
    )
    if [index for index, _ in indexed_palette] != list(range(len(indexed_palette))):
        raise ValueError("Schematic palette indices are not contiguous")
    palette = List[Compound]([block_state(name) for _, name in indexed_palette])
    air_states = {
        index for index, name in indexed_palette
        if name.split("[", 1)[0] in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}
    }

    block_entities: dict[tuple[int, int, int], Compound] = {}
    for source_entity in sponge_blocks.get("BlockEntities", []):
        position = tuple(int(axis) for axis in source_entity["Pos"])
        payload = Compound()
        if "Data" in source_entity:
            payload["Data"] = source_entity["Data"]
        payload["id"] = String(str(source_entity["Id"]))
        block_entities[position] = payload

    blocks = List[Compound]()
    layer_size = width * length
    for flat_index, state in enumerate(states):
        if state in air_states:
            continue
        y, within_layer = divmod(flat_index, layer_size)
        z, x = divmod(within_layer, width)
        position = (x, y, z)
        entry = Compound({
            "pos": List[Int]([Int(x), Int(y), Int(z)]),
            "state": Int(state),
        })
        if position in block_entities:
            entry["nbt"] = block_entities[position]
        blocks.append(entry)

    root = Compound({
        "DataVersion": Int(int(schematic["DataVersion"])),
        "size": List[Int]([Int(width), Int(height), Int(length)]),
        "palette": palette,
        "blocks": blocks,
        "entities": List[Compound](),
    })
    destination.parent.mkdir(parents=True, exist_ok=True)
    File(root).save(destination, gzipped=True)
    return [width, height, length], len(blocks), len(block_entities)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    size, blocks, block_entities = convert(args.source, args.destination)
    print(f"Converted size={size}, blocks={blocks}, block_entities={block_entities}")


if __name__ == "__main__":
    main()
