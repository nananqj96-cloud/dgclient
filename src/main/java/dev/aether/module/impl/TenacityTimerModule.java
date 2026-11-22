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
      // Timer is handled via mixin; this module is a toggle/setting holder
   }

   public float getSpeed() { return speed.getFloat(); }
}
