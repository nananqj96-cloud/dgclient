package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityFuckerModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Fucker range", 10.0, 5.0, 50.0, 1.0, "m"));
   public final BooleanSetting auto = this.addSetting(new BooleanSetting("Auto", "Auto-interact with containers", true));

   public TenacityFuckerModule() {
      super("Fucker", "Automatically opens containers in range", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      // Container auto-open handled via block interaction mixin
   }
}
