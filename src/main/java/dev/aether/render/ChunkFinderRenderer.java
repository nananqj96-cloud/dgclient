package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.AetherClient;
import dev.aether.module.impl.ChunkFinderModule;
import dev.aether.util.Colors;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class ChunkFinderRenderer {
   private static final float RENDER_Y = 55.0F;
   private static final int FILL_ALPHA = 190;

   private ChunkFinderRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 camera, ChunkFinderModule module) {
      if (!module.flaggedChunks().isEmpty()) {
         int accent = AetherClient.themes().current().accent();
         int fill = Colors.withAlpha(Colors.darken(accent, 0.58F), 190);
         int outline = Colors.withAlpha(accent, 255);

         for (long key : module.flaggedChunks()) {
            double x0 = (double)ChunkPos.getX(key) * 16.0;
            double z0 = (double)ChunkPos.getZ(key) * 16.0;
            double x1 = x0 + 16.0;
            double z1 = z0 + 16.0;
            FlatOverlay.fillQuad(bufferSource, poseStack, camera, x0, z0, x1, z1, 55.0, fill);
            FlatOverlay.edge(bufferSource, poseStack, camera, x0, z0, x1, z0, 55.0, outline, 2.0F);
            FlatOverlay.edge(bufferSource, poseStack, camera, x1, z0, x1, z1, 55.0, outline, 2.0F);
            FlatOverlay.edge(bufferSource, poseStack, camera, x1, z1, x0, z1, 55.0, outline, 2.0F);
            FlatOverlay.edge(bufferSource, poseStack, camera, x0, z1, x0, z0, 55.0, outline, 2.0F);
         }

         FlatOverlay.flush(bufferSource);
      }
   }
}
