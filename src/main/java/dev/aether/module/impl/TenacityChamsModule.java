package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityChamsModule extends Module {
   public final BooleanSetting seeThrough = this.addSetting(new BooleanSetting("SeeThrough", "See through players", true));
   public final SliderSetting colorR = this.addSetting(new SliderSetting("Red", "Color red", 255.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorG = this.addSetting(new SliderSetting("Green", "Color green", 0.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorB = this.addSetting(new SliderSetting("Blue", "Color blue", 100.0, 0.0, 255.0, 1.0));
   public final SliderSetting alpha = this.addSetting(new SliderSetting("Alpha", "Opacity", 150.0, 0.0, 255.0, 1.0));

   public TenacityChamsModule() {
      super("Chams", "Colored player models with transparency", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      for (var e : mc.level.entitiesForRendering()) {
         if (e instanceof net.minecraft.world.entity.LivingEntity) {
            e.setGlowingTag(true);
         }
      }

   }
}
