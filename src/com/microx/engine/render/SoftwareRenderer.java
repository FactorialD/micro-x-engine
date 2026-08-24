package com.microx.engine.render;

import javax.microedition.lcdui.Graphics;
import com.microx.engine.assets.AssetManager;
import com.microx.engine.world.Player;
import com.microx.engine.world.PortalWorld;

/** Public, JSR-184-free rendering API. Owns all storage used by a frame. */
public final class SoftwareRenderer {
    private static final int DEFAULT_BUDGET = 2 * 1024 * 1024;
    private final FrameCoordinator frame = new FrameCoordinator();
    private int requestedWidth, requestedHeight, memoryBudget = DEFAULT_BUDGET;

    public void configure(int width, int height) { configure(width, height, DEFAULT_BUDGET); }
    public void configure(int width, int height, int bytes) {
        requestedWidth = width; requestedHeight = height;
        memoryBudget = bytes < 32768 ? 32768 : bytes;
        frame.configure(width, height, memoryBudget);
    }
    /** Finalizes frame scratch after the location assets have been loaded. */
    public void load() { frame.prepareAssets(); }
    public void setAssets(AssetManager assets) { frame.setAssets(assets); }
    public void render(Graphics g, Player player, PortalWorld portals) {
        int w = g.getClipWidth(), h = g.getClipHeight();
        if (w != requestedWidth || h != requestedHeight) configure(w, h, memoryBudget);
        frame.render(g, player, portals);
    }
    public int submittedTriangles() { return frame.submittedTriangles; }
    public int clippedTriangles() { return frame.clippedTriangles; }
    public int drawnTriangles() { return frame.drawnTriangles; }
    public int internalWidth() { return frame.width(); }
    public int internalHeight() { return frame.height(); }
    public void release() { frame.release(); }
}
