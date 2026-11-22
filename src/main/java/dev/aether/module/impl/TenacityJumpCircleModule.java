package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityJumpCircleModule extends Module {
   public final SliderSetting radius = this.addSetting(new SliderSetting("Radius", "Circle radius", 2.0, 0.5, 5.0, 0.1, "m"));
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Circle scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting highlight = this.addSetting(new BooleanSetting("Highlight", "Highlight center", true));

   public TenacityJumpCircleModule() {
      super("JumpCircle", "Draws a circle on the ground for jump timing", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Jump circle rendering handled by HUD overlay
   }
}
