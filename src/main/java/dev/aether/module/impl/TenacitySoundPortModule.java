package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacitySoundPortModule extends Module {
   public final SliderSetting volume = this.addSetting(new SliderSetting("Volume", "Sound volume", 100.0, 0.0, 100.0, 5.0, "%"));
   public final BooleanSetting directional = this.addSetting(new BooleanSetting("Directional", "Directional sound", true));
   public final BooleanSetting filter = this.addSetting(new BooleanSetting("Filter", "Filter ambient sounds", true));

   public TenacitySoundPortModule() {
      super("SoundPort", "Customizable sound management", Category.MISC);
   }

   @Override
   public void onTick() {
      // Sound port handled via mixin
   }
}
