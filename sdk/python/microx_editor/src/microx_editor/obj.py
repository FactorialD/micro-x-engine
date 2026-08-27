"""Validator for the OBJ subset consumed by AssetConverter.writeModel."""
from pathlib import Path
import math
from .io import Project
class ObjError(ValueError): pass

def validate_obj(path: str|Path) -> dict[str,int]:
    vertices=[]; uv=[]; faces=0; rooms=set(); materials={"default"}
    for no,raw in enumerate(Path(path).read_text(encoding="utf-8").splitlines(),1):
        line=raw.strip()
        if line.startswith("# microx room "):
            try: rooms.add(int(line[14:].strip()))
            except ValueError: raise ObjError(f"line {no}: invalid room")
            continue
        if line.startswith("# microx material "):
            p=line[18:].split()
            if len(p)!=2: raise ObjError(f"line {no}: material metadata needs name and texture id")
            materials.add(p[0]); continue
        line=line.split("#",1)[0].strip()
        if not line: continue
        p=line.split(); op=p[0]
        if op=="v":
            if len(p)<4: raise ObjError(f"line {no}: vertex needs x y z")
            try: xyz=tuple(float(x) for x in p[1:4])
            except ValueError: raise ObjError(f"line {no}: invalid vertex number")
            if not all(math.isfinite(x) and -32768 <= x < 32768 for x in xyz): raise ObjError(f"line {no}: Q16.16 overflow")
            vertices.append(xyz)
        elif op=="vt":
            if len(p)<3: raise ObjError(f"line {no}: texture coordinate needs u v")
            try: uv.append((float(p[1]),float(p[2])))
            except ValueError: raise ObjError(f"line {no}: invalid UV")
        elif op in ("o","g") and len(p)>1 and p[1].startswith("room_"):
            try: rooms.add(int(p[1][5:]))
            except ValueError: raise ObjError(f"line {no}: invalid room_N")
        elif op=="usemtl":
            if len(p)!=2: raise ObjError(f"line {no}: usemtl needs one name")
            materials.add(p[1])
        elif op=="f":
            if len(p)<4: raise ObjError(f"line {no}: face needs at least three corners")
            points=[]
            for corner in p[1:]:
                q=corner.split("/")
                if len(q)<2 or not q[1]: raise ObjError(f"line {no}: missing UV in face")
                try: vi=int(q[0]); ti=int(q[1])
                except ValueError: raise ObjError(f"line {no}: invalid OBJ index")
                vi=vi-1 if vi>0 else len(vertices)+vi; ti=ti-1 if ti>0 else len(uv)+ti
                if vi<0 or vi>=len(vertices) or ti<0 or ti>=len(uv): raise ObjError(f"line {no}: OBJ index out of range")
                points.append(vertices[vi])
            a,b,c=points[:3]; u=tuple(b[i]-a[i] for i in range(3)); v=tuple(c[i]-a[i] for i in range(3))
            cross=(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0])
            if cross==(0.0,0.0,0.0): raise ObjError(f"line {no}: degenerate polygon")
            faces += len(points)-2
    if not vertices or not faces: raise ObjError("OBJ has no renderable faces")
    return {"vertices":len(vertices),"uv":len(uv),"triangles":faces,"rooms":len(rooms),"materials":len(materials)}

def replace_obj(project:Project, source:str|Path, destination:str|Path):
    target=project.path(destination)
    if target.suffix != ".obj": raise ObjError("Generated .mesh files cannot be edited")
    result=validate_obj(source); project.atomic_write(target,Path(source).read_text(encoding="utf-8")); return result
