package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.TargetESPModule;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws a highlight box and a tracer line to the entity the player is currently looking at.
 */
public final class TargetEspRenderer {
   private TargetEspRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, TargetESPModule module) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return;
      }
      HitResult hit = mc.player.pick(module.rangeValue(), 0.0F, false);
      if (!(hit instanceof EntityHitResult entityHit)) {
         return;
      }
      Entity target = entityHit.getEntity();
      if (target == mc.player) {
         return;
      }
      Vec3 pos = target.getPosition();
      float dx = (float) (pos.x - cam.x());
      float dy = (float) (pos.y - cam.y());
      float dz = (float) (pos.z - cam.z());
      float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist > module.rangeValue()) {
         return;
      }

      int rgb = module.colorRgb();
      Pose pose = poseStack.last();
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      // Tracer line from camera to target.
      float fx = dx / dist;
      float fy = dy / dist;
      float fz = dz / dist;
      int tracer = Colors.withAlpha(rgb, 0.6F);
      line3(lines, pose, 0.0F, 0.0F, 0.0F, dx, dy, dz, tracer, 1.5F);

      // Highlight box.
      float w = target.getBbWidth() / 2.0F;
      float h = target.getBbHeight();
      float x0 = dx - w;
      float y0 = dy;
      float z0 = dz - w;
      float x1 = dx + w;
      float y1 = dy + h;
      float z1 = dz + w;
      int box = Colors.withAlpha(rgb, 1.0F);
      edge(lines, pose, x0, y0, z0, x1, y0, z0, box);
      edge(lines, pose, x1, y0, z0, x1, y0, z1, box);
      edge(lines, pose, x1, y0, z1, x0, y0, z1, box);
      edge(lines, pose, x0, y0, z1, x0, y0, z0, box);
      edge(lines, pose, x0, y0, z0, x0, y1, z0, box);
      edge(lines, pose, x1, y0, z0, x1, y1, z0, box);
      edge(lines, pose, x1, y1, z0, x1, y1, z1, box);
      edge(lines, pose, x1, y1, z1, x0, y1, z1, box);
      edge(lines, pose, x0, y1, z1, x0, y1, z0, box);
      edge(lines, pose, x0, y1, z0, x0, y0, z0, box);
      edge(lines, pose, x0, y1, z0, x1, y1, z0, box);
      edge(lines, pose, x1, y1, z0, x1, y1, z1, box);
      edge(lines, pose, x1, y1, z1, x0, y1, z1, box);
      edge(lines, pose, x0, y1, z1, x0, y1, z0, box);

      FlatOverlay.flush(bufferSource);
   }

   private static void edge(VertexConsumer buf, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
      Vector3f n = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
      if (n.lengthSquared() > 1.0E-9F) {
         n.normalize();
      } else {
         n.set(0.0F, 1.0F, 0.0F);
      }
      buf.addVertex(pose, x1, y1, z1).setColor(argb).setNormal(pose, n).setLineWidth(1.5F);
      buf.addVertex(pose, x2, y2, z2).setColor(argb).setNormal(pose, n).setLineWidth(1.5F);
   }

   private static void line3(VertexConsumer buf, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
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
