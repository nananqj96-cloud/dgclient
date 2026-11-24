package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.SpawnerNametagsModule;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Renders miscellaneous block ESP elements such as spawner highlighting.
 */
public final class MiscBlockEspRenderer {
   private MiscBlockEspRenderer() {
   }

   public static void renderSpawners(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, SpawnerNametagsModule spawnerNametagsModule) {
      // Spawner ESP rendering stub - detailed spawner highlighting disabled
   }
}
