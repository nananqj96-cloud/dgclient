package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;

/**
 * Heat-vision style tinting on nearby living entities: colour shifts from red (low health) to
 * green (full health), with an optional translucent fill.
 */
public class HeatColorModule extends Module {
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Max distance to tint", 24.0, 4.0, 64.0, 1.0)
   );
   public final BooleanSetting playersOnly = this.addSetting(
      new BooleanSetting("Players Only", "Only tint other players", true)
   );
   public final BooleanSetting filled = this.addSetting(
      new BooleanSetting("Filled", "Also draw a translucent fill", false)
   );

   public HeatColorModule() {
      super("Heat Color", "Heat-vision tint on nearby entities", Category.VISUALS);
   }

   public float rangeValue() {
      return this.range.getFloat();
   }
}
