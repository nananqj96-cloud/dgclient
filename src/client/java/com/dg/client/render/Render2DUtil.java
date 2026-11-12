package com.dg.client.render;

import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/**
 * 2D vector primitives for DGClient's neon green & white UI built natively on
 * Minecraft 1.21.11's GuiRenderState / DrawContext pipeline:
 * rounded rectangles, multi-pass gaussian outer bloom, drop shadows, gradient quads,
 * circles, rotated 2D lines, and scissor clipping.
 */
public final class Render2DUtil {
    private static DrawContext activeContext;

    private Render2DUtil() {
    }

    public static void setContext(DrawContext context) {
        activeContext = context;
    }

    public static DrawContext getContext() {
        return activeContext;
    }

    public static void drawQuad(Matrix3x2fStack matrices, float x, float y, float w, float h, int color) {
        DrawContext ctx = activeContext;
        if (ctx == null || w <= 0.0f || h <= 0.0f || ColorUtil.getAlpha(color) == 0) {
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.max(x1 + 1, Math.round(x + w));
        int y2 = Math.max(y1 + 1, Math.round(y + h));
        ctx.fill(x1, y1, x2, y2, color);
    }

    public static void drawGradientQuad(
            Matrix3x2fStack matrices,
            float x,
            float y,
            float w,
            float h,
            int topLeft,
            int topRight,
            int bottomRight,
            int bottomLeft
    ) {
        DrawContext ctx = activeContext;
        if (ctx == null || w <= 0.0f || h <= 0.0f) {
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.max(x1 + 1, Math.round(x + w));
        int y2 = Math.max(y1 + 1, Math.round(y + h));
        // If horizontal gradient differs from vertical, slice horizontally for smooth interpolation
        if (topLeft != topRight || bottomLeft != bottomRight) {
            int steps = Math.max(2, Math.min(24, x2 - x1));
            for (int i = 0; i < steps; i++) {
                float t = i / (float) Math.max(1, steps - 1);
                int sx1 = x1 + Math.round((x2 - x1) * (i / (float) steps));
                int sx2 = x1 + Math.round((x2 - x1) * ((i + 1) / (float) steps));
                if (sx2 <= sx1) continue;
                int topCol = ColorUtil.lerp(topLeft, topRight, t);
                int botCol = ColorUtil.lerp(bottomLeft, bottomRight, t);
                ctx.fillGradient(sx1, y1, sx2, y2, topCol, botCol);
            }
        } else {
            ctx.fillGradient(x1, y1, x2, y2, topLeft, bottomLeft);
        }
    }

    public static void drawRoundedRect(Matrix3x2fStack matrices, float x, float y, float w, float h, float radius, int color) {
        DrawContext ctx = activeContext;
        if (ctx == null || w <= 0.0f || h <= 0.0f || ColorUtil.getAlpha(color) == 0) {
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.max(x1 + 1, Math.round(x + w));
        int y2 = Math.max(y1 + 1, Math.round(y + h));
        int r = Math.min(Math.round(Math.max(0.0f, radius)), Math.min(x2 - x1, y2 - y1) / 2);
        if (r <= 1) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        // Central vertical band between top & bottom corner arcs
        if (y1 + r < y2 - r) {
            ctx.fill(x1, y1 + r, x2, y2 - r, color);
        }
        // Scanline corner arcs (top & bottom)
        for (int dy = 0; dy < r; dy++) {
            double distFromCenter = (r - 0.5) - dy;
            int inset = Math.max(0, (int) Math.round(r - Math.sqrt(Math.max(0.0, r * r - distFromCenter * distFromCenter))));
            int lx = x1 + inset;
            int rx = x2 - inset;
            if (rx > lx) {
                ctx.fill(lx, y1 + dy, rx, y1 + dy + 1, color);
                int by = y2 - 1 - dy;
                if (by >= y1 + r || y1 + dy != by) {
                    ctx.fill(lx, by, rx, by + 1, color);
                }
            }
        }
    }

    public static void drawRoundedOutline(
            Matrix3x2fStack matrices,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float thickness,
            int color
    ) {
        DrawContext ctx = activeContext;
        if (ctx == null || w <= 0.0f || h <= 0.0f || thickness <= 0.0f || ColorUtil.getAlpha(color) == 0) {
            return;
        }
        int x1 = Math.round(x);
        int y1 = Math.round(y);
        int x2 = Math.max(x1 + 1, Math.round(x + w));
        int y2 = Math.max(y1 + 1, Math.round(y + h));
        int t = Math.max(1, Math.round(thickness));
        int r = Math.min(Math.round(Math.max(0.0f, radius)), Math.min(x2 - x1, y2 - y1) / 2);

        if (r <= 1) {
            ctx.fill(x1, y1, x2, y1 + t, color);
            ctx.fill(x1, y2 - t, x2, y2, color);
            ctx.fill(x1, y1 + t, x1 + t, y2 - t, color);
            ctx.fill(x2 - t, y1 + t, x2, y2 - t, color);
            return;
        }

        // Top & bottom straight edges
        if (x1 + r < x2 - r) {
            ctx.fill(x1 + r, y1, x2 - r, y1 + t, color);
            ctx.fill(x1 + r, y2 - t, x2 - r, y2, color);
        }
        // Left & right straight edges
        if (y1 + r < y2 - r) {
            ctx.fill(x1, y1 + r, x1 + t, y2 - r, color);
            ctx.fill(x2 - t, y1 + r, x2, y2 - r, color);
        }
        // Corner arc pixels
        int rInner = Math.max(0, r - t);
        for (int dy = 0; dy < r; dy++) {
            double dc = (r - 0.5) - dy;
            int outerInset = Math.max(0, (int) Math.round(r - Math.sqrt(Math.max(0.0, r * r - dc * dc))));
            int innerInset = (dc >= rInner)
                    ? r
                    : Math.max(outerInset + 1, (int) Math.round(r - Math.sqrt(Math.max(0.0, rInner * rInner - dc * dc))));
            int spanEnd = Math.min(r, Math.max(outerInset + t, innerInset));
            // Top-left & Top-right
            ctx.fill(x1 + outerInset, y1 + dy, x1 + spanEnd, y1 + dy + 1, color);
            ctx.fill(x2 - spanEnd, y1 + dy, x2 - outerInset, y1 + dy + 1, color);
            // Bottom-left & Bottom-right
            int by = y2 - 1 - dy;
            if (by != y1 + dy) {
                ctx.fill(x1 + outerInset, by, x1 + spanEnd, by + 1, color);
                ctx.fill(x2 - spanEnd, by, x2 - outerInset, by + 1, color);
            }
        }
    }

    public static void drawGaussianGlow(
            Matrix3x2fStack matrices,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float spread,
            int glowColor,
            float intensity
    ) {
        if (spread <= 0.5f || intensity <= 0.01f) {
            return;
        }
        int passes = Math.max(4, Math.min(8, Math.round(spread * 0.65f)));
        float step = spread / passes;
        float baseAlpha = MathUtil.clamp(intensity, 0.0f, 2.0f) * 32.0f;

        for (int i = passes; i >= 1; i--) {
            float t = (float) i / (float) passes;
            float gaussian = (float) Math.exp(-(t * t) * 3.2);
            int ringAlpha = MathUtil.clamp(Math.round(baseAlpha * gaussian), 1, 90);
            float expand = i * step;
            drawRoundedOutline(
                    matrices,
                    x - expand,
                    y - expand,
                    w + expand * 2.0f,
                    h + expand * 2.0f,
                    radius + expand,
                    Math.max(1.0f, step * 1.35f),
                    ColorUtil.withAlpha(glowColor, ringAlpha)
            );
        }
    }

    public static void drawDropShadow(Matrix3x2fStack matrices, float x, float y, float w, float h, float radius, float spread, int maxAlpha) {
        int passes = 5;
        float step = spread / passes;
        for (int i = passes; i >= 1; i--) {
            float t = (float) i / (float) passes;
            int alpha = Math.round(maxAlpha * (float) Math.exp(-(t * t) * 2.8) * 0.30f);
            if (alpha <= 0) {
                continue;
            }
            float ex = i * step;
            drawRoundedRect(matrices, x - ex, y - ex + 2.0f, w + ex * 2.0f, h + ex * 2.0f, radius + ex, ColorUtil.rgba(0, 0, 0, alpha));
        }
    }

    public static void drawCircle(Matrix3x2fStack matrices, float cx, float cy, float radius, int color) {
        drawRoundedRect(matrices, cx - radius, cy - radius, radius * 2.0f, radius * 2.0f, radius, color);
    }

    public static void drawCircleOutline(Matrix3x2fStack matrices, float cx, float cy, float radius, float thickness, int color) {
        drawRoundedOutline(matrices, cx - radius, cy - radius, radius * 2.0f, radius * 2.0f, radius, thickness, color);
    }

    public static void drawLine2D(Matrix3x2fStack matrices, float x1, float y1, float x2, float y2, float thickness, int startColor, int endColor) {
        DrawContext ctx = activeContext;
        if (ctx == null) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.5f) {
            return;
        }
        float angle = (float) Math.atan2(dy, dx);
        int ilen = Math.max(1, Math.round(len));
        int halfT = Math.max(1, Math.round(thickness * 0.5f));

        matrices.pushMatrix();
        matrices.translate(x1, y1);
        matrices.rotate(angle);
        if (startColor == endColor) {
            ctx.fill(0, -halfT, ilen, halfT, startColor);
        } else {
            int segs = Math.min(12, Math.max(2, ilen / 16));
            for (int i = 0; i < segs; i++) {
                int sx1 = Math.round(ilen * (i / (float) segs));
                int sx2 = Math.round(ilen * ((i + 1) / (float) segs));
                if (sx2 > sx1) {
                    int col = ColorUtil.lerp(startColor, endColor, i / (float) Math.max(1, segs - 1));
                    ctx.fill(sx1, -halfT, sx2, halfT, col);
                }
            }
        }
        matrices.popMatrix();
    }

    public static void pushScissor(DrawContext context, float x, float y, float w, float h) {
        context.enableScissor(
                Math.round(x),
                Math.round(y),
                Math.round(x + Math.max(0.0f, w)),
                Math.round(y + Math.max(0.0f, h))
        );
    }

    public static void popScissor(DrawContext context) {
        context.disableScissor();
    }
}
