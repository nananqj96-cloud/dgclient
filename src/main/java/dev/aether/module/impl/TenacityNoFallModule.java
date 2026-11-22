package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityNoFallModule extends Module {
   public final SliderSetting distance = this.addSetting(new SliderSetting("Distance", "Max fall distance", 3.0, 0.0, 10.0, 0.5, "b"));
   public final BooleanSetting reset = this.addSetting(new BooleanSetting("Reset", "Reset position on land", true));

   public TenacityNoFallModule() {
      super("NoFall", "Prevents fall damage", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (mc.player.fallDistance > distance.getFloat()) {
         mc.player.fallDistance = 0.0F;
      }
   }
}
