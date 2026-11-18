package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.LootESPModule;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Highlights loot-bearing blocks (chests, barrels, shulkers, etc.) through walls with a coloured
 * outline box.
 */
public final class LootEspRenderer {
   private LootEspRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, LootESPModule module) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      float range = module.rangeValue();
      float rangeSq = range * range;
      int rgb = module.colorRgb();

      Pose pose = poseStack.last();
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      for (BlockEntity be : ((ClientLevel) mc.level).getBlockEntities()) {
         if (!isLootBlock(be, module)) {
            continue;
         }
         AABB box = new AABB(be.getBlockPos());
         double cx = (box.minX + box.maxX) / 2.0;
         double cy = (box.minY + box.maxY) / 2.0;
         double cz = (box.minZ + box.maxZ) / 2.0;
         double dx = cx - cam.x();
         double dy = cy - cam.y();
         double dz = cz - cam.z();
         if (dx * dx + dy * dy + dz * dz > rangeSq) {
            continue;
         }

         float x0 = (float) (box.minX - cam.x());
         float y0 = (float) (box.minY - cam.y());
         float z0 = (float) (box.minZ - cam.z());
         float x1 = (float) (box.maxX - cam.x());
         float y1 = (float) (box.maxY - cam.y());
         float z1 = (float) (box.maxZ - cam.z());
         int color = Colors.withAlpha(rgb, 0.85F);

         edge(lines, pose, x0, y0, z0, x1, y0, z0, color);
         edge(lines, pose, x1, y0, z0, x1, y0, z1, color);
         edge(lines, pose, x1, y0, z1, x0, y0, z1, color);
         edge(lines, pose, x0, y0, z1, x0, y0, z0, color);
         edge(lines, pose, x0, y0, z0, x0, y1, z0, color);
         edge(lines, pose, x1, y0, z0, x1, y1, z0, color);
         edge(lines, pose, x1, y1, z0, x1, y1, z1, color);
         edge(lines, pose, x1, y1, z1, x0, y1, z1, color);
         edge(lines, pose, x0, y1, z1, x0, y1, z0, color);
         edge(lines, pose, x0, y1, z0, x0, y0, z0, color);
         edge(lines, pose, x0, y1, z0, x1, y1, z0, color);
         edge(lines, pose, x1, y1, z0, x1, y1, z1, color);
         edge(lines, pose, x1, y1, z1, x0, y1, z1, color);
         edge(lines, pose, x0, y1, z1, x0, y1, z0, color);
      }

      FlatOverlay.flush(bufferSource);
   }

   private static boolean isLootBlock(BlockEntity be, LootESPModule module) {
      if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
         return true;
      }
      if (module.onlyChests.get()) {
         return false;
      }
      if (be instanceof ShulkerBoxBlockEntity) {
         return true;
      }
      // Cover a few more common loot containers.
      BlockEntityType<?> type = be.getType();
      if (type == null) {
         return false;
      }
      return type.equals(BlockEntityType.ENDER_CHEST)
         || type.equals(BlockEntityType.TRAPPED_CHEST)
         || type.equals(BlockEntityType.FURNACE)
         || type.equals(BlockEntityType.BLAST_FURNACE)
         || type.equals(BlockEntityType.SMOKER);
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
