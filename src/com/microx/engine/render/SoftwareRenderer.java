package com.microx.engine.render;

import javax.microedition.lcdui.Graphics;
import com.microx.engine.assets.AssetManager;
import com.microx.engine.world.Player;
import com.microx.engine.world.PortalWorld;

/** Public renderer. Budget split: 45% framebuffer/depth, 40% assets, 10% scratch, 5% reserve. */
public final class SoftwareRenderer {
    private static final int DEFAULT_BUDGET = 2 * 1024 * 1024;
    private final FrameCoordinator frame = new FrameCoordinator();
    private int requestedWidth, requestedHeight, memoryBudget = DEFAULT_BUDGET, resolutionMode;

    public void configure(int width, int height) {
        configure(width, height, DEFAULT_BUDGET);
    }
    public void configure(int width, int height, int bytes) {
        requestedWidth = width;
        requestedHeight = height;
        memoryBudget = bytes < 32768 ? 32768 : bytes;
        frame.configure(width, height, memoryBudget, resolutionMode);
    }
    /** Finalizes frame scratch after the location assets have been loaded. */
    public void load() {
        frame.prepareAssets();
    }
    public void setAssets(AssetManager assets) {
        frame.setAssets(assets);
    }
    public void setEnvironment(int sky, int wall, int floor) {
        frame.setEnvironment(sky, wall, floor);
    }
    /** Recreates buffers at full, half or quarter internal resolution. */
    public void setResolutionMode(int mode) {
        resolutionMode = mode < 0 ? 0 : mode > 2 ? 2 : mode;
        frame.releaseBuffers();
        frame.configure(requestedWidth, requestedHeight, memoryBudget, resolutionMode);
    }
    public void render(Graphics g, Player player, PortalWorld portals,
            com.microx.engine.world.EntityPool entities) {
        int w = g.getClipWidth(), h = g.getClipHeight();
        if (w != requestedWidth || h != requestedHeight)
            configure(w, h, memoryBudget);
        frame.render(g, player, portals, entities);
    }
    public int submittedTriangles() {
        return frame.submittedTriangles;
    }
    public int clippedTriangles() {
        return frame.clippedTriangles;
    }
    public int drawnTriangles() {
        return frame.drawnTriangles;
    }
    public int internalWidth() {
        return frame.width();
    }
    public int internalHeight() {
        return frame.height();
    }
    public int memoryBudget() {
        return memoryBudget;
    }
    public void release() {
        frame.release();
    }
}
