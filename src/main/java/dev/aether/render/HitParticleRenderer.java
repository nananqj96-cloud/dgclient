package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.HitParticlesModule;
import dev.aether.util.Colors;
import java.util.Deque;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class HitParticleRenderer {
   private static final Identifier TEXTURE_AETHER = Identifier.fromNamespaceAndPath(
      "aetherclient", "textures/misc/aether.png"
   );
   private static final int HEART_SEGMENTS = 20;
   private static final float[] HEART_X = new float[21];
   private static final float[] HEART_Y = new float[21];
   private static final float SHOCK_TIME = 0.5F;

   private HitParticleRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, HitParticlesModule module) {
      Deque<HitParticlesModule.HitParticle> particles = module.particles();
      Deque<HitParticlesModule.Shock> shocks = module.shocks();
      if (!particles.isEmpty() || !shocks.isEmpty()) {
         long now = System.nanoTime();

         while (!particles.isEmpty() && particles.peekFirst().ageSeconds(now) > particles.peekFirst().lifetime) {
            particles.removeFirst();
         }

         while (!shocks.isEmpty() && shocks.peekFirst().ageSeconds(now) > 0.5F) {
            shocks.removeFirst();
         }

         if (!particles.isEmpty() || !shocks.isEmpty()) {
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
            float glow = module.glowStrength();
            Pose pose = poseStack.last();
            VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

            for (HitParticlesModule.Shock shock : shocks) {
               renderShock(lines, pose, cam, right, up, shock, now);
            }

            boolean anyAether = false;

            for (HitParticlesModule.HitParticle p : particles) {
               float age = p.ageSeconds(now);
               if (!(age < 0.0F) && !(age > p.lifetime)) {
                  if (p.styleId == 2) {
                     renderLightning(lines, pose, cam, right, up, p, age);
                  } else if (p.styleId == 3) {
                     anyAether = true;
                  }
               }
            }

            VertexConsumer fill = bufferSource.getBuffer(FlatOverlay.FILL);

            for (HitParticlesModule.HitParticle px : particles) {
               float age = px.ageSeconds(now);
               if (!(age < 0.0F) && !(age > px.lifetime)) {
                  if (px.styleId == 1) {
                     renderHeart(fill, pose, cam, right, up, px, age, glow);
                  } else if (px.styleId == 0) {
                     renderSpark(fill, pose, cam, right, up, px, age, glow);
                  }
               }
            }

            FlatOverlay.flush(bufferSource);
            if (anyAether) {
               VertexConsumer glyphs = bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(TEXTURE_AETHER));

               for (HitParticlesModule.HitParticle pxx : particles) {
                  if (pxx.styleId == 3) {
                     float age = pxx.ageSeconds(now);
                     if (!(age < 0.0F) && !(age > pxx.lifetime)) {
                        renderAether(glyphs, pose, cam, right, up, fwd, pxx, age);
                     }
                  }
               }
            }
         }
      }
   }

   private static void renderSpark(
      VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, HitParticlesModule.HitParticle p, float age, float glow
   ) {
      float t = age / p.lifetime;
      float alpha = fadeAlpha(t);
      if (!(alpha <= 0.01F)) {
         float bx = (float)(p.x(age) - cam.x);
         float by = (float)(p.y(age) - cam.y);
         float bz = (float)(p.z(age) - cam.z);
         float vy = p.vy - p.gravity * age;
         float sr = p.vx * right.x + vy * right.y + p.vz * right.z;
         float su = p.vx * up.x + vy * up.y + p.vz * up.z;
         float slen = Mth.sqrt(sr * sr + su * su);
         float dirR;
         float dirU;
         if (slen > 1.0E-4F) {
            dirR = sr / slen;
            dirU = su / slen;
         } else {
            dirR = 0.0F;
            dirU = 1.0F;
         }

         float shrink = 0.35F + 0.65F * (1.0F - t);
         float streak = 0.28F * p.size * shrink;
         float width = 0.055F * p.size * shrink;
         Vector3f lAx = axis(right, up, dirR * streak, dirU * streak);
         Vector3f sAx = axis(right, up, -dirU * width, dirR * width);
         int core = Colors.withAlpha(Colors.lighten(p.rgb, 0.55F), alpha);
         quad(buf, pose, bx, by, bz, lAx, sAx, core);
         if (glow > 0.01F) {
            float halo = 0.06F * p.size * shrink * (1.0F + 0.6F * glow);
            Vector3f hA = axis(right, up, halo, halo);
            Vector3f hB = axis(right, up, -halo, halo);
            int haloColor = Colors.withAlpha(p.rgb, alpha * 0.22F * glow);
            quad(buf, pose, bx, by, bz, hA, hB, haloColor);
         }
      }
   }

   private static void renderHeart(
      VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, HitParticlesModule.HitParticle p, float age, float glow
   ) {
      float t = age / p.lifetime;
      float alpha = fadeAlpha(t);
      if (!(alpha <= 0.01F)) {
         float pop = t < 0.2F ? easeOutBack(t / 0.2F) : 1.0F;
         float scale = 0.16F * p.size * pop;
         float bx = (float)(p.x(age) - cam.x);
         float by = (float)(p.y(age) - cam.y);
         float bz = (float)(p.z(age) - cam.z);
         float wobble = 0.18F * Mth.sin((double)(age * 6.0F + p.rot));
         int core = Colors.withAlpha(Colors.lighten(p.rgb, 0.25F), alpha);
         fanHeart(buf, pose, right, up, bx, by, bz, scale, wobble, core);
         if (glow > 0.01F) {
            int haloColor = Colors.withAlpha(p.rgb, alpha * 0.3F * glow);
            fanHeart(buf, pose, right, up, bx, by, bz, scale * (1.35F + 0.35F * glow), wobble, haloColor);
         }
      }
   }

   private static void fanHeart(VertexConsumer buf, Pose pose, Vector3f right, Vector3f up, float bx, float by, float bz, float scale, float shearX, int argb) {
      for (int i = 0; i < 20; i++) {
         float ax0 = (HEART_X[i] + shearX * HEART_Y[i]) * scale;
         float ay0 = HEART_Y[i] * scale;
         float ax1 = (HEART_X[i + 1] + shearX * HEART_Y[i + 1]) * scale;
         float ay1 = HEART_Y[i + 1] * scale;
         v(buf, pose, bx, by, bz, argb);
         v(buf, pose, bx + right.x * ax0 + up.x * ay0, by + right.y * ax0 + up.y * ay0, bz + right.z * ax0 + up.z * ay0, argb);
         v(buf, pose, bx + right.x * ax1 + up.x * ay1, by + right.y * ax1 + up.y * ay1, bz + right.z * ax1 + up.z * ay1, argb);
         v(buf, pose, bx, by, bz, argb);
      }
   }

   private static void renderLightning(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, HitParticlesModule.HitParticle p, float age) {
      float t = age / p.lifetime;
      float alpha = fadeAlpha(t);
      if (!(alpha <= 0.01F)) {
         float flicker = 0.45F + 0.55F * Mth.abs(Mth.sin((double)(age * 42.0F + p.rot)));
         alpha *= flicker;
         float sx = (float)(p.ox - cam.x);
         float sy = (float)(p.oy - cam.y);
         float sz = (float)(p.oz - cam.z);
         float ex = (float)(p.x(age) - cam.x);
         float ey = (float)(p.y(age) - cam.y);
         float ez = (float)(p.z(age) - cam.z);
         float dx = ex - sx;
         float dy = ey - sy;
         float dz = ez - sz;
         float dr = dx * right.x + dy * right.y + dz * right.z;
         float du = dx * up.x + dy * up.y + dz * up.z;
         float dlen = Mth.sqrt(dr * dr + du * du);
         float pr;
         float pu;
         if (dlen > 1.0E-4F) {
            pr = -du / dlen;
            pu = dr / dlen;
         } else {
            pr = 1.0F;
            pu = 0.0F;
         }

         float amp = 0.16F * p.size;
         int core = Colors.withAlpha(Colors.lighten(p.rgb, 0.6F), alpha);
         float lineW = 2.4F * p.size;
         int kinks = 4;
         float px = sx;
         float py = sy;
         float pz = sz;

         for (int i = 1; i <= kinks; i++) {
            float f = (float)i / (float)kinks;
            float taper = Mth.sin((double)(f * (float) Math.PI));
            float j = i == kinks ? 0.0F : (hash(p, i) * 2.0F - 1.0F) * amp * taper;
            float nx = sx + dx * f + (right.x * pr + up.x * pu) * j;
            float ny = sy + dy * f + (right.y * pr + up.y * pu) * j;
            float nz = sz + dz * f + (right.z * pr + up.z * pu) * j;
            line(buf, pose, px, py, pz, nx, ny, nz, core, lineW);
            px = nx;
            py = ny;
            pz = nz;
         }
      }
   }

   private static void renderAether(
      VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, Vector3f fwd, HitParticlesModule.HitParticle p, float age
   ) {
      float t = age / p.lifetime;
      float alpha = fadeAlpha(t);
      if (!(alpha <= 0.01F)) {
         float pop = t < 0.22F ? easeOutBack(t / 0.22F) : 1.0F;
         float half = 0.16F * p.size * pop;
         float angle = p.rot + p.rotSpeed * age * 0.5F;
         float ca = Mth.cos((double)angle);
         float sa = Mth.sin((double)angle);
         Vector3f rAx = axis(right, up, ca * half, sa * half);
         Vector3f uAx = axis(right, up, -sa * half, ca * half);
         float bx = (float)(p.x(age) - cam.x);
         float by = (float)(p.y(age) - cam.y);
         float bz = (float)(p.z(age) - cam.z);
         int argb = Colors.withAlpha(p.rgb, alpha);
         float nx = -fwd.x;
         float ny = -fwd.y;
         float nz = -fwd.z;
         texVertex(buf, pose, bx - rAx.x - uAx.x, by - rAx.y - uAx.y, bz - rAx.z - uAx.z, 0.0F, 0.0F, argb, nx, ny, nz);
         texVertex(buf, pose, bx - rAx.x + uAx.x, by - rAx.y + uAx.y, bz - rAx.z + uAx.z, 0.0F, 1.0F, argb, nx, ny, nz);
         texVertex(buf, pose, bx + rAx.x + uAx.x, by + rAx.y + uAx.y, bz + rAx.z + uAx.z, 1.0F, 1.0F, argb, nx, ny, nz);
         texVertex(buf, pose, bx + rAx.x - uAx.x, by + rAx.y - uAx.y, bz + rAx.z - uAx.z, 1.0F, 0.0F, argb, nx, ny, nz);
      }
   }

   private static void renderShock(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, HitParticlesModule.Shock shock, long now) {
      float age = shock.ageSeconds(now);
      if (!(age < 0.0F) && !(age >= 0.5F)) {
         float t = age / 0.5F;
         float radius = 0.15F + easeOutCubic(t) * 1.15F;
         float alpha = 0.8F * (1.0F - easeInQuad(t));
         int color = Colors.withAlpha(Colors.lighten(shock.rgb, 0.2F), alpha);
         float bx = (float)(shock.x - cam.x);
         float by = (float)(shock.y - cam.y);
         float bz = (float)(shock.z - cam.z);
         int segs = 28;
         float prevX = 0.0F;
         float prevY = 0.0F;
         float prevZ = 0.0F;

         for (int i = 0; i <= segs; i++) {
            float a = (float)i / (float)segs * (float) (Math.PI * 2);
            float ox = Mth.cos((double)a) * radius;
            float oy = Mth.sin((double)a) * radius;
            float x = bx + right.x * ox + up.x * oy;
            float y = by + right.y * ox + up.y * oy;
            float z = bz + right.z * ox + up.z * oy;
            if (i > 0) {
               line(buf, pose, prevX, prevY, prevZ, x, y, z, color, 2.2F);
            }

            prevX = x;
            prevY = y;
            prevZ = z;
         }
      }
   }

   private static Vector3f axis(Vector3f right, Vector3f up, float a, float b) {
      return new Vector3f(right.x * a + up.x * b, right.y * a + up.y * b, right.z * a + up.z * b);
   }

   private static void quad(VertexConsumer buf, Pose pose, float bx, float by, float bz, Vector3f ax1, Vector3f ax2, int argb) {
      v(buf, pose, bx - ax1.x - ax2.x, by - ax1.y - ax2.y, bz - ax1.z - ax2.z, argb);
      v(buf, pose, bx + ax1.x - ax2.x, by + ax1.y - ax2.y, bz + ax1.z - ax2.z, argb);
      v(buf, pose, bx + ax1.x + ax2.x, by + ax1.y + ax2.y, bz + ax1.z + ax2.z, argb);
      v(buf, pose, bx - ax1.x + ax2.x, by - ax1.y + ax2.y, bz - ax1.z + ax2.z, argb);
   }

   private static void v(VertexConsumer buf, Pose pose, float x, float y, float z, int argb) {
      buf.addVertex(pose, x, y, z).setColor(argb);
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

   private static void texVertex(VertexConsumer buf, Pose pose, float x, float y, float z, float u, float vv, int argb, float nx, float ny, float nz) {
      buf.addVertex(pose, x, y, z).setColor(argb).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
   }

   private static float hash(HitParticlesModule.HitParticle p, int salt) {
      float s = Mth.sin((double)((float)(p.ox * 12.9898 + p.oz * 78.233 + (double)p.rot * 3.17 + (double)salt * 43.123))) * 43758.547F;
      return s - (float)Mth.floor(s);
   }

   private static float fadeAlpha(float t) {
      t = clamp01(t);
      return t < 0.12F ? t / 0.12F : 1.0F - easeInQuad((t - 0.12F) / 0.88F);
   }

   private static float clamp01(float t) {
      return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
   }

   private static float easeInQuad(float t) {
      t = clamp01(t);
      return t * t;
   }

   private static float easeOutCubic(float t) {
      t = clamp01(t);
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   private static float easeOutBack(float t) {
      t = clamp01(t);
      float overshoot = 2.4F;
      float c3 = overshoot + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + overshoot * u * u;
   }

   static {
      for (int i = 0; i <= 20; i++) {
         double t = (double)i / 20.0 * Math.PI * 2.0;
         double hx = 16.0 * Math.pow(Math.sin(t), 3.0);
         double hy = 13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t);
         HEART_X[i] = (float)(hx / 17.0);
         HEART_Y[i] = (float)(hy / 17.0);
      }
   }
}
