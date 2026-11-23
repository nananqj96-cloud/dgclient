package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityKeystrokesModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Keystroke scale", 1.0, 0.5, 3.0, 0.1));
   public final BooleanSetting click = this.addSetting(new BooleanSetting("Click", "Show mouse clicks", true));
   public final BooleanSetting keys = this.addSetting(new BooleanSetting("Keys", "Show key presses", true));

   public TenacityKeystrokesModule() {
      super("Keystrokes", "Displays keystroke and mouse overlay", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(true);


   }
}
