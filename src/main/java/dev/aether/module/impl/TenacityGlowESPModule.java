package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class TenacityGlowESPModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "ESP range", 50.0, 10.0, 200.0, 5.0, "m"));
   public final SliderSetting colorR = this.addSetting(new SliderSetting("Red", "Glow red", 100.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorG = this.addSetting(new SliderSetting("Green", "Glow green", 100.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorB = this.addSetting(new SliderSetting("Blue", "Glow blue", 255.0, 0.0, 255.0, 1.0));

   public TenacityGlowESPModule() {
      super("GlowESP", "Renders glowing outlines on entities", Category.RENDER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return;
      // Glow rendering handled via mixin; settings used by renderer
   }
}
