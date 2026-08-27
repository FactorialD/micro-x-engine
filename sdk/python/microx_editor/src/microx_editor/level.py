"""Comment-preserving MXL2 level parser/serializer."""
from dataclasses import dataclass
from pathlib import Path
from .io import Project
class LevelError(ValueError): pass
KINDS=("room","floor","ceiling","edge","portal","spawn","transition","entity")
ARITY={"room":4,"floor":6,"ceiling":6,"edge":7,"portal":11,"spawn":6,"transition":3,"entity":6}
@dataclass
class Line:
    raw:str; kind:str|None=None; values:list[str]|None=None; comment:str=""
@dataclass
class Level:
    lines:list[Line]
    def records(self,kind): return [x for x in self.lines if x.kind==kind]

def parse_level_text(text:str)->Level:
    lines=[]; seen_header=False; declared=None
    for no,raw in enumerate(text.splitlines(),1):
        code,sep,comment=raw.partition("#"); p=code.split()
        if not p: lines.append(Line(raw,comment=("#"+comment if sep else ""))); continue
        if p[0]=="MXL2": seen_header=True; lines.append(Line(raw,"MXL2",[],"#"+comment if sep else "")); continue
        if p[0]=="counts":
            if len(p)!=10: raise LevelError(f"line {no}: counts needs 9 values")
            try: declared=[int(x) for x in p[1:]]
            except ValueError: raise LevelError(f"line {no}: invalid declared count")
            lines.append(Line(raw,"counts",p[1:],"#"+comment if sep else "")); continue
        kind=p[0]
        if kind not in KINDS: raise LevelError(f"line {no}: unknown record {kind}")
        if len(p)-1 != ARITY[kind]: raise LevelError(f"line {no}: {kind} needs {ARITY[kind]} fields")
        lines.append(Line(raw,kind,p[1:],"#"+comment if sep else ""))
    if not seen_header or declared is None: raise LevelError("MXL2 header and counts are required")
    level=Level(lines); validate_level(level,declared); return level

def validate_level(level:Level,declared=None):
    counts=[len(level.records(k)) for k in KINDS]
    if declared and counts != declared[:8]: raise LevelError(f"declared counts {declared[:8]} do not match records {counts}")
    rooms=counts[0]; portals=level.records("portal")
    def integer(v,what):
        try: n=int(v)
        except ValueError: raise LevelError(f"invalid integer in {what}")
        if n < -32767 or n > 32767: raise LevelError(f"Q16.16 overflow in {what}")
        return n
    for kind in KINDS:
        for row in level.records(kind):
            vals=row.values or []
            # Coordinate fields are integers in the converter's source contract.
            coord={"room":range(4),"floor":range(1,6),"ceiling":range(1,6),"edge":range(1,7),"portal":range(3,9),"spawn":range(2,5),"entity":range(2,5)}.get(kind,())
            for i in coord: integer(vals[i],kind)
            if kind in ("floor","ceiling","edge","spawn") and not 0<=int(vals[0 if kind!="spawn" else 1])<rooms: raise LevelError(f"invalid room reference in {kind}")
            if kind=="portal" and (not 0<=int(vals[1])<rooms or not 0<=int(vals[2])<rooms): raise LevelError("invalid portal room reference")
            bounds=(vals if kind=="room" else vals[1:])
            if kind in ("room","floor","ceiling") and (int(bounds[0])>int(bounds[1]) or int(bounds[2])>int(bounds[3])): raise LevelError(f"unordered bounds in {kind}")
    for i,p in enumerate(portals):
        reverse=int(p.values[9])
        if reverse>=0 and (reverse>=len(portals) or int(portals[reverse].values[9])!=i): raise LevelError("portal reverse link is not bidirectional")

def serialize_level(level:Level)->str:
    validate_level(level)
    counts=[len(level.records(k)) for k in KINDS]
    out=[]
    for line in level.lines:
        if line.kind is None: out.append(line.raw)
        elif line.kind=="MXL2": out.append("MXL2"+(" "+line.comment if line.comment else ""))
        elif line.kind=="counts":
            capacity=max(counts[7],int(line.values[8]) if line.values and len(line.values)>8 else counts[7])
            out.append("counts "+" ".join(map(str,counts+[capacity]))+(" "+line.comment if line.comment else ""))
        else: out.append(line.kind+" "+" ".join(line.values or [])+(" "+line.comment if line.comment else ""))
    return "\n".join(out)+"\n"
def load_level(project:Project,path): return parse_level_text(project.read_text(path))
def save_level(project:Project,path,level):
    if project.path(path).suffix != ".level": raise LevelError("Generated .lvl files cannot be edited")
    project.atomic_write(path,serialize_level(level))
