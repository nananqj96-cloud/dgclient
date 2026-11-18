package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.BlockHighlightModule;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Renders a growing animated highlight box and outline on the block the player is looking at.
 */
public final class BlockHighlightRenderer {
   private BlockHighlightRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, BlockHighlightModule module) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return;
      }
      HitResult hit = mc.player.pick(8.0, 0.0F, false);
      if (!(hit instanceof BlockHitResult blockHit)) {
         return;
      }
      AABB box = mc.level.getBlockState(blockHit.getBlockPos()).getOcclusionShape().toBoundingBox(blockHit.getBlockPos());
      if (box == null || box.isEmpty()) {
         box = new AABB(blockHit.getBlockPos());
      }

      float ax = (float) (box.minX - cam.x());
      float ay = (float) (box.minY - cam.y());
      float az = (float) (box.minZ - cam.z());
      float bx = (float) (box.maxX - cam.x());
      float by = (float) (box.maxY - cam.y());
      float bz = (float) (box.maxZ - cam.z());

      int fillRgb = module.fillRgb();
      int outlineRgb = module.outlineRgb();
      float lineWidth = module.lineWidthValue();

      Pose pose = poseStack.last();
      VertexConsumer fill = bufferSource.getBuffer(FlatOverlay.FILL);
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      int fillColor = Colors.withAlpha(fillRgb, 0.35F);
      fill.addVertex(pose, ax, ay, az).setColor(fillColor);
      fill.addVertex(pose, bx, ay, az).setColor(fillColor);
      fill.addVertex(pose, bx, by, bz).setColor(fillColor);
      fill.addVertex(pose, ax, by, bz).setColor(fillColor);

      int lineColor = Colors.withAlpha(outlineRgb, 1.0F);
      edge(lines, pose, ax, ay, az, bx, ay, az, lineColor, lineWidth);
      edge(lines, pose, bx, ay, az, bx, ay, bz, lineColor, lineWidth);
      edge(lines, pose, bx, ay, bz, ax, ay, bz, lineColor, lineWidth);
      edge(lines, pose, ax, ay, bz, ax, ay, az, lineColor, lineWidth);
      edge(lines, pose, ax, ay, az, ax, by, az, lineColor, lineWidth);
      edge(lines, pose, bx, ay, az, bx, by, az, lineColor, lineWidth);
      edge(lines, pose, bx, by, az, bx, by, bz, lineColor, lineWidth);
      edge(lines, pose, bx, by, bz, ax, by, bz, lineColor, lineWidth);
      edge(lines, pose, ax, by, bz, ax, by, az, lineColor, lineWidth);
      edge(lines, pose, ax, by, az, ax, ay, az, lineColor, lineWidth);
      edge(lines, pose, ax, by, az, bx, by, az, lineColor, lineWidth);
      edge(lines, pose, bx, by, az, bx, by, bz, lineColor, lineWidth);
      edge(lines, pose, bx, by, bz, ax, by, bz, lineColor, lineWidth);
      edge(lines, pose, ax, by, bz, ax, by, az, lineColor, lineWidth);

      FlatOverlay.flush(bufferSource);
   }

   private static void edge(VertexConsumer buf, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
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
