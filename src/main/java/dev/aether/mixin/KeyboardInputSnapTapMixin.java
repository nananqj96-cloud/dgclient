package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.impl.TenacitySnapTapModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class KeyboardInputSnapTapMixin {
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void aetherclient$applySnapTap(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.input == (Object)this) {
         var modules = AetherClient.modules();
         if (modules != null && modules.tenacitySnapTap != null && modules.tenacitySnapTap.isEnabled()) {
            Input original = ((ClientInputAccessor) mc.player.input).aetherclient$getKeyPresses();
            Input modified = TenacitySnapTapModule.apply(original);
            if (modified != original) {
               ((ClientInputAccessor) mc.player.input).aetherclient$setKeyPresses(modified);
            }
         }
      }
   }
}
