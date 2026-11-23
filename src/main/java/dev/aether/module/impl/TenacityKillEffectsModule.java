package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityKillEffectsModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Effect scale", 1.0, 0.5, 3.0, 0.1));
   public final BooleanSetting particles = this.addSetting(new BooleanSetting("Particles", "Kill particle effect", true));
   public final BooleanSetting lightning = this.addSetting(new BooleanSetting("Lightning", "Lightning strike effect", true));

   public TenacityKillEffectsModule() {
      super("KillEffects", "Visual effects when killing entities", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      for (var e : mc.level.entitiesForRendering()) {
         if (e instanceof net.minecraft.world.entity.LivingEntity) e.setGlowingTag(true);
      }

   }
}
