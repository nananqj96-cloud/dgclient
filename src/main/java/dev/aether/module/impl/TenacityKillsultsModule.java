package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityKillsultsModule extends Module {
   public final BooleanSetting onKill = this.addSetting(new BooleanSetting("OnKill", "Send killsult on kill", true));
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Killsult delay ms", 500.0, 100.0, 3000.0, 100.0, "ms"));

   private long lastKill;

   public TenacityKillsultsModule() {
      super("Killsults", "Sends killsult messages on kill", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      // Killsult sending handled via event mixin
   }
}
