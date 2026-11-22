package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityDerpModule extends Module {
   public final SliderSetting chance = this.addSetting(new SliderSetting("Chance", "Rotation randomness %", 20.0, 0.0, 100.0, 5.0, "%"));

   public TenacityDerpModule() {
      super("Derp", "Randomly adds rotation jitter to your view", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (Math.random() * 100 < chance.getFloat()) {
         mc.player.setYRot(mc.player.getYRot() + (float)((Math.random() - 0.5) * 90.0));
      }
   }
}
