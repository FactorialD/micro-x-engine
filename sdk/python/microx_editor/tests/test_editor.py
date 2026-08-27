import sys
sys.path.insert(0, str(__import__("pathlib").Path(__file__).parents[1] / "src"))
import base64,tempfile,unittest
from pathlib import Path
from microx_editor.io import Project,ProjectError
from microx_editor.data import *
from microx_editor.level import *
from microx_editor.obj import validate_obj,ObjError
from microx_editor.images import inspect_png,AssetError
from microx_editor.app import resolve_unsaved
class Tests(unittest.TestCase):
 def level_text(self, counts=(1,1,1,0,0,1,0,0,1), replacements=None):
  n=list(counts); rows=['MXL2','counts '+' '.join(map(str,n))]
  samples={
   'room':'room 0 1 0 1', 'floor':'floor 0 0 1 0 1 0',
   'ceiling':'ceiling 0 0 1 0 1 1', 'edge':'edge 0 0 0 1 0 0 1',
   'portal':'portal 0 0 0 0 1 0 1 0 1 -1 -1',
   'spawn':'spawn 0 0 0 0 0 0', 'transition':'transition 0 0 level_1',
   'entity':'entity 0 0 0 0 0 0'}
  replacements=replacements or {}
  for kind,count in zip(KINDS,n[:8]):
   rows.extend([replacements.get(kind,samples[kind])]*count)
  return '\n'.join(rows)+'\n'
 def project(self):
  d=Path(tempfile.mkdtemp()); (d/'assets-src/levels').mkdir(parents=True); (d/'assets-src/data').mkdir(); return Project(d)
 def test_confined_atomic_utf8(self):
  p=self.project(); p.atomic_write('assets-src/data/x.txt','привіт')
  self.assertEqual(p.read_text('assets-src/data/x.txt'),'привіт')
  with self.assertRaises(ProjectError):p.atomic_write('../escape','x')
 def test_data_roundtrip_comments_and_validation(self):
  s='# c\n\n1|ключ|Опис|type=ammo,cells=1,stack=1,value=0,damageBonus=0\n'; t=parse_data(s); self.assertEqual(serialize_data(t),s); validate_tables({'items':t})
  with self.assertRaises(DataError):validate_tables({'items':parse_data('1|a|A\n1|b|B\n')})
  with self.assertRaises(DataError):validate_tables({'dialogs':parse_data('1|a|A|next=2\n2|b|B|next=1\n')})
 def test_gameplay_format_limits_and_modified_utf(self):
  row=lambda i:DataLine('',Row(i,f'k{i}','d'))
  table=lambda n:Table([row(i) for i in range(1,n+1)])
  with self.assertRaises(DataError):validate_tables({f't{i}':Table([]) for i in range(17)})
  with self.assertRaises(DataError):validate_tables({'items':table(257)})
  with self.assertRaises(DataError):validate_tables({f't{i}':table(205) for i in range(5)})
  with self.assertRaises(DataError):parse_data('1|key|description|meta|extra\n')
  self.assertEqual(modified_utf_size('\0'),4)
  self.assertEqual(modified_utf_size('é'),4)
  self.assertEqual(modified_utf_size('€'),5)
  self.assertEqual(modified_utf_size('😀'),8)
  with self.assertRaisesRegex(DataError,'modified UTF-8'):modified_utf_size('x'*65536)
 def test_duplicate_gameplay_basename_reports_both_paths(self):
  p=self.project(); a=p.path('assets-src/data/a/same.txt'); b=p.path('assets-src/data/b/same.txt')
  a.parent.mkdir(); b.parent.mkdir(); a.write_text('1|a|A\n'); b.write_text('2|b|B\n')
  with self.assertRaises(DataError) as error:load_tables(p)
  self.assertIn(str(a),str(error.exception)); self.assertIn(str(b),str(error.exception))

 def test_converter_metadata_parity_table(self):
  base="type=ammo,cells=1,stack=1,value=0,damageBonus=0"
  def check(meta, message, extra=""):
   tables={"items":parse_data(f"1|ammo|Ammo|{base}\n2|case|Case|{meta}\n"+extra)}
   with self.assertRaisesRegex(DataError,message): validate_tables(tables)
  cases=(("type=ammo,cells=1,stack=1,value=0,damageBonus=0,bad=1","unknown metadata"),
         ("type=ammo,cells=1,stack=1,value=0,damageBonus","invalid metadata token"),
         ("type=ammo,cells=1,stack=1,value=0,damageBonus=0,value=1","duplicate metadata"),
         ("type=ammo,cells=1,stack=1,value=0,damageBonus=","invalid metadata token"),
         ("type=weapon,cells=1,stack=1,value=0","missing required"),
         ("type=weapon,cells=1,stack=1,value=0,ammo=99,magazine=1,damage=1,range=1,cooldown=1,reload=1,spread=0,durability=1","unknown ammo"))
  for meta,message in cases:
   with self.subTest(meta=meta):check(meta,message)
  with self.assertRaisesRegex(DataError,'duplicate or empty stable key'):
   validate_tables({'items':parse_data(f'1|same|A|{base}\n2|same|B|{base}\n')})
  with self.assertRaisesRegex(DataError,'unknown metadata'):
   validate_tables({'items':parse_data(f'1|a|A|{base}\n'),'other':parse_data('1|x|X|ref=items:1\n')})
 def test_item_schema_boundaries(self):
  common='cells=1,stack=1,value=0'
  ammo='1|ammo|Ammo|type=ammo,'+common+',damageBonus=0\n'
  for kind,schema in ITEM_SCHEMAS.items():
   for field,(low,high) in schema.items():
    if kind=='weapon' and field=='ammo':continue
    for value in (low,high):
     values={k:str(bounds[0]) for k,bounds in schema.items()};values[field]=str(value)
     if kind=='weapon':values['ammo']='1'
     meta='type='+kind+','+common+','+','.join(f'{k}={v}' for k,v in values.items())
     with self.subTest(kind=kind,field=field,value=value):validate_tables({'items':parse_data(ammo+f'2|x|X|{meta}\n')})
    for value in (low-1,high+1):
     values={k:str(bounds[0]) for k,bounds in schema.items()};values[field]=str(value)
     if kind=='weapon':values['ammo']='1'
     meta='type='+kind+','+common+','+','.join(f'{k}={v}' for k,v in values.items())
     with self.subTest(kind=kind,field=field,value=value),self.assertRaises(DataError):validate_tables({'items':parse_data(ammo+f'2|x|X|{meta}\n')})
  non_ammo='2|not_ammo|N|type=consumable,cells=1,stack=1,value=0,health=0,bleeding=0,radiation=0\n'
  weapon='3|gun|G|type=weapon,cells=1,stack=1,value=0,ammo=2,magazine=1,damage=1,range=1,cooldown=1,reload=1,spread=0,durability=1\n'
  with self.assertRaisesRegex(DataError,'does not reference'):validate_tables({'items':parse_data(ammo+non_ammo+weapon)})


 def test_structured_count_update_and_unsaved_guard(self):
  level=parse_level_text(self.level_text())
  floor=level.records('floor')[0]
  level.lines.insert(level.lines.index(floor)+1,Line('', 'floor', list(floor.values)))
  serialized=serialize_level(level)
  self.assertIn('counts 1 2 1 0 0 1 0 0 1',serialized)
  saved=[]
  self.assertFalse(resolve_unsaved(True,None,lambda:saved.append(1)))
  self.assertTrue(resolve_unsaved(True,False,lambda:saved.append(1)));self.assertEqual(saved,[])
  self.assertTrue(resolve_unsaved(True,True,lambda:saved.append(1)));self.assertEqual(saved,[1])

 def test_level_roundtrip_and_links(self):
  s='MXL2\n# hi\ncounts 1 1 1 0 0 1 0 0 1\nroom 0 2 0 2\nfloor 0 0 2 0 2 0\nceiling 0 0 2 0 2 2\nspawn 1 0 1 0 1 0\n'
  self.assertEqual(serialize_level(parse_level_text(s)),s)
  with self.assertRaises(LevelError):parse_level_text(s.replace('counts 1','counts 2',1))
 def test_level_count_boundaries_match_converter(self):
  limits=((1,256),(1,1024),(1,1024),(0,2048),(0,1024),(1,256),(0,256),(0,1024),(1,1024))
  baseline=[1,1,1,0,0,1,0,0,1]
  for index,(low,high) in enumerate(limits):
   for value in (low,high):
    counts=baseline.copy(); counts[index]=value
    if index==7: counts[8]=max(counts[8],value)
    if index==8 and counts[7]>value: counts[7]=value
    with self.subTest(field=index,value=value): parse_level_text(self.level_text(counts))
   for value in (low-1,high+1):
    counts=baseline.copy(); counts[index]=value
    with self.subTest(field=index,value=value),self.assertRaises(LevelError):
     parse_level_text(self.level_text(counts))
 def test_level_capacity_and_portal_link_boundaries(self):
  with self.assertRaisesRegex(LevelError,'exceeds capacity'):
   parse_level_text(self.level_text((1,1,1,0,0,1,0,2,1)))
  valid='portal 0 0 0 0 1 0 1 0 1 -1 -1'
  parse_level_text(self.level_text((1,1,1,0,1,1,0,0,1),{'portal':valid}))
  cases=(
   ('portal 0 0 0 0 1 0 1 0 1 -2 -1',(1,1,1,0,1,1,0,0,1),'reverse index'),
   ('portal 0 0 0 0 1 0 1 0 1 1 -1',(1,1,1,0,1,1,0,0,1),'reverse index'),
   ('portal 0 0 0 0 1 0 1 0 1 -1 -2',(1,1,1,0,1,1,0,0,1),'transition index'),
   ('portal 0 0 0 0 1 0 1 0 1 -1 1',(1,1,1,0,1,1,1,0,1),'transition index'))
  for portal,counts,message in cases:
   with self.subTest(portal=portal),self.assertRaisesRegex(LevelError,message):
    parse_level_text(self.level_text(counts,{'portal':portal}))
  linked=self.level_text((1,1,1,0,2,1,0,0,1),
                         {'portal':'portal 0 0 0 0 1 0 1 0 1 0 -1'})
  with self.assertRaisesRegex(LevelError,'bidirectional'): parse_level_text(linked)
 def test_level_header_contract(self):
  good=self.level_text()
  invalid=(good.replace('MXL2\n','MXL2\nMXL2\n'),
           good.replace('counts ','counts 1 1 1 0 0 1 0 0 1\ncounts ',1),
           good.replace('MXL2\ncounts 1 1 1 0 0 1 0 0 1\n','counts 1 1 1 0 0 1 0 0 1\nMXL2\n'),
           good.replace('MXL2\n','MXL2\nroom 0 1 0 1\n'))
  for text in invalid:
   with self.subTest(text=text[:30]),self.assertRaises(LevelError): parse_level_text(text)
 def test_level_numeric_and_location_fields(self):
  valid_rows={
   'room':('room -32768 32767 -32768 32767',),
   'spawn':('spawn 65535 0 -32768 32767 0 -32768',),
   'transition':('transition 65535 65535 '+'a'*64,),
   'entity':('entity 65535 65535 -32768 32767 0 65535',)}
  for kind,(row,) in valid_rows.items():
   counts=[1,1,1,0,0,1,0,0,1]; counts[KINDS.index(kind)]=1
   with self.subTest(kind=kind): parse_level_text(self.level_text(counts,{kind:row}))
  invalid_rows={
   'room':('room -32769 1 0 1','room 0 32768 0 1','room nope 1 0 1'),
   'spawn':('spawn -1 0 0 0 0 0','spawn 0 0 0 0 0 32768'),
   'transition':('transition 0 0 bad/location','transition 0 0 '+('a'*65)),
   'entity':('entity 65536 0 0 0 0 0','entity 0 -1 0 0 0 0','entity 0 0 0 0 0 65536')}
  for kind,rows in invalid_rows.items():
   for row in rows:
    counts=[1,1,1,0,0,1,0,0,1]; counts[KINDS.index(kind)]=1
    with self.subTest(kind=kind,row=row),self.assertRaises(LevelError):
     parse_level_text(self.level_text(counts,{kind:row}))
 def test_serialize_rejects_capacity_instead_of_repairing_it(self):
  level=parse_level_text(self.level_text((1,1,1,0,0,1,0,1,1)))
  level.records('counts')[0].values[8]='0'
  with self.assertRaisesRegex(LevelError,'capacity count'): serialize_level(level)
 def test_obj_contract(self):
  d=Path(tempfile.mkdtemp()); good=d/'a.obj'; good.write_text('v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nf 1/1 2/2 3/3\n')
  self.assertEqual(validate_obj(good)['triangles'],1)
  good.write_text('v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n')
  with self.assertRaises(ObjError):validate_obj(good)
 def test_png_rejected(self):
  p=Path(tempfile.mktemp(suffix='.png')); p.write_bytes(b'not png')
  with self.assertRaises(AssetError):inspect_png(p)
if __name__=='__main__':unittest.main()
