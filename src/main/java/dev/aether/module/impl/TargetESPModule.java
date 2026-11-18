package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;

/**
 * Draws a highlight box and tracer line to your current target (the entity you are looking at),
 * making it easy to keep track of them in busy fights.
 */
public class TargetESPModule extends Module {
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Maximum target distance", 100.0, 20.0, 300.0, 10.0)
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Target highlight colour", -65536));

   public TargetESPModule() {
      super("Target ESP", "Highlight the entity you are looking at", Category.VISUALS);
   }

   public float rangeValue() {
      return this.range.getFloat();
   }

   public int colorRgb() {
      return this.color.get() & 16777215;
   }
}
