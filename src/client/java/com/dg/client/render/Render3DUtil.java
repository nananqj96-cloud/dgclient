package com.dg.client.render;

import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * 3D world rendering utilities: filled boxes, vertical gradient boxes, outlines,
 * corner-bracket ESP boxes, multi-pass glowing outlines, 3D lines with color gradients,
 * camera-facing floating labels, and 3D-to-2D screen projection.
 */
public final class Render3DUtil {
    private Render3DUtil() {
    }

    public static void begin3D(boolean throughWalls) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    public static void end3D() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        GL11.glLineWidth(1.0f);
    }

    public static Box offsetToCamera(Box worldBox, Camera camera) {
        Vec3d cam = camera.getPos();
        return worldBox.offset(-cam.x, -cam.y, -cam.z);
    }

    public static void drawFilledBox(MatrixStack matrices, Camera camera, Box worldBox, int color, boolean throughWalls) {
        if (ColorUtil.getAlpha(color) <= 0) {
            return;
        }
        drawGradientBoxY(matrices, camera, worldBox, color, color, throughWalls);
    }

    /**
     * Draws a translucent 3D box with smooth vertical gradient between bottomColor and topColor.
     */
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
        begin3D(throughWalls);
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

        flush(buf);
        end3D();
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

        Matrix4f mat = matrices.peek().getPositionMatrix();
        begin3D(throughWalls);
        GL11.glLineWidth(MathUtil.clamp(lineWidth, 1.0f, 6.0f));
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Bottom ring
        edge(buf, mat, x1, y1, z1, x2, y1, z1, color);
        edge(buf, mat, x2, y1, z1, x2, y1, z2, color);
        edge(buf, mat, x2, y1, z2, x1, y1, z2, color);
        edge(buf, mat, x1, y1, z2, x1, y1, z1, color);
        // Top ring
        edge(buf, mat, x1, y2, z1, x2, y2, z1, color);
        edge(buf, mat, x2, y2, z1, x2, y2, z2, color);
        edge(buf, mat, x2, y2, z2, x1, y2, z2, color);
        edge(buf, mat, x1, y2, z2, x1, y2, z1, color);
        // Vertical pillars
        edge(buf, mat, x1, y1, z1, x1, y2, z1, color);
        edge(buf, mat, x2, y1, z1, x2, y2, z1, color);
        edge(buf, mat, x2, y1, z2, x2, y2, z2, color);
        edge(buf, mat, x1, y1, z2, x1, y2, z2, color);

        flush(buf);
        end3D();
    }

    /**
     * Multi-pass shader-style glowing 3D box outline + soft volumetric shell.
     */
    public static void drawGlowingBox(
            MatrixStack matrices,
            Camera camera,
            Box worldBox,
            int glowColor,
            int coreEdgeColor,
            float intensity,
            boolean throughWalls
    ) {
        int passes = 4;
        for (int i = passes; i >= 1; i--) {
            double expand = i * 0.028 * intensity;
            int shellAlpha = MathUtil.clamp(Math.round(24.0f * intensity / i), 3, 60);
            drawFilledBox(matrices, camera, worldBox.expand(expand), ColorUtil.withAlpha(glowColor, shellAlpha), throughWalls);
            int wireAlpha = MathUtil.clamp(Math.round(90.0f * intensity / i), 8, 160);
            drawBoxOutline(matrices, camera, worldBox.expand(expand * 0.8), 1.0f + i * 0.6f, ColorUtil.withAlpha(glowColor, wireAlpha), throughWalls);
        }
        drawBoxOutline(matrices, camera, worldBox, 1.8f, coreEdgeColor, throughWalls);
    }

    /**
     * 3D tactical corner-bracket box (used by ESP Corner Mode and BaseFinder accents).
     */
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

        Matrix4f mat = matrices.peek().getPositionMatrix();
        begin3D(throughWalls);
        GL11.glLineWidth(MathUtil.clamp(lineWidth, 1.0f, 6.0f));
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

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
            edge(buf, mat, c[0], c[1], c[2], c[0] + c[3] * dx, c[1], c[2], color);
            edge(buf, mat, c[0], c[1], c[2], c[0], c[1] + c[4] * dy, c[2], color);
            edge(buf, mat, c[0], c[1], c[2], c[0], c[1], c[2] + c[5] * dz, color);
        }

        flush(buf);
        end3D();
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
        Vec3d cam = camera.getPos();
        float x1 = (float) (startWorld.x - cam.x);
        float y1 = (float) (startWorld.y - cam.y);
        float z1 = (float) (startWorld.z - cam.z);
        float x2 = (float) (endWorld.x - cam.x);
        float y2 = (float) (endWorld.y - cam.y);
        float z2 = (float) (endWorld.z - cam.z);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        begin3D(throughWalls);
        GL11.glLineWidth(MathUtil.clamp(width, 1.0f, 5.0f));
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buf.vertex(mat, x1, y1, z1).color(startColor);
        buf.vertex(mat, x2, y2, z2).color(endColor);
        flush(buf);
        end3D();
    }

    /**
     * Projects a 3D world position into 2D scaled GUI coordinates [screenX, screenY, ndcZ].
     * Returns null when the point lies behind the camera clip plane.
     */
    public static float[] projectToScreen(
            Vec3d worldPos,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            int scaledWidth,
            int scaledHeight
    ) {
        Vec3d cam = camera.getPos();
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

    /**
     * Renders a crisp camera-facing billboard text tag in 3D space with a dark charcoal pill
     * and neon green border.
     */
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
        Vec3d camPos = camera.getPos();
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

        Matrix4f mat = matrices.peek().getPositionMatrix();
        begin3D(throughWalls);
        BufferBuilder bg = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int bgCol = 0xCC0A0F0C;
        bg.vertex(mat, -halfW, bottom, 0.01f).color(bgCol);
        bg.vertex(mat, halfW, bottom, 0.01f).color(bgCol);
        bg.vertex(mat, halfW, top, 0.01f).color(bgCol);
        bg.vertex(mat, -halfW, top, 0.01f).color(bgCol);
        flush(bg);

        GL11.glLineWidth(1.5f);
        BufferBuilder border = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        edge(border, mat, -halfW, top, 0.0f, halfW, top, 0.0f, borderColor);
        edge(border, mat, halfW, top, 0.0f, halfW, bottom, 0.0f, borderColor);
        edge(border, mat, halfW, bottom, 0.0f, -halfW, bottom, 0.0f, borderColor);
        edge(border, mat, -halfW, bottom, 0.0f, -halfW, top, 0.0f, borderColor);
        flush(border);

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        TextRenderer.TextLayerType layerType = throughWalls
                ? TextRenderer.TextLayerType.SEE_THROUGH
                : TextRenderer.TextLayerType.NORMAL;
        tr.draw(styled, -width * 0.5f, 0.0f, textColor, false, mat, immediate, layerType, 0, 0xF000F0);
        immediate.draw();
        end3D();

        matrices.pop();
    }

    private static void edge(BufferBuilder buf, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buf.vertex(mat, x1, y1, z1).color(color);
        buf.vertex(mat, x2, y2, z2).color(color);
    }

    private static void flush(BufferBuilder buf) {
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }
}
