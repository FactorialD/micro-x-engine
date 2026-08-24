package com.microx.engine.math;

/** Desktop golden tests; deliberately dependency-free so Ant can run them anywhere. */
public final class FixedMathTest {
    private static int checks;

    public static void main(String[] args) {
        fixedBoundaries();
        anglesAndInterpolation();
        vectorsAndClipping();
        cameraAndProjection();
        System.out.println("FixedMathTest: " + checks + " checks passed");
    }

    private static void fixedBoundaries() {
        equal("one", 65536, Fixed.ONE);
        equal("largest coordinate", Integer.MAX_VALUE, Fixed.fromInt(32768));
        equal("smallest coordinate", Integer.MIN_VALUE, Fixed.fromInt(-32768));
        equal("negative truncation", -1, Fixed.toInt(-Fixed.ONE - Fixed.HALF));
        equal("negative floor", -2, Fixed.floorToInt(-Fixed.ONE - 1));
        equal("multiply", Fixed.fromInt(-6), Fixed.mul(Fixed.fromInt(-2), Fixed.fromInt(3)));
        equal("multiply overflow", Integer.MAX_VALUE, Fixed.mul(Integer.MAX_VALUE, Fixed.fromInt(2)));
        equal("add overflow", Integer.MAX_VALUE, Fixed.add(Integer.MAX_VALUE, 1));
        equal("subtract overflow", Integer.MIN_VALUE, Fixed.sub(Integer.MIN_VALUE, 1));
        equal("positive divide by zero", Integer.MAX_VALUE, Fixed.div(Fixed.ONE, 0));
        equal("negative divide by zero", Integer.MIN_VALUE, Fixed.div(-Fixed.ONE, 0));
        equal("zero divide by zero", Integer.MAX_VALUE, Fixed.div(0, 0));
    }

    private static void anglesAndInterpolation() {
        equal("negative angle", 359, Fixed.normalizeAngle(-1));
        equal("minimum angle", 232, Fixed.normalizeAngle(Integer.MIN_VALUE));
        equal("sin -90", -Fixed.ONE, Fixed.sin(-90));
        equal("cos -180", -Fixed.ONE, Fixed.cos(-180));
        equal("sin 30 golden", 32768, Fixed.sin(30));
        equal("lerp midpoint", Fixed.fromInt(15),
              Fixed.lerp(Fixed.fromInt(10), Fixed.fromInt(20), Fixed.HALF));
        equal("lerp clamps", Fixed.fromInt(10),
              Fixed.lerp(Fixed.fromInt(10), Fixed.fromInt(20), -Fixed.ONE));
    }

    private static void vectorsAndClipping() {
        int[] x = {Fixed.ONE, 0, 0};
        int[] y = {0, Fixed.ONE, 0};
        int[] out = new int[12];
        equal("dot perpendicular", 0, Math3D.dot(x, 0, y, 0));
        Math3D.cross(x, 0, y, 0, out, 0);
        equal("cross z", Fixed.ONE, out[2]);
        int[] v = {Fixed.fromInt(3), Fixed.fromInt(4), 0};
        near("vector length", Fixed.fromInt(5), Math3D.normalize(v, 0), 2);
        near("normalized x", 39322, v[0], 2);
        near("normalized y", 52429, v[1], 2);

        int[] triangle = {-Fixed.ONE, 0, Fixed.HALF, Fixed.ONE, 0, Fixed.fromInt(2),
                          0, Fixed.ONE, Fixed.fromInt(2)};
        equal("near clip vertex count", 4,
              Math3D.clipTriangleNear(triangle, 0, Fixed.ONE, out));
        equal("first clipped z", Fixed.ONE, out[2]);
        int onNear = 0;
        for (int i = 2; i < 12; i += 3) if (out[i] == Fixed.ONE) onNear++;
        equal("near clip intersections", 2, onNear);
    }

    private static void cameraAndProjection() {
        int[] eye = {0, 0, -Fixed.fromInt(5)};
        int[] target = {0, 0, 0};
        int[] up = {0, Fixed.ONE, 0};
        int[] view = new int[16];
        int[] scratch = new int[9];
        int[] origin = {0, 0, 0};
        int[] transformed = new int[4];
        Math3D.lookAt(eye, 0, target, 0, up, 0, view, scratch);
        Math3D.transform(view, origin, 0, transformed, 0);
        equal("camera origin x", 0, transformed[0]);
        equal("camera origin z", Fixed.fromInt(5), transformed[2]);

        int[] projection = new int[16];
        int[] point = {Fixed.ONE, Fixed.ONE, Fixed.fromInt(5)};
        int[] ndc = new int[3];
        Math3D.perspective(Fixed.ONE, Fixed.ONE, Fixed.ONE, Fixed.fromInt(10), projection);
        Math3D.transform(projection, point, 0, transformed, 0);
        truth("perspective divide", Math3D.perspectiveDivide(transformed, 0, ndc, 0));
        near("projected x", Fixed.ONE / 5, ndc[0], 1);
        near("projected y", Fixed.ONE / 5, ndc[1], 1);
        near("projected z", 58254, ndc[2], 2); // (10/9*5 - 10/9) / 5 = 8/9
        transformed[3] = 0;
        truth("reject divide at eye", !Math3D.perspectiveDivide(transformed, 0, ndc, 0));
    }

    private static void equal(String name, int expected, int actual) {
        checks++; if (expected != actual) throw new AssertionError(name + ": " + actual + " != " + expected);
    }
    private static void near(String name, int expected, int actual, int tolerance) {
        checks++; if (actual < expected - tolerance || actual > expected + tolerance)
            throw new AssertionError(name + ": " + actual + " != " + expected + " +/- " + tolerance);
    }
    private static void truth(String name, boolean value) {
        checks++; if (!value) throw new AssertionError(name);
    }
}
