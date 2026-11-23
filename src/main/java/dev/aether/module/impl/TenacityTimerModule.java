package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTimerModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Time scale", 2.0, 0.1, 10.0, 0.1, "x"));

   public TenacityTimerModule() {
      super("Timer", "Modifies game time speed", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(1.01, 1.0, 1.01));

   }

   public float getSpeed() { return speed.getFloat(); }
}
