package com.microx.engine.math;

/**
 * Saturating signed Q16.16 arithmetic used by the entire runtime.
 *
 * <p>One metre/scalar unit is {@code 65536}.  The representable interval is
 * [-32768, 32767.99998].  Operations which do not fit clamp to an int endpoint
 * instead of wrapping.  Division by zero clamps according to the numerator's
 * sign (zero is treated as positive).  Angles accepted by sin/cos are integral
 * degrees; the lookup table is immutable and allocated only at class loading.</p>
 */
public final class Fixed {
    public static final int SHIFT = 16;
    public static final int ONE = 1 << SHIFT;
    public static final int HALF = ONE >> 1;
    public static final int MASK = ONE - 1;

    private Fixed() {}

    public static int saturate(long value) {
        if (value > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        return (int) value;
    }

    public static int fromInt(int value) {
        return saturate((long) value * ONE);
    }
    public static int fromLong(long value) {
        if (value > 32767L)
            return Integer.MAX_VALUE;
        if (value < -32768L)
            return Integer.MIN_VALUE;
        return (int) (value * ONE);
    }
    public static int fromRatio(int numerator, int denominator) {
        return div(fromInt(numerator), fromInt(denominator));
    }
    /** Truncates toward zero. */
    public static int toInt(int value) {
        return value < 0 ? -(-value >> SHIFT) : value >> SHIFT;
    }
    public static int floorToInt(int value) {
        return value >> SHIFT;
    }
    public static int roundToInt(int value) {
        return toInt(add(value, value < 0 ? -HALF : HALF));
    }

    public static int add(int a, int b) {
        return saturate((long) a + b);
    }
    public static int sub(int a, int b) {
        return saturate((long) a - b);
    }
    public static int neg(int value) {
        return value == Integer.MIN_VALUE ? Integer.MAX_VALUE : -value;
    }
    public static int abs(int value) {
        return value < 0 ? neg(value) : value;
    }
    public static int mul(int a, int b) {
        return saturate(((long) a * b) / ONE);
    }
    public static int div(int a, int b) {
        if (b == 0)
            return a < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        return saturate(((long) a * ONE) / b);
    }
    public static int clamp(int value, int low, int high) {
        if (low > high) {
            int swap = low;
            low = high;
            high = swap;
        }
        return value < low ? low : (value > high ? high : value);
    }
    public static int lerp(int a, int b, int t) {
        t = clamp(t, 0, ONE);
        return saturate((long) a + ((long) b - a) * t / ONE);
    }

    public static int normalizeAngle(int degrees) {
        int angle = degrees % 360;
        return angle < 0 ? angle + 360 : angle;
    }
    public static int sin(int degrees) {
        int angle = normalizeAngle(degrees);
        int quadrant = angle / 90;
        int offset = angle % 90;
        if (quadrant == 0)
            return FixedTrigTable.SIN_QUARTER[offset];
        if (quadrant == 1)
            return FixedTrigTable.SIN_QUARTER[90 - offset];
        if (quadrant == 2)
            return -FixedTrigTable.SIN_QUARTER[offset];
        return -FixedTrigTable.SIN_QUARTER[90 - offset];
    }
    public static int cos(int degrees) {
        return sin(normalizeAngle(degrees) + 90);
    }
}
