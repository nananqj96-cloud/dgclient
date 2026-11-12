package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;

public final class SpeedMineModule extends Module {
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Changes normal block-breaking progress. Servers may limit high values.", 140.0, 100.0, 250.0, 5.0, "%")
   );

   public SpeedMineModule() {
      super("Speed Mine", "Mines blocks faster while keeping normal mining behaviour.", Category.MISC);
   }

   public float apply(float f) {
      if (Float.isFinite(f) && !(f <= 0.0F)) {
         float var2 = Math.clamp(this.speed.getFloat() / 100.0F, 1.0F, 2.5F);
         return Math.min(1.0F, f * var2);
      } else {
         return f;
      }
   }
}
