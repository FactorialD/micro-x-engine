"""Comment-preserving MXL2 level parser/serializer."""

import re
from dataclasses import dataclass

from .io import Project


class LevelError(ValueError):
    pass


KINDS = ("room", "floor", "ceiling", "edge", "portal", "spawn", "transition", "entity")
ARITY = {"room": 4, "floor": 6, "ceiling": 6, "edge": 7, "portal": 11,
         "spawn": 6, "transition": 3, "entity": 9}
COUNT_LIMITS = ((1, 256), (1, 1024), (1, 1024), (0, 2048),
                (0, 1024), (1, 256), (0, 256), (0, 1024), (1, 1024))
LOCATION = re.compile(r"[A-Za-z0-9_-]{1,64}\Z")


@dataclass
class Line:
    raw: str
    kind: str | None = None
    values: list[str] | None = None
    comment: str = ""


@dataclass
class Level:
    lines: list[Line]

    def records(self, kind):
        return [line for line in self.lines if line.kind == kind]


def _integer(value: str, what: str) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        raise LevelError(f"invalid integer in {what}") from None


def _range(value: str, low: int, high: int, what: str) -> int:
    number = _integer(value, what)
    if not low <= number <= high:
        raise LevelError(f"{what} must be in {low}..{high}, got {number}")
    return number


def validate_unsigned_id(value: str, what: str) -> int:
    """Validate an ID stored by the converter as an unsigned 16-bit value."""
    return _range(value, 0, 65535, what)


def validate_signed_short(value: str, what: str) -> int:
    return _range(value, -32768, 32767, what)


def validate_entity_stable_id(value: str, what: str = "entity stable ID") -> int:
    # AssetConverter currently reads this with Tokens.id(), despite writing an int.
    return _range(value, 0, 65535, what)


def validate_location(value: str, what: str = "transition location") -> str:
    if not isinstance(value, str) or LOCATION.fullmatch(value) is None:
        raise LevelError(f"invalid {what}: expected [A-Za-z0-9_-]{{1,64}}")
    return value


def validate_source_coordinate(value: str, what: str) -> int:
    """Validate an integer which AssetConverter converts to Q16.16."""
    return _range(value, -32768, 32767, what)


def parse_level_text(text: str) -> Level:
    lines = []
    structural = []
    for no, raw in enumerate(text.splitlines(), 1):
        code, separator, comment = raw.partition("#")
        parts = code.split()
        preserved_comment = "#" + comment if separator else ""
        if not parts:
            lines.append(Line(raw, comment=preserved_comment))
            continue
        kind = parts[0]
        if kind == "MXL2":
            if len(parts) != 1:
                raise LevelError(f"line {no}: MXL2 takes no values")
            values = []
        elif kind == "environment":
            if len(parts) != 4 or any(len(c) != 6 for c in parts[1:]):
                raise LevelError(f"line {no}: environment needs three RGB888 colors")
            try: values = [format(int(c, 16), "06x") for c in parts[1:]]
            except ValueError: raise LevelError(f"line {no}: invalid environment color")
            if len(set(values)) != 3: raise LevelError(f"line {no}: environment colors must differ")
        elif kind == "counts":
            if len(parts) != 10:
                raise LevelError(f"line {no}: counts needs 9 values")
            values = parts[1:]
        elif kind in KINDS:
            if len(parts) - 1 != ARITY[kind]:
                raise LevelError(f"line {no}: {kind} needs {ARITY[kind]} fields")
            values = parts[1:]
        else:
            raise LevelError(f"line {no}: unknown record {kind}")
        structural.append((kind, no))
        lines.append(Line(raw, kind, values, preserved_comment))

    headers = [item for item in structural if item[0] == "MXL2"]
    count_headers = [item for item in structural if item[0] == "counts"]
    if len(headers) != 1:
        raise LevelError(f"expected exactly one MXL2 header, found {len(headers)}")
    if len(count_headers) != 1:
        raise LevelError(f"expected exactly one counts header, found {len(count_headers)}")
    if [x[0] for x in structural[:3]] != ["MXL2", "environment", "counts"]:
        raise LevelError("MXL2, environment and counts must be the first records")

    record_kinds = [kind for kind, _ in structural[3:]]
    positions = [KINDS.index(kind) for kind in record_kinds]
    if positions != sorted(positions):
        raise LevelError("records must follow converter order: " + ", ".join(KINDS))

    level = Level(lines)
    validate_level(level, level.records("counts")[0].values)
    return level


