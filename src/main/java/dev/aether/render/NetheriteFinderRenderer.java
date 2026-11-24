package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.NetheriteFinderModule;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Renders Netherite Finder chunk tracer overlays.
 */
public final class NetheriteFinderRenderer {
   private NetheriteFinderRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, NetheriteFinderModule netheriteFinderModule) {
      // Netherite Finder rendering stub - detailed chunk tracer rendering disabled
   }
}
