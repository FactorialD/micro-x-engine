package com.microx.engine.assets;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Loads textual geometry and compact textures while keeping asset ownership separate. */
public final class AssetManager {
    private static final int TEXTURE_MAGIC = 0x4d585432, FORMAT_VERSION = 2,
                             MAX_ASSET_BYTES = 768 * 1024;
    private MeshSection[] sharedSections, locationSections;
    private TextureData[] sharedTextures, locationTextures;
    private String locationErrorPath;

    public boolean loadShared(String root) {
        MeshSection[] sections = readGeometry(root + "/geometry.txt");
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
        String meshPath = root + "/geometry.txt";
        MeshSection[] sections = readGeometry(meshPath);
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
        return null;
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

    private MeshSection[] readGeometry(String path) {
        InputStream raw = getClass().getResourceAsStream(path);
        if (raw == null)
            return null;
        try {
            MeshSection[] result = GeometryLoader.read(raw);
            if (bytes(result, null) > MAX_ASSET_BYTES)
                throw new OutOfMemoryError("geometry.txt exceeds asset budget");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(path + ": invalid geometry: " + e.toString());
        } finally {
            try {
                raw.close();
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
