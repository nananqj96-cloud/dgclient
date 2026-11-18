package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.AirParticlesModule;
import dev.aether.util.Colors;
import java.util.Deque;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders the ambient air particles (snow, fireflies with trails, leaves, embers, dust) drifting
 * around the player. Each particle drifts with wind and gravity, twinkles/fades, and fireflies leave
 * a fading glowing trail behind them.
 */
public final class AirParticlesRenderer {
   private static final int MAX_AGE_NS = 8_000_000_000L; // ~8s lifetime

   private AirParticlesRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, AirParticlesModule module) {
      Deque<AirParticlesModule.Particle> particles = module.particles();
      if (particles.isEmpty()) {
         return;
      }
      long now = System.nanoTime();
      float speed = module.speedUnit();
      float wind = module.windUnit();
      float size = module.sizeValue();
      boolean glow = module.glow.get();
      boolean trails = module.trails.get();
      int custom = module.colorRgb();

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

      // Prune dead particles and integrate motion.
      java.util.Iterator<AirParticlesModule.Particle> it = particles.iterator();
      java.util.List<AirParticlesModule.Particle> alive = new java.util.ArrayList<>();
      while (it.hasNext()) {
         AirParticlesModule.Particle p = it.next();
         long age = now - p.born();
         if (age > MAX_AGE_NS) {
            it.remove();
            continue;
         }
         alive.add(p);
      }

      float windX = (float) Math.sin(now / 3.0E9) * wind * 0.4;
      float windZ = (float) Math.cos(now / 2.5E9) * wind * 0.3;

      for (AirParticlesModule.Particle p : alive) {
         long age = now - p.born();
         float t = (float) (age / (double) MAX_AGE_NS);
         float alpha = fadeAlpha(t, p.kind());
         if (alpha <= 0.02F) {
            continue;
         }
         int rgb = particleColor(p.kind(), custom, t);
         float px = (float) (p.x() + p.vx() * age * 1.0E-9 * speed * 10.0 + windX * age * 1.0E-9);
         float py = (float) (p.y() + p.vy() * age * 1.0E-9 * speed * 10.0 - age * 1.0E-9 * 0.6);
         float pz = (float) (p.z() + p.vz() * age * 1.0E-9 * speed * 10.0 + windZ * age * 1.0E-9);

         float sx = px - cam.x();
         float sy = py - cam.y();
         float sz = pz - cam.z();

         if (p.kind() == AirParticlesModule.TYPE_FIREFLY && trails) {
            // Glowing trail behind the firefly.
            float tw = 0.5F * size;
            Vector3f d = new Vector3f(sx - (float) p.vx(), sy - (float) p.vy(), sz - (float) p.vz());
            float dl = d.length();
            if (dl > 1.0E-4F) {
               d.div(dl);
            }
            for (int k = 0; k < 6; k++) {
               float back = (float) (k + 1) * 0.5F;
               float bx = sx - d.x * back;
               float by = sy - d.y * back;
               float bz = sz - d.z * back;
               float ta = alpha * (1.0F - k / 7.0F) * 0.5F;
               if (ta <= 0.02F) break;
               int tcol = Colors.withAlpha(rgb, ta);
               float h = tw * (1.0F - k / 8.0F);
               quad(fill, pose, bx, by, bz, right, up, h, tcol);
            }
         }

         float h = size * (0.6F + 0.4F * alpha) * particleSize(p.kind());
         if (glow) {
            int halo = Colors.withAlpha(rgb, alpha * 0.25F);
            quad(fill, pose, sx, sy, sz, right, up, h * 2.0F, halo);
         }
         int col = Colors.withAlpha(rgb, alpha);
         if (p.kind() == AirParticlesModule.TYPE_SNOW) {
            // Snow: small white flake (cross).
            line3(lines, pose, sx - h, sy, sz, sx + h, sy, sz, col, 1.4F);
            line3(lines, pose, sx, sy - h, sz, sx, sy + h, sz, col, 1.4F);
         } else if (p.kind() == AirParticlesModule.TYPE_FIREFLY) {
            quad(fill, pose, sx, sy, sz, right, up, h, col);
            line3(lines, pose, sx - h, sy, sz, sx + h, sy, sz, Colors.withAlpha(Colors.lighten(rgb, 0.6F), alpha), 1.2F);
            line3(lines, pose, sx, sy - h, sz, sx, sy + h, sz, Colors.withAlpha(Colors.lighten(rgb, 0.6F), alpha), 1.2F);
         } else if (p.kind() == AirParticlesModule.TYPE_LEAVES) {
            // Leaves: small tilted quad.
            quad(fill, pose, sx, sy, sz, right, up, h, col);
         } else if (p.kind() == AirParticlesModule.TYPE_EMBER) {
            quad(fill, pose, sx, sy, sz, right, up, h, col);
         } else {
            quad(fill, pose, sx, sy, sz, right, up, h, col);
         }
      }

      FlatOverlay.flush(bufferSource);
   }

   private static int particleColor(int kind, int custom, float t) {
      switch (kind) {
         case AirParticlesModule.TYPE_SNOW:
            return 0xFFE8F0FF;
         case AirParticlesModule.TYPE_FIREFLY:
            float hue = (System.nanoTime() / 1.0E9 * 60.0F + t * 40.0F) % 360.0F;
            return Colors.hsvToRgb(60.0F + hue * 0.2F, 0.9F, 1.0F) & 16777215;
         case AirParticlesModule.TYPE_LEAVES:
            return 0xFF8BC34A;
         case AirParticlesModule.TYPE_EMBER:
            float eh = (System.nanoTime() / 1.0E9 * 40.0F) % 60.0F;
            return Colors.hsvToRgb(10.0F + eh * 0.5F, 0.95F, 1.0F) & 16777215;
         case AirParticlesModule.TYPE_DUST:
            return 0xFFD7BB88;
         default:
            return custom;
      }
   }

   private static float particleSize(int kind) {
      switch (kind) {
         case AirParticlesModule.TYPE_SNOW:
            return 0.7F;
         case AirParticlesModule.TYPE_FIREFLY:
            return 0.9F;
         case AirParticlesModule.TYPE_LEAVES:
            return 1.1F;
         case AirParticlesModule.TYPE_EMBER:
            return 0.6F;
         default:
            return 0.7F;
      }
   }

   private static float fadeAlpha(float t, int kind) {
      t = clamp01(t);
      // Fireflies/embers twinkle; others fade smoothly.
      if (kind == AirParticlesModule.TYPE_FIREFLY || kind == AirParticlesModule.TYPE_EMBER) {
         float twinkle = 0.65F + 0.35F * (float) Math.sin(System.nanoTime() / 1.0E8 + t * 10.0F);
         return t < 0.1F ? t / 0.1F : (1.0F - (t - 0.1F) / 0.9F) * twinkle;
      }
      return t < 0.1F ? t / 0.1F : (1.0F - (t - 0.1F) / 0.9F);
   }

   private static float clamp01(float t) {
      return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
   }

   private static void quad(VertexConsumer buf, Pose pose, float bx, float by, float bz, Vector3f right, Vector3f up, float half, int argb) {
      float h = Math.max(0.05F, half);
      buf.addVertex(pose, bx - right.x * h - up.x * h, by - right.y * h - up.y * h, bz - right.z * h - up.z * h).setColor(argb);
      buf.addVertex(pose, bx + right.x * h - up.x * h, by + right.y * h - up.y * h, bz + right.z * h - up.z * h).setColor(argb);
      buf.addVertex(pose, bx + right.x * h + up.x * h, by + right.y * h + up.y * h, bz + right.z * h + up.z * h).setColor(argb);
      buf.addVertex(pose, bx - right.x * h + up.x * h, by - right.y * h + up.y * h, bz - right.z * h + up.z * h).setColor(argb);
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
