package com.dg.client.util;

/**
 * Common interpolation, clamping, and easing helpers.
 */
public final class MathUtil {
    private MathUtil() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float lerp(float start, float end, float delta) {
        return start + (end - start) * clamp(delta, 0.0f, 1.0f);
    }

    public static double lerp(double start, double end, double delta) {
        return start + (end - start) * clamp(delta, 0.0, 1.0);
    }

    public static float approach(float current, float target, float speed) {
        float factor = clamp(speed, 0.0f, 1.0f);
        float next = current + (target - current) * factor;
        if (Math.abs(target - next) < 0.001f) {
            return target;
        }
        return next;
    }

    public static float easeOutCubic(float t) {
        float x = clamp(t, 0.0f, 1.0f) - 1.0f;
        return x * x * x + 1.0f;
    }

    public static float easeInOutQuad(float t) {
        float x = clamp(t, 0.0f, 1.0f);
        return x < 0.5f ? 2.0f * x * x : 1.0f - (float) Math.pow(-2.0 * x + 2.0, 2.0) / 2.0f;
    }

    public static double roundToStep(double value, double step) {
        if (step <= 0.0) {
            return value;
        }
        double rounded = Math.round(value / step) * step;
        return Math.round(rounded * 10000.0) / 10000.0;
    }
}
