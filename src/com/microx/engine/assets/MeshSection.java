package com.microx.engine.assets;

/** Compact immutable structure-of-arrays mesh section belonging to one room. */
public final class MeshSection {
    private final int room,texture; private final int[] xyz,uv; private final short[] indices;
    public MeshSection(int roomId,int textureId,int[] positions,int[] coordinates,short[] triangleIndices){room=roomId;texture=textureId;xyz=positions;uv=coordinates;indices=triangleIndices;}
    public int room(){return room;} public int texture(){return texture;} public int vertexCount(){return xyz.length/3;} public int triangleCount(){return indices.length/3;}
    public int x(int i){return xyz[i*3];}public int y(int i){return xyz[i*3+1];}public int z(int i){return xyz[i*3+2];}
    public int u(int i){return uv[i*2];}public int v(int i){return uv[i*2+1];}public int index(int i){return indices[i]&0xffff;}
}
