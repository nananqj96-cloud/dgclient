package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.BlockEspModule;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;

/**
 * Renders outlines on blocks in the world (Block ESP).
 */
public final class BlockEspRenderer {
   private BlockEspRenderer() {
   }

   public static void render(PoseStack poseStack, BufferSource bufferSource, BlockEspModule module) {
      // Block ESP rendering stub - detailed block highlighting disabled
   }
}
