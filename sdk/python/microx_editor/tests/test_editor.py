import sys
sys.path.insert(0, str(__import__("pathlib").Path(__file__).parents[1] / "src"))
import base64,tempfile,unittest
from pathlib import Path
from microx_editor.io import Project,ProjectError
from microx_editor.data import *
from microx_editor.level import *
from microx_editor.obj import validate_obj,ObjError
from microx_editor.images import inspect_png,AssetError
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
  s='# c\n\n1|ключ|Опис|\n'; t=parse_data(s); self.assertEqual(serialize_data(t),s); validate_tables({'items':t})
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
  huge=Table([DataLine('',Row(1,'key','description','x'*65536))])
  with self.assertRaisesRegex(DataError,'modified UTF-8'):validate_tables({'items':huge})
 def test_duplicate_gameplay_basename_reports_both_paths(self):
  p=self.project(); a=p.path('assets-src/data/a/same.txt'); b=p.path('assets-src/data/b/same.txt')
  a.parent.mkdir(); b.parent.mkdir(); a.write_text('1|a|A\n'); b.write_text('2|b|B\n')
  with self.assertRaises(DataError) as error:load_tables(p)
  self.assertIn(str(a),str(error.exception)); self.assertIn(str(b),str(error.exception))
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
