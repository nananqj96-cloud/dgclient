package dev.aether.mixin;

import net.minecraft.client.player.KeyboardInput;
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
      // Snap Tap input override stub - Anubis-internal API disabled
   }
}
