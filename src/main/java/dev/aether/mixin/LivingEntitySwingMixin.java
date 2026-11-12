package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public class LivingEntitySwingMixin {
   @ModifyReturnValue(
      method = {"getCurrentSwingDuration"},
      at = {@At("RETURN")}
   )
   private int aetherclient$swingSpeed(int original) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (Minecraft.getInstance().player != self) {
         return original;
      } else {
         ModuleManager modules = AetherClient.modules();
         if (modules != null && modules.swingSpeed != null && modules.swingSpeed.isEnabled()) {
            float multiplier = modules.swingSpeed.multiplier();
            return multiplier <= 0.01F ? original : Math.max(1, Math.round((float)original / multiplier));
         } else {
            return original;
         }
      }
   }
}
