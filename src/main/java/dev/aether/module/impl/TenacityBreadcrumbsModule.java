package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class TenacityBreadcrumbsModule extends Module {
   public final SliderSetting max = this.addSetting(new SliderSetting("Max", "Maximum trail points", 100.0, 10.0, 500.0, 10.0));
   public final BooleanSetting showLine = this.addSetting(new BooleanSetting("Line", "Show connecting line", true));
   private final List<double[]> trail = new ArrayList<>();

   public TenacityBreadcrumbsModule() {
      super("Breadcrumbs", "Leaves a trail of your movement path", Category.RENDER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      double[] pos = {mc.player.getX(), mc.player.getY(), mc.player.getZ()};
      if (trail.isEmpty() || Math.abs(trail.get(trail.size() - 1)[0] - pos[0]) > 0.5
         || Math.abs(trail.get(trail.size() - 1)[2] - pos[2]) > 0.5) {
         trail.add(pos);
         while (trail.size() > max.getInt()) trail.remove(0);
      }
   }

   @Override
   protected void onDisable() {
      trail.clear();
   }
}
