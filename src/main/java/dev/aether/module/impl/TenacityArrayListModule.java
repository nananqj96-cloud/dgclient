package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityArrayListModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "ArrayList scale", 1.0, 0.5, 3.0, 0.1));
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Animation speed", 1.0, 0.1, 5.0, 0.1));
   public final BooleanSetting rainbow = this.addSetting(new BooleanSetting("Rainbow", "Rainbow colors", true));

   public TenacityArrayListModule() {
      super("ArrayList", "Shows enabled modules as an animated list", Category.RENDER);
   }

   @Override
   public void onTick() {
      // ArrayList rendering handled by HUD overlay
   }
}
