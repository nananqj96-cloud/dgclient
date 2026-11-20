/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_5348
 *  org.joml.Matrix3x2fStack
 */
package com.fakeclient.gui;

import com.fakeclient.Settings;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3675;
import net.minecraft.class_5348;
import org.joml.Matrix3x2fStack;

public final class Gui {
    public static final int PANEL_R = 18;
    public static final int BTN_R = 9;
    private static final float[][] INSET_CACHE = new float[65][];
    private static final float[] EMPTY_INSETS = new float[0];

    private Gui() {
    }

    public static class_2561 font(String s) {
        return class_2561.method_43470((String)s);
    }

    public static class_2561 bold(String s) {
        return class_2561.method_43470((String)s).method_27692(class_124.field_1067);
    }

    public static void text(class_332 ctx, class_327 tr, class_2561 t, int x, int y, int color) {
        ctx.method_51439(tr, t, x, y, color, true);
    }

    public static void centered(class_332 ctx, class_327 tr, class_2561 t, int cx, int y, int color) {
        ctx.method_51439(tr, t, cx - tr.method_27525((class_5348)t) / 2, y, color, true);
    }

    public static int tracked(class_332 ctx, class_327 tr, String s, int x, int y, int color, int gap) {
        int cx = x;
        for (int i = 0; i < s.length(); ++i) {
            class_2561 ch = Gui.bold(String.valueOf(s.charAt(i)));
            ctx.method_51439(tr, ch, cx, y, color, true);
            cx += tr.method_27525((class_5348)ch) + gap;
        }
        return cx - x;
    }

    public static int alpha(int argb, float mul) {
        int a = Math.round((float)(argb >>> 24 & 0xFF) * Gui.clamp01(mul));
        return a << 24 | argb & 0xFFFFFF;
    }

    public static void pushScale(class_332 ctx, int x, int y, int w, int h, float scale) {
        Matrix3x2fStack mat = ctx.method_51448();
        float cx = (float)x + (float)w / 2.0f;
        float cy = (float)y + (float)h / 2.0f;
        mat.pushMatrix();
        mat.translate(cx, cy);
        mat.scale(scale);
        mat.translate(-cx, -cy);
    }

    public static void pushOffset(class_332 ctx, float dx, float dy) {
        Matrix3x2fStack mat = ctx.method_51448();
        mat.pushMatrix();
        mat.translate(dx, dy);
    }

    public static void pop(class_332 ctx) {
        ctx.method_51448().popMatrix();
    }

    public static void dropShadow(class_332 ctx, int x, int y, int w, int h, int r, int spread, int strength) {
        int rings = 2;
        int step = Math.max(2, spread / rings);
        for (int i = rings; i >= 1; --i) {
            int k = i * step;
            int a = Math.max(1, Math.round((float)(strength * (rings - i + 1)) / (float)(rings * 2)));
            Gui.roundedRect(ctx, x - k, y - k + 2, w + 2 * k, h + 2 * k, r + k, Math.min(255, a) << 24);
        }
    }

    public static void bloom(class_332 ctx, int x, int y, int w, int h, int r, int spread, float strength) {
        float f = Gui.glowFactor() * Gui.clamp01(strength);
        if (f <= 0.12f) {
            return;
        }
        int step = Math.max(2, spread / 2);
        for (int i = 2; i >= 1; --i) {
            int k = i * step;
            int a = Math.round(30.0f * f * (float)(3 - i) / 3.0f);
            if (a <= 0) continue;
            Gui.roundedRect(ctx, x - k, y - k, w + 2 * k, h + 2 * k, r + k, Math.min(255, a) << 24 | 0xFFFFFF);
        }
    }

    public static void topHighlight(class_332 ctx, int x, int y, int w, int r, int alpha) {
        Gui.aaSpan(ctx, (float)x + (float)r * 0.7f, (float)(x + w) - (float)r * 0.7f, y, alpha << 24 | 0xFFFFFF, alpha);
    }

    public static void iconPlate(class_332 ctx, int x, int y, int d, float lit) {
        lit = Gui.clamp01(lit);
        int r = d / 2;
        Gui.roundedRect(ctx, x, y, d, d, r, Gui.lerp(0x14FFFFFF, 0x3DFFFFFF, lit));
        Gui.roundedGradient(ctx, x + 1, y + 1, d - 2, d - 2, r - 1, Gui.lerp(-14474458, -13619147, lit), Gui.lerp(-15395560, -15000801, lit));
        Gui.topHighlight(ctx, x + 1, y + 1, d - 2, r, Math.round(Gui.lerp01(14.0f, 34.0f, lit)));
    }

