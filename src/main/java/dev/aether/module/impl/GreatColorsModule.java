package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;

public class GreatColorsModule extends Module {
   public final SliderSetting saturation = this.addSetting(
      new SliderSetting(
         "Saturation",
         "Adjust color saturation (100% = normal)",
         100.0,
         0.0,
         200.0,
         5.0,
         "%"
      )
   );

   public GreatColorsModule() {
      super("GreatColors", "Adjust the saturation of the game's colors for richer or more muted visuals", Category.RENDER);
   }

   public float getSaturation() {
      return saturation.getFloat() / 100.0F;
   }

   public boolean isActive() {
      return isEnabled() && getSaturation() != 1.0F;
   }
}
