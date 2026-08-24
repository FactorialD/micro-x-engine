package com.microx.engine.math;

/**
 * Allocation-free Q16.16 vector, camera and projection helpers.
 * Vectors are three consecutive ints and matrices are row-major 4x4 arrays.
 * Callers own all output and scratch arrays, so these methods are safe in the
 * frame loop without creating garbage.
 */
public final class Math3D {
    private Math3D() {}

    public static int dot(int[] a, int ao, int[] b, int bo) {
        long sum = ((long)a[ao] * b[bo]) / Fixed.ONE;
        sum += ((long)a[ao + 1] * b[bo + 1]) / Fixed.ONE;
        sum += ((long)a[ao + 2] * b[bo + 2]) / Fixed.ONE;
        return Fixed.saturate(sum);
    }

    public static void cross(int[] a, int ao, int[] b, int bo, int[] out, int oo) {
        int x = Fixed.sub(Fixed.mul(a[ao + 1], b[bo + 2]), Fixed.mul(a[ao + 2], b[bo + 1]));
        int y = Fixed.sub(Fixed.mul(a[ao + 2], b[bo]), Fixed.mul(a[ao], b[bo + 2]));
        int z = Fixed.sub(Fixed.mul(a[ao], b[bo + 1]), Fixed.mul(a[ao + 1], b[bo]));
        out[oo] = x; out[oo + 1] = y; out[oo + 2] = z;
    }

    /** Normalizes a vector in place and returns its original Q16.16 length. */
    public static int normalize(int[] vector, int offset) {
        int ax = Fixed.abs(vector[offset]);
        int ay = Fixed.abs(vector[offset + 1]);
        int az = Fixed.abs(vector[offset + 2]);
        int max = ax > ay ? (ax > az ? ax : az) : (ay > az ? ay : az);
        if (max == 0) return 0;
        int sx = Fixed.div(vector[offset], max);
        int sy = Fixed.div(vector[offset + 1], max);
        int sz = Fixed.div(vector[offset + 2], max);
        long squared = (long)sx * sx + (long)sy * sy + (long)sz * sz;
        int scaledLength = (int)isqrt(squared);
        int length = Fixed.mul(max, scaledLength);
        vector[offset] = Fixed.div(sx, scaledLength);
        vector[offset + 1] = Fixed.div(sy, scaledLength);
        vector[offset + 2] = Fixed.div(sz, scaledLength);
        return length;
    }

    private static long isqrt(long value) {
        long result = 0, bit = 1L << 62;
        while (bit > value) bit >>= 2;
        while (bit != 0) {
            if (value >= result + bit) {
                value -= result + bit;
                result = (result >> 1) + bit;
            } else result >>= 1;
            bit >>= 2;
        }
        return result;
    }

    public static void identity(int[] out) {
        int i; for (i = 0; i < 16; i++) out[i] = 0;
        out[0] = out[5] = out[10] = out[15] = Fixed.ONE;
    }

    public static void multiply(int[] a, int[] b, int[] out) {
        int row, column, k;
        for (row = 0; row < 4; row++) for (column = 0; column < 4; column++) {
            long sum = 0;
            for (k = 0; k < 4; k++) sum += ((long)a[row * 4 + k] * b[k * 4 + column]) / Fixed.ONE;
            out[row * 4 + column] = Fixed.saturate(sum);
        }
    }

    public static void transform(int[] matrix, int[] input, int io, int[] output, int oo) {
        int x = input[io], y = input[io + 1], z = input[io + 2];
        output[oo] = sum4(matrix, 0, x, y, z);
        output[oo + 1] = sum4(matrix, 4, x, y, z);
        output[oo + 2] = sum4(matrix, 8, x, y, z);
        output[oo + 3] = sum4(matrix, 12, x, y, z);
    }

    private static int sum4(int[] m, int o, int x, int y, int z) {
        long sum = ((long)m[o] * x) / Fixed.ONE + ((long)m[o + 1] * y) / Fixed.ONE;
        sum += ((long)m[o + 2] * z) / Fixed.ONE + m[o + 3];
        return Fixed.saturate(sum);
    }

    /** Builds a right-handed view with +Z forward. Scratch must contain 9 ints. */
    public static void lookAt(int[] eye, int eo, int[] target, int to, int[] up, int uo,
                              int[] out, int[] scratch) {
        scratch[6] = Fixed.sub(target[to], eye[eo]);
        scratch[7] = Fixed.sub(target[to + 1], eye[eo + 1]);
        scratch[8] = Fixed.sub(target[to + 2], eye[eo + 2]);
        normalize(scratch, 6);
        cross(up, uo, scratch, 6, scratch, 0); normalize(scratch, 0);
        cross(scratch, 6, scratch, 0, scratch, 3);
        identity(out);
        copyViewRow(scratch, 0, eye, eo, out, 0);
        copyViewRow(scratch, 3, eye, eo, out, 4);
        copyViewRow(scratch, 6, eye, eo, out, 8);
    }

    private static void copyViewRow(int[] axis, int ao, int[] eye, int eo, int[] out, int row) {
        out[row] = axis[ao]; out[row + 1] = axis[ao + 1]; out[row + 2] = axis[ao + 2];
        out[row + 3] = Fixed.neg(dot(axis, ao, eye, eo));
    }

    /**
     * Builds a +Z-forward perspective matrix. Focal X/Y are cot(fov/2)
     * values; near/far are positive view-space distances.
     */
    public static void perspective(int focalX, int focalY, int near, int far, int[] out) {
        identity(out);
        out[0] = focalX; out[5] = focalY;
        out[10] = Fixed.div(far, Fixed.sub(far, near));
        out[11] = Fixed.neg(Fixed.mul(near, out[10]));
        out[14] = Fixed.ONE; out[15] = 0;
    }

    /** Returns false for points at/behind the eye; output contains xyz NDC. */
    public static boolean perspectiveDivide(int[] clip, int co, int[] output, int oo) {
        int w = clip[co + 3];
        if (w <= 0) return false;
        output[oo] = Fixed.div(clip[co], w);
        output[oo + 1] = Fixed.div(clip[co + 1], w);
        output[oo + 2] = Fixed.div(clip[co + 2], w);
        return true;
    }

    /** Signed distance from ax+by+cz+d=0. Plane and point use Q16.16. */
    public static int planeDistance(int[] plane, int po, int[] point, int pointOffset) {
        return Fixed.add(dot(plane, po, point, pointOffset), plane[po + 3]);
    }

    /** Clips a view-space triangle against z >= near. Output has room for 12 ints. */
    public static int clipTriangleNear(int[] triangle, int offset, int near, int[] output) {
        int count = 0, i;
        for (i = 0; i < 3; i++) {
            int current = offset + i * 3;
            int previous = offset + ((i + 2) % 3) * 3;
            boolean cin = triangle[current + 2] >= near;
            boolean pin = triangle[previous + 2] >= near;
            if (cin != pin) {
                int t = Fixed.div(Fixed.sub(near, triangle[previous + 2]),
                                  Fixed.sub(triangle[current + 2], triangle[previous + 2]));
                output[count * 3] = Fixed.lerp(triangle[previous], triangle[current], t);
                output[count * 3 + 1] = Fixed.lerp(triangle[previous + 1], triangle[current + 1], t);
                output[count * 3 + 2] = near; count++;
            }
            if (cin) {
                output[count * 3] = triangle[current]; output[count * 3 + 1] = triangle[current + 1];
                output[count * 3 + 2] = triangle[current + 2]; count++;
            }
        }
        return count;
    }
}
