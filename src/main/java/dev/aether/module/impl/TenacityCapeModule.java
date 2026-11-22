package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityCapeModule extends Module {
   public final SliderSetting colorR = this.addSetting(new SliderSetting("Red", "Cape red", 72.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorG = this.addSetting(new SliderSetting("Green", "Cape green", 81.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorB = this.addSetting(new SliderSetting("Blue", "Cape blue", 176.0, 0.0, 255.0, 1.0));
   public final BooleanSetting glow = this.addSetting(new BooleanSetting("Glow", "Cape glow", true));

   public TenacityCapeModule() {
      super("Cape", "Adds a custom colored cape to your player", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Cape rendering handled via mixin
   }
}
