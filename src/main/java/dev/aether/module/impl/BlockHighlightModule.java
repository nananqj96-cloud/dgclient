package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;

/**
 * Highlights the block you are looking at with a growing animated box and outline, in your chosen
 * colour (or the client theme colour).
 */
public class BlockHighlightModule extends Module {
   public final BooleanSetting clientColor = this.addSetting(
      new BooleanSetting("Client Color", "Use the client theme colour instead of custom colours", true)
   );
   public final ColorSetting fillColor = this.addSetting(
      new ColorSetting("Fill", "Fill colour (when Client Color is off)", -42464)
   );
   public final ColorSetting outlineColor = this.addSetting(
      new ColorSetting("Outline", "Outline colour (when Client Color is off)", -922746881)
   );
   public final SliderSetting lineWidth = this.addSetting(
      new SliderSetting("Line Width", "Outline line width", 2.0, 1.0, 5.0, 0.5, "px")
   );

   public BlockHighlightModule() {
      super("Block Highlight", "Highlight the block you are looking at", Category.VISUALS);
   }

   public int fillRgb() {
      return this.clientColor.get() ? themeAccent() : (this.fillColor.get() & 16777215);
   }

   public int outlineRgb() {
      return this.clientColor.get() ? themeAccent() : (this.outlineColor.get() & 16777215);
   }

   private static int themeAccent() {
      dev.aether.theme.Theme theme = dev.aether.AetherClient.themes() != null
         ? dev.aether.AetherClient.themes().current() : null;
      return theme != null ? theme.accent() : 0xFF38BDF8;
   }

   public float lineWidthValue() {
      return this.lineWidth.getFloat();
   }
}
