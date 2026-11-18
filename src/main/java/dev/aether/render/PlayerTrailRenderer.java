package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.PlayerTrailModule;
import dev.aether.util.Colors;
import java.util.Deque;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders a fading motion trail behind the local player in world space. Draws a ribbon of quads
 * along the sampled player path, with per-segment alpha fading toward the tail, optional glow
 * halo, and a "stars" style that drops small fading markers instead of a ribbon.
 */
public final class PlayerTrailRenderer {
   private PlayerTrailRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, PlayerTrailModule module) {
      Deque<PlayerTrailModule.Sample> trail = module.trail();
      if (trail.size() < 2) {
         return;
      }
      long now = System.nanoTime();
      float fade = module.fadeAmount();
      float thickness = module.thickness();
      int style = module.styleId();
      int rgb = module.currentRgb();

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

      // Compute per-sample alpha based on age toward the tail.
      int n = trail.size();
      PlayerTrailModule.Sample[] arr = trail.toArray(new PlayerTrailModule.Sample[0]);
      float[] alpha = new float[n];
      for (int i = 0; i < n; i++) {
         float t = (float)i / (float)(n - 1);
         alpha[i] = (float)Math.pow(t, 1.5F) * fade;
      }

      if (style == PlayerTrailModule.STYLE_STARS) {
         for (int i = 0; i < n; i++) {
            PlayerTrailModule.Sample s = arr[i];
            if (alpha[i] <= 0.01F) {
               continue;
            }
            float bx = (float)(s.x() - cam.x());
            float by = (float)(s.y() - cam.y());
            float bz = (float)(s.z() - cam.z());
            int col = Colors.withAlpha(rgb, alpha[i]);
            markerQuad(lines, pose, bx, by, bz, right, up, 0.5F * (0.5F + 0.5F * alpha[i]), col);
         }
      } else if (style == PlayerTrailModule.STYLE_GLOW) {
         for (int i = 0; i < n - 1; i++) {
            PlayerTrailModule.Sample a = arr[i];
            PlayerTrailModule.Sample b = arr[i + 1];
            float mid = (alpha[i] + alpha[i + 1]) * 0.5F;
            if (mid <= 0.01F) {
               continue;
            }
            ribbon(fill, lines, pose, cam, right, up, a, b, thickness * (0.6F + 0.4F * mid), rgb, mid, true);
         }
      } else {
         for (int i = 0; i < n - 1; i++) {
            PlayerTrailModule.Sample a = arr[i];
            PlayerTrailModule.Sample b = arr[i + 1];
            float mid = (alpha[i] + alpha[i + 1]) * 0.5F;
            if (mid <= 0.01F) {
               continue;
            }
            ribbon(fill, lines, pose, cam, right, up, a, b, thickness, rgb, mid, false);
         }
      }

      FlatOverlay.flush(bufferSource);
   }

   private static void ribbon(VertexConsumer fill, VertexConsumer lines, Pose pose, Vec3 cam,
                              Vector3f right, Vector3f up, PlayerTrailModule.Sample a, PlayerTrailModule.Sample b,
                              float thickness, int rgb, float alpha, boolean glow) {
      float ax = (float)(a.x() - cam.x());
      float ay = (float)(a.y() - cam.y());
      float az = (float)(a.z() - cam.z());
      float bx = (float)(b.x() - cam.x());
      float by = (float)(b.y() - cam.y());
      float bz = (float)(b.z() - cam.z());

      float dx = bx - ax;
      float dy = by - ay;
      float dz = bz - az;
      float rl = right.x * dx + right.y * dy + right.z * dz;
      float ul = up.x * dx + up.y * dy + up.z * dz;
      float len = Mth.sqrt(rl * rl + ul * ul);
      float rr;
      float ur;
      if (len > 1.0E-4F) {
         rr = -ul / len;
         ur = rl / len;
      } else {
         rr = 1.0F;
         ur = 0.0F;
      }

      Vector3f rA = axis(right, up, rr * thickness, ur * thickness);
      Vector3f rB = axis(right, up, -rr * thickness, ur * thickness);

      int col = Colors.withAlpha(rgb, alpha);
      quad(fill, pose, ax, ay, az, rA, rB, col);
      quad(fill, pose, bx, by, bz, rA, rB, col);

      if (glow) {
         float halo = thickness * 1.8F;
         Vector3f hA = axis(right, up, rr * halo, ur * halo);
         Vector3f hB = axis(right, up, -rr * halo, ur * halo);
         int hcol = Colors.withAlpha(rgb, alpha * 0.18F);
         quad(fill, pose, ax, ay, az, hA, hB, hcol);
         quad(fill, pose, bx, by, bz, hA, hB, hcol);
      }

      // Bright center line along the ribbon.
      int lcol = Colors.withAlpha(Colors.lighten(rgb, 0.5F), alpha);
      line(lines, pose, ax, ay, az, bx, by, bz, lcol, 2.0F);
   }

   private static void markerQuad(VertexConsumer lines, Pose pose, float bx, float by, float bz,
                                  Vector3f right, Vector3f up, float half, int col) {
      for (int i = 0; i < 6; i++) {
         float a0 = (float)i / 6.0F * (float) (Math.PI * 2);
         float a1 = (float)(i + 1) / 6.0F * (float) (Math.PI * 2);
         Vector3f p0 = axis(right, up, Mth.cos(a0) * half, Mth.sin(a0) * half);
         Vector3f p1 = axis(right, up, Mth.cos(a1) * half, Mth.sin(a1) * half);
         line(lines, pose, bx + p0.x, by + p0.y, bz + p0.z, bx + p1.x, by + p1.y, bz + p1.z, col, 1.6F);
      }
   }

   private static Vector3f axis(Vector3f right, Vector3f up, float a, float b) {
      return new Vector3f(right.x * a + up.x * b, right.y * a + up.y * b, right.z * a + up.z * b);
   }

   private static void quad(VertexConsumer buf, Pose pose, float bx, float by, float bz, Vector3f a1, Vector3f a2, int argb) {
      buf.addVertex(pose, bx - a1.x - a2.x, by - a1.y - a2.y, bz - a1.z - a2.z).setColor(argb);
      buf.addVertex(pose, bx + a1.x - a2.x, by + a1.y - a2.y, bz + a1.z - a2.z).setColor(argb);
      buf.addVertex(pose, bx + a1.x + a2.x, by + a1.y + a2.y, bz + a1.z + a2.z).setColor(argb);
      buf.addVertex(pose, bx - a1.x + a2.x, by - a1.y + a2.y, bz - a1.z + a2.z).setColor(argb);
   }

   private static void line(VertexConsumer buf, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
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
