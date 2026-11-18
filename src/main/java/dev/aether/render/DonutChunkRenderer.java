package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.util.Colors;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a DonutLarp chunk/area overlay with a clean, minimalistic design: a translucent fill
 * with slightly rounded corners, a thin brighter outline, and a soft outer glow. Shared by the
 * three DonutLarp modules so they all share the same look (colors are supplied per module).
 *
 * <p>Everything is drawn flat at a given Y with no depth test, so it reads cleanly over the world
 * without harsh edges.
 */
public final class DonutChunkRenderer {
   private static final int ARC_SEGMENTS = 8;

   private DonutChunkRenderer() {
   }

   /**
    * Draws a single rounded, glowing rectangle.
    *
    * @param x0       near X (world blocks)
    * @param z0       near Z
    * @param x1       far X
    * @param z1       far Z
    * @param y        base Y of the footprint
    * @param height   vertical extent (0 = flat footprint only)
    * @param fill     RGBA fill color
    * @param outline  RGBA outline color
    * @param glow     glow spread in blocks
    * @param radius   corner radius in blocks
    * @param opacity  fill opacity multiplier (0..1)
    */
   public static void renderRect(BufferSource bufferSource, PoseStack poseStack, Vec3 camera,
                                 double x0, double z0, double x1, double z1, double y, double height,
                                 int fill, int outline, float glow, float radius, float opacity) {
      int fillColor = Colors.withAlpha(fill, opacity);
      int lineColor = Colors.withAlpha(outline, 1.0F);

      // Soft outer glow: an inflated, very translucent fill.
      if (glow > 0.01F) {
         FlatOverlay.fillQuad(bufferSource, poseStack, camera,
            x0 - glow, z0 - glow, x1 + glow, z1 + glow, y,
            Colors.withAlpha(outline, Math.min(1.0F, opacity * 0.22F)));
      }

      // Translucent fill footprint.
      FlatOverlay.fillQuad(bufferSource, poseStack, camera, x0, z0, x1, z1, y, fillColor);

      // Rounded outline.
      roundedOutline(bufferSource, poseStack, camera, x0, z0, x1, z1, y, lineColor, radius, 2.0F);

      // Vertical sides when a height is set (gives a subtle 3D box).
      if (height > 0.02) {
         double y2 = y + height;
         com.mojang.blaze3d.vertex.VertexConsumer vbuf = bufferSource.getBuffer(FlatOverlay.LINES);
         PoseStack.Pose pose = poseStack.last();
         verticalEdge(vbuf, pose, camera, x0, z0, y, y2, lineColor);
         verticalEdge(vbuf, pose, camera, x1, z0, y, y2, lineColor);
         verticalEdge(vbuf, pose, camera, x1, z1, y, y2, lineColor);
         verticalEdge(vbuf, pose, camera, x0, z1, y, y2, lineColor);
      }
   }

   private static void verticalEdge(com.mojang.blaze3d.vertex.VertexConsumer vbuf, PoseStack.Pose pose,
                                     Vec3 camera, double x, double z, double y0, double y1, int color) {
      org.joml.Vector3f normal = new org.joml.Vector3f((float)(x - camera.x), 0.0F, (float)(z - camera.z));
      if (normal.length() < 1.0E-4F) {
         normal.set(1.0F, 0.0F, 0.0F);
      } else {
         normal.normalize();
      }
      vbuf.addVertex(pose, (float)(x - camera.x), (float)(y0 - camera.y), (float)(z - camera.z))
         .setColor(color).setNormal(pose, normal).setLineWidth(2.0F);
      vbuf.addVertex(pose, (float)(x - camera.x), (float)(y1 - camera.y), (float)(z - camera.z))
         .setColor(color).setNormal(pose, normal).setLineWidth(2.0F);
   }

   private static void roundedOutline(BufferSource bufferSource, PoseStack poseStack, Vec3 camera,
                                       double x0, double z0, double x1, double z1, double y,
                                       int color, float radius, float width) {
      double w = x1 - x0;
      double h = z1 - z0;
      double r = Math.min(radius, w / 2.0, h / 2.0);
      if (r <= 0.01) {
         FlatOverlay.edge(bufferSource, poseStack, camera, x0, z0, x1, z0, y, color, width);
         FlatOverlay.edge(bufferSource, poseStack, camera, x1, z0, x1, z1, y, color, width);
         FlatOverlay.edge(bufferSource, poseStack, camera, x1, z1, x0, z1, y, color, width);
         FlatOverlay.edge(bufferSource, poseStack, camera, x0, z1, x0, z0, y, color, width);
         return;
      }
      // Straight edges, trimmed by the corner radius.
      double bx0 = x0 + r, bx1 = x1 - r;
      double bz0 = z0 + r, bz1 = z1 - r;
      if (bx1 > bx0) {
         FlatOverlay.edge(bufferSource, poseStack, camera, bx0, z0, bx1, z0, y, color, width);
         FlatOverlay.edge(bufferSource, poseStack, camera, bx0, z1, bx1, z1, y, color, width);
      }
      if (bz1 > bz0) {
         FlatOverlay.edge(bufferSource, poseStack, camera, x0, bz0, x0, bz1, y, color, width);
         FlatOverlay.edge(bufferSource, poseStack, camera, x1, bz0, x1, bz1, y, color, width);
      }
      // Four quarter-circle corner arcs.
      arc(bufferSource, poseStack, camera, x0 + r, z0 + r, r, 180.0, 270.0, y, color, width);
      arc(bufferSource, poseStack, camera, x1 - r, z0 + r, r, 270.0, 360.0, y, color, width);
      arc(bufferSource, poseStack, camera, x1 - r, z1 - r, r, 0.0, 90.0, y, color, width);
      arc(bufferSource, poseStack, camera, x0 + r, z1 - r, r, 90.0, 180.0, y, color, width);
   }

   /** Draws a circular arc (in the XZ plane) between two angles (degrees, 0 = +X, 90 = +Z). */
   private static void arc(BufferSource bufferSource, PoseStack poseStack, Vec3 camera,
                           double cx, double cz, double r, double a0, double a1, double y,
                           int color, float width) {
      double rad = Math.toRadians(a0);
      double step = Math.toRadians((a1 - a0) / ARC_SEGMENTS);
      double px = cx + r * Math.cos(rad);
      double pz = cz + r * Math.sin(rad);
      for (int i = 1; i <= ARC_SEGMENTS; i++) {
         double ang = rad + step * i;
         double nx = cx + r * Math.cos(ang);
         double nz = cz + r * Math.sin(ang);
         FlatOverlay.edge(bufferSource, poseStack, camera, px, pz, nx, nz, y, color, width);
         px = nx;
         pz = nz;
      }
   }

   public static void flush(BufferSource bufferSource) {
      FlatOverlay.flush(bufferSource);
   }
}
