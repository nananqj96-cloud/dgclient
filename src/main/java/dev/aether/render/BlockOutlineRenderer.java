package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.Modules;
import dev.aether.util.Colors;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BlockOutlineRenderer {
   private BlockOutlineRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, BlockOutlineRenderState state, Vec3 camera, Modules.BlockOutlineModule module) {
      double dx = (double)state.pos().getX() - camera.x;
      double dy = (double)state.pos().getY() - camera.y;
      double dz = (double)state.pos().getZ() - camera.z;
      double seconds = (double)(System.nanoTime() % 1000000000000L) / 1.0E9;
      int color = animatedColor(module, seconds);
      float pulse = pulseFactor(module, seconds);
      float fill = module.fillOpacity.getFloat() / 100.0F;
      if (fill > 0.004F) {
         VertexConsumer quads = bufferSource.getBuffer(RenderTypes.debugQuads());
         int fillColor = Colors.withAlpha(color, fill * (0.75F + 0.25F * pulse));

         for (AABB box : state.shape().toAabbs()) {
            emitBox(poseStack, quads, box.inflate(-0.002).move(dx, dy, dz), fillColor);
         }

         bufferSource.endLastBatch();
      }

      float thickness = module.thickness.getFloat();
      float glow = module.glow.getFloat() / 100.0F;
      if (glow > 0.02F) {
         VertexConsumer halo = bufferSource.getBuffer(RenderTypes.secondaryBlockOutline());
         ShapeRenderer.renderShape(poseStack, halo, state.shape(), dx, dy, dz, Colors.withAlpha(color, (0.16F + 0.22F * pulse) * glow), thickness * 3.2F);
         bufferSource.endLastBatch();
         VertexConsumer mid = bufferSource.getBuffer(RenderTypes.lines());
         ShapeRenderer.renderShape(poseStack, mid, state.shape(), dx, dy, dz, Colors.withAlpha(color, (0.3F + 0.25F * pulse) * glow), thickness * 2.0F);
         bufferSource.endLastBatch();
      }

      VertexConsumer core = bufferSource.getBuffer(RenderTypes.lines());
      ShapeRenderer.renderShape(poseStack, core, state.shape(), dx, dy, dz, Colors.withAlpha(color, 0.85F + 0.15F * pulse), thickness);
      bufferSource.endLastBatch();
   }

   private static int animatedColor(Modules.BlockOutlineModule module, double seconds) {
      if (module.rainbow.get()) {
         return Colors.hsvToRgb((float)(seconds * 42.0 % 360.0), 0.75F, 1.0F);
      } else {
         int base = module.color.get() | 0xFF000000;
         if (module.animation.is("Gradient Flow")) {
            float[] hsv = Colors.rgbToHsv(base);
            float hue = (hsv[0] + (float)(Math.sin(seconds * 1.6) * 28.0)) % 360.0F;
            if (hue < 0.0F) {
               hue += 360.0F;
            }

            return Colors.hsvToRgb(hue, Math.max(0.4F, hsv[1]), hsv[2]);
         } else {
            return base;
         }
      }
   }

   private static float pulseFactor(Modules.BlockOutlineModule module, double seconds) {
      return module.animation.is("Pulse") ? (float)(0.5 + 0.5 * Math.sin(seconds * Math.PI * 2.0 / 1.6)) : 1.0F;
   }

   private static void emitBox(PoseStack poseStack, VertexConsumer buffer, AABB b, int color) {
      Pose pose = poseStack.last();
      float x0 = (float)b.minX;
      float y0 = (float)b.minY;
      float z0 = (float)b.minZ;
      float x1 = (float)b.maxX;
      float y1 = (float)b.maxY;
      float z1 = (float)b.maxZ;
      quad(buffer, pose, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
      quad(buffer, pose, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
      quad(buffer, pose, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
      quad(buffer, pose, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
      quad(buffer, pose, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      quad(buffer, pose, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
   }

   private static void quad(
      VertexConsumer buffer,
      Pose pose,
      int color,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx2,
      float dy2,
      float dz2
   ) {
      buffer.addVertex(pose, ax, ay, az).setColor(color);
      buffer.addVertex(pose, bx, by, bz).setColor(color);
      buffer.addVertex(pose, cx, cy, cz).setColor(color);
      buffer.addVertex(pose, dx2, dy2, dz2).setColor(color);
   }
}
