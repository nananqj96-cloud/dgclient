package dev.aether.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class WorldProjection {
   private static final Matrix4f mvp = new Matrix4f();
   private static final Vector4f scratch = new Vector4f();
   private static Vec3 camPos = Vec3.ZERO;
   private static int fbWidth;
   private static int fbHeight;
   private static float partialTick;
   private static boolean valid;

   private WorldProjection() {
   }

   public static void capture(Matrix4f projection, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      Camera camera = mc.gameRenderer.getMainCamera();
      if (camera == null) {
         valid = false;
      } else {
         Quaternionf view = camera.rotation().conjugate(new Quaternionf());
         mvp.set(projection).rotate(view);
         camPos = camera.position();
         RenderTarget target = mc.getMainRenderTarget();
         fbWidth = target.width;
         fbHeight = target.height;
         WorldProjection.partialTick = partialTick;
         valid = true;
      }
   }

   public static void invalidate() {
      valid = false;
   }

   public static boolean isValid() {
      return valid;
   }

   public static float partialTick() {
      return partialTick;
   }

   public static float[] project(double wx, double wy, double wz) {
      float[] px = projectRaw(wx, wy, wz);
      if (px == null) {
         return null;
      } else {
         float scale = OverlayRenderer.uiScale();
         return new float[]{px[0] / scale, px[1] / scale};
      }
   }

   public static float[] projectRaw(double wx, double wy, double wz) {
      if (!valid) {
         return null;
      } else {
         scratch.set((float)(wx - camPos.x), (float)(wy - camPos.y), (float)(wz - camPos.z), 1.0F);
         mvp.transform(scratch);
         if (scratch.w <= 1.0E-4F) {
            return null;
         } else {
            float ndcX = scratch.x / scratch.w;
            float ndcY = scratch.y / scratch.w;
            float px = (ndcX * 0.5F + 0.5F) * (float)fbWidth;
            float py = (1.0F - (ndcY * 0.5F + 0.5F)) * (float)fbHeight;
            return new float[]{px, py};
         }
      }
   }
}
