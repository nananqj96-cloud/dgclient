package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.WeatherChangerModule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the Weather Changer module override the client-side rain and thunder gradients so the
 * forced weather state takes effect regardless of the server.
 */
@Mixin({Level.class})
public class WorldWeatherMixin {
   @Inject(
      method = {"getRainGradient(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$forceRain(float delta, CallbackInfoReturnable<Float> cir) {
      ModuleManager modules = AetherClient.modules();
      if (modules != null && modules.weatherChanger != null) {
         float v = modules.weatherChanger.clearValue();
         if (v != -1.0F) {
            cir.setReturnValue(v);
         }
      }
   }

   @Inject(
      method = {"getThunderGradient(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$forceThunder(float delta, CallbackInfoReturnable<Float> cir) {
      ModuleManager modules = AetherClient.modules();
      if (modules != null && modules.weatherChanger != null) {
         float v = modules.weatherChanger.thunderValue();
         if (v != -1.0F) {
            cir.setReturnValue(v);
         }
      }
   }
}
