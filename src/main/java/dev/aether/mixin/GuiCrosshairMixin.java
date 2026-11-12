package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FreecamModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public class GuiCrosshairMixin {
   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$hideVanillaCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      ModuleManager modules = AetherClient.modules();
      if (modules != null && modules.customCrosshair != null && modules.customCrosshair.shouldHideVanilla()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"renderCrosshair"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"
      )}
   )
   private boolean aetherclient$freecamCrosshairInThirdPerson(boolean isFirstPerson) {
      FreecamModule freecam = FreecamModule.get();
      return freecam != null && freecam.isActive() && freecam.isShowPlayerModel() && !isFirstPerson ? true : isFirstPerson;
   }
}
