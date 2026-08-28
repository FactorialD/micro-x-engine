"""Lossless gameplay-table parser and AssetConverter-compatible validation."""
from dataclasses import dataclass
from .io import Project

class DataError(ValueError): pass

MAX_TABLES=16; MAX_ROWS=256; MAX_TOTAL_ROWS=1024
MAX_PACKED_BYTES=32768; MAX_MODIFIED_UTF_BYTES=65535
COMMON_ITEM_FIELDS={"type":None,"cells":(1,64),"stack":(1,32767),"value":(0,32767)}
ITEM_SCHEMAS={
 "weapon":{"ammo":(1,65535),"magazine":(1,255),"damage":(1,32767),
           "range":(1,32767),"cooldown":(1,32767),"reload":(1,32767),
           "spread":(0,90),"durability":(1,100)},
 "armor":{"physical":(-100,100),"anomaly":(-100,100),"radiation":(-100,100)},
 "artifact":{"physical":(-100,100),"anomaly":(-100,100),"radiation":(-100,100)},
 "consumable":{"health":(0,100),"bleeding":(0,100),"radiation":(-100,100)},
 "ammo":{"damageBonus":(-1000,1000)},
 "bolt":{},
 "detector":{},
}
TABLE_METADATA={"dialogs":{"next","ref"},"quests":{"requires","ref"},"npcs":{"ref"}}

def metadata_fields(table:str, values=None):
 """Return the declarative metadata fields used by both validation and the UI."""
 values=values or {}
 if table=="items":
  kind=values.get("type","")
  return tuple(COMMON_ITEM_FIELDS)+tuple(ITEM_SCHEMAS.get(kind,{}))
 return tuple(sorted(TABLE_METADATA.get(table,set())))

def serialize_metadata(values:dict[str,str], fields=None):
 """Serialize populated form values without inventing empty metadata tokens."""
 order=fields or values.keys()
 return ",".join(f"{key}={values[key]}" for key in order if values.get(key,"")!="")

@dataclass
class Row: id:int; key:str; description:str; meta:str=""
@dataclass
class DataLine: raw:str; row:Row|None=None; had_meta:bool=False; number:int=0
@dataclass
class Table:
 lines:list[DataLine]
 source:str="<memory>"
 def rows(self): return [x.row for x in self.lines if x.row is not None]
 def row_lines(self): return [x for x in self.lines if x.row is not None]

def parse_data(text:str,source="<memory>")->Table:
 lines=[]
 for no,raw in enumerate(text.splitlines(),1):
  if not raw.strip() or raw.lstrip().startswith("#"): lines.append(DataLine(raw,number=no)); continue
  p=raw.split("|")
  if len(p)<3 or len(p)>4: raise DataError(f"line {no}: record: expected id|key|description|meta")
  try: ident=int(p[0])
  except ValueError: raise DataError(f"line {no}: id: invalid stable ID") from None
  lines.append(DataLine(raw,Row(ident,p[1],p[2],p[3] if len(p)==4 else ""),len(p)==4,no))
 return Table(lines,source)

def serialize_data(table:Table)->str:
 out=[]
 for line in table.lines:
  if line.row is None: out.append(line.raw)
  else:
   r=line.row; out.append(f"{r.id}|{r.key}|{r.description}"+(f"|{r.meta}" if r.meta or line.had_meta else ""))
 return "\n".join(out)+"\n"

def parse_metadata(value:str,where="metadata"):
 result={}
 if not value:return result
 for token in value.split(","):
  at=token.find("=")
  if at<1 or at==len(token)-1: raise DataError(f"{where}: invalid metadata token {token}")
  key,val=token[:at],token[at+1:]
  if key in result: raise DataError(f"{where}: duplicate metadata key {key}")
  result[key]=val
 return result

def _number(values,key,limits,where):
 if key not in values: raise DataError(f"{where}: {key}: missing required metadata")
 try:value=int(values[key])
 except ValueError: raise DataError(f"{where}: {key}: invalid integer") from None
 if not limits[0]<=value<=limits[1]: raise DataError(f"{where}: {key}: value must be in {limits[0]}..{limits[1]}")
 return value

