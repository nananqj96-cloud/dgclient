package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityDragonWingsModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Wing scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting glow = this.addSetting(new BooleanSetting("Glow", "Wing glow", true));
   public final BooleanSetting trail = this.addSetting(new BooleanSetting("Trail", "Wing trail", true));

   public TenacityDragonWingsModule() {
      super("DragonWings", "Adds dragon wings to your player", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Dragon wings rendering handled via mixin
   }
}
