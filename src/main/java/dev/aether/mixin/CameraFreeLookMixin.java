package dev.aether.mixin;

import dev.aether.module.impl.FreeLookModule;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public abstract class CameraFreeLookMixin {
   @ModifyArgs(
      method = {"setup"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setRotation(FF)V"
      )
   )
   private void aetherclient$freeLookRotation(Args args) {
      FreeLookModule freeLook = FreeLookModule.get();
      if (freeLook != null && freeLook.isActive()) {
         args.set(0, 0.0F);
            args.set(1, freeLook.getCameraPitch());
         }
      }
   }

   @Inject(
      method = {"getMaxZoom"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$freeLookThroughWalls(float distance, CallbackInfoReturnable<Float> cir) {
      FreeLookModule freeLook = FreeLookModule.get();
      if (freeLook != null && freeLook.isActive()) {
         cir.setReturnValue(distance);
         }
      }
   }
}
