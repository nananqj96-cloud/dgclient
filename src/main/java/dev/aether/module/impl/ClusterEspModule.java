package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.ClusterEspRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;

public class ClusterEspModule extends Module {
   public final SliderSetting minimumBlocks = this.addSetting(
      new SliderSetting("Minimum Matches", "Minimum matching cluster-light positions required in one chunk", 4.0, 1.0, 32.0, 1.0)
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Cluster Color", "Color used for fully grown cluster markers and tracers", -4895489));
   public final SliderSetting fillAlpha = this.addSetting(new SliderSetting("Fill Alpha", "Opacity inside cluster boxes", 45.0, 0.0, 255.0, 1.0));
   public final SliderSetting outlineAlpha = this.addSetting(new SliderSetting("Outline Alpha", "Opacity of cluster outlines", 220.0, 0.0, 255.0, 1.0));
   public final BooleanSetting tracers = this.addSetting(new BooleanSetting("Tracers", "Draw a tracer to each cluster center", true));

   public ClusterEspModule() {
      super("Cluster ESP", "Highlights only fully grown amethyst clusters", Category.RENDER);
   }

   @Override
   public void onTick() {
      ClusterEspRenderer.update(this);
   }

   @Override
   protected void onDisable() {
      ClusterEspRenderer.clear();
   }
}
