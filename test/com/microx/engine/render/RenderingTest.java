package com.microx.engine.render;
import com.microx.engine.assets.AssetManager;
import com.microx.engine.Telemetry;
import com.microx.engine.assets.TextureData;
import com.microx.engine.assets.MeshSection;
import com.microx.engine.math.Fixed;
import com.microx.engine.world.PortalWorld;
import com.microx.engine.world.EntityPool;
import com.microx.tools.AssetConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
public final class RenderingTest {
    public static void main(String[] a) throws Exception {
        objImport();
        clippingAndWinding();
        depthAndUv();
        previewPipelineStages();
        previewFitsEveryOrbitAngle();
        missingTexture();
        portalOcclusion();
        budget();
        telemetryCounters();
        System.out.println("RenderingTest: OK");
    }
    private static void objImport() throws Exception {
        Path d = Files.createTempDirectory("mx-obj"), o = d.resolve("geometry.txt"),
             m = d.resolve("validation.bin");
        Files.write(o,
                ("# microx material wall texture=3 color=123456\no room_2\nv 0 0 2\nv 1 0 2\nv 1 1 2\nv 0 1 2\nvt 0 0\nvt 1 0\nvt 1 1\nvt 0 1\nusemtl wall\nf -4/-4 -3/-3 -2/-2 -1/-1\n")
                        .getBytes(StandardCharsets.US_ASCII));
        AssetConverter.writeModel(o, m);
        DataInputStream in = new DataInputStream(Files.newInputStream(m));
        ok(in.readInt() == 0x4d584d32 && in.readUnsignedShort() == 2, "MXM2 v2");
        ok(in.readUnsignedShort() == 1, "section");
        ok(in.readUnsignedShort() == 2 && in.readShort() == 3 && in.readInt() == 0x123456,
                "material texture and fallback color");
        ok(in.readUnsignedShort() == 4 && in.readUnsignedShort() == 2, "triangulation/dedup");
        in.close();
    }
    private static void clippingAndWinding() {
        Clipper c = new Clipper();
        ok(c.clip(-Fixed.ONE, 0, Fixed.ONE / 16, 0, 0, Fixed.ONE, 0, Fixed.ONE, Fixed.ONE, 0, 0,
                   Fixed.ONE, Fixed.ONE, 0, Fixed.ONE, Fixed.ONE / 8)
                        >= 3,
                "clipping");
        int[] rgb = new int[64];
        short[] z = new short[64];
        Rasterizer r = new Rasterizer();
        r.target(rgb, z, 8, 8);
        r.clear(0);
        TextureData t = texture(0xffffff);
        ok(!r.draw(1, 1, Fixed.ONE, 0, 0, 6, 1, Fixed.ONE, 0, 0, 1, 6, Fixed.ONE, 0, 0, t),
                "winding culled");
        ok(r.draw(1, 1, Fixed.ONE, 0, 0, 1, 6, Fixed.ONE, 0, 0, 6, 1, Fixed.ONE, 0, 0, t),
                "winding drawn");
    }
    private static void depthAndUv() {
        int[] rgb = new int[64];
        short[] z = new short[64];
        Rasterizer r = new Rasterizer();
        r.target(rgb, z, 8, 8);
        r.clear(0);
        r.draw(1, 1, Fixed.fromInt(4), 0, 0, 1, 6, Fixed.fromInt(4), 0, 0, 6, 1, Fixed.fromInt(4),
                0, 0, texture(0xff0000));
        r.draw(1, 1, Fixed.ONE, 0, 0, 1, 6, Fixed.ONE, 0, 0, 6, 1, Fixed.ONE, 0, 0,
                texture(0x00ff00));
        ok(rgb[18] == 0x00ff00, "depth ordering");
        TextureData t = new TextureData(2, 1, new int[] {0x112233, 0xabcdef}, new byte[] {0, 1});
        ok(t.sample(0, 0) == 0x112233 && t.sample(Fixed.ONE, 0) == 0xabcdef, "UV sampling");
    }
    private static void previewPipelineStages() {
        FrameCoordinator frame = new FrameCoordinator();
        frame.configure(64, 64, 2 * 1024 * 1024, 0);
        javax.microedition.lcdui.Graphics graphics = new javax.microedition.lcdui.Graphics(64, 64);

        // Put one vertex behind the preview near plane while the other two remain visible,
        // so the shared clipper must produce a fan.
        RenderCamera preview = new RenderCamera();
        preview.preview(0, 0, 0, Fixed.ONE, 0, 64, 64);
        int behindNear = preview.z + preview.near / 2;
        int[] crossing = {-Fixed.ONE, -Fixed.ONE, behindNear, Fixed.ONE, -Fixed.ONE, 0, -Fixed.ONE,
                Fixed.ONE, 0};
        MeshSection clipped =
                new MeshSection(0, -1, 0xabcdef, crossing, new int[6], new short[] {0, 1, 2});
        frame.renderPreview(graphics, new MeshSection[] {clipped}, null, 0, 0, 0, Fixed.ONE, 0);
        ok(frame.submittedTriangles == 1 && frame.clippedTriangles > 0 && frame.drawnTriangles > 0,
                "preview uses transform, near clipping and rasterization pipeline");

        int z = 0;
        int[] positions = {
                -Fixed.ONE, -Fixed.ONE, z, -Fixed.ONE, Fixed.ONE, z, Fixed.ONE, -Fixed.ONE, z};
        MeshSection back =
                new MeshSection(0, -1, 0xffffff, positions, new int[6], new short[] {0, 1, 2});
        frame.renderPreview(graphics, new MeshSection[] {back}, null, 0, 0, 0, Fixed.ONE, 0);
        ok(frame.submittedTriangles == 1 && frame.drawnTriangles == 0
                        && frame.clippedTriangles == 1,
                "preview uses shared back-face culling");
    }
    private static void previewFitsEveryOrbitAngle() {
        checkOrbitFit(Fixed.ONE, Fixed.ONE, Fixed.ONE, 240, 320, "cube portrait");
        checkOrbitFit(Fixed.ONE, Fixed.ONE, Fixed.ONE, 320, 240, "cube landscape");
        checkOrbitFit(
                Fixed.fromInt(8), Fixed.ONE, Fixed.fromInt(2), 240, 320, "elongated portrait");
        checkOrbitFit(
                Fixed.fromInt(8), Fixed.ONE, Fixed.fromInt(2), 320, 240, "elongated landscape");
    }
    private static void checkOrbitFit(
            int sizeX, int sizeY, int sizeZ, int width, int height, String label) {
        int hx = sizeX / 2, hy = sizeY / 2, hz = sizeZ / 2;
        int radius =
                (int) Math.ceil(Math.sqrt((double) hx * hx + (double) hy * hy + (double) hz * hz));
        RenderCamera camera = new RenderCamera();
        for (int angle = 0; angle < 360; angle++) {
            camera.preview(0, 0, 0, radius, angle, width, height);
            for (int xi = -1; xi <= 1; xi += 2)
                for (int yi = -1; yi <= 1; yi += 2)
                    for (int zi = -1; zi <= 1; zi += 2) {
                        int wx = xi * hx, wy = yi * hy, wz = zi * hz;
                        int dx = Fixed.sub(wx, camera.x), dz = Fixed.sub(wz, camera.z);
                        int viewX = Fixed.add(Fixed.mul(dx, camera.cos), Fixed.mul(dz, camera.sin));
                        int viewZ = Fixed.sub(Fixed.mul(dz, camera.cos), Fixed.mul(dx, camera.sin));
                        ok(viewZ >= camera.near, label + " stays before near plane");
                        long px = width / 2L + (long) viewX * camera.focalX / viewZ / Fixed.ONE;
                        long py = height / 2L - (long) wy * camera.focalY / viewZ / Fixed.ONE;
                        ok(px > 0 && px < width - 1 && py > 0 && py < height - 1,
                                label + " stays inside viewport");
                    }
        }
    }
    private static void missingTexture() {
        ok(new AssetManager().texture(0) == null, "missing texture remains distinguishable");
        int[] rgb = new int[64];
        short[] z = new short[64];
        Rasterizer r = new Rasterizer();
        r.target(rgb, z, 8, 8);
        r.clear(0);
        r.draw(1, 1, Fixed.ONE, 0, 0, 1, 6, Fixed.ONE, 0, 0, 6, 1, Fixed.ONE, 0, 0, null, 0x123456);
        ok(rgb[18] == 0x123456, "section fallback color for unavailable texture");
        r.clear(0);
        r.draw(1, 1, Fixed.ONE, 0, 0, 1, 6, Fixed.ONE, 0, 0, 6, 1, Fixed.ONE, 0, 0, null, 0xff00ff);
        ok(rgb[18] == 0xff00ff, "default fallback is FF00FF");
    }
    private static void portalOcclusion() {
        PortalWorld w = new PortalWorld(3, 2);
        w.room(0, -Fixed.ONE, Fixed.ONE, -Fixed.ONE, Fixed.ONE);
        w.room(1, -Fixed.ONE, Fixed.ONE, Fixed.ONE, Fixed.fromInt(3));
        w.room(2, -Fixed.ONE, Fixed.ONE, Fixed.fromInt(3), Fixed.fromInt(5));
        w.portal(0, 0, 0, 1, -Fixed.ONE / 4, Fixed.ONE / 4, 0, Fixed.ONE, Fixed.ONE, Fixed.ONE, -1,
                -1);
        w.portal(1, 1, 1, 2, Fixed.fromInt(10), Fixed.fromInt(11), 0, Fixed.ONE, Fixed.fromInt(2),
                Fixed.fromInt(2), -1, -1);
        ok(w.updateVisibility(0, 0, 0, 0, Fixed.ONE, Fixed.fromInt(40), Fixed.fromInt(40), 80, 80)
                        == 2,
                "portal occlusion");
        ok(w.visibleRight(1) - w.visibleLeft(1) < 80, "portal clip rect");
    }
    private static void budget() {
        TextureData t = new TextureData(256, 256, new int[256], new byte[65536]);
        ok(t.memoryBytes() == 66576, "atlas footprint");
        ok(240L * 320L * 6L < 2L * 1024L * 1024L * 45L / 100L, "buffer budget");
    }
    private static void telemetryCounters() {
        EntityPool entities = new EntityPool(8);
        entities.spawn(EntityPool.HUMAN, 0, 0, 0, 100);
        int mutant = entities.spawn(EntityPool.MUTANT, 0, 0, 0, 100);
        entities.spawn(EntityPool.ITEM, 0, 0, 0, 1);
        entities.spawn(EntityPool.ANOMALY, 0, 0, 0, 1);
        ok(entities.typeCount(EntityPool.HUMAN) == 1 && entities.typeCount(EntityPool.MUTANT) == 1
                        && entities.typeCount(EntityPool.ITEM) == 1
                        && entities.typeCount(EntityPool.ANOMALY) == 1,
                "entity type counters");
        ok(entities.killToCorpse(mutant) && entities.typeCount(EntityPool.MUTANT) == 0
                        && entities.typeCount(EntityPool.CORPSE) == 1,
                "corpse type counter");
        ok(Telemetry.kib(1023) == 0 && Telemetry.kib(1024) == 1
                        && Telemetry.kib(Long.MAX_VALUE) == Long.MAX_VALUE / 1024L,
                "long KiB conversion");
        AssetManager assets = new AssetManager();
        ok(assets.locationTextureCount() == 0 && assets.sharedTextureCount() == 0
                        && assets.residentTextureCount() == 0 && assets.residentSectionCount() == 0,
                "empty asset telemetry counters");
    }
    private static TextureData texture(int c) {
        return new TextureData(1, 1, new int[] {c}, new byte[] {0});
    }
    private static void ok(boolean v, String m) {
        if (!v)
            throw new AssertionError(m);
    }
}
