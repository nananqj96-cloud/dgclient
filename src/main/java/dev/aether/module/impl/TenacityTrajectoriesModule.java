package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTrajectoriesModule extends Module {
   public final BooleanSetting arrows = this.addSetting(new BooleanSetting("Arrows", "Show arrow trajectories", true));
   public final BooleanSetting fireballs = this.addSetting(new BooleanSetting("Fireballs", "Show fireball trajectories", true));
   public final SliderSetting steps = this.addSetting(new SliderSetting("Steps", "Trajectory steps", 20.0, 5.0, 50.0, 1.0));

   public TenacityTrajectoriesModule() {
      super("Trajectories", "Predicts projectile flight paths", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(arrows.get() || fireballs.get());


   }
}
