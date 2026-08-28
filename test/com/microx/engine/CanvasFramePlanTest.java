package com.microx.engine;

import java.io.InputStream;
import com.microx.engine.assets.MeshSection;
import com.microx.engine.assets.TestGeometry;
import com.microx.engine.ui.UIStateMachine;

public final class CanvasFramePlanTest {
    private static final String[] RESOURCES = {"/test/cube/geometry.txt",
            "/test/pyramid/geometry.txt", "/levels/cordon/geometry.txt",
            "/levels/garbage/geometry.txt"};

    public static void main(String[] args) throws Exception {
        for (int selected = 0; selected < RESOURCES.length; selected++) {
            UIStateMachine ui = new UIStateMachine();
            ui.show(UIStateMachine.TEST_MENU);
            for (int row = 0; row < selected; row++)
                ui.command(Input.DOWN);
            ui.command(Input.ACCEPT);
            check(ui.state() == UIStateMachine.TEST_VIEW, "preview remains active for row " + selected);
            check(ui.action() == UIStateMachine.ACTION_TEST_OPEN, "open action for row " + selected);
            InputStream in = CanvasFramePlanTest.class.getResourceAsStream(RESOURCES[selected]);
            check(in != null, "preview resource " + RESOURCES[selected]);
            MeshSection[] sections = TestGeometry.read(in);
            in.close();
            check(sections != null && sections.length > 0,
                    "preview resource opens for row " + selected);
        }
        check(CanvasFramePlan.preview(UIStateMachine.TEST_VIEW), "test view uses preview");
        check(!CanvasFramePlan.paintView(UIStateMachine.TEST_VIEW),
                "standard view must not clear preview");
        check(CanvasFramePlan.previewAngle(16) != CanvasFramePlan.previewAngle(32),
                "next frame changes model angle");
        System.out.println("CanvasFramePlanTest OK");
    }

    private static void check(boolean value, String label) {
        if (!value)
            throw new AssertionError(label);
    }
}
