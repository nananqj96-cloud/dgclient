package com.dg.client.render;

import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * 3D world rendering utilities built natively on Minecraft 1.21.11's Blaze3D RenderPipeline
 * & RenderLayer system: filled boxes, vertical gradient boxes, outlines, corner-bracket ESP boxes,
 * clean flat chunk markers, batched single-draw-call 3D tracer lines, camera-facing floating labels,
 * and 3D-to-2D screen projection.
 */
public final class Render3DUtil {
    public record LineSegment(
            Vec3d startWorld,
            Vec3d endWorld,
            int startColor,
            int endColor,
            float width
    ) {
    }
    private static final RenderPipeline ESP_QUADS_THROUGH_WALLS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("dgclient", "pipeline/esp_quads_xray"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline ESP_QUADS_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("dgclient", "pipeline/esp_quads_depth"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline ESP_LINES_THROUGH_WALLS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("dgclient", "pipeline/esp_lines_xray"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline ESP_LINES_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("dgclient", "pipeline/esp_lines_depth"))
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final RenderLayer LAYER_QUADS_XRAY = RenderLayer.of(
            "dgclient_quads_xray",
            RenderSetup.builder(ESP_QUADS_THROUGH_WALLS_PIPELINE).translucent().expectedBufferSize(1024).build()
    );

    private static final RenderLayer LAYER_QUADS_DEPTH = RenderLayer.of(
            "dgclient_quads_depth",
            RenderSetup.builder(ESP_QUADS_DEPTH_PIPELINE).translucent().expectedBufferSize(1024).build()
    );

    private static final RenderLayer LAYER_LINES_XRAY = RenderLayer.of(
            "dgclient_lines_xray",
            RenderSetup.builder(ESP_LINES_THROUGH_WALLS_PIPELINE).translucent().expectedBufferSize(1024).build()
    );

    private static final RenderLayer LAYER_LINES_DEPTH = RenderLayer.of(
            "dgclient_lines_depth",
            RenderSetup.builder(ESP_LINES_DEPTH_PIPELINE).translucent().expectedBufferSize(1024).build()
    );

    private Render3DUtil() {
    }

    public static void initPipelines() {
        // Forces static registration of RenderPipelines during client init
    }

    public static Box offsetToCamera(Box worldBox, Camera camera) {
        Vec3d cam = camera.getCameraPos();
        return worldBox.offset(-cam.x, -cam.y, -cam.z);
    }

    public static void drawFilledBox(MatrixStack matrices, Camera camera, Box worldBox, int color, boolean throughWalls) {
        if (ColorUtil.getAlpha(color) <= 0) {
            return;
        }
        drawGradientBoxY(matrices, camera, worldBox, color, color, throughWalls);
    }

    public static void drawGradientBoxY(
            MatrixStack matrices,
            Camera camera,
            Box worldBox,
            int bottomColor,
            int topColor,
            boolean throughWalls
    ) {
        Box b = offsetToCamera(worldBox, camera);
        float x1 = (float) b.minX;
        float y1 = (float) b.minY;
        float z1 = (float) b.minZ;
        float x2 = (float) b.maxX;
        float y2 = (float) b.maxY;
        float z2 = (float) b.maxZ;

        Matrix4f mat = matrices.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Bottom
        buf.vertex(mat, x1, y1, z1).color(bottomColor);
        buf.vertex(mat, x2, y1, z1).color(bottomColor);
        buf.vertex(mat, x2, y1, z2).color(bottomColor);
        buf.vertex(mat, x1, y1, z2).color(bottomColor);

        // Top
        buf.vertex(mat, x1, y2, z2).color(topColor);
        buf.vertex(mat, x2, y2, z2).color(topColor);
        buf.vertex(mat, x2, y2, z1).color(topColor);
        buf.vertex(mat, x1, y2, z1).color(topColor);

        // North (-Z)
        buf.vertex(mat, x1, y1, z1).color(bottomColor);
        buf.vertex(mat, x1, y2, z1).color(topColor);
        buf.vertex(mat, x2, y2, z1).color(topColor);
        buf.vertex(mat, x2, y1, z1).color(bottomColor);

        // South (+Z)
        buf.vertex(mat, x2, y1, z2).color(bottomColor);
        buf.vertex(mat, x2, y2, z2).color(topColor);
        buf.vertex(mat, x1, y2, z2).color(topColor);
        buf.vertex(mat, x1, y1, z2).color(bottomColor);

        // West (-X)
        buf.vertex(mat, x1, y1, z2).color(bottomColor);
        buf.vertex(mat, x1, y2, z2).color(topColor);
        buf.vertex(mat, x1, y2, z1).color(topColor);
        buf.vertex(mat, x1, y1, z1).color(bottomColor);

        // East (+X)
        buf.vertex(mat, x2, y1, z1).color(bottomColor);
        buf.vertex(mat, x2, y2, z1).color(topColor);
        buf.vertex(mat, x2, y2, z2).color(topColor);
        buf.vertex(mat, x2, y1, z2).color(bottomColor);

        flushLayer(buf, throughWalls ? LAYER_QUADS_XRAY : LAYER_QUADS_DEPTH);
    }

    public static void drawBoxOutline(
            MatrixStack matrices,
            Camera camera,
            Box worldBox,
            float lineWidth,
            int color,
            boolean throughWalls
    ) {
        if (ColorUtil.getAlpha(color) <= 0) {
            return;
        }
        Box b = offsetToCamera(worldBox, camera);
        float x1 = (float) b.minX;
        float y1 = (float) b.minY;
        float z1 = (float) b.minZ;
        float x2 = (float) b.maxX;
        float y2 = (float) b.maxY;
        float z2 = (float) b.maxZ;
        float lw = MathUtil.clamp(lineWidth, 1.0f, 6.0f);

        MatrixStack.Entry entry = matrices.peek();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

        // Bottom ring
        edge(buf, entry, x1, y1, z1, x2, y1, z1, color, lw);
        edge(buf, entry, x2, y1, z1, x2, y1, z2, color, lw);
        edge(buf, entry, x2, y1, z2, x1, y1, z2, color, lw);
        edge(buf, entry, x1, y1, z2, x1, y1, z1, color, lw);
        // Top ring
        edge(buf, entry, x1, y2, z1, x2, y2, z1, color, lw);
        edge(buf, entry, x2, y2, z1, x2, y2, z2, color, lw);
        edge(buf, entry, x2, y2, z2, x1, y2, z2, color, lw);
        edge(buf, entry, x1, y2, z2, x1, y2, z1, color, lw);
        // Vertical pillars
        edge(buf, entry, x1, y1, z1, x1, y2, z1, color, lw);
        edge(buf, entry, x2, y1, z1, x2, y2, z1, color, lw);
        edge(buf, entry, x2, y1, z2, x2, y2, z2, color, lw);
        edge(buf, entry, x1, y1, z2, x1, y2, z2, color, lw);

        flushLayer(buf, throughWalls ? LAYER_LINES_XRAY : LAYER_LINES_DEPTH);
    }

    public static void drawGlowingBox(
            MatrixStack matrices,
            Camera camera,
            Box worldBox,
            int glowColor,
            int coreEdgeColor,
            float intensity,
            boolean throughWalls
    ) {
        int passes = 3;
        for (int i = passes; i >= 1; i--) {
            double expand = i * 0.03 * intensity;
            int shellAlpha = MathUtil.clamp(Math.round(24.0f * intensity / i), 3, 60);
            drawFilledBox(matrices, camera, worldBox.expand(expand), ColorUtil.withAlpha(glowColor, shellAlpha), throughWalls);
            int wireAlpha = MathUtil.clamp(Math.round(90.0f * intensity / i), 8, 160);
            drawBoxOutline(matrices, camera, worldBox.expand(expand * 0.8), 1.0f + i * 0.6f, ColorUtil.withAlpha(glowColor, wireAlpha), throughWalls);
        }
        drawBoxOutline(matrices, camera, worldBox, 1.8f, coreEdgeColor, throughWalls);
    }

    public static void drawCornerBox(
            MatrixStack matrices,
            Camera camera,
            Box worldBox,
            float cornerFraction,
            float lineWidth,
            int color,
            boolean throughWalls
    ) {
        Box b = offsetToCamera(worldBox, camera);
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ;
        float x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        float dx = (x2 - x1) * cornerFraction;
        float dy = (y2 - y1) * cornerFraction;
        float dz = (z2 - z1) * cornerFraction;
        float lw = MathUtil.clamp(lineWidth, 1.0f, 6.0f);

        MatrixStack.Entry entry = matrices.peek();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

        float[][] corners = {
                {x1, y1, z1, 1, 1, 1},
                {x2, y1, z1, -1, 1, 1},
                {x2, y1, z2, -1, 1, -1},
                {x1, y1, z2, 1, 1, -1},
                {x1, y2, z1, 1, -1, 1},
                {x2, y2, z1, -1, -1, 1},
                {x2, y2, z2, -1, -1, -1},
                {x1, y2, z2, 1, -1, -1}
        };
        for (float[] c : corners) {
            edge(buf, entry, c[0], c[1], c[2], c[0] + c[3] * dx, c[1], c[2], color, lw);
            edge(buf, entry, c[0], c[1], c[2], c[0], c[1] + c[4] * dy, c[2], color, lw);
            edge(buf, entry, c[0], c[1], c[2], c[0], c[1], c[2] + c[5] * dz, color, lw);
        }

        flushLayer(buf, throughWalls ? LAYER_LINES_XRAY : LAYER_LINES_DEPTH);
    }

    public static void drawLine3D(
            MatrixStack matrices,
            Camera camera,
            Vec3d startWorld,
            Vec3d endWorld,
            int startColor,
            int endColor,
            float width,
            boolean throughWalls
    ) {
        Vec3d cam = camera.getCameraPos();
        float x1 = (float) (startWorld.x - cam.x);
        float y1 = (float) (startWorld.y - cam.y);
        float z1 = (float) (startWorld.z - cam.z);
        float x2 = (float) (endWorld.x - cam.x);
        float y2 = (float) (endWorld.y - cam.y);
        float z2 = (float) (endWorld.z - cam.z);
        float lw = MathUtil.clamp(width, 1.0f, 5.0f);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) {
            return;
        }
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
        buf.vertex(mat, x1, y1, z1).color(startColor).normal(entry, nx, ny, nz).lineWidth(lw);
        buf.vertex(mat, x2, y2, z2).color(endColor).normal(entry, nx, ny, nz).lineWidth(lw);
        flushLayer(buf, throughWalls ? LAYER_LINES_XRAY : LAYER_LINES_DEPTH);
    }

    /**
     * Renders an entire collection of 3D tracer/wireframe lines in ONE single GPU draw call
     * so hundreds of loaded storage containers never cause draw-call bottlenecks or FPS drops.
     */
    public static void drawLinesBatch(
            MatrixStack matrices,
            Camera camera,
            List<LineSegment> segments,
            boolean throughWalls
    ) {
        if (segments == null || segments.isEmpty()) {
            return;
        }
        Vec3d cam = camera.getCameraPos();
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.LINES,
                VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH
        );
        int emitted = 0;
        for (LineSegment seg : segments) {
            float x1 = (float) (seg.startWorld.x - cam.x);
            float y1 = (float) (seg.startWorld.y - cam.y);
            float z1 = (float) (seg.startWorld.z - cam.z);
            float x2 = (float) (seg.endWorld.x - cam.x);
            float y2 = (float) (seg.endWorld.y - cam.y);
            float z2 = (float) (seg.endWorld.z - cam.z);
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4f) {
                continue;
            }
            float lw = MathUtil.clamp(seg.width, 0.8f, 5.0f);
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;
            buf.vertex(mat, x1, y1, z1).color(seg.startColor).normal(entry, nx, ny, nz).lineWidth(lw);
            buf.vertex(mat, x2, y2, z2).color(seg.endColor).normal(entry, nx, ny, nz).lineWidth(lw);
            emitted++;
        }
        if (emitted > 0) {
            flushLayer(buf, throughWalls ? LAYER_LINES_XRAY : LAYER_LINES_DEPTH);
        } else {
            buf.endNullable();
        }
    }

    /**
     * Unprojects a normalized screen anchor (ndcY = 0 for crosshair, -0.90 for bottom, +0.90 for top)
     * into a 3D world point ~0.10m in front of the camera lens, cancelling view-bobbing and FOV distortion
     * while supporting full 360-degree 3D tracer lines (including targets behind the player).
     */
    public static Vec3d computeTracerOrigin(
            Camera camera,
            Matrix4f viewMatrix,
            Matrix4f projectionMatrix,
            float ndcX,
            float ndcY
    ) {
        Vec3d camPos = camera.getCameraPos();
        if (viewMatrix != null && projectionMatrix != null) {
            float detP = projectionMatrix.determinant();
            float detV = viewMatrix.determinant();
            if (Math.abs(detP) > 1e-9f && Math.abs(detV) > 1e-9f) {
                Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
                Matrix4f invView = new Matrix4f(viewMatrix).invert();
                Vector4f p = new Vector4f(ndcX, ndcY, 0.0f, 1.0f).mul(invProj).mul(invView);
                if (Math.abs(p.w) > 1e-6f) {
                    p.div(p.w);
                    return new Vec3d(camPos.x + p.x, camPos.y + p.y, camPos.z + p.z);
                }
            }
        }
        Vec3d forward = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        return camPos.add(forward.multiply(0.20));
    }

    /**
     * Draws a clean, non-cluttered 16x16 horizontal chunk boundary marker with clear visual
     * hierarchy between low-, medium-, and high-confidence suspicious chunk detections.
     */
    public static void drawFlatChunkMarker(
            MatrixStack matrices,
            Camera camera,
            int chunkX,
            int chunkZ,
            double markerY,
            int outlineColor,
            int accentColor,
            int fillAlpha,
            float lineWidth,
            float cornerPostHeight,
            boolean highConfidence,
            boolean throughWalls
    ) {
        Vec3d cam = camera.getCameraPos();
        float inset = 0.08f;
        float x1 = (float) ((chunkX << 4) + inset - cam.x);
        float z1 = (float) ((chunkZ << 4) + inset - cam.z);
        float x2 = (float) ((chunkX << 4) + 16.0 - inset - cam.x);
        float z2 = (float) ((chunkZ << 4) + 16.0 - inset - cam.z);
        float y = (float) (markerY - cam.y);

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();

        if (fillAlpha > 0) {
            int fillCol = ColorUtil.withAlpha(outlineColor, fillAlpha);
            BufferBuilder quadBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            quadBuf.vertex(mat, x1, y, z1).color(fillCol);
            quadBuf.vertex(mat, x1, y, z2).color(fillCol);
            quadBuf.vertex(mat, x2, y, z2).color(fillCol);
            quadBuf.vertex(mat, x2, y, z1).color(fillCol);
            flushLayer(quadBuf, throughWalls ? LAYER_QUADS_XRAY : LAYER_QUADS_DEPTH);
        }

        float lw = MathUtil.clamp(lineWidth, 1.0f, 4.0f);
        BufferBuilder lineBuf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.LINES,
                VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH
        );

        // Outer chunk boundary perimeter
        edge(lineBuf, entry, x1, y, z1, x2, y, z1, outlineColor, lw);
        edge(lineBuf, entry, x2, y, z1, x2, y, z2, outlineColor, lw);
        edge(lineBuf, entry, x2, y, z2, x1, y, z2, outlineColor, lw);
        edge(lineBuf, entry, x1, y, z2, x1, y, z1, outlineColor, lw);

        // Subtle vertical corner ticks for medium/high confidence readability at shallow viewing angles
        if (cornerPostHeight > 0.05f) {
            float yTop = y + cornerPostHeight;
            edge(lineBuf, entry, x1, y, z1, x1, yTop, z1, accentColor, lw);
            edge(lineBuf, entry, x2, y, z1, x2, yTop, z1, accentColor, lw);
            edge(lineBuf, entry, x2, y, z2, x2, yTop, z2, accentColor, lw);
            edge(lineBuf, entry, x1, y, z2, x1, yTop, z2, accentColor, lw);
        }

        // High-confidence inner corner chevrons (crisp white/accent double-frame cue)
        if (highConfidence) {
            float in = 0.55f;
            float br = 2.6f;
            float yIn = y + 0.03f;
            float ix1 = x1 + in, iz1 = z1 + in, ix2 = x2 - in, iz2 = z2 - in;
            edge(lineBuf, entry, ix1, yIn, iz1, ix1 + br, yIn, iz1, accentColor, lw);
            edge(lineBuf, entry, ix1, yIn, iz1, ix1, yIn, iz1 + br, accentColor, lw);
            edge(lineBuf, entry, ix2, yIn, iz1, ix2 - br, yIn, iz1, accentColor, lw);
            edge(lineBuf, entry, ix2, yIn, iz1, ix2, yIn, iz1 + br, accentColor, lw);
            edge(lineBuf, entry, ix2, yIn, iz2, ix2 - br, yIn, iz2, accentColor, lw);
            edge(lineBuf, entry, ix2, yIn, iz2, ix2, yIn, iz2 - br, accentColor, lw);
            edge(lineBuf, entry, ix1, yIn, iz2, ix1 + br, yIn, iz2, accentColor, lw);
            edge(lineBuf, entry, ix1, yIn, iz2, ix1, yIn, iz2 - br, accentColor, lw);
        }

        flushLayer(lineBuf, throughWalls ? LAYER_LINES_XRAY : LAYER_LINES_DEPTH);
    }

    public static float[] projectToScreen(
            Vec3d worldPos,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            int scaledWidth,
            int scaledHeight
    ) {
        Vec3d cam = camera.getCameraPos();
        Vector4f clip = new Vector4f(
                (float) (worldPos.x - cam.x),
                (float) (worldPos.y - cam.y),
                (float) (worldPos.z - cam.z),
                1.0f
        );
        clip.mul(positionMatrix);
        clip.mul(projectionMatrix);
        if (clip.w <= 0.05f) {
            return null;
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;
        float sx = (ndcX * 0.5f + 0.5f) * scaledWidth;
        float sy = (1.0f - (ndcY * 0.5f + 0.5f)) * scaledHeight;
        return new float[]{sx, sy, ndcZ};
    }

    public static void drawBillboardLabel(
            MatrixStack matrices,
            Camera camera,
            Vec3d worldPos,
            String label,
            int textColor,
            int borderColor,
            float baseScale,
            boolean throughWalls
    ) {
        if (label == null || label.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        if (tr == null) {
            return;
        }
        Vec3d camPos = camera.getCameraPos();
        double dist = camPos.distanceTo(worldPos);
        float distScale = (float) Math.max(1.0, dist * 0.085) * 0.025f * baseScale;

        matrices.push();
        matrices.translate(worldPos.x - camPos.x, worldPos.y - camPos.y, worldPos.z - camPos.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(distScale, -distScale, distScale);

        Text styled = DGFontRenderer.styled(label, false);
        float width = tr.getWidth(styled);
        float halfW = width * 0.5f + 4.0f;
        float top = -2.0f;
        float bottom = 10.5f;

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();
        BufferBuilder bg = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int bgCol = 0xCC0A0F0C;
        bg.vertex(mat, -halfW, bottom, 0.01f).color(bgCol);
        bg.vertex(mat, halfW, bottom, 0.01f).color(bgCol);
        bg.vertex(mat, halfW, top, 0.01f).color(bgCol);
        bg.vertex(mat, -halfW, top, 0.01f).color(bgCol);
        flushLayer(bg, throughWalls ? LAYER_QUADS_XRAY : LAYER_QUADS_DEPTH);

        BufferBuilder border = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
        edge(border, entry, -halfW, top, 0.0f, halfW, top, 0.0f, borderColor, 1.5f);
        edge(border, entry, halfW, top, 0.0f, halfW, bottom, 0.0f, borderColor, 1.5f);
        edge(border, entry, halfW, bottom, 0.0f, -halfW, bottom, 0.0f, borderColor, 1.5f);
        edge(border, entry, -halfW, bottom, 0.0f, -halfW, top, 0.0f, borderColor, 1.5f);
        flushLayer(border, throughWalls ? LAYER_LINES_XRAY : LAYER_LINES_DEPTH);

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        TextRenderer.TextLayerType layerType = throughWalls
                ? TextRenderer.TextLayerType.SEE_THROUGH
                : TextRenderer.TextLayerType.NORMAL;
        tr.draw(styled, -width * 0.5f, 0.0f, textColor, false, mat, immediate, layerType, 0, 0xF000F0);
        immediate.draw();

        matrices.pop();
    }

    private static void edge(BufferBuilder buf, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int color, float lw) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 1e-4f ? dx / len : 1.0f;
        float ny = len > 1e-4f ? dy / len : 0.0f;
        float nz = len > 1e-4f ? dz / len : 0.0f;
        Matrix4f mat = entry.getPositionMatrix();
        buf.vertex(mat, x1, y1, z1).color(color).normal(entry, nx, ny, nz).lineWidth(lw);
        buf.vertex(mat, x2, y2, z2).color(color).normal(entry, nx, ny, nz).lineWidth(lw);
    }

    private static void flushLayer(BufferBuilder buf, RenderLayer layer) {
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            layer.draw(built);
        }
    }
}
