package com.microx.engine.assets;

/** Compact immutable structure-of-arrays mesh section belonging to one room. */
public final class MeshSection {
    private final int room, texture, color;
    private final int[] xyz, uv;
    private final short[] indices;
    public MeshSection(int roomId, int textureId, int[] positions, int[] coordinates,
            short[] triangleIndices) {
        this(roomId, textureId, 0xff00ff, positions, coordinates, triangleIndices);
    }
    public MeshSection(int roomId, int textureId, int fallbackColor, int[] positions,
            int[] coordinates, short[] triangleIndices) {
        if (roomId < 0 || positions == null || coordinates == null || triangleIndices == null
                || positions.length == 0 || positions.length % 3 != 0
                || coordinates.length != positions.length / 3 * 2 || triangleIndices.length == 0
                || triangleIndices.length % 3 != 0)
            throw new IllegalArgumentException("invalid mesh section");
        int vertices = positions.length / 3;
        for (int i = 0; i < triangleIndices.length; i++)
            if ((triangleIndices[i] & 65535) >= vertices)
                throw new IllegalArgumentException("mesh index out of range");
        room = roomId;
        texture = textureId;
        color = fallbackColor & 0xffffff;
        xyz = positions;
        uv = coordinates;
        indices = triangleIndices;
    }
    public int room() {
        return room;
    }
    public int texture() {
        return texture;
    }
    public int color() {
        return color;
    }
    public int vertexCount() {
        return xyz.length / 3;
    }
    public int triangleCount() {
        return indices.length / 3;
    }
    public int x(int i) {
        return xyz[i * 3];
    }
    public int y(int i) {
        return xyz[i * 3 + 1];
    }
    public int z(int i) {
        return xyz[i * 3 + 2];
    }
    public int u(int i) {
        return uv[i * 2];
    }
    public int v(int i) {
        return uv[i * 2 + 1];
    }
    public int index(int i) {
        return indices[i] & 0xffff;
    }
    public int memoryBytes() {
        return 24 + xyz.length * 4 + uv.length * 4 + indices.length * 2;
    }
}
