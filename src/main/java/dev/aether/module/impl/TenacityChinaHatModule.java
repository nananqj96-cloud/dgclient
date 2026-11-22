package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityChinaHatModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Hat scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting float = this.addSetting(new BooleanSetting("Float", "Floating hat", true));
   public final BooleanSetting glow = this.addSetting(new BooleanSetting("Glow", "Hat glow", true));

   public TenacityChinaHatModule() {
      super("ChinaHat", "Adds a floating China hat to your player", Category.RENDER);
   }

   @Override
   public void onTick() {
      // China hat rendering handled via mixin
   }
}
