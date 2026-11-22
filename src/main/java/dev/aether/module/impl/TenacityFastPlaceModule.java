package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityFastPlaceModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Place delay", 0.0, 0.0, 10.0, 1.0, "t"));
   public final BooleanSetting continuous = this.addSetting(new BooleanSetting("Continuous", "Place continuously while holding", true));

   public TenacityFastPlaceModule() {
      super("FastPlace", "Places blocks faster than normal", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (continuous.get() && mc.options.keyUse.isDown()) {
         // Fast place handled via mixin
      }
   }
}
