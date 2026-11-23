package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacitySpeedMineModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Mine speed multiplier", 5.0, 1.0, 20.0, 0.5));
   public final BooleanSetting autoBreak = this.addSetting(new BooleanSetting("AutoBreak", "Auto-break blocks instantly", true));

   public TenacitySpeedMineModule() {
      super("SpeedMine", "Mines blocks at increased speed", Category.PLAYER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.getAbilities().mayfly = true;

   }
}
