package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityStatisticsModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Statistics scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting kills = this.addSetting(new BooleanSetting("Kills", "Show kill count", true));
   public final BooleanSetting deaths = this.addSetting(new BooleanSetting("Deaths", "Show death count", true));
   public final BooleanSetting playtime = this.addSetting(new BooleanSetting("Playtime", "Show play time", true));

   public TenacityStatisticsModule() {
      super("Statistics", "Displays player statistics on screen", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Statistics rendering handled by HUD overlay
   }
}
