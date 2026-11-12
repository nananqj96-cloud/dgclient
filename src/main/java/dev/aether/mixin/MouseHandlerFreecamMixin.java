package dev.aether.mixin;

import dev.aether.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
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
      method = {"turnPlayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$freecamMouse(double d, CallbackInfo ci) {
      FreecamModule freecam = FreecamModule.get();
      if (freecam != null && freecam.isActive()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.screen == null) {
            double sensitivity = (Double)mc.options.sensitivity().get() * 0.6 + 0.2;
            double factor = sensitivity * sensitivity * sensitivity * 8.0;
            double dx = this.accumulatedDX * factor * (double)freecam.getLookSensitivity();
            double dy = this.accumulatedDY * factor * (double)freecam.getLookSensitivity();
            float newYaw = freecam.getCurrentYaw() + (float)dx * 0.15F;
            float newPitch = freecam.getCurrentPitch() + (float)dy * 0.15F;
            freecam.setRotation(newYaw, newPitch);
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"onScroll(JDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$freecamScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      FreecamModule freecam = FreecamModule.get();
      Minecraft mc = Minecraft.getInstance();
      if (freecam != null && mc.screen == null && window == mc.getWindow().handle() && freecam.adjustSpeedFromScroll(vertical)) {
         ci.cancel();
      }
   }
}
