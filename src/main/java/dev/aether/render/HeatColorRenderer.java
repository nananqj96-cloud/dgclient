package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.HeatColorModule;
import dev.aether.util.Colors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders a heat-vision tint around nearby living entities. The colour shifts from red (low health)
 * to green (full health), with an optional translucent fill box.
 */
public final class HeatColorRenderer {
   private HeatColorRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, HeatColorModule module) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      Camera camera = mc.gameRenderer.getMainCamera();
      Vec3 eyePos = mc.player.getEyePosition();
      float rangeSq = module.rangeValue() * module.rangeValue();
      float tick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

      Pose pose = poseStack.last();
      VertexConsumer fill = bufferSource.getBuffer(FlatOverlay.FILL);
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      boolean drew = false;
      for (Entity entity : mc.level.getEntities()) {
         if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            continue;
         }
         if (entity == mc.player) {
            continue;
         }
         if (module.playersOnly.get() && !(entity instanceof Player)) {
            continue;
         }
         net.minecraft.util.math.Box bb = living.getBoundingBox();
         Vec3 lerped = new Vec3((bb.minX + bb.maxX) / 2.0, (bb.minY + bb.maxY) / 2.0, (bb.minZ + bb.maxZ) / 2.0);
         if (eyePos.distanceToSquared(lerped) > rangeSq) {
            continue;
         }
         float health = living.getMaxHealth() <= 0.0F ? 1.0F
            : Mth.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F);
         // Red -> green heat gradient.
         int rgb = Colors.hsvToRgb(health * 0.333F * 360.0F, 1.0F, 1.0F) & 16777215;

         float x0 = (float) bb.minX;
         float y0 = (float) bb.minY;
         float z0 = (float) bb.minZ;
         float x1 = (float) bb.maxX;
         float y1 = (float) bb.maxY;
         float z1 = (float) bb.maxZ;

         // Camera-relative coordinates.
         float ax = x0 - cam.x();
         float ay = y0 - cam.y();
         float az = z0 - cam.z();
         float bx = x1 - cam.x();
         float by = y1 - cam.y();
         float bz = z1 - cam.z();

         if (module.filled.get()) {
            int fillColor = Colors.withAlpha(rgb, 0.5F);
            quad(fill, pose, ax, ay, az, bx, by, bz, fillColor);
         }
         int lineColor = Colors.withAlpha(rgb, 1.0F);
         // Draw box edges.
         edge(lines, pose, ax, ay, az, bx, ay, az, lineColor);
         edge(lines, pose, bx, ay, az, bx, ay, bz, lineColor);
         edge(lines, pose, bx, ay, bz, ax, ay, bz, lineColor);
         edge(lines, pose, ax, ay, bz, ax, ay, az, lineColor);
         edge(lines, pose, ax, ay, az, ax, by, az, lineColor);
         edge(lines, pose, bx, ay, az, bx, by, az, lineColor);
         edge(lines, pose, bx, by, az, bx, by, bz, lineColor);
         edge(lines, pose, bx, by, bz, ax, by, bz, lineColor);
         edge(lines, pose, ax, by, bz, ax, by, az, lineColor);
         edge(lines, pose, ax, by, az, ax, ay, az, lineColor);
         edge(lines, pose, ax, by, az, bx, by, az, lineColor);
         edge(lines, pose, bx, by, az, bx, by, bz, lineColor);
         edge(lines, pose, bx, by, bz, ax, by, bz, lineColor);
         edge(lines, pose, ax, by, bz, ax, by, az, lineColor);
         drew = true;
      }

      if (drew) {
         FlatOverlay.flush(bufferSource);
      }
   }

   private static void quad(VertexConsumer buf, Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
      buf.addVertex(pose, x0, y0, z0).setColor(argb);
      buf.addVertex(pose, x1, y0, z0).setColor(argb);
      buf.addVertex(pose, x1, y1, z1).setColor(argb);
      buf.addVertex(pose, x0, y1, z1).setColor(argb);
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
}
