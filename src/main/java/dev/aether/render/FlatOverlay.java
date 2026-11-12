package dev.aether.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.util.Colors;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class FlatOverlay {
   private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
      .withLocation("aetherclient/pipeline/flat_fill")
      .withCull(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .build();
   public static final RenderType FILL = RenderType.create(
      "aetherclient:flat_fill", RenderSetup.builder(FILL_PIPELINE).sortOnUpload().createRenderSetup()
   );
   private static final RenderPipeline LINE_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.LINES_SNIPPET})
      .withLocation("aetherclient/pipeline/flat_lines")
      .withDepthWrite(false)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .build();
   public static final RenderType LINES = RenderType.create(
      "aetherclient:flat_lines", RenderSetup.builder(LINE_PIPELINE).createRenderSetup()
   );

   private FlatOverlay() {
   }

   public static void fillQuad(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, double x0, double z0, double x1, double z1, double y, int color) {
      VertexConsumer buffer = bufferSource.getBuffer(FILL);
      Pose pose = poseStack.last();
      float ax = (float)(x0 - cam.x);
      float az = (float)(z0 - cam.z);
      float bx = (float)(x1 - cam.x);
      float bz = (float)(z1 - cam.z);
      float fy = (float)(y - cam.y);
      buffer.addVertex(pose, ax, fy, az).setColor(color);
      buffer.addVertex(pose, ax, fy, bz).setColor(color);
      buffer.addVertex(pose, bx, fy, bz).setColor(color);
      buffer.addVertex(pose, bx, fy, az).setColor(color);
   }

   public static void edge(
      BufferSource bufferSource, PoseStack poseStack, Vec3 cam, double x0, double z0, double x1, double z1, double y, int color, float width
   ) {
      VertexConsumer buffer = bufferSource.getBuffer(LINES);
      Pose pose = poseStack.last();
      Vector3f normal = new Vector3f((float)(x1 - x0), 0.0F, (float)(z1 - z0)).normalize();
      buffer.addVertex(pose, (float)(x0 - cam.x), (float)(y - cam.y), (float)(z0 - cam.z)).setColor(color).setNormal(pose, normal).setLineWidth(width);
      buffer.addVertex(pose, (float)(x1 - cam.x), (float)(y - cam.y), (float)(z1 - cam.z)).setColor(color).setNormal(pose, normal).setLineWidth(width);
   }

   public static void marker(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, double x, double z, double y, double r, int color) {
      fillQuadRot(bufferSource, poseStack, cam, x, z, y, r, color);
      int outline = Colors.withAlpha(color, 1.0F);
      edge(bufferSource, poseStack, cam, x - r, z, x, z - r, y, outline, 2.0F);
      edge(bufferSource, poseStack, cam, x, z - r, x + r, z, y, outline, 2.0F);
      edge(bufferSource, poseStack, cam, x + r, z, x, z + r, y, outline, 2.0F);
      edge(bufferSource, poseStack, cam, x, z + r, x - r, z, y, outline, 2.0F);
   }

   private static void fillQuadRot(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, double cx, double cz, double y, double r, int color) {
      VertexConsumer buffer = bufferSource.getBuffer(FILL);
      Pose pose = poseStack.last();
      float fy = (float)(y - cam.y);
      buffer.addVertex(pose, (float)(cx - r - cam.x), fy, (float)(cz - cam.z)).setColor(color);
      buffer.addVertex(pose, (float)(cx - cam.x), fy, (float)(cz - r - cam.z)).setColor(color);
      buffer.addVertex(pose, (float)(cx + r - cam.x), fy, (float)(cz - cam.z)).setColor(color);
      buffer.addVertex(pose, (float)(cx - cam.x), fy, (float)(cz + r - cam.z)).setColor(color);
   }

   public static void flush(BufferSource bufferSource) {
      bufferSource.endBatch(FILL);
      bufferSource.endBatch(LINES);
   }
}
