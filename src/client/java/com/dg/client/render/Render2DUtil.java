package com.dg.client.render;

import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * High-precision 2D immediate-mode vector primitives for DGClient's neon green & white UI:
 * anti-aliased rounded rectangles, multi-pass gaussian outer bloom, drop shadows,
 * gradient quads, circles, and glowing outlines.
 */
public final class Render2DUtil {
    private static final int CORNER_SEGMENTS = 7;

    private Render2DUtil() {
    }

    public static void setup2D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    public static void end2D() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawQuad(MatrixStack matrices, float x, float y, float w, float h, int color) {
        if (w <= 0.0f || h <= 0.0f || ColorUtil.getAlpha(color) == 0) {
            return;
        }
        Matrix4f mat = matrices.peek().getPositionMatrix();
        setup2D();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x, y + h, 0.0f).color(color);
        buf.vertex(mat, x + w, y + h, 0.0f).color(color);
        buf.vertex(mat, x + w, y, 0.0f).color(color);
        buf.vertex(mat, x, y, 0.0f).color(color);
        drawBuilt(buf);
        end2D();
    }

    public static void drawGradientQuad(
            MatrixStack matrices,
            float x,
            float y,
            float w,
            float h,
            int topLeft,
            int topRight,
            int bottomRight,
            int bottomLeft
    ) {
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        Matrix4f mat = matrices.peek().getPositionMatrix();
        setup2D();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x, y + h, 0.0f).color(bottomLeft);
        buf.vertex(mat, x + w, y + h, 0.0f).color(bottomRight);
        buf.vertex(mat, x + w, y, 0.0f).color(topRight);
        buf.vertex(mat, x, y, 0.0f).color(topLeft);
        drawBuilt(buf);
        end2D();
    }

    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float w, float h, float radius, int color) {
        if (w <= 0.0f || h <= 0.0f || ColorUtil.getAlpha(color) == 0) {
            return;
        }
        float r = Math.min(Math.max(0.0f, radius), Math.min(w, h) * 0.5f);
        if (r < 0.5f) {
            drawQuad(matrices, x, y, w, h, color);
            return;
        }
        Matrix4f mat = matrices.peek().getPositionMatrix();
        setup2D();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x + w * 0.5f, y + h * 0.5f, 0.0f).color(color);

        float[][] centers = {
                {x + w - r, y + r, -90.0f, 0.0f},
                {x + w - r, y + h - r, 0.0f, 90.0f},
                {x + r, y + h - r, 90.0f, 180.0f},
                {x + r, y + r, 180.0f, 270.0f}
        };

        for (float[] c : centers) {
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                double rad = Math.toRadians(c[2] + (c[3] - c[2]) * (i / (float) CORNER_SEGMENTS));
                float px = c[0] + (float) Math.cos(rad) * r;
                float py = c[1] + (float) Math.sin(rad) * r;
                buf.vertex(mat, px, py, 0.0f).color(color);
            }
        }
        // Close fan back to first perimeter vertex
        double rad0 = Math.toRadians(centers[0][2]);
        buf.vertex(mat, centers[0][0] + (float) Math.cos(rad0) * r, centers[0][1] + (float) Math.sin(rad0) * r, 0.0f).color(color);
        drawBuilt(buf);
        end2D();
    }

    public static void drawRoundedOutline(
            MatrixStack matrices,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float thickness,
            int color
    ) {
        if (w <= 0.0f || h <= 0.0f || thickness <= 0.0f || ColorUtil.getAlpha(color) == 0) {
            return;
        }
        float r = Math.min(Math.max(0.0f, radius), Math.min(w, h) * 0.5f);
        float halfT = thickness * 0.5f;
        float rOuter = r + halfT;
        float rInner = Math.max(0.0f, r - halfT);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        setup2D();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float[][] centers = {
                {x + w - r, y + r, -90.0f, 0.0f},
                {x + w - r, y + h - r, 0.0f, 90.0f},
                {x + r, y + h - r, 90.0f, 180.0f},
                {x + r, y + r, 180.0f, 270.0f}
        };

        for (float[] c : centers) {
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                double rad = Math.toRadians(c[2] + (c[3] - c[2]) * (i / (float) CORNER_SEGMENTS));
                float cos = (float) Math.cos(rad);
                float sin = (float) Math.sin(rad);
                buf.vertex(mat, c[0] + cos * rOuter, c[1] + sin * rOuter, 0.0f).color(color);
                buf.vertex(mat, c[0] + cos * rInner, c[1] + sin * rInner, 0.0f).color(color);
            }
        }
        double rad0 = Math.toRadians(centers[0][2]);
        float cos0 = (float) Math.cos(rad0);
        float sin0 = (float) Math.sin(rad0);
        buf.vertex(mat, centers[0][0] + cos0 * rOuter, centers[0][1] + sin0 * rOuter, 0.0f).color(color);
        buf.vertex(mat, centers[0][0] + cos0 * rInner, centers[0][1] + sin0 * rInner, 0.0f).color(color);
        drawBuilt(buf);
        end2D();
    }

    /**
     * Multi-pass gaussian outer bloom around a rounded rectangle.
     * Uses true exponential gaussian falloff exp(-t^2 * 3.4) across concentric rings
     * for smooth, authentic neon bloom rather than a flat border.
     */
    public static void drawGaussianGlow(
            MatrixStack matrices,
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
        int passes = Math.max(5, Math.min(11, Math.round(spread * 0.85f)));
        float step = spread / passes;
        float baseAlpha = MathUtil.clamp(intensity, 0.0f, 2.0f) * 34.0f;

        for (int i = passes; i >= 1; i--) {
            float t = (float) i / (float) passes;
            float gaussian = (float) Math.exp(-(t * t) * 3.2);
            int ringAlpha = MathUtil.clamp(Math.round(baseAlpha * gaussian), 1, 95);
            float expand = i * step;
            drawRoundedOutline(
                    matrices,
                    x - expand,
                    y - expand,
                    w + expand * 2.0f,
                    h + expand * 2.0f,
                    radius + expand,
                    step * 1.45f,
                    ColorUtil.withAlpha(glowColor, ringAlpha)
            );
        }
    }

    public static void drawDropShadow(MatrixStack matrices, float x, float y, float w, float h, float radius, float spread, int maxAlpha) {
        int passes = 6;
        float step = spread / passes;
        for (int i = passes; i >= 1; i--) {
            float t = (float) i / (float) passes;
            int alpha = Math.round(maxAlpha * (float) Math.exp(-(t * t) * 2.8) * 0.32f);
            if (alpha <= 0) {
                continue;
            }
            float ex = i * step;
            drawRoundedRect(matrices, x - ex, y - ex + 2.0f, w + ex * 2.0f, h + ex * 2.0f, radius + ex, ColorUtil.rgba(0, 0, 0, alpha));
        }
    }

    public static void drawCircle(MatrixStack matrices, float cx, float cy, float radius, int color) {
        drawRoundedRect(matrices, cx - radius, cy - radius, radius * 2.0f, radius * 2.0f, radius, color);
    }

    public static void drawCircleOutline(MatrixStack matrices, float cx, float cy, float radius, float thickness, int color) {
        drawRoundedOutline(matrices, cx - radius, cy - radius, radius * 2.0f, radius * 2.0f, radius, thickness, color);
    }

    public static void drawLine2D(MatrixStack matrices, float x1, float y1, float x2, float y2, float thickness, int startColor, int endColor) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.001f) {
            return;
        }
        float nx = (-dy / len) * (thickness * 0.5f);
        float ny = (dx / len) * (thickness * 0.5f);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        setup2D();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1 + nx, y1 + ny, 0.0f).color(startColor);
        buf.vertex(mat, x2 + nx, y2 + ny, 0.0f).color(endColor);
        buf.vertex(mat, x2 - nx, y2 - ny, 0.0f).color(endColor);
        buf.vertex(mat, x1 - nx, y1 - ny, 0.0f).color(startColor);
        drawBuilt(buf);
        end2D();
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

    private static void drawBuilt(BufferBuilder buf) {
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }
}