def validate_tables(tables:dict[str,Table]):
 if len(tables)>MAX_TABLES: raise DataError(f"gameplay table count exceeds {MAX_TABLES}")
 total=sum(len(t.rows()) for t in tables.values())
 parsed={}; ids_by_table={}
 for name,t in tables.items():
  modified_utf_size(name,f"table {name}: name")
  if len(t.rows())>MAX_ROWS: raise DataError(f"table {name} exceeds {MAX_ROWS} rows")
  ids=set(); keys=set(); ids_by_table[name]={}
  for ordinal,line in enumerate(t.row_lines(),1):
   r=line.row; where=f"{t.source}:line {line.number or ordinal}:{name}:record {r.id}"
   if not 1<=r.id<=65535 or r.id in ids: raise DataError(f"{where}: id: duplicate or invalid stable ID")
   ids.add(r.id); ids_by_table[name][r.id]=r
   if not r.key or r.key in keys: raise DataError(f"{where}: key: duplicate or empty stable key")
   keys.add(r.key)
   if len(r.key)>32: raise DataError(f"{where}: key: must be 1..32 characters")
   if len(r.description.encode('utf-8'))>240: raise DataError(f"{where}: description: exceeds 240 UTF-8 bytes")
   parsed[name,r.id]=parse_metadata(r.meta,where+":metadata")
 if total>MAX_TOTAL_ROWS: raise DataError(f"gameplay row count exceeds {MAX_TOTAL_ROWS}")
 items=ids_by_table.get("items")
 if not items: raise DataError("items: required table is missing or empty")
 for ident,row in items.items():
  where=f"items:record {ident}"; values=parsed["items",ident]
  for key,limits in COMMON_ITEM_FIELDS.items():
   if key not in values: raise DataError(f"{where}: {key}: missing required metadata")
   if limits:_number(values,key,limits,where)
  kind=values["type"]
  if kind not in ITEM_SCHEMAS: raise DataError(f"{where}: type: unknown item type {kind}")
  schema=ITEM_SCHEMAS[kind]
  for key,limits in schema.items():_number(values,key,limits,where)
  unknown=set(values)-set(COMMON_ITEM_FIELDS)-set(schema)
  if unknown: raise DataError(f"{where}: metadata: unknown metadata key {sorted(unknown)[0]}")
 for ident,row in items.items():
  values=parsed["items",ident]
  if values["type"]=="weapon":
   ammo=int(values["ammo"])
   if ammo not in items: raise DataError(f"items:record {ident}: ammo: unknown ammo item {ammo}")
   if parsed["items",ammo].get("type")!="ammo": raise DataError(f"items:record {ident}: ammo: does not reference an ammo item")
 allrefs={f"{name}:{ident}" for name,rows in ids_by_table.items() for ident in rows}
 for name,t in tables.items():
  if name=="items":continue
  allowed=TABLE_METADATA.get(name,set())
  for r in t.rows():
   where=f"{name}:record {r.id}"; values=parsed[name,r.id]
   unknown=set(values)-allowed
   if unknown:raise DataError(f"{where}: metadata: unknown metadata key {sorted(unknown)[0]}")
   for edge in ("next","requires"):
    if edge in values:_number(values,edge,(1,65535),where)
   if "ref" in values and values["ref"] not in allrefs:raise DataError(f"{where}: ref: unknown reference {values['ref']}")
 for name,edge in (("dialogs","next"),("quests","requires")):
  rows=ids_by_table.get(name,{})
  for start in rows:
   seen=set(); cur=start
   while cur in rows:
    if cur in seen:raise DataError(f"{name}:record {start}: {edge}: cycle")
    seen.add(cur); value=parsed[name,cur].get(edge)
    if value is None:break
    cur=int(value)
    if cur not in rows:
     if name=='quests':raise DataError(f"{name}:record {start}: {edge}: unknown {edge} {cur}")
     break
 budget=5
 for name,t in tables.items():
  budget+=modified_utf_size(name,f"table {name}: name")+2
  for r in t.rows():
   budget+=2+modified_utf_size(r.key,f"{name}:{r.id}: key")+modified_utf_size(r.description,f"{name}:{r.id}: description")+modified_utf_size(r.meta,f"{name}:{r.id}: metadata")
 if budget>MAX_PACKED_BYTES:raise DataError(f"gameplay tables exceed {MAX_PACKED_BYTES}-byte RecordStore budget (serialized size {budget})")

def modified_utf_size(value:str,label="string"):
 size=0
 for c in value:
  n=ord(c); size += 2 if n==0 else 1 if n<=0x7f else 2 if n<=0x7ff else 3 if n<=0xffff else 6
  if size>MAX_MODIFIED_UTF_BYTES:raise DataError(f"{label} exceeds modified UTF-8 limit of {MAX_MODIFIED_UTF_BYTES} bytes")
 return size+2

def load_tables(project:Project):
 root=project.path("res/data",existing=True); result={}; old=list(root.rglob("*.data"))
 if old:raise DataError(f"legacy .data file is forbidden: {old[0]}")
 paths={}
 for p in sorted(root.rglob("*.txt")):
  if p.stem in paths:raise DataError(f"duplicate table name {p.stem}: {paths[p.stem]} and {p}")
  paths[p.stem]=p; result[p.stem]=parse_data(p.read_text(encoding="utf-8"),str(p))
 validate_tables(result); return result

def save_table(project,path,table,all_tables):
 target=project.path(path)
 if target.suffix!=".txt":raise DataError("Gameplay data must use .txt")
 updated=dict(all_tables);updated[target.stem]=table;validate_tables(updated)
 project.atomic_write(target,serialize_data(table))
