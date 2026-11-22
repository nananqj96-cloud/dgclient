package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityKeepSprintModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Sprint speed", 1.0, 0.5, 3.0, 0.1));
   public final BooleanSetting always = this.addSetting(new BooleanSetting("Always", "Keep sprint always", false));

   public TenacityKeepSprintModule() {
      super("KeepSprint", "Prevents sprint from stopping during combat", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null || mc.options == null) return;
      if (always.get() || mc.options.keySprint.isDown() || mc.options.keyForward.isDown()) {
         mc.options.keySprint.setDown(true);
      }
   }
}
