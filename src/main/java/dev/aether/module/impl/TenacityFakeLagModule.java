package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityFakeLagModule extends Module {
   public final SliderSetting ticks = this.addSetting(new SliderSetting("Ticks", "Fake lag ticks", 10.0, 1.0, 50.0, 1.0, "t"));
   public final BooleanSetting freeze = this.addSetting(new BooleanSetting("Freeze", "Freeze rendering during lag", true));

   public TenacityFakeLagModule() {
      super("FakeLag", "Simulates lag to appear unresponsive", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      // Fake lag handled via mixin
   }
}
