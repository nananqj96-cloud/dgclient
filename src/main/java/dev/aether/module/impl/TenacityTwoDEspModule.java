package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTwoDEspModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "ESP range", 50.0, 10.0, 200.0, 5.0, "m"));
   public final SliderSetting colorR = this.addSetting(new SliderSetting("Red", "ESP red", 255.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorG = this.addSetting(new SliderSetting("Green", "ESP green", 100.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorB = this.addSetting(new SliderSetting("Blue", "ESP blue", 100.0, 0.0, 255.0, 1.0));
   public final BooleanSetting box = this.addSetting(new BooleanSetting("Box", "Draw 2D boxes", true));
   public final BooleanSetting outline = this.addSetting(new BooleanSetting("Outline", "Draw outlines", true));

   public TenacityTwoDEspModule() {
      super("2D ESP", "2D bounding box ESP for entities", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      for (var e : mc.level.entitiesForRendering()) {
         if (e instanceof net.minecraft.world.entity.LivingEntity && e != mc.player) {
            e.setGlowingTag(true);
         }
      }

   }
}
