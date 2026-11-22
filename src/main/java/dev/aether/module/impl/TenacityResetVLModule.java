package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityResetVLModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Reset delay in ms", 1000.0, 500.0, 5000.0, 100.0, "ms"));
   private long lastReset;

   public TenacityResetVLModule() {
      super("ResetVL", "Resets visibility list periodically", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      long now = System.currentTimeMillis();
      if (now - lastReset > delay.getInt()) {
         lastReset = now;
         // VL reset handled via mixin
      }
   }
}
