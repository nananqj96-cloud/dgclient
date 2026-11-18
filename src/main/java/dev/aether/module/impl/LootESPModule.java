package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;

/**
 * Highlights blocks that typically contain loot (chests, barrels, shulker boxes, etc.) through
 * walls with a coloured box, so you can spot valuable containers at a glance.
 */
public class LootESPModule extends Module {
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Maximum block distance", 60.0, 10.0, 200.0, 5.0)
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Loot highlight colour", -256));
   public final BooleanSetting onlyChests = this.addSetting(
      new BooleanSetting("Chests Only", "Only highlight chests and barrels", false)
   );

   public LootESPModule() {
      super("Loot ESP", "Highlight loot-bearing blocks through walls", Category.VISUALS);
   }

   public float rangeValue() {
      return this.range.getFloat();
   }

   public int colorRgb() {
      return this.color.get() & 16777215;
   }
}
