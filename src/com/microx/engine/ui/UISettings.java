package com.microx.engine.ui;
public final class UISettings {
    public int volume = 5, resolution = 0, controls = 0;
    public boolean debug;
    public void change(int row, int direction) {
        if (row == 0)
            volume = clamp(volume + direction, 0, 10);
        else if (row == 1)
            resolution = clamp(resolution + direction, 0, 2);
        else if (row == 2)
            debug = !debug;
        else if (row == 3)
            controls = 1 - controls;
    }
    private int clamp(int n, int a, int b) {
        return n < a ? a : n > b ? b : n;
    }
}
