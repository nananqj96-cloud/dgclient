package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAmbienceModule extends Module {
   public final SliderSetting brightness = this.addSetting(new SliderSetting("Brightness", "Ambient brightness", 100.0, 0.0, 200.0, 10.0, "%"));
   public final SliderSetting saturation = this.addSetting(new SliderSetting("Saturation", "Color saturation", 100.0, 0.0, 200.0, 10.0, "%"));
   public final BooleanSetting override = this.addSetting(new BooleanSetting("Override", "Override world lighting", true));

   public TenacityAmbienceModule() {
      super("Ambience", "Adjusts world lighting and color ambiance", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      mc.level.setWeather(net.minecraft.world.level.weather.WeatherStatistics.CLEAR, 0, false);

   }
}
