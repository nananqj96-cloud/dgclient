package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityBedDefenderModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Bed defender range", 5.0, 1.0, 20.0, 0.5, "m"));
   public final BooleanSetting auto = this.addSetting(new BooleanSetting("Auto", "Auto-defend beds", true));

   public TenacityBedDefenderModule() {
      super("BedDefender", "Protects beds from being destroyed", Category.PLAYER);
   }

   @Override
   public void onTick() {
      // Bed defender handled via block interaction mixin
   }
}
