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
 def test_level_roundtrip_and_links(self):
  s='MXL2\n# hi\ncounts 1 1 1 0 0 1 0 0 1\nroom 0 2 0 2\nfloor 0 0 2 0 2 0\nceiling 0 0 2 0 2 2\nspawn 1 0 1 0 1 0\n'
  self.assertEqual(serialize_level(parse_level_text(s)),s)
  with self.assertRaises(LevelError):parse_level_text(s.replace('counts 1','counts 2',1))
 def test_obj_contract(self):
  d=Path(tempfile.mkdtemp()); good=d/'a.obj'; good.write_text('v 0 0 0\nv 1 0 0\nv 0 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nf 1/1 2/2 3/3\n')
  self.assertEqual(validate_obj(good)['triangles'],1)
  good.write_text('v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n')
  with self.assertRaises(ObjError):validate_obj(good)
 def test_png_rejected(self):
  p=Path(tempfile.mktemp(suffix='.png')); p.write_bytes(b'not png')
  with self.assertRaises(AssetError):inspect_png(p)
if __name__=='__main__':unittest.main()
