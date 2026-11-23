package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;

/**
 * Forces a client-side weather state — clear, rain or thunder — regardless of the server's actual
 * weather, so the world always looks the way you want.
 */
public class WeatherChangerModule extends Module {
   public final ModeSetting weather = this.addSetting(
      new ModeSetting("Weather", "Weather", "Which weather to force on the client", "Clear", "Clear", "Rain", "Thunder")
   );

   public WeatherChangerModule() {
      super("Weather Changer", "Force a client-side weather state", Category.MISC);
   }

   /** Returns 0 = clear, 1 = rain, or -1 = disabled (use server weather). */
   public float clearValue() {
      if (!this.isEnabled()) {
         return -1.0F;
      }
      return "Clear".equals(this.weather.get()) ? 0.0F : 1.0F;
   }

   /** Returns 1 = thunder, 0 = no thunder, or -1 = disabled. */
   public float thunderValue() {
      if (!this.isEnabled()) {
         return -1.0F;
      }
      return "Thunder".equals(this.weather.get()) ? 1.0F : 0.0F;
   }
}
