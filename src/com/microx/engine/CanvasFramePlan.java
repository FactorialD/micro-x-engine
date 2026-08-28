package com.microx.engine;

import com.microx.engine.ui.UIStateMachine;

/** Pure frame-composition decisions, kept separate from MIDP drawing for host-side tests. */
final class CanvasFramePlan {
    private CanvasFramePlan() {
    }

    static boolean preview(int state) {
        return state == UIStateMachine.TEST_VIEW;
    }

    static boolean paintView(int state) {
        return state != UIStateMachine.GAMEPLAY && !preview(state);
    }

    static int previewAngle(long now) {
        return (int) ((now / 16) % 360);
    }
}
