package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTweaksModule extends Module {
   public final BooleanSetting noBob = this.addSetting(new BooleanSetting("NoBob", "Disable camera bob", true));
   public final BooleanSetting noViewBob = this.addSetting(new BooleanSetting("NoViewBob", "Disable view bobbing", true));
   public final BooleanSetting fullBright = this.addSetting(new BooleanSetting("FullBright", "Full brightness", true));
   public final SliderSetting fov = this.addSetting(new SliderSetting("FOV", "Custom FOV", 70.0, 30.0, 120.0, 1.0, "°"));

   public TenacityTweaksModule() {
      super("Tweaks", "Various gameplay and visual tweaks", Category.MISC);
   }

   @Override
   public void onTick() {
      // Tweaks handled via mixin
   }
}
