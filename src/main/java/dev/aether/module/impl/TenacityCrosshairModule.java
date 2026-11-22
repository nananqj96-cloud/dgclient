package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityCrosshairModule extends Module {
   public final BooleanSetting toggle = this.addSetting(new BooleanSetting("Toggle", "Toggle crosshair", false));
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Crosshair scale", 1.0, 0.5, 3.0, 0.1));
   public final SliderSetting gap = this.addSetting(new SliderSetting("Gap", "Crosshair gap", 5.0, 1.0, 20.0, 1.0));

   public TenacityCrosshairModule() {
      super("Crosshair", "Customizes the crosshair", Category.RENDER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) return;
      // Rendering handled via mixin
   }
}
