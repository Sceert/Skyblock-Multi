#!/usr/bin/env python3
"""Convert a Sponge v3 .schem clipboard into a vanilla structure .nbt.

The converter intentionally exports blocks and block entities only. Air and
structure-void entries are omitted so placing the template cannot erase the
fortress or other independently generated Nexus modules.
"""

from __future__ import annotations

import argparse
import gzip
import io
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Any, BinaryIO


@dataclass
class Tag:
    kind: int
    value: Any


def read_exact(stream: BinaryIO, size: int) -> bytes:
    data = stream.read(size)
    if len(data) != size:
        raise EOFError("Unexpected end of NBT stream")
    return data


def read_string(stream: BinaryIO) -> str:
    size = struct.unpack(">H", read_exact(stream, 2))[0]
    return read_exact(stream, size).decode("utf-8")


def read_payload(stream: BinaryIO, kind: int) -> Tag:
    if kind == 1:
        return Tag(kind, struct.unpack(">b", read_exact(stream, 1))[0])
    if kind == 2:
        return Tag(kind, struct.unpack(">h", read_exact(stream, 2))[0])
    if kind == 3:
        return Tag(kind, struct.unpack(">i", read_exact(stream, 4))[0])
    if kind == 4:
        return Tag(kind, struct.unpack(">q", read_exact(stream, 8))[0])
    if kind == 5:
        return Tag(kind, struct.unpack(">f", read_exact(stream, 4))[0])
    if kind == 6:
        return Tag(kind, struct.unpack(">d", read_exact(stream, 8))[0])
    if kind == 7:
        size = struct.unpack(">i", read_exact(stream, 4))[0]
        return Tag(kind, read_exact(stream, size))
    if kind == 8:
        return Tag(kind, read_string(stream))
    if kind == 9:
        child_kind = struct.unpack(">B", read_exact(stream, 1))[0]
        size = struct.unpack(">i", read_exact(stream, 4))[0]
        return Tag(kind, [read_payload(stream, child_kind) for _ in range(size)])
    if kind == 10:
        value: dict[str, Tag] = {}
        while True:
            child_kind = struct.unpack(">B", read_exact(stream, 1))[0]
            if child_kind == 0:
                break
            name = read_string(stream)
            value[name] = read_payload(stream, child_kind)
        return Tag(kind, value)
    if kind == 11:
        size = struct.unpack(">i", read_exact(stream, 4))[0]
        return Tag(kind, list(struct.unpack(f">{size}i", read_exact(stream, 4 * size))))
    if kind == 12:
        size = struct.unpack(">i", read_exact(stream, 4))[0]
        return Tag(kind, list(struct.unpack(f">{size}q", read_exact(stream, 8 * size))))
    raise ValueError(f"Unsupported NBT tag type: {kind}")


def read_nbt(path: Path) -> tuple[str, Tag]:
    with gzip.open(path, "rb") as source:
        kind = struct.unpack(">B", read_exact(source, 1))[0]
        if kind != 10:
            raise ValueError("NBT root must be a compound")
        return read_string(source), read_payload(source, kind)


def write_string(stream: BinaryIO, value: str) -> None:
    encoded = value.encode("utf-8")
    stream.write(struct.pack(">H", len(encoded)))
    stream.write(encoded)


def write_payload(stream: BinaryIO, tag: Tag) -> None:
    kind, value = tag.kind, tag.value
    if kind == 1:
        stream.write(struct.pack(">b", value))
    elif kind == 2:
        stream.write(struct.pack(">h", value))
    elif kind == 3:
        stream.write(struct.pack(">i", value))
    elif kind == 4:
        stream.write(struct.pack(">q", value))
    elif kind == 5:
        stream.write(struct.pack(">f", value))
    elif kind == 6:
        stream.write(struct.pack(">d", value))
    elif kind == 7:
        stream.write(struct.pack(">i", len(value)))
        stream.write(value)
    elif kind == 8:
        write_string(stream, value)
    elif kind == 9:
        child_kind = value[0].kind if value else 10
        stream.write(struct.pack(">Bi", child_kind, len(value)))
        for child in value:
            if child.kind != child_kind:
                raise ValueError("Mixed NBT list types are invalid")
            write_payload(stream, child)
    elif kind == 10:
        for name, child in value.items():
            stream.write(struct.pack(">B", child.kind))
            write_string(stream, name)
            write_payload(stream, child)
        stream.write(b"\0")
    elif kind == 11:
        stream.write(struct.pack(">i", len(value)))
        if value:
            stream.write(struct.pack(f">{len(value)}i", *value))
    elif kind == 12:
        stream.write(struct.pack(">i", len(value)))
        if value:
            stream.write(struct.pack(f">{len(value)}q", *value))
    else:
        raise ValueError(f"Unsupported NBT tag type: {kind}")