    public static void glowText(class_332 ctx, class_327 tr, class_2561 t, int x, int y, int rgb) {
        float f = Gui.glowFactor();
        if (f > 0.0f) {
            int halo = Math.min(255, Math.round(64.0f * f)) << 24 | rgb & 0xFFFFFF;
            ctx.method_51439(tr, t, x + 1, y, halo, false);
            ctx.method_51439(tr, t, x - 1, y, halo, false);
            ctx.method_51439(tr, t, x, y + 1, halo, false);
            ctx.method_51439(tr, t, x, y - 1, halo, false);
        }
        ctx.method_51439(tr, t, x, y, 0xFF000000 | rgb & 0xFFFFFF, false);
    }

    public static int accent() {
        Settings s = Settings.get();
        if (!s.rainbow) {
            return s.accent;
        }
        float hue = (float)(System.currentTimeMillis() % 6000L) / 6000.0f;
        return 0xFF000000 | Gui.hsbToRgb(hue, 0.55f, 1.0f);
    }

    public static float hueOf(int rgb) {
        float min;
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float d = max - (min = Math.min(r, Math.min(g, b)));
        if (d <= 1.0E-4f) {
            return 0.0f;
        }
        float h = max == r ? (g - b) / d % 6.0f : (max == g ? (b - r) / d + 2.0f : (r - g) / d + 4.0f);
        return h < 0.0f ? h + 1.0f : (h /= 6.0f);
    }

    public static int hsbToRgb(float h, float s, float v) {
        if (s <= 0.0f) {
            int g = (int)(v * 255.0f + 0.5f);
            return g << 16 | g << 8 | g;
        }
        float hh = (h - (float)Math.floor(h)) * 6.0f;
        int sector = (int)hh;
        float f = hh - (float)sector;
        int vi = (int)(v * 255.0f + 0.5f);
        int p = (int)(v * (1.0f - s) * 255.0f + 0.5f);
        int q = (int)(v * (1.0f - s * f) * 255.0f + 0.5f);
        int t = (int)(v * (1.0f - s * (1.0f - f)) * 255.0f + 0.5f);
        return switch (sector) {
            case 0 -> vi << 16 | t << 8 | p;
            case 1 -> q << 16 | vi << 8 | p;
            case 2 -> p << 16 | vi << 8 | t;
            case 3 -> p << 16 | q << 8 | vi;
            case 4 -> t << 16 | p << 8 | vi;
            default -> vi << 16 | p << 8 | q;
        };
    }

    public static void backdrop(class_332 ctx, int w, int h) {
        Gui.backdrop(ctx, w, h, 1.0f);
    }

    public static void backdrop(class_332 ctx, int w, int h, float factor) {
        int dim = Math.max(0, Math.min(100, Settings.get().bgDim));
        factor = Math.max(0.0f, Math.min(1.0f, factor));
        int aTop = (int)(220.0f * ((float)dim / 100.0f) * factor);
        int aBot = (int)(240.0f * ((float)dim / 100.0f) * factor);
        ctx.method_25296(0, 0, w, h, aTop << 24 | 0xA0A0B, aBot << 24 | 0x50505);
    }

