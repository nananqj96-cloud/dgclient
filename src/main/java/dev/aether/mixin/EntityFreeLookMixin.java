package dev.aether.mixin;

import dev.aether.module.impl.FreeLookModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public class EntityFreeLookMixin {
   @Inject(
      method = {"turn(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$freeLookTurn(double d, double e, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if ((Object)this == mc.player) {
         FreeLookModule freeLook = FreeLookModule.get();
         if (freeLook != null && freeLook.cameraMode()) {
            freeLook.addCameraLook(d, e);
            ci.cancel();
         }
      }
   }
}
