package dev.aether.mixin;

import dev.aether.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerFreecamMixin {
   @Inject(
      method = {"aiStep"},
      at = {@At("HEAD")}
   )
   private void aetherclient$freecamReapplyBeforeAiStep(CallbackInfo ci) {
      this.aetherclient$reapply();
   }

   @Inject(
      method = {"sendPosition"},
      at = {@At("HEAD")}
   )
   private void aetherclient$freecamReapplyBeforeSendPosition(CallbackInfo ci) {
      this.aetherclient$reapply();
   }

   private void aetherclient$reapply() {
      LocalPlayer self = (LocalPlayer)(Object)this;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == self) {
         FreecamModule f = FreecamModule.get();
         if (f != null && f.isActive()) {
            FreecamModule.reapplyBodyInput(mc);
         }
      }
   }
}
