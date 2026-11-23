package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;

public class AspectRatioModule extends Module {
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Which direction to stretch the screen",
         "Both",
         "Horizontal",
         "Vertical",
         "Both"
      )
   );
   public final SliderSetting amount = this.addSetting(
      new SliderSetting(
         "Amount",
         "How much to stretch (100% = no stretch)",
         100.0,
         50.0,
         200.0,
         5.0,
         "%"
      )
   );

   public AspectRatioModule() {
      super("AspectRatio", "Stretches your screen horizontally and/or vertically for custom aspect ratios", Category.RENDER);
   }

   public float getHorizontalStretch() {
      String m = mode.get();
      return (m.equals("Horizontal") || m.equals("Both")) ? amount.getFloat() / 100.0F : 1.0F;
   }

   public float getVerticalStretch() {
      String m = mode.get();
      return (m.equals("Vertical") || m.equals("Both")) ? amount.getFloat() / 100.0F : 1.0F;
   }

   public boolean isActive() {
      return isEnabled() && (getHorizontalStretch() != 1.0F || getVerticalStretch() != 1.0F);
   }
}
