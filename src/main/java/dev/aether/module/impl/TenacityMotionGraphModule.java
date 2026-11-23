package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityMotionGraphModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Graph scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting velocity = this.addSetting(new BooleanSetting("Velocity", "Show velocity graph", true));
   public final BooleanSetting position = this.addSetting(new BooleanSetting("Position", "Show position graph", true));

   public TenacityMotionGraphModule() {
      super("MotionGraph", "Displays a graph of your motion", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      var d = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(d.x, d.y * (velocity.get() ? 1.02f : 1.0f), d.z);


   }
}
