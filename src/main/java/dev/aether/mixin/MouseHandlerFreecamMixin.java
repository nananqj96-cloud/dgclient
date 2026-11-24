package dev.aether.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public abstract class MouseHandlerFreecamMixin {
   @Shadow
   private double accumulatedDX;
   @Shadow
   private double accumulatedDY;

   @Inject(
      method = {"onScroll(JDD)V"},
      at = {@At("TAIL")}
   )
   private void aetherclient$freecamMouse(double d, CallbackInfo ci) {
      // Freecam mouse look stub - Anubis-internal API disabled
   }
}