def write_nbt(path: Path, root: Tag) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(path, "wb", compresslevel=9) as target:
        target.write(b"\x0a\x00\x00")
        write_payload(target, root)


def compound(tag: Tag, name: str) -> Tag:
    child = tag.value[name]
    if child.kind != 10:
        raise ValueError(f"Expected compound tag {name}")
    return child


def decode_varints(data: bytes, expected: int) -> list[int]:
    result: list[int] = []
    value = 0
    shift = 0
    for byte in data:
        value |= (byte & 0x7F) << shift
        if byte & 0x80:
            shift += 7
            if shift > 35:
                raise ValueError("Invalid VarInt in Sponge block data")
        else:
            result.append(value)
            value = 0
            shift = 0
    if shift:
        raise ValueError("Truncated VarInt in Sponge block data")
    if len(result) != expected:
        raise ValueError(f"Expected {expected} blocks, decoded {len(result)}")
    return result


def parse_block_state(serialized: str) -> Tag:
    if "[" not in serialized:
        return Tag(10, {"Name": Tag(8, serialized)})
    name, raw_properties = serialized[:-1].split("[", 1)
    properties = {}
    for entry in raw_properties.split(","):
        key, value = entry.split("=", 1)
        properties[key] = Tag(8, value)
    return Tag(10, {
        "Name": Tag(8, name),
        "Properties": Tag(10, properties),
    })


def block_entity_map(blocks: Tag) -> dict[tuple[int, int, int], Tag]:
    result: dict[tuple[int, int, int], Tag] = {}
    entries = blocks.value.get("BlockEntities")
    if entries is None:
        return result
    for entry in entries.value:
        data = dict(entry.value)
        pos_tag = data.pop("Pos", None)
        if pos_tag is None:
            continue
        identifier = data.pop("Id", None)
        if identifier is not None:
            data["id"] = identifier
        data.pop("x", None)
        data.pop("y", None)
        data.pop("z", None)
        result[tuple(pos_tag.value)] = Tag(10, data)
    return result


def convert(source: Path, target: Path, include_air: bool = False) -> None:
    root_name, root = read_nbt(source)
    # WorldEdit 7.4 writes an unnamed root with a nested Schematic compound;
    # older Sponge writers may name the root itself Schematic.
    if root_name == "" and "Schematic" in root.value:
        root = compound(root, "Schematic")
    elif root_name != "Schematic":
        raise ValueError(f"Unexpected root name: {root_name!r}")

    width = root.value["Width"].value
    height = root.value["Height"].value
    length = root.value["Length"].value
    blocks_source = compound(root, "Blocks")
    palette_source = compound(blocks_source, "Palette")

    source_palette = {
        tag.value: state for state, tag in palette_source.value.items()
    }
    source_ids = decode_varints(
        blocks_source.value["Data"].value,
        width * height * length,
    )
    entities = block_entity_map(blocks_source)

    palette: list[Tag] = []
    palette_indexes: dict[str, int] = {}
    blocks: list[Tag] = []
    skipped = {"minecraft:structure_void"}
    if not include_air:
        skipped.add("minecraft:air")

    for index, source_id in enumerate(source_ids):
        state = source_palette[source_id]
        if state in skipped:
            continue
        x = index % width
        z = (index // width) % length
        y = index // (width * length)
        palette_index = palette_indexes.get(state)
        if palette_index is None:
            palette_index = len(palette)
            palette_indexes[state] = palette_index
            palette.append(parse_block_state(state))

        block = {
            "pos": Tag(9, [Tag(3, x), Tag(3, y), Tag(3, z)]),
            "state": Tag(3, palette_index),
        }
        block_nbt = entities.get((x, y, z))
        if block_nbt is not None:
            block["nbt"] = block_nbt
        blocks.append(Tag(10, block))

    data_version = root.value["DataVersion"].value
    structure = Tag(10, {
        "DataVersion": Tag(3, data_version),
        "size": Tag(9, [Tag(3, width), Tag(3, height), Tag(3, length)]),
        "palette": Tag(9, palette),
        "blocks": Tag(9, blocks),
        "entities": Tag(9, []),
    })
    write_nbt(target, structure)
    print(
        f"{source.name}: {width}x{height}x{length}, "
        f"{len(blocks)} blocks, {len(palette)} states -> {target}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("target", type=Path)
    parser.add_argument(
        "--include-air",
        action="store_true",
        help="Preserve air so the template exactly replaces the selected volume.",
    )
    args = parser.parse_args()
    convert(args.source, args.target, args.include_air)


if __name__ == "__main__":
    main()
