package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.DebugHoleEspModule;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class HoleEspRenderer {
   private static final int SCAN_INTERVAL_TICKS = 10;
   private static final int RADIUS = 16;
   private static final int VERTICAL = 6;
   private static final Set<Block> UNBREAKABLE = Set.of(
      Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.BEDROCK, Blocks.RESPAWN_ANCHOR, Blocks.REINFORCED_DEEPSLATE
   );
   private static volatile List<HoleEspRenderer.Hole> cache = List.of();
   private static int tickCounter;

   private HoleEspRenderer() {
   }

   public static void clear() {
      cache = List.of();
      tickCounter = 0;
   }

   public static void scan(DebugHoleEspModule module) {
      if (tickCounter++ % 10 == 0) {
         Minecraft mc = Minecraft.getInstance();
         ClientLevel level = mc.level;
         LocalPlayer player = mc.player;
         if (level != null && player != null) {
            int need = module.depth.is("Any") ? 1 : Integer.parseInt(module.depth.get());
            BlockPos origin = player.blockPosition();
            MutableBlockPos p = new MutableBlockPos();
            List<HoleEspRenderer.Hole> found = new ArrayList<>();

            for (int dx = -16; dx <= 16; dx++) {
               for (int dz = -16; dz <= 16; dz++) {
                  for (int dy = -6; dy <= 6; dy++) {
                     int x = origin.getX() + dx;
                     int y = origin.getY() + dy;
                     int z = origin.getZ() + dz;
                     if (isHole(level, p, x, y, z, need)) {
                        found.add(new HoleEspRenderer.Hole(x, y, z, wallsSafe(level, p, x, y, z)));
                     }
                  }
               }
            }

            cache = found;
         }
      }
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, DebugHoleEspModule module) {
      List<HoleEspRenderer.Hole> snapshot = cache;
      if (!snapshot.isEmpty()) {
         int safeColor = module.safe.get();
         int unsafeColor = module.unsafe.get();
         boolean tracers = module.tracers.get();
         Vector3fc forward = tracers ? Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector() : null;

         for (HoleEspRenderer.Hole hole : snapshot) {
            int color = hole.safe() ? safeColor : unsafeColor;
            EspBoxRenderer.outline(
               bufferSource,
               poseStack,
               cam,
               (double)hole.x(),
               (double)hole.y(),
               (double)hole.z(),
               (double)(hole.x() + 1),
               (double)(hole.y() + 1),
               (double)(hole.z() + 1),
               color,
               2.0F
            );
            if (tracers) {
               EspBoxRenderer.tracer(
                  bufferSource,
                  poseStack,
                  cam,
                  forward,
                  (double)hole.x() + 0.5,
                  (double)hole.y() + 0.5,
                  (double)hole.z() + 0.5,
                  Colors.withAlpha(color, 0.72F),
                  1.2F
               );
            }
         }

         EspBoxRenderer.flush(bufferSource);
      }
   }

   private static boolean isHole(ClientLevel level, MutableBlockPos p, int x, int y, int z, int need) {
      if (!level.getBlockState(p.set(x, y - 1, z)).blocksMotion()) {
         return false;
      } else {
         for (int i = 0; i < need; i++) {
            if (!level.getBlockState(p.set(x, y + i, z)).isAir()) {
               return false;
            }
         }

         return level.getBlockState(p.set(x + 1, y, z)).blocksMotion()
            && level.getBlockState(p.set(x - 1, y, z)).blocksMotion()
            && level.getBlockState(p.set(x, y, z + 1)).blocksMotion()
            && level.getBlockState(p.set(x, y, z - 1)).blocksMotion();
      }
   }

   private static boolean wallsSafe(ClientLevel level, MutableBlockPos p, int x, int y, int z) {
      return isUnbreakable(level.getBlockState(p.set(x, y - 1, z)))
         && isUnbreakable(level.getBlockState(p.set(x + 1, y, z)))
         && isUnbreakable(level.getBlockState(p.set(x - 1, y, z)))
         && isUnbreakable(level.getBlockState(p.set(x, y, z + 1)))
         && isUnbreakable(level.getBlockState(p.set(x, y, z - 1)));
   }

   private static boolean isUnbreakable(BlockState state) {
      return UNBREAKABLE.contains(state.getBlock());
   }

   private static record Hole(int x, int y, int z, boolean safe) {
   }
}
