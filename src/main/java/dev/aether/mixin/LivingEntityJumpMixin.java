package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public class LivingEntityJumpMixin {
   @Inject(
      method = {"jumpFromGround"},
      at = {@At("HEAD")}
   )
   private void aetherclient$onJump(CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer player && player == Minecraft.getInstance().player) {
         ModuleManager modules = AetherClient.modules();
         if (modules != null && modules.jumpCircles != null && modules.jumpCircles.isEnabled()) {
            modules.jumpCircles.onPlayerJump(player);
         }
      }
   }
}
