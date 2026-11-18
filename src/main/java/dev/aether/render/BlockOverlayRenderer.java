package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.BlockOverlayModule;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders a fill/outline/glow overlay on the block the player is looking at.
 */
public final class BlockOverlayRenderer {
   private BlockOverlayRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, BlockOverlayModule module) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return;
      }
      HitResult hit = mc.player.pick(8.0, 0.0F, false);
      if (!(hit instanceof BlockHitResult blockHit)) {
         return;
      }
      AABB box = mc.level.getBlockState(blockHit.getBlockPos()).getOcclusionShape().bounds();
      if (box == null || box.getSize() == 0.0) {
         box = new AABB(blockHit.getBlockPos());
      }

      float ax = (float) (box.minX - cam.x());
      float ay = (float) (box.minY - cam.y());
      float az = (float) (box.minZ - cam.z());
      float bx = (float) (box.maxX - cam.x());
      float by = (float) (box.maxY - cam.y());
      float bz = (float) (box.maxZ - cam.z());

      int rgb = module.primaryRgb();
      float fillAlpha = module.fillAlphaValue();
      float lineWidth = module.lineWidthValue();
      float glow = module.glowStrengthValue();
      String mode = module.mode.get();

      Pose pose = poseStack.last();
      VertexConsumer fill = bufferSource.getBuffer(FlatOverlay.FILL);
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      if (mode.equals("Fill") || mode.equals("Fill + Outline") || mode.equals("Glow")) {
         int fillColor = Colors.withAlpha(rgb, fillAlpha);
         fill.addVertex(pose, ax, ay, az).setColor(fillColor);
         fill.addVertex(pose, bx, ay, az).setColor(fillColor);
         fill.addVertex(pose, bx, by, bz).setColor(fillColor);
         fill.addVertex(pose, ax, by, bz).setColor(fillColor);
      }

      if (mode.equals("Outline") || mode.equals("Fill + Outline") || mode.equals("Glow")) {
         int lineColor = Colors.withAlpha(rgb, 1.0F);
         if (mode.equals("Glow")) {
            int halo = Colors.withAlpha(rgb, 0.3F * glow);
            edgeGlow(fill, pose, ax, ay, az, bx, by, bz, halo, glow);
         }
         edge(lines, pose, ax, ay, az, bx, ay, az, lineColor, lineWidth);
         edge(lines, pose, bx, ay, az, bx, ay, bz, lineColor, lineWidth);
         edge(lines, pose, bx, ay, bz, ax, ay, bz, lineColor, lineWidth);
         edge(lines, pose, ax, ay, bz, ax, ay, az, lineColor, lineWidth);
         edge(lines, pose, ax, ay, az, ax, by, az, lineColor, lineWidth);
         edge(lines, pose, bx, ay, az, bx, by, az, lineColor, lineWidth);
         edge(lines, pose, bx, by, az, bx, by, bz, lineColor, lineWidth);
         edge(lines, pose, bx, by, bz, ax, by, bz, lineColor, lineWidth);
         edge(lines, pose, ax, by, bz, ax, by, az, lineColor, lineWidth);
         edge(lines, pose, ax, by, az, ax, ay, az, lineColor, lineWidth);
         edge(lines, pose, ax, by, az, bx, by, az, lineColor, lineWidth);
         edge(lines, pose, bx, by, az, bx, by, bz, lineColor, lineWidth);
         edge(lines, pose, bx, by, bz, ax, by, bz, lineColor, lineWidth);
         edge(lines, pose, ax, by, bz, ax, by, az, lineColor, lineWidth);
      }

      FlatOverlay.flush(bufferSource);
   }

   private static void edgeGlow(VertexConsumer fill, Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int argb, float glow) {
      // Draw enlarged glow quads around the box.
      float m = 0.5F * glow;
      int c = argb;
      fill.addVertex(pose, x0 - m, y0 - m, z0 - m).setColor(c);
      fill.addVertex(pose, x1 + m, y0 - m, z0 - m).setColor(c);
      fill.addVertex(pose, x1 + m, y1 + m, z1 + m).setColor(c);
      fill.addVertex(pose, x0 - m, y1 + m, z1 + m).setColor(c);
   }

   private static void edge(VertexConsumer buf, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
      Vector3f n = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
      if (n.lengthSquared() > 1.0E-9F) {
         n.normalize();
      } else {
         n.set(0.0F, 1.0F, 0.0F);
      }
      buf.addVertex(pose, x1, y1, z1).setColor(argb).setNormal(pose, n).setLineWidth(width);
      buf.addVertex(pose, x2, y2, z2).setColor(argb).setNormal(pose, n).setLineWidth(width);
   }
}
