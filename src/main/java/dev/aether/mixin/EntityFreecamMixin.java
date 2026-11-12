package dev.aether.mixin;

import dev.aether.module.impl.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class EntityFreecamMixin {
   @Inject(
      method = {"isInvisibleTo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$freecamSeeOwnBody(Player viewer, CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)(Object)this;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && self == mc.player && viewer == mc.player) {
         FreecamModule freecam = FreecamModule.get();
         if (freecam != null && freecam.isActive() && freecam.isShowPlayerModel()) {
            cir.setReturnValue(false);
         }
      }
   }
}
