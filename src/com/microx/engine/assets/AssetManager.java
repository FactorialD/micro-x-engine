package com.microx.engine.assets;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Loads compact binary render data and keeps shared/location ownership separate. */
public final class AssetManager {
    private static final int MESH_MAGIC = 0x4d584d32, TEXTURE_MAGIC = 0x4d585432,
                             FORMAT_VERSION = 2, MAX_ASSET_BYTES = 768 * 1024;
    private static final TextureData MISSING_TEXTURE =
            new TextureData(1, 1, new int[] {0xff00ff}, new byte[] {0});
    private MeshSection[] sharedSections, locationSections;
    private TextureData[] sharedTextures, locationTextures;
    private String locationErrorPath;

    public boolean loadShared(String root) {
        MeshSection[] sections = readMesh(root + "/geometry.mesh");
        TextureData[] textures = readTextures(root + "/textures.tex");
        if (bytes(sections, textures) > MAX_ASSET_BYTES)
            throw new OutOfMemoryError("shared asset pack exceeds 768 KiB");
        releaseShared();
        sharedSections = sections;
        sharedTextures = textures;
        return sections != null || textures != null;
    }
    public boolean loadLocation(String name, int volume) {
        String root = "/levels/" + name;
        String meshPath = root + "/geometry.mesh";
        MeshSection[] sections = readMesh(meshPath);
        if (sections == null || sections.length == 0) {
            locationErrorPath = meshPath;
            return false;
        }
        TextureData[] textures = readTextures(root + "/textures.tex");
        if (bytes(sections, textures) > MAX_ASSET_BYTES)
            throw new OutOfMemoryError("location asset pack exceeds 768 KiB");
        unloadLocation();
        locationSections = sections;
        locationTextures = textures;
        locationErrorPath = null;
        return true;
    }
    public String locationErrorPath() {
        return locationErrorPath;
    }
    public int locationSectionCount() {
        return locationSections == null ? 0 : locationSections.length;
    }
    /** Resident counts are direct array-length reads and allocate no telemetry snapshots. */
    public int sharedSectionCount() {
        return sharedSections == null ? 0 : sharedSections.length;
    }
    public int locationTextureCount() {
        return locationTextures == null ? 0 : locationTextures.length;
    }
    public int sharedTextureCount() {
        return sharedTextures == null ? 0 : sharedTextures.length;
    }
    public int residentSectionCount() {
        return sharedSectionCount() + locationSectionCount();
    }
    public int residentTextureCount() {
        return sharedTextureCount() + locationTextureCount();
    }
    public MeshSection locationSection(int index) {
        return locationSections[index];
    }
    public int maximumLocationVertices() {
        int max = 0, i;
        if (locationSections != null)
            for (i = 0; i < locationSections.length; i++)
                if (locationSections[i].vertexCount() > max)
                    max = locationSections[i].vertexCount();
        return max;
    }
    public TextureData texture(int id) {
        if (id >= 0 && locationTextures != null && id < locationTextures.length)
            return locationTextures[id];
        int shared = -id - 1;
        if (shared >= 0 && sharedTextures != null && shared < sharedTextures.length)
            return sharedTextures[shared];
        return MISSING_TEXTURE;
    }
    public void unloadLocation() {
        locationSections = null;
        locationTextures = null;
    }
    public void releaseShared() {
        sharedSections = null;
        sharedTextures = null;
    }
    public void release() {
        unloadLocation();
        releaseShared();
    }
    public int residentBytes() {
        return bytes(sharedSections, sharedTextures) + bytes(locationSections, locationTextures);
    }
    private static int bytes(MeshSection[] sections, TextureData[] textures) {
        int n = 0, i;
        if (sections != null)
            for (i = 0; i < sections.length; i++) n += sections[i].memoryBytes();
        if (textures != null)
            for (i = 0; i < textures.length; i++) n += textures[i].memoryBytes();
        return n;
    }

    private MeshSection[] readMesh(String path) {
        InputStream raw = getClass().getResourceAsStream(path);
        if (raw == null)
            return null;
        DataInputStream in = new DataInputStream(raw);
        try {
            if (in.readInt() != MESH_MAGIC || in.readUnsignedShort() != FORMAT_VERSION)
                throw new IOException("unsupported MXM2 version");
            int sectionCount = in.readUnsignedShort();
            if (sectionCount < 1 || sectionCount > 4096)
                throw new IOException("invalid mesh section count");
            MeshSection[] result = new MeshSection[sectionCount];
            int used = 0, s;
            for (s = 0; s < sectionCount; s++) {
                int room = in.readUnsignedShort(), texture = in.readShort(),
                    vertices = in.readUnsignedShort(), triangles = in.readUnsignedShort();
                if (vertices < 3 || triangles < 1)
                    throw new IOException("empty mesh section");
                used += 24 + vertices * 20 + triangles * 6;
                if (used > MAX_ASSET_BYTES)
                    throw new OutOfMemoryError("MXM2 exceeds asset budget");
                int[] xyz = new int[vertices * 3], uv = new int[vertices * 2];
                short[] index = new short[triangles * 3];
                int i;
                for (i = 0; i < xyz.length; i++) xyz[i] = in.readInt();
                for (i = 0; i < uv.length; i++) uv[i] = in.readInt();
                for (i = 0; i < index.length; i++) {
                    index[i] = in.readShort();
                    if ((index[i] & 0xffff) >= vertices)
                        throw new IOException("mesh index out of range");
                }
                result[s] = new MeshSection(room, texture, xyz, uv, index);
            }
            if (in.read() != -1)
                throw new IOException("trailing MXM2 data");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(path + ": invalid MXM2: " + e.toString());
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
    private TextureData[] readTextures(String path) {
        InputStream raw = getClass().getResourceAsStream(path);
        if (raw == null)
            return null;
        DataInputStream in = new DataInputStream(raw);
        try {
            if (in.readInt() != TEXTURE_MAGIC || in.readUnsignedShort() != FORMAT_VERSION)
                throw new IOException("unsupported MXT2 version");
            int count = in.readUnsignedShort();
            if (count < 1 || count > 256)
                throw new IOException("invalid texture count");
            TextureData[] result = new TextureData[count];
            int used = 0, t;
            for (t = 0; t < count; t++) {
                int w = in.readUnsignedShort(), h = in.readUnsignedShort(),
                    colors = in.readUnsignedShort();
                if (w <= 0 || h <= 0 || w > 256 || h > 256 || colors < 1 || colors > 256)
                    throw new IOException("invalid atlas metadata");
                used += 16 + colors * 4 + w * h;
                if (used > MAX_ASSET_BYTES)
                    throw new OutOfMemoryError("MXT2 exceeds asset budget");
                int[] palette = new int[colors];
                byte[] pixels = new byte[w * h];
                int i;
                for (i = 0; i < colors; i++)
                    palette[i] = (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8)
                            | in.readUnsignedByte();
                in.readFully(pixels);
                for (i = 0; i < pixels.length; i++)
                    if ((pixels[i] & 255) >= colors)
                        throw new IOException("palette index out of range");
                result[t] = new TextureData(w, h, palette, pixels);
            }
            if (in.read() != -1)
                throw new IOException("trailing MXT2 data");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(path + ": invalid MXT2: " + e.toString());
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
}
