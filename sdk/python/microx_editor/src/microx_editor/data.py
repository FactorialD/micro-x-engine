"""Lossless gameplay-table parser plus converter-compatible validation."""
from dataclasses import dataclass
from .io import Project
class DataError(ValueError): pass
@dataclass
class Row: id:int; key:str; description:str; meta:str=""
@dataclass
class DataLine: raw:str; row:Row|None=None; had_meta:bool=False
@dataclass
class Table:
    lines:list[DataLine]
    def rows(self): return [x.row for x in self.lines if x.row is not None]
def parse_data(text:str)->Table:
    lines=[]
    for no,raw in enumerate(text.splitlines(),1):
        if not raw.strip() or raw.lstrip().startswith("#"): lines.append(DataLine(raw)); continue
        p=raw.split("|")
        if len(p)<3 or len(p)>4: raise DataError(f"line {no}: expected id|key|description|meta")
        try: ident=int(p[0])
        except ValueError: raise DataError(f"line {no}: invalid stable ID")
        lines.append(DataLine(raw,Row(ident,p[1],p[2],p[3] if len(p)==4 else ""),len(p)==4))
    return Table(lines)
def serialize_data(table:Table)->str:
    out=[]
    for line in table.lines:
        if line.row is None: out.append(line.raw)
        else:
            r=line.row; out.append(f"{r.id}|{r.key}|{r.description}"+(f"|{r.meta}" if r.meta or line.had_meta else ""))
    return "\n".join(out)+"\n"
def validate_tables(tables:dict[str,Table]):
    allrefs=set()
    for name,t in tables.items():
        ids=set()
        for r in t.rows():
            if not 1<=r.id<=65535 or r.id in ids: raise DataError(f"{name}: duplicate or invalid stable ID {r.id}")
            ids.add(r.id); allrefs.add(f"{name}:{r.id}")
            if not 1<=len(r.key)<=32: raise DataError(f"{name}:{r.id}: key must be 1..32 characters")
            if len(r.description.encode("utf-8"))>240: raise DataError(f"{name}:{r.id}: description exceeds 240 UTF-8 bytes")
    def meta(r,key):
        for x in r.meta.split(","):
            if x.startswith(key+"="): return x.split("=",1)[1]
    for name,t in tables.items():
        for r in t.rows():
            for x in r.meta.split(","):
                if x.startswith("ref=") and x[4:] not in allrefs: raise DataError(f"{name}:{r.id}: unknown reference {x[4:]}")
        if name in ("dialogs","quests"):
            edge="next" if name=="dialogs" else "requires"; by={r.id:r for r in t.rows()}
            for start in t.rows():
                seen=set(); cur=start
                while cur:
                    if cur.id in seen: raise DataError(f"{name}:{start.id}: {edge} cycle")
                    seen.add(cur.id); nxt=meta(cur,edge)
                    if nxt is None: break
                    try: cur=by.get(int(nxt))
                    except ValueError: raise DataError(f"{name}:{start.id}: invalid {edge}")
                    if cur is None: raise DataError(f"{name}:{start.id}: unknown {edge} {nxt}")
    budget=16+sum(8+len(r.key.encode())+len(r.description.encode()) for t in tables.values() for r in t.rows())
    if budget>32768: raise DataError("gameplay tables exceed 32768-byte RecordStore budget")
def load_tables(project:Project):
    root=project.path("assets-src/data",existing=True); result={}
    old=list(root.rglob("*.data"))
    if old: raise DataError(f"legacy .data file is forbidden: {old[0]}")
    for p in sorted(root.rglob("*.txt")): result[p.stem]=parse_data(p.read_text(encoding="utf-8"))
    validate_tables(result); return result
def save_table(project:Project,path,table:Table,all_tables:dict[str,Table]):
    target=project.path(path)
    if target.suffix != ".txt": raise DataError("Gameplay data must use .txt")
    updated=dict(all_tables); updated[target.stem]=table; validate_tables(updated)
    project.atomic_write(target,serialize_data(table))
