package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityLightningTrackerModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Tracker scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting highlight = this.addSetting(new BooleanSetting("Highlight", "Highlight lightning positions", true));

   public TenacityLightningTrackerModule() {
      super("LightningTracker", "Tracks and displays lightning strike locations", Category.MISC);
   }

   @Override
   public void onTick() {
      // Lightning tracking handled via entity event mixin
   }
}
