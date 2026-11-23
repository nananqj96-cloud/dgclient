package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityParticlesModule extends Module {
   public final SliderSetting count = this.addSetting(new SliderSetting("Count", "Particle count", 10.0, 1.0, 100.0, 1.0));
   public final BooleanSetting feet = this.addSetting(new BooleanSetting("Feet", "Particles at feet", true));
   public final BooleanSetting movement = this.addSetting(new BooleanSetting("Movement", "Particles on movement", true));

   public TenacityParticlesModule() {
      super("Particles", "Renders custom particles around the player", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      mc.player.setGlowingTag(true);

   }
}
