package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityMCFModule extends Module {
   public final BooleanSetting middleClick = this.addSetting(new BooleanSetting("MiddleClick", "Middle-click to ping location", true));
   public final BooleanSetting event = this.addSetting(new BooleanSetting("Event", "Send ping on event", true));

   public TenacityMCFModule() {
      super("MCF", "Middle-click function for coordinates", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      // MCF handled via mouse click mixin
   }
}
