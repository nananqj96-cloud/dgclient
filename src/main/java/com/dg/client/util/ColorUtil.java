package com.dg.client.util;

/**
 * Strict green-and-white palette and color math utilities for DGClient.
 * Palette accents stay exclusively in the neon green (#39FF14..#00FF88),
 * pure/soft white (#FFFFFF..#C8DBD0), and dark emerald charcoal (#0A0F0C..#121A15) range.
 */
public final class ColorUtil {
    public static final int NEON_GREEN = 0xFF39FF14;
    public static final int MINT_GREEN = 0xFF00FF88;
    public static final int PURE_WHITE = 0xFFFFFFFF;
    public static final int SOFT_WHITE = 0xFFD6E5DD;
    public static final int MUTED_WHITE = 0xFF8FA69A;
    public static final int PANEL_DARK = 0xF20A0F0C;
    public static final int PANEL_BG = 0xEE0E1611;
    public static final int PANEL_ELEVATED = 0xF2121A15;
    public static final int PANEL_HOVER = 0xCC19261E;
    public static final int BORDER_SUBTLE = 0x66274232;

    private ColorUtil() {
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((MathUtil.clamp(a, 0, 255) & 0xFF) << 24)
                | ((MathUtil.clamp(r, 0, 255) & 0xFF) << 16)
                | ((MathUtil.clamp(g, 0, 255) & 0xFF) << 8)
                | (MathUtil.clamp(b, 0, 255) & 0xFF);
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((MathUtil.clamp(alpha, 0, 255) & 0xFF) << 24);
    }

    public static int scaleAlpha(int argb, float factor) {
        int a = getAlpha(argb);
        return withAlpha(argb, Math.round(a * MathUtil.clamp(factor, 0.0f, 1.0f)));
    }

    public static int getAlpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    public static int getRed(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    public static int getGreen(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    public static int getBlue(int argb) {
        return argb & 0xFF;
    }

    public static int lerp(int c1, int c2, float t) {
        float k = MathUtil.clamp(t, 0.0f, 1.0f);
        int a = Math.round(getAlpha(c1) + (getAlpha(c2) - getAlpha(c1)) * k);
        int r = Math.round(getRed(c1) + (getRed(c2) - getRed(c1)) * k);
        int g = Math.round(getGreen(c1) + (getGreen(c2) - getGreen(c1)) * k);
        int b = Math.round(getBlue(c1) + (getBlue(c2) - getBlue(c1)) * k);
        return rgba(r, g, b, a);
    }

    /**
     * Animated green-and-white wave accent (#39FF14 -> #00FF88 -> #E8FFF2 -> #39FF14).
     */
    public static int animatedAccent(double offsetSeconds, float speed) {
        double time = (System.currentTimeMillis() / 1000.0) * speed + offsetSeconds;
        float wave = (float) ((Math.sin(time * 2.4) + 1.0) * 0.5);
        if (wave < 0.65f) {
            return lerp(NEON_GREEN, MINT_GREEN, wave / 0.65f);
        }
        return lerp(MINT_GREEN, 0xFFE6FFF0, (wave - 0.65f) / 0.35f);
    }

    /**
     * Distance color strictly within the green/white palette: neon green when close, pure white when far.
     */
    public static int distanceGreenWhite(double distance, double maxDistance) {
        float ratio = (float) MathUtil.clamp(distance / Math.max(1.0, maxDistance), 0.0, 1.0);
        return lerp(NEON_GREEN, PURE_WHITE, ratio);
    }

    /**
     * Health color strictly within the green/white palette: bright neon green at full HP,
     * crisp pulsing white when low HP.
     */
    public static int healthGreenWhite(float healthRatio) {
        float clamped = MathUtil.clamp(healthRatio, 0.0f, 1.0f);
        return lerp(PURE_WHITE, NEON_GREEN, clamped);
    }

    public static int hsvToArgb(float h, float s, float v, int alpha) {
        float hue = (h % 360.0f + 360.0f) % 360.0f;
        float sat = MathUtil.clamp(s, 0.0f, 1.0f);
        float val = MathUtil.clamp(v, 0.0f, 1.0f);
        float c = val * sat;
        float x = c * (1.0f - Math.abs(((hue / 60.0f) % 2.0f) - 1.0f));
        float m = val - c;
        float r1 = 0, g1 = 0, b1 = 0;
        if (hue < 60) {
            r1 = c; g1 = x;
        } else if (hue < 120) {
            r1 = x; g1 = c;
        } else if (hue < 180) {
            g1 = c; b1 = x;
        } else if (hue < 240) {
            g1 = x; b1 = c;
        } else if (hue < 300) {
            r1 = x; b1 = c;
        } else {
            r1 = c; b1 = x;
        }
        return rgba(
                Math.round((r1 + m) * 255.0f),
                Math.round((g1 + m) * 255.0f),
                Math.round((b1 + m) * 255.0f),
                alpha
        );
    }

    public static float[] argbToHsv(int argb) {
        float r = getRed(argb) / 255.0f;
        float g = getGreen(argb) / 255.0f;
        float b = getBlue(argb) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0.0f) {
            h = 120.0f;
        } else if (max == r) {
            h = 60.0f * (((g - b) / delta) % 6.0f);
        } else if (max == g) {
            h = 60.0f * (((b - r) / delta) + 2.0f);
        } else {
            h = 60.0f * (((r - g) / delta) + 4.0f);
        }
        if (h < 0.0f) {
            h += 360.0f;
        }
        float s = max == 0.0f ? 0.0f : (delta / max);
        return new float[]{h, s, max};
    }
}
