package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTPAKillerModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Detection range", 100.0, 10.0, 500.0, 10.0, "m"));
   public final BooleanSetting notify = this.addSetting(new BooleanSetting("Notify", "Notify on TPA detection", true));

   public TenacityTPAKillerModule() {
      super("TPAKiller", "Detects and blocks TPA requests", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      // TPA packet interception handled via mixin
   }
}
