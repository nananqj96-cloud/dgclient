package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.SkinProtectModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public class AbstractClientPlayerMixin {
   @Inject(
      method = {"getSkin"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void aetherclient$replaceSkin(CallbackInfoReturnable<PlayerSkin> cir) {
      ModuleManager modules = AetherClient.modules();
      if (modules != null) {
         SkinProtectModule skinProtect = modules.skinProtect;
         if (skinProtect != null && skinProtect.isEnabled()) {
            PlayerSkin replacement = skinProtect.replacementSkin();
            if (replacement != null) {
               AbstractClientPlayer self = (AbstractClientPlayer)(Object)this;
               if (skinProtect.shouldReplace(self.getUUID())) {
                  cir.setReturnValue(replacement);
               }
            }
         }
      }
   }
}
