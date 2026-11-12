package com.dg.client.module.modules.esp;

import com.dg.client.event.events.Render3DEvent;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.Render3DUtil;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared 3D & 2D ESP rendering modes, frustum checks, and loaded-chunk iteration helpers.
 */
public final class EspRenderHelper {
    public enum RenderMode {
        BOX_BOTH,
        BOX_OUTLINE,
        BOX_FILL,
        SHADER_GLOW,
        BOX_2D,
        CORNER_BOX
    }

    public record ProjectedRect(float x1, float y1, float x2, float y2, int color, float healthRatio) {
    }

    private EspRenderHelper() {
    }

    public static void renderTargetBox(
            Render3DEvent event,
            Box worldBox,
            RenderMode mode,
            int primaryColor,
            int fillAlpha,
            float lineWidth,
            boolean throughWalls,
            List<ProjectedRect> out2dQueue,
            float healthRatio
    ) {
        switch (mode) {
            case BOX_OUTLINE -> Render3DUtil.drawBoxOutline(
                    event.getMatrices(), event.getCamera(), worldBox, lineWidth, primaryColor, throughWalls);
            case BOX_FILL -> Render3DUtil.drawFilledBox(
                    event.getMatrices(), event.getCamera(), worldBox, ColorUtil.withAlpha(primaryColor, fillAlpha), throughWalls);
            case BOX_BOTH -> {
                Render3DUtil.drawFilledBox(
                        event.getMatrices(), event.getCamera(), worldBox, ColorUtil.withAlpha(primaryColor, fillAlpha), throughWalls);
                Render3DUtil.drawBoxOutline(
                        event.getMatrices(), event.getCamera(), worldBox, lineWidth, primaryColor, throughWalls);
            }
            case SHADER_GLOW -> Render3DUtil.drawGlowingBox(
                    event.getMatrices(), event.getCamera(), worldBox, primaryColor, ColorUtil.PURE_WHITE, 0.85f, throughWalls);
            case CORNER_BOX -> Render3DUtil.drawCornerBox(
                    event.getMatrices(), event.getCamera(), worldBox, 0.28f, lineWidth, primaryColor, throughWalls);
            case BOX_2D -> {
                if (out2dQueue != null) {
                    ProjectedRect rect = projectBoxTo2D(event, worldBox, primaryColor, healthRatio);
                    if (rect != null) {
                        out2dQueue.add(rect);
                    }
                }
            }
        }
    }

    public static ProjectedRect projectBoxTo2D(Render3DEvent event, Box box, int color, float healthRatio) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) {
            return null;
        }
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        Matrix4f posMat = event.getPositionMatrix();
        Matrix4f projMat = event.getProjectionMatrix();

        double[][] corners = {
                {box.minX, box.minY, box.minZ},
                {box.minX, box.minY, box.maxZ},
                {box.maxX, box.minY, box.minZ},
                {box.maxX, box.minY, box.maxZ},
                {box.minX, box.maxY, box.minZ},
                {box.minX, box.maxY, box.maxZ},
                {box.maxX, box.maxY, box.minZ},
                {box.maxX, box.maxY, box.maxZ}
        };

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        int valid = 0;

        for (double[] c : corners) {
            float[] p = Render3DUtil.projectToScreen(new Vec3d(c[0], c[1], c[2]), event.getCamera(), posMat, projMat, sw, sh);
            if (p != null) {
                valid++;
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        if (valid < 2 || maxX - minX < 1.5f || maxY - minY < 1.5f) {
            return null;
        }
        if (maxX < 0 || minX > sw || maxY < 0 || minY > sh) {
            return null;
        }
        return new ProjectedRect(minX, minY, maxX, maxY, color, healthRatio);
    }

    public static void draw2DProjectedRects(DrawContext ctx, List<ProjectedRect> rects) {
        if (rects.isEmpty()) {
            return;
        }
        MatrixStack ms = ctx.getMatrices();
        for (ProjectedRect r : rects) {
            float w = r.x2() - r.x1();
            float h = r.y2() - r.y1();
            Render2DUtil.drawRoundedOutline(ms, r.x1() - 1.0f, r.y1() - 1.0f, w + 2.0f, h + 2.0f, 1.5f, 2.2f, 0xBB0A0F0C);
            Render2DUtil.drawRoundedOutline(ms, r.x1(), r.y1(), w, h, 1.5f, 1.1f, r.color());

            if (r.healthRatio() >= 0.0f) {
                float barH = h * Math.min(1.0f, Math.max(0.0f, r.healthRatio()));
                int hpCol = ColorUtil.healthGreenWhite(r.healthRatio());
                Render2DUtil.drawQuad(ms, r.x1() - 4.0f, r.y1(), 2.0f, h, 0xAA0A0F0C);
                Render2DUtil.drawQuad(ms, r.x1() - 3.6f, r.y2() - barH, 1.3f, barH, hpCol);
            }
        }
    }

    /**
     * Collects loaded WorldChunks around the player within radiusBlocks.
     */
    public static List<WorldChunk> getLoadedChunksInRange(double radiusBlocks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<WorldChunk> chunks = new ArrayList<>();
        if (mc.world == null || mc.player == null) {
            return chunks;
        }
        int chunkRadius = Math.min(mc.options.getClampedViewDistance() + 1, (int) Math.ceil(radiusBlocks / 16.0));
        ChunkPos center = mc.player.getChunkPos();
        var cm = mc.world.getChunkManager();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (dx * dx + dz * dz > (chunkRadius + 1) * (chunkRadius + 1)) {
                    continue;
                }
                WorldChunk wc = cm.getWorldChunk(center.x + dx, center.z + dz);
                if (wc != null && !wc.isEmpty()) {
                    chunks.add(wc);
                }
            }
        }
        return chunks;
    }
}
