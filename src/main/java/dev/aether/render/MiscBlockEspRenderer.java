package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.SpawnerNametagsModule;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class MiscBlockEspRenderer {
   private MiscBlockEspRenderer() {
   }

   public static void renderSpawners(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, SpawnerNametagsModule spawnerNametagsModule) {
      int var4 = spawnerNametagsModule.color.get();

      for (BlockPos var6 : spawnerNametagsModule.visiblePositions()) {
         EspBoxRenderer.beam(
            bufferSource,
            poseStack,
            vec3,
            (double)var6.getX() + 0.5,
            (double)var6.getY() + 0.5,
            (double)var6.getZ() + 0.5,
            (double)var6.getY() + 500.5,
            var4,
            8.0F
         );
      }

      EspBoxRenderer.flush(bufferSource);
   }
}
