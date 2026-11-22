package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTargetHudModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Target HUD scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting health = this.addSetting(new BooleanSetting("Health", "Show health bar", true));
   public final BooleanSetting distance = this.addSetting(new BooleanSetting("Distance", "Show distance", true));
   public final BooleanSetting name = this.addSetting(new BooleanSetting("Name", "Show target name", true));

   public TenacityTargetHudModule() {
      super("TargetHUD", "Displays a HUD overlay on your current target", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Target HUD rendering handled by HUD overlay
   }
}
