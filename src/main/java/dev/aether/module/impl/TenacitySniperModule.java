package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacitySniperModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Sniper range", 100.0, 20.0, 500.0, 10.0, "m"));
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Snipe delay ms", 100.0, 50.0, 1000.0, 50.0, "ms"));
   public final BooleanSetting auto = this.addSetting(new BooleanSetting("Auto", "Auto-snipe on death", true));

   private long lastSnipe;

   public TenacitySniperModule() {
      super("Sniper", "Tracks and snipes player deaths", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      // Sniper handling via death event mixin
   }
}
