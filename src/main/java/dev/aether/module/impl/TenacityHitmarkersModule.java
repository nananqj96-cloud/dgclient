package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityHitmarkersModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Hitmarker scale", 1.0, 0.5, 3.0, 0.1));
   public final BooleanSetting sounds = this.addSetting(new BooleanSetting("Sounds", "Play hitmarker sounds", true));
   public final BooleanSetting damage = this.addSetting(new BooleanSetting("Damage", "Show damage indicators", true));

   public TenacityHitmarkersModule() {
      super("Hitmarkers", "Shows hit markers when hitting entities", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Hitmarker rendering handled by HUD overlay
   }
}
