package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityBrightnessModule extends Module {
   public final SliderSetting brightness = this.addSetting(new SliderSetting("Brightness", "Brightness level", 100.0, 0.0, 100.0, 5.0, "%"));

   public TenacityBrightnessModule() {
      super("Brightness", "Increases screen brightness for dark areas", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.options.gamma().set(brightness.getFloat() / 100.0 * 100.0);

   }
}
