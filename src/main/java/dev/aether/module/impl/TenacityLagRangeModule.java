package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityLagRangeModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Lag range in ms", 0.0, 0.0, 2000.0, 100.0, "ms"));

   public TenacityLagRangeModule() {
      super("LagRange", "Delays entity interaction range", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      // Lag range manipulation handled via mixin
   }
}
