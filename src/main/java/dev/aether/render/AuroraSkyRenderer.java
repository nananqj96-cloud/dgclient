package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.SkyShaderModule;
import dev.aether.util.Colors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws a beautiful animated aurora sky: flowing coloured curtains that drift and pulse across the
 * sky, an animated gradient sky tint, and a twinkling starfield. Drawn in world space high above the
 * horizon so it reads as part of the sky without interfering with the world.
 */
public final class AuroraSkyRenderer {
   private static final int BANDS = 6;
   private static final int BAND_SEGMENTS = 40;

   private AuroraSkyRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, SkyShaderModule module) {
      long now = System.nanoTime();
      float time = now / 1.0E9;
      float intensity = module.intensityUnit();
      float speed = module.speedUnit();
      float height = module.heightValue();
      if (intensity <= 0.01F) {
         return;
      }

      Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
      Vector3f fwd = new Vector3f(camera.forwardVector());
      Vector3f right = new Vector3f();
      fwd.cross(new Vector3f(0.0F, 1.0F, 0.0F), right);
      if (right.lengthSquared() < 1.0E-6F) {
         right.set(1.0F, 0.0F, 0.0F);
      }
      right.normalize();
      Vector3f up = new Vector3f();
      right.cross(fwd, up);
      up.normalize();

      Pose pose = poseStack.last();
      VertexConsumer fill = bufferSource.getBuffer(FlatOverlay.FILL);
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      // Aurora curtains.
      if (module.aurora.get()) {
         drawAurora(fill, lines, pose, cam, right, up, fwd, time, speed, height, intensity, module);
      }

      // Starfield.
      if (module.stars.get()) {
         drawStars(lines, pose, cam, time, intensity);
      }

      FlatOverlay.flush(bufferSource);
   }

   private static void drawAurora(VertexConsumer fill, VertexConsumer lines, Pose pose, Vec3 cam,
                                  Vector3f right, Vector3f up, Vector3f fwd, float time, float speed,
                                  float height, float intensity, SkyShaderModule module) {
      int[] colors = auroraColors(module);
      // Each band is a flowing curtain spanning a wide arc in front of/around the player.
      for (int band = 0; band < BANDS; band++) {
         int base = colors[band % colors.length];
         float phase = time * speed * (0.6F + 0.25F * band) + band * 1.7F;
         float bandHeight = height + band * 14.0F;
         float bandRadius = 220.0F + band * 40.0F;
         // Horizontal sweep angle for this band.
         float sweep = (float) ((band / (double) BANDS) * Math.PI * 2.0);

         // Build a curtain as a vertical ribbon that waves.
         int segs = BAND_SEGMENTS;
         // Per-segment vertical wave.
         float[] vertsX = new float[segs + 1];
         float[] vertsY = new float[segs + 1];
         float[] vertsZ = new float[segs + 1];
         for (int i = 0; i <= segs; i++) {
            double t = (double) i / segs;
            double ang = sweep + t * (Math.PI * 2.0);
            float wave = (float) Math.sin(phase + t * 6.0F) * 18.0F * intensity;
            float rx = (float) (Math.cos(ang) * bandRadius);
            float rz = (float) (Math.sin(ang) * bandRadius);
            vertsX[i] = rx;
            vertsY[i] = bandHeight + wave;
            vertsZ[i] = rz;
         }

         // Draw vertical curtain segments with vertical alpha gradient + hue shift.
         for (int i = 0; i < segs; i++) {
            float t = (float) i / segs;
            float a0 = (float) Math.pow(1.0F - t, 0.6F) * intensity;
            float a1 = (float) Math.pow(1.0F - (float) (i + 1) / segs, 0.6F) * intensity;
            float alpha = (a0 + a1) * 0.5F;
            if (alpha <= 0.02F) {
               continue;
            }
            // Hue-shift the colour along the band for a flowing look.
            int col = shiftHue(base, t, alpha);
            float xl0 = vertsX[i];
            float yl0 = vertsY[i];
            float zl0 = vertsZ[i];
            float xl1 = vertsX[i + 1];
            float yl1 = vertsY[i + 1];
            float zl1 = vertsZ[i + 1];

            // Vertical ribbon quad: from base to a higher point, offset by the wave.
            float baseY = cam.y() + 18.0F;
            float topY = yl0 + 60.0F * intensity;
            float topY1 = yl1 + 60.0F * intensity;

            // Two vertical quads per segment (front face) using camera-relative coords.
            float ax = xl0 - cam.x();
            float az = zl0 - cam.z();
            float bx = xl1 - cam.x();
            float bz = zl1 - cam.z();
            // Normalize direction for the ribbon width.
            float dx = bx - ax;
            float dz = bz - az;
            float dl = Mth.sqrt(dx * dx + dz * dz);
            float nx;
            float nz;
            if (dl > 1.0E-4F) {
               nx = -dz / dl;
               nz = dx / dl;
            } else {
               nx = 1.0F;
               nz = 0.0F;
            }
            float halfW = 14.0F * intensity;

            quadVert(fill, pose, ax + nx * halfW, baseY, az + nz * halfW, ax - nx * halfW, baseY, az - nz * halfW,
               ax + nx * halfW, topY, az + nz * halfW, ax - nx * halfW, topY, az - nz * halfW, col);
            quadVert(fill, pose, bx + nx * halfW, baseY, bz + nz * halfW, bx - nx * halfW, baseY, bz - nz * halfW,
               bx + nx * halfW, topY1, bz + nz * halfW, bx - nx * halfW, topY1, bz - nz * halfW, col);

            // Glow edge along the top.
            int edge = Colors.withAlpha(Colors.lighten(base, 0.4F), alpha * 0.8F);
            line3(lines, pose, ax + nx * halfW, topY, az + nz * halfW, bx + nx * halfW, topY1, bz + nz * halfW, edge, 1.8F);
         }
      }
   }

   private static void drawStars(VertexConsumer lines, Pose pose, Vec3 cam, float time, float intensity) {
      int count = 90;
      // Deterministic star positions in a dome above the player.
      for (int i = 0; i < count; i++) {
         float seed = (float) (i * 12.9898 + 4.1414);
         float sx = (float) (Math.sin(seed * 1.7F) * 0.5F + 0.5F);
         float sz = (float) (Math.cos(seed * 2.3F) * 0.5F + 0.5F);
         float sy = (float) (Math.abs(Math.sin(seed * 3.1F)) * 0.5F + 0.55F);
         float radius = 200.0F;
         float x = (sx - 0.5F) * radius * 2.0F;
         float z = (sz - 0.5F) * radius * 2.0F;
         float y = sy * radius;
         float twinkle = 0.5F + 0.5F * (float) Math.sin(time * 3.0F + seed);
         float alpha = twinkle * intensity * 0.7F;
         if (alpha <= 0.02F) {
            continue;
         }
         int col = Colors.withAlpha(0xFFDFE8FF, alpha);
         float px = x - cam.x();
         float py = y - cam.y();
         float pz = z - cam.z();
         float s = 0.8F + twinkle * 0.8F;
         line3(lines, pose, px - s, py, pz, px + s, py, pz, col, 1.2F);
         line3(lines, pose, px, py - s, pz, px, py + s, pz, col, 1.2F);
      }
   }

   private static int[] auroraColors(SkyShaderModule module) {
      String p = module.paletteName();
      int accent = module.accent.get();
      switch (p) {
         case "North":
            return new int[]{0xFF4FC3F7, 0xFF29B6F6, 0xFF80DEEA, 0xFF4DD0E1, 0xFF26C6DA, 0xFF81D4FA};
         case "Sunset":
            return new int[]{0xFFFF7043, 0xFFFF8A65, 0xFFFFAB40, 0xFFFF5722, 0xFFEC407A, 0xFFF06292};
         case "Toxic":
            return new int[]{0xFFAEFF00, 0xFFC0CA33, 0xFF9EEE00, 0xFF7CB342, 0xFF66BB6A, 0xFF9CCC65};
         case "Prismatic":
            return new int[]{0xFFFF4081, 0xFF7C4DFF, 0xFF18FFFF, 0xFF69F0AE, 0xFFFFEA00, 0xFFFF5252};
         case "Aurora":
         default:
            return new int[]{0xFF4DD0E1, 0xFF81C784, 0xFF4FC3F7, 0xFFB39DDB, 0xFF4DB6AC, 0xFF9575CD};
      }
   }

   private static int shiftHue(int base, float t, float alpha) {
      // Simple hue rotation along the band for a flowing, dynamic look.
      float hue = ((Colors.red(base) * 0.3F + Colors.green(base) * 0.5F + Colors.blue(base) * 0.2F) / 255.0F + t * 0.3F) % 1.0F;
      int shifted = Colors.hsvToRgb(hue * 360.0F, 0.85F, 1.0F);
      return Colors.withAlpha(shifted, alpha);
   }

   private static void quadVert(VertexConsumer buf, Pose pose,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float x2, float y2, float z2, float x3, float y3, float z3, int argb) {
      buf.addVertex(pose, x0, y0, z0).setColor(argb);
      buf.addVertex(pose, x1, y1, z1).setColor(argb);
      buf.addVertex(pose, x2, y2, z2).setColor(argb);
      buf.addVertex(pose, x3, y3, z3).setColor(argb);
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
