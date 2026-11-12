package dev.aether.mixin;

import dev.aether.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class KeyboardInputFreecamMixin {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void aetherclient$freecamReapplyCachedBodyInput(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.input == (Object)this) {
         FreecamModule f = FreecamModule.get();
         if (f != null && f.isActive()) {
            FreecamModule.reapplyBodyInput(mc);
         }
      }
   }
}
