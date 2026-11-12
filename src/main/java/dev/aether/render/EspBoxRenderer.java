package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class EspBoxRenderer {
   private static final float TRACER_START = 0.35F;
   private static final float TRACER_NEAR = 0.1F;

   private EspBoxRenderer() {
   }

   public static void outline(
      BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, double d, double e, double f, double g, double h, double i, int j, float k
   ) {
      VertexConsumer var17 = bufferSource.getBuffer(FlatOverlay.LINES);
      Pose var18 = poseStack.last();
      float var19 = (float)(d - vec3.x);
      float var20 = (float)(e - vec3.y);
      float var21 = (float)(f - vec3.z);
      float var22 = (float)(g - vec3.x);
      float var23 = (float)(h - vec3.y);
      float var24 = (float)(i - vec3.z);
      line(var17, var18, var19, var20, var21, var22, var20, var21, j, k);
      line(var17, var18, var22, var20, var21, var22, var20, var24, j, k);
      line(var17, var18, var22, var20, var24, var19, var20, var24, j, k);
      line(var17, var18, var19, var20, var24, var19, var20, var21, j, k);
      line(var17, var18, var19, var23, var21, var22, var23, var21, j, k);
      line(var17, var18, var22, var23, var21, var22, var23, var24, j, k);
      line(var17, var18, var22, var23, var24, var19, var23, var24, j, k);
      line(var17, var18, var19, var23, var24, var19, var23, var21, j, k);
      line(var17, var18, var19, var20, var21, var19, var23, var21, j, k);
      line(var17, var18, var22, var20, var21, var22, var23, var21, j, k);
      line(var17, var18, var22, var20, var24, var22, var23, var24, j, k);
      line(var17, var18, var19, var20, var24, var19, var23, var24, j, k);
   }

   public static void fill(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, double d, double e, double f, double g, double h, double i, int j) {
      VertexConsumer var16 = bufferSource.getBuffer(FlatOverlay.FILL);
      Pose var17 = poseStack.last();
      float var18 = (float)(d - vec3.x);
      float var19 = (float)(e - vec3.y);
      float var20 = (float)(f - vec3.z);
      float var21 = (float)(g - vec3.x);
      float var22 = (float)(h - vec3.y);
      float var23 = (float)(i - vec3.z);
      quad(var16, var17, j, var18, var19, var20, var21, var19, var20, var21, var19, var23, var18, var19, var23);
      quad(var16, var17, j, var18, var22, var20, var18, var22, var23, var21, var22, var23, var21, var22, var20);
      quad(var16, var17, j, var18, var19, var20, var18, var22, var20, var21, var22, var20, var21, var19, var20);
      quad(var16, var17, j, var18, var19, var23, var21, var19, var23, var21, var22, var23, var18, var22, var23);
      quad(var16, var17, j, var18, var19, var20, var18, var19, var23, var18, var22, var23, var18, var22, var20);
      quad(var16, var17, j, var21, var19, var20, var21, var22, var20, var21, var22, var23, var21, var19, var23);
   }

   public static void tracer(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, Vector3fc vector3fc, double d, double e, double f, int i, float g) {
      float var12 = vector3fc.x();
      float var13 = vector3fc.y();
      float var14 = vector3fc.z();
      float var15 = (float)Math.sqrt((double)(var12 * var12 + var13 * var13 + var14 * var14));
      if (var15 > 1.0E-6F) {
         var12 /= var15;
         var13 /= var15;
         var14 /= var15;
      }

      float var16 = var12 * 0.35F;
      float var17 = var13 * 0.35F;
      float var18 = var14 * 0.35F;
      float var19 = (float)(d - vec3.x);
      float var20 = (float)(e - vec3.y);
      float var21 = (float)(f - vec3.z);
      float var22 = var19 * var12 + var20 * var13 + var21 * var14;
      if (var22 < 0.1F) {
         float var23 = -0.25F / (var22 - 0.35F);
         var19 = var16 + (var19 - var16) * var23;
         var20 = var17 + (var20 - var17) * var23;
         var21 = var18 + (var21 - var18) * var23;
      }

      VertexConsumer var25 = bufferSource.getBuffer(FlatOverlay.LINES);
      Pose var24 = poseStack.last();
      line(var25, var24, var16, var17, var18, var19, var20, var21, i, g);
   }

   public static void beam(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, double d, double e, double f, double g, int i, float h) {
      VertexConsumer var13 = bufferSource.getBuffer(FlatOverlay.LINES);
      Pose var14 = poseStack.last();
      line(var13, var14, (float)(d - vec3.x), (float)(e - vec3.y), (float)(f - vec3.z), (float)(d - vec3.x), (float)(g - vec3.y), (float)(f - vec3.z), i, h);
   }

   public static void ring(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, double d, double e, double f, double g, int i, int j, float h) {
      VertexConsumer var14 = bufferSource.getBuffer(FlatOverlay.LINES);
      Pose var15 = poseStack.last();
      float var16 = (float)(e - vec3.y);
      double var17 = d + g;
      double var19 = f;

      for (int var21 = 1; var21 <= i; var21++) {
         double var22 = (Math.PI * 2) * (double)var21 / (double)i;
         double var24 = d + Math.cos(var22) * g;
         double var26 = f + Math.sin(var22) * g;
         line(var14, var15, (float)(var17 - vec3.x), var16, (float)(var19 - vec3.z), (float)(var24 - vec3.x), var16, (float)(var26 - vec3.z), j, h);
         var17 = var24;
         var19 = var26;
      }
   }

   public static void flush(BufferSource bufferSource) {
      FlatOverlay.flush(bufferSource);
   }

   private static void line(VertexConsumer vertexConsumer, Pose pose, float f, float g, float h, float i, float j, float k, int l, float m) {
      Vector3f var10 = new Vector3f(i - f, j - g, k - h);
      if (var10.lengthSquared() > 1.0E-9F) {
         var10.normalize();
      } else {
         var10.set(0.0F, 1.0F, 0.0F);
      }

      vertexConsumer.addVertex(pose, f, g, h).setColor(l).setNormal(pose, var10).setLineWidth(m);
      vertexConsumer.addVertex(pose, i, j, k).setColor(l).setNormal(pose, var10).setLineWidth(m);
   }

   private static void quad(
      VertexConsumer vertexConsumer,
      Pose pose,
      int i,
      float f,
      float g,
      float h,
      float j,
      float k,
      float l,
      float m,
      float n,
      float o,
      float p,
      float q,
      float r
   ) {
      vertexConsumer.addVertex(pose, f, g, h).setColor(i);
      vertexConsumer.addVertex(pose, j, k, l).setColor(i);
      vertexConsumer.addVertex(pose, m, n, o).setColor(i);
      vertexConsumer.addVertex(pose, p, q, r).setColor(i);
   }
}
