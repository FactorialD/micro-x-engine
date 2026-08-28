"""Dependency-free PNG contract inspection."""
import struct, zlib
from pathlib import Path
from .io import Project
PNG = b"\x89PNG\r\n\x1a\n"
class AssetError(ValueError): pass

def inspect_png(path: str | Path) -> dict[str, int]:
    data = Path(path).read_bytes()
    if not data.startswith(PNG): raise AssetError("Not a PNG file")
    pos=8; width=height=color_type=bit_depth=None; palette=0; chunks=[]
    while pos + 12 <= len(data):
        size=struct.unpack(">I", data[pos:pos+4])[0]; kind=data[pos+4:pos+8]; body=data[pos+8:pos+8+size]
        if len(body)!=size: raise AssetError("Truncated PNG")
        if kind==b"IHDR": width,height,bit_depth,color_type=struct.unpack(">IIBB", body[:10])
        elif kind==b"PLTE": palette=size//3
        elif kind==b"IDAT": chunks.append(body)
        pos += 12+size
    if not width or not height: raise AssetError("PNG has no valid IHDR")
    if width>256 or height>256: raise AssetError("atlas exceeds 256x256")
    if color_type == 3:
        if bit_depth not in (1,2,4,8): raise AssetError("unsupported indexed PNG depth")
        colors=palette
    elif color_type in (0,2,6) and bit_depth == 8:
        channels={0:1,2:3,6:4}[color_type]; stride=width*channels
        try: raw=zlib.decompress(b"".join(chunks))
        except zlib.error as e: raise AssetError("invalid compressed PNG data") from e
        rows=[]; previous=bytearray(stride); offset=0
        for _ in range(height):
            method=raw[offset]; scan=bytearray(raw[offset+1:offset+1+stride]); offset += stride+1
            for i in range(stride):
                a=scan[i-channels] if i>=channels else 0; b=previous[i]; c=previous[i-channels] if i>=channels else 0
                if method==1: scan[i]=(scan[i]+a)&255
                elif method==2: scan[i]=(scan[i]+b)&255
                elif method==3: scan[i]=(scan[i]+((a+b)//2))&255
                elif method==4:
                    q=a+b-c; pa=abs(q-a); pb=abs(q-b); pc=abs(q-c); scan[i]=(scan[i]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
                elif method!=0: raise AssetError("unsupported PNG filter")
            rows.append(scan); previous=scan
        rgb=set()
        for row in rows:
            for x in range(width):
                q=row[x*channels:(x+1)*channels]
                rgb.add((q[0],q[0],q[0]) if channels==1 else tuple(q[:3]))
                if len(rgb)>256: raise AssetError("atlas has more than 256 colors")
        colors=len(rgb)
    else: raise AssetError("unsupported PNG color type/depth")
    if not 1 <= colors <= 256: raise AssetError("atlas must contain 1..256 colors")
    footprint=16 + colors*4 + width*height
    if footprint > 96*1024: raise AssetError("runtime footprint exceeds 96 KiB")
    return {"width":width,"height":height,"colors":colors,"footprint":footprint}

def replace_atlas(project: Project, source: str|Path, destination="assets-src/textures.png") -> dict[str,int]:
    target=project.path(destination)
    if target.name != "textures.png": raise AssetError("AssetConverter only accepts atlas textures.png")
    info=inspect_png(source); data=Path(source).read_bytes()
    # Binary counterpart of atomic UTF-8 write.
    import os, tempfile
    fd,name=tempfile.mkstemp(prefix=".textures.",suffix=".tmp",dir=target.parent)
    try:
        with os.fdopen(fd,"wb") as f: f.write(data); f.flush(); os.fsync(f.fileno())
        os.replace(name,target)
    except BaseException:
        try: os.unlink(name)
        except FileNotFoundError: pass
        raise
    return info
