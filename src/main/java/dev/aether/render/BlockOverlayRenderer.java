package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.BlockOverlayModule;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a fill/outline/glow overlay on the block the player is looking at.
 */
public final class BlockOverlayRenderer {
   private BlockOverlayRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, BlockOverlayModule module) {
      // Block overlay rendering stub - detailed block outline rendering disabled
   }
}