    public static void panel(class_332 ctx, int x, int y, int w, int h) {
        Gui.shadow(ctx, x, y, w, h, 18);
        Gui.bloom(ctx, x, y, w, h, 18, 6, 0.75f);
        int op = Math.max(40, Math.min(100, Settings.get().panelOpacity));
        int a = (int)(255.0f * ((float)op / 100.0f));
        Gui.roundedRect(ctx, x, y, w, h, 18, Gui.withAlpha(0xFFFFFF, (int)(46.0f * ((float)op / 100.0f))));
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, 17, a << 24 | 0x141416, a << 24 | 0xA0A0B);
        Gui.topHighlight(ctx, x + 1, y + 1, w - 2, 18, Math.round(26.0f * ((float)op / 100.0f)));
    }

    public static void header(class_332 ctx, int x, int y, int w, int h) {
        for (int i = 0; i < h; ++i) {
            int inset = i < 18 ? Gui.cornerInset(i, 18) : 0;
            int a = (int)(24.0f * (1.0f - (float)i / (float)h));
            ctx.method_25294(x + inset, y + i, x + w - inset, y + i + 1, a << 24 | 0xFFFFFF);
        }
        float f = Gui.glowFactor();
        if (f > 0.12f) {
            int a = Math.min(255, Math.round(46.0f * f));
            ctx.method_25296(x + 1, y + h - 4, x + w - 1, y + h, 0 | Gui.accent() & 0xFFFFFF, a << 24 | Gui.accent() & 0xFFFFFF);
        }
        ctx.method_25294(x + 1, y + h - 1, x + w - 1, y + h, 0xFF000000 | Gui.accent() & 0xFFFFFF);
    }

    public static void glass(class_332 ctx, int x, int y, int w, int h, int rgb, boolean hover) {
        Gui.roundedRect(ctx, x, y, w, h, 9, hover ? 0x44FFFFFF : 0x22FFFFFF);
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, 8, 0xFF000000 | Gui.lighten(rgb, hover ? 16 : 8), 0xFF000000 | rgb);
    }

    public static void card(class_332 ctx, int x, int y, int w, int h, int rgb, boolean hover) {
        Gui.card(ctx, x, y, w, h, rgb, hover ? 1.0f : 0.0f);
    }

    public static void card(class_332 ctx, int x, int y, int w, int h, int rgb, float hover) {
        hover = Gui.clamp01(hover);
        int r = 12;
        Gui.roundedRect(ctx, x, y, w, h, r, Gui.lerp(0x20FFFFFF, 0x3CFFFFFF, hover));
        int amt = Math.round(Gui.lerp01(5.0f, 10.0f, hover));
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, 0xFF000000 | Gui.lighten(rgb, amt), 0xFF000000 | rgb);
    }

    public static void pill(class_332 ctx, int x, int y, int w, int h, int rgb, boolean hover) {
        Gui.pill(ctx, x, y, w, h, rgb, hover ? 1.0f : 0.0f);
    }

    public static void pill(class_332 ctx, int x, int y, int w, int h, int rgb, float hover) {
        hover = Gui.clamp01(hover);
        int r = h / 2;
        Gui.roundedRect(ctx, x, y, w, h, r, Gui.lerp(0x33FFFFFF, 0x66FFFFFF, hover));
        int amt = Math.round(Gui.lerp01(12.0f, 24.0f, hover));
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, 0xFF000000 | Gui.lighten(rgb, amt), 0xFF000000 | rgb);
    }

    public static void chip(class_332 ctx, int x, int y, int w, int h, int rgb, boolean hover, boolean active) {
        int border = active ? 0xFF000000 | Gui.accent() & 0xFFFFFF : (hover ? -1996488705 : 0x40FFFFFF);
        Gui.roundedRect(ctx, x, y, w, h, 9, border);
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, 8, 0xE0000000 | Gui.lighten(rgb, 18), 0xC8000000 | rgb);
    }

    public static void slider(class_332 ctx, int x, int y, int w, int h, float t, boolean hover) {
        Gui.slider(ctx, x, y, w, h, t, hover ? 1.0f : 0.0f);
    }

    public static void slider(class_332 ctx, int x, int y, int w, int h, float t, float hover) {
        hover = Gui.clamp01(hover);
        int r = h / 2;
        Gui.roundedRect(ctx, x, y, w, h, r, Gui.lerp(0x2EFFFFFF, 0x66FFFFFF, hover));
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, -14540250, -15263973);
        int fw = Math.max(h, Math.round((float)w * t));
        if (fw > 2) {
            Gui.roundedGradient(ctx, x + 1, y + 1, fw - 2, h - 2, r - 1, 0xFF000000 | Gui.lighten(Gui.accent() & 0xFFFFFF, 26), 0xFF000000 | Gui.accent() & 0xFFFFFF);
        }
        int grow = Math.round(hover * 3.0f);
        int kd = h + 4 + grow;
        int kx = x + Math.round((float)(w - kd) * t);
        int ky = y - 2 - grow / 2;
        Gui.roundedRect(ctx, kx, ky + 1, kd, kd, kd / 2, 0x66000000);
        Gui.roundedGradient(ctx, kx, ky, kd, kd, kd / 2, -1, -2235412);
    }

    public static void toggleSwitch(class_332 ctx, int x, int y, int w, int h, boolean on, boolean hover) {
        Gui.toggleSwitch(ctx, x, y, w, h, on ? 1.0f : 0.0f, hover ? 1.0f : 0.0f);
    }

    public static void toggleSwitch(class_332 ctx, int x, int y, int w, int h, float t, float hover) {
        float raw = t;
        t = Gui.clamp01(t);
        hover = Gui.clamp01(hover);
        int r = h / 2;
        int accent = Gui.accent() & 0xFFFFFF;
        int trackTop = Gui.lerp(-14671836, -855308, t);
        int trackBot = Gui.lerp(-15461352, -2697510, t);
        if (t > 0.65f) {
            Gui.bloom(ctx, x, y, w, h, r, 4, (t - 0.65f) / 0.35f * 0.8f);
        }
        Gui.roundedRect(ctx, x, y, w, h, r, Gui.lerp(Gui.lerp(0x1AFFFFFF, 0x33FFFFFF, hover), -1929379841, t));
        Gui.roundedGradient(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, trackTop, trackBot);
        if (t < 0.9f) {
            Gui.aaSpan(ctx, (float)x + (float)r * 0.6f, (float)(x + w) - (float)r * 0.6f, y + 1, Math.round(68.0f * (1.0f - t)) << 24, Math.round(68.0f * (1.0f - t)));
        }
        int kd = h - 6;
        int kx = Math.round(Gui.lerp01(x + 3, x + w - kd - 3, raw));
        int ky = y + 3;
        Gui.roundedRect(ctx, kx, ky + 1, kd, kd, kd / 2, 0x66000000);
        Gui.roundedGradient(ctx, kx, ky, kd, kd, kd / 2, Gui.lerp(-1513236, -15066594, t), Gui.lerp(-3881780, -15856110, t));
        if (kd >= 8) {
            Gui.roundedRect(ctx, kx + 2, ky + 2, kd - 5, kd - 6, (kd - 6) / 2, Gui.lerp(0x59FFFFFF, 0x26FFFFFF, t));
        }
    }

    public static void keyBox(class_332 ctx, int x, int y, int w, int h, boolean hover, boolean active) {
        int border = active ? 0xFF000000 | Gui.accent() & 0xFFFFFF : (hover ? -1426063361 : 0x55FFFFFF);
        Gui.roundedRect(ctx, x, y, w, h, 3, border);
        Gui.roundedRect(ctx, x + 1, y + 1, w - 2, h - 2, 2, -267974126);
    }

    public static void keyPill(class_332 ctx, int x, int y, int w, int h, boolean hover, boolean active) {
        Gui.keyPill(ctx, x, y, w, h, hover ? 1.0f : 0.0f, active);
    }

    public static void keyPill(class_332 ctx, int x, int y, int w, int h, float hover, boolean active) {
        hover = Gui.clamp01(hover);
        int r = h / 2;
        int border = active ? 0xFF000000 | Gui.accent() & 0xFFFFFF : Gui.lerp(0x55FFFFFF, -1426063361, hover);
        Gui.roundedRect(ctx, x, y, w, h, r, border);
        Gui.roundedRect(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, -267974126);
    }

    public static String keyName(int code) {
        if (code <= 0) {
            return "None";
        }
        try {
            return class_3675.class_307.field_1668.method_1447(code).method_27445().getString();
        }
        catch (Exception e) {
            return "Key " + code;
        }
    }

    private static float glowFactor() {
        return (float)Math.max(0, Math.min(150, Settings.get().glow)) / 100.0f;
    }

    public static void glow(class_332 ctx, int x, int y, int w, int h, int r, int rgb, int spread, int baseAlpha) {
        float f = Gui.glowFactor();
        if (f <= 0.0f) {
            return;
        }
        for (int k = spread; k >= 1; --k) {
            int a = Math.max(1, Math.round((float)baseAlpha * f * (float)(spread - k + 1) / (float)(spread + 2)));
            Gui.roundedRect(ctx, x - k, y - k, w + 2 * k, h + 2 * k, r + k, Math.min(255, a) << 24 | rgb & 0xFFFFFF);
        }
    }

    public static void roundedRect(class_332 ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        int base = color >>> 24 & 0xFF;
        if (base == 0 || w <= 0 || h <= 0) {
            return;
        }
        if (h > r * 2) {
            ctx.method_25294(x, y + r, x + w, y + h - r, color);
        }
        Gui.corners(ctx, x, y, w, h, r, false, color, color, 1);
    }

    public static void roundedGradient(class_332 ctx, int x, int y, int w, int h, int r, int top, int bot) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (w <= 0 || h <= 0) {
            return;
        }
        int span = Math.max(1, h - 1);
        if (h > r * 2) {
            ctx.method_25296(x, y + r, x + w, y + h - r, Gui.lerp(top, bot, (float)r / (float)span), Gui.lerp(top, bot, (float)(h - r - 1) / (float)span));
        }
        Gui.corners(ctx, x, y, w, h, r, true, top, bot, span);
    }

    private static float[] insetTable(int r) {
        if (r <= 0) {
            return EMPTY_INSETS;
        }
        if (r < INSET_CACHE.length) {
            float[] cached = INSET_CACHE[r];
            if (cached == null) {
                cached = Gui.buildInsets(r);
                Gui.INSET_CACHE[r] = cached;
            }
            return cached;
        }
        return Gui.buildInsets(r);
    }

    private static float[] buildInsets(int r) {
        float[] t = new float[r];
        for (int i = 0; i < r; ++i) {
            t[i] = Gui.cornerInsetF((float)i + 0.5f, r);
        }
        return t;
    }

    private static void corners(class_332 ctx, int x, int y, int w, int h, int r, boolean gradient, int top, int bot, int span) {
        float[] t = Gui.insetTable(r);
        if (t.length == 0) {
            return;
        }
        for (int pass = 0; pass < 2; ++pass) {
            int end;
            boolean upper = pass == 0;
            int i = upper ? 0 : h - r;
            int n = end = upper ? r : h;
            while (i < end) {
                int j;
                int inset = Math.round(t[upper ? i : h - 1 - i]);
                for (j = i + 1; j < end && Math.round(t[upper ? j : h - 1 - j]) == inset; ++j) {
                }
                int right = x + w - inset;
                int left = x + inset;
                if (right > left) {
                    if (gradient) {
                        ctx.method_25296(left, y + i, right, y + j, Gui.lerp(top, bot, (float)i / (float)span), Gui.lerp(top, bot, (float)(j - 1) / (float)span));
                    } else {
                        ctx.method_25294(left, y + i, right, y + j, top);
                    }
                }
                i = j;
            }
        }
    }

    private static void aaSpan(class_332 ctx, float left, float right, int y, int color, int base) {
        if (right - left <= 0.0f) {
            return;
        }
        int sL = (int)Math.ceil(left);
        int sR = (int)Math.floor(right);
        if (sR > sL) {
            ctx.method_25294(sL, y, sR, y + 1, color);
        }
        int c0 = (int)Math.floor(left);
        int c1 = (int)Math.ceil(right);
        for (int c = c0; c < c1; ++c) {
            float cov;
            if (c >= sL && c < sR || (cov = Math.min((float)(c + 1), right) - Math.max((float)c, left)) <= 0.004f) continue;
            ctx.method_25294(c, y, c + 1, y + 1, cov >= 0.996f ? color : Gui.withAlpha(color, Math.round((float)base * cov)));
        }
    }

    private static void shadow(class_332 ctx, int x, int y, int w, int h, int r) {
        int dy = 3;
        for (int k = 9; k >= 1; --k) {
            int a = Math.max(2, 26 - k * 3);
            Gui.roundedRect(ctx, x - k, y - k + dy, w + 2 * k, h + 2 * k, r + k, a << 24);
        }
    }

    private static float cornerInsetF(float d, float r) {
        float rd = r - d;
        return (float)((double)r - Math.sqrt(Math.max(0.0f, r * r - rd * rd)));
    }

    private static int cornerInset(int dy, int r) {
        double d = (double)r - Math.sqrt((double)r * (double)r - (double)(r - dy) * (double)(r - dy));
        return (int)Math.round(d);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
    }

    public static float clamp01(float v) {
        return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
    }

    public static float lerp01(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int withAlpha(int argb, int a) {
        return (a & 0xFF) << 24 | argb & 0xFFFFFF;
    }

    public static int lighten(int rgb, int amt) {
        int r = Math.min(255, (rgb >> 16 & 0xFF) + amt);
        int g = Math.min(255, (rgb >> 8 & 0xFF) + amt);
        int b = Math.min(255, (rgb & 0xFF) + amt);
        return r << 16 | g << 8 | b;
    }

    public static int darken(int argb, int amt) {
        int a = argb >>> 24 & 0xFF;
        int r = Math.max(0, (argb >> 16 & 0xFF) - amt);
        int g = Math.max(0, (argb >> 8 & 0xFF) - amt);
        int b = Math.max(0, (argb & 0xFF) - amt);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int lerp(int a, int b, float t) {
        int aa = a >>> 24 & 0xFF;
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int ba = b >>> 24 & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int na = (int)((float)aa + (float)(ba - aa) * t);
        int nr = (int)((float)ar + (float)(br - ar) * t);
        int ng = (int)((float)ag + (float)(bg - ag) * t);
        int nb = (int)((float)ab + (float)(bb - ab) * t);
        return na << 24 | nr << 16 | ng << 8 | nb;
    }
}
