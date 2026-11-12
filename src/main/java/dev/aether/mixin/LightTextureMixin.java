package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin({LightTexture.class})
public class LightTextureMixin {
   @ModifyExpressionValue(
      method = {"updateLightTexture"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;"
         )
      )}
   )
   private Object aetherclient$fullbrightGamma(Object original) {
      ModuleManager modules = AetherClient.modules();
      return modules != null && modules.fullbright != null && modules.fullbright.isEnabled() ? (double)modules.fullbright.gamma.getFloat() : original;
   }
}