def validate_level(level: Level, declared=None):
    structural = [line.kind for line in level.lines if line.kind is not None]
    if structural.count("MXL2") != 1 or structural.count("environment") != 1 or structural.count("counts") != 1:
        raise LevelError("model must contain exactly one MXL2 and one counts header")
    if structural[:3] != ["MXL2", "environment", "counts"]:
        raise LevelError("MXL2, environment and counts must be the first records")
    record_positions = [KINDS.index(kind) for kind in structural[3:] if kind in KINDS]
    if len(record_positions) != len(structural) - 3 or record_positions != sorted(record_positions):
        raise LevelError("records must follow converter order: " + ", ".join(KINDS))
    counts = [len(level.records(kind)) for kind in KINDS]
    if declared is None:
        count_lines = level.records("counts")
        if len(count_lines) != 1 or count_lines[0].values is None:
            raise LevelError("exactly one counts header is required")
        declared = count_lines[0].values
    if len(declared) != 9:
        raise LevelError("counts needs 9 values")

    declared_counts = [
        _range(value, low, high, f"{KINDS[i] if i < 8 else 'capacity'} count")
        for i, (value, (low, high)) in enumerate(zip(declared, COUNT_LIMITS))
    ]
    if counts != declared_counts[:8]:
        raise LevelError(f"declared counts {declared_counts[:8]} do not match records {counts}")
    capacity = declared_counts[8]
    if counts[7] > capacity:
        raise LevelError(f"entity count {counts[7]} exceeds capacity {capacity}")

    rooms, portals, transitions = counts[0], counts[4], counts[6]

    def room_index(value, what):
        return _range(value, 0, rooms - 1, what)

    def coordinates(values, indexes, kind):
        return [validate_source_coordinate(values[index], f"{kind} field {index + 1}")
                for index in indexes]

    for kind in KINDS:
        for row_number, row in enumerate(level.records(kind), 1):
            values = row.values or []
            where = f"{kind} {row_number}"
            if len(values) != ARITY[kind]:
                raise LevelError(f"{where} needs {ARITY[kind]} fields")
            if kind == "room":
                q = coordinates(values, range(4), where)
                if q[0] > q[1] or q[2] > q[3]:
                    raise LevelError(f"unordered bounds in {where}")
            elif kind in ("floor", "ceiling"):
                room_index(values[0], f"{where} room index")
                q = coordinates(values, range(1, 6), where)
                if q[0] > q[1] or q[2] > q[3]:
                    raise LevelError(f"unordered bounds in {where}")
            elif kind == "edge":
                room_index(values[0], f"{where} room index")
                coordinates(values, range(1, 7), where)
            elif kind == "portal":
                validate_unsigned_id(values[0], f"{where} ID")
                room_index(values[1], f"{where} from-room index")
                room_index(values[2], f"{where} to-room index")
                q = coordinates(values, range(3, 9), where)
                if q[0] > q[1] or q[2] > q[3] or q[4] > q[5]:
                    raise LevelError(f"unordered bounds in {where}")
                _range(values[9], -1, portals - 1, f"{where} reverse index")
                _range(values[10], -1, transitions - 1, f"{where} transition index")
            elif kind == "spawn":
                validate_unsigned_id(values[0], f"{where} ID")
                room_index(values[1], f"{where} room index")
                coordinates(values, range(2, 5), where)
                validate_signed_short(values[5], f"{where} yaw")
            elif kind == "transition":
                validate_unsigned_id(values[0], f"{where} ID")
                validate_unsigned_id(values[1], f"{where} spawn ID")
                validate_location(values[2], f"{where} location")
            elif kind == "entity":
                validate_entity_stable_id(values[0], f"{where} stable ID")
                validate_unsigned_id(values[1], f"{where} type ID")
                coordinates(values, range(2, 5), where)
                validate_unsigned_id(values[5], f"{where} health")
                validate_unsigned_id(values[6], f"{where} faction ID")
                validate_unsigned_id(values[7], f"{where} sprite ID")
                validate_unsigned_id(values[8], f"{where} aux ID")

    portal_rows = level.records("portal")
    for index, portal in enumerate(portal_rows):
        reverse = _integer((portal.values or [])[9], f"portal {index + 1} reverse index")
        if reverse >= 0:
            linked_reverse = _integer((portal_rows[reverse].values or [])[9],
                                      f"portal {reverse + 1} reverse index")
            if linked_reverse != index:
                raise LevelError("portal reverse link is not bidirectional")


def serialize_level(level: Level) -> str:
    # Structural editors mutate records, so counts are derived data. Keep only
    # the explicitly authored entity capacity and refresh the eight row counts.
    count_line = level.records("counts")[0]
    capacity = count_line.values[8]
    count_line.values = [str(len(level.records(kind))) for kind in KINDS] + [capacity]
    validate_level(level)
    counts = [len(level.records(kind)) for kind in KINDS]
    capacity = _integer(capacity, "capacity count")
    output = []
    for line in level.lines:
        suffix = " " + line.comment if line.comment else ""
        if line.kind is None:
            output.append(line.raw)
        elif line.kind == "MXL2":
            output.append("MXL2" + suffix)
        elif line.kind == "environment":
            output.append("environment " + " ".join(line.values or []) + suffix)
        elif line.kind == "counts":
            output.append("counts " + " ".join(map(str, counts + [capacity])) + suffix)
        else:
            output.append(line.kind + " " + " ".join(line.values or []) + suffix)
    return "\n".join(output) + "\n"


def load_level(project: Project, path):
    return parse_level_text(project.read_text(path))


def save_level(project: Project, path, level):
    target = project.path(path)
    if not (target.name == "level.txt" and target.parent.parent.name == "levels"
            and target.parent.parent.parent.name == "res"):
        raise LevelError("Levels must be stored as res/levels/<location>/level.txt")
    project.atomic_write(path, serialize_level(level))
