package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityRadarModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Radar range", 50.0, 10.0, 200.0, 5.0, "m"));
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Radar scale", 1.0, 0.5, 3.0, 0.1));
   public final BooleanSetting players = this.addSetting(new BooleanSetting("Players", "Show players", true));
   public final BooleanSetting entities = this.addSetting(new BooleanSetting("Entities", "Show entities", true));

   public TenacityRadarModule() {
      super("Radar", "Displays a mini-map radar", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Radar rendering handled by HUD overlay
   }
}
