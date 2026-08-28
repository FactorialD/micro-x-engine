package com.microx.engine.assets;

import java.io.IOException;
import java.io.InputStream;

/** Narrow public bridge used by the debug preview without exposing the parser itself. */
public final class TestGeometry {
    private TestGeometry() {}
    public static MeshSection[] read(InputStream in) throws IOException {
        return GeometryLoader.read(in);
    }
}
