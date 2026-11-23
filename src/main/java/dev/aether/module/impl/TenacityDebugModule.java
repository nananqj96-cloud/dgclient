package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityDebugModule extends Module {
   public final SliderSetting ping = this.addSetting(new SliderSetting("Ping", "Display ping", 1.0, 0.0, 1.0, 1.0));
   public final BooleanSetting fps = this.addSetting(new BooleanSetting("FPS", "Display FPS", true));
   public final BooleanSetting tps = this.addSetting(new BooleanSetting("TPS", "Display TPS", true));
   public final BooleanSetting coordinates = this.addSetting(new BooleanSetting("Coords", "Display coordinates", true));

   public TenacityDebugModule() {
      super("Debug", "Displays debug information on screen", Category.MISC);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) return;
      mc.debugOverlayRendererEnabled = true;

   }
}
