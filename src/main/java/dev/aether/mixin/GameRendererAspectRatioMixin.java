package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.impl.AspectRatioModule;
import dev.aether.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class GameRendererAspectRatioMixin {
   @Inject(
      method = {"render(Lnet/minecraft/client/DeltaTracker;Z)V"},
      at = {@At("HEAD")}
   )
   private void aetherclient$applyAspectRatio(CallbackInfo ci) {
      ModuleManager modules = AetherClient.modules();
      if (modules == null || modules.aspectRatio == null || !modules.aspectRatio.isActive()) {
         return;
      }
      AspectRatioModule ar = modules.aspectRatio;
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.getWindow() == null) return;

      int windowW = mc.getWindow().getWidth();
      int windowH = mc.getWindow().getHeight();

      // Compute stretched viewport dimensions
      int stretchedW = (int) (windowW * ar.getHorizontalStretch());
      int stretchedH = (int) (windowH * ar.getVerticalStretch());

      // Clamp to reasonable limits
      stretchedW = Math.max(1, Math.min(stretchedW, windowW * 3));
      stretchedH = Math.max(1, Math.min(stretchedH, windowH * 3));

      GL33C.glViewport(0, 0, stretchedW, stretchedH);
   }
}
