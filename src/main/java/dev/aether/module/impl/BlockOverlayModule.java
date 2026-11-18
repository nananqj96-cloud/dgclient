package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;

/**
 * Overlays the block you are looking at with a fill and/or outline in a variety of styles —
 * outline, fill, fill + outline, glow, or shader — with static, gradient or rainbow colours.
 */
public class BlockOverlayModule extends Module {
   public final ModeSetting mode = this.addSetting(
      new ModeSetting("Mode", "Overlay render style", "Fill + Outline", "Outline", "Fill", "Fill + Outline", "Glow")
   );
   public final ModeSetting colorMode = this.addSetting(
      new ModeSetting("Color", "Colour source", "Theme", "Static", "Gradient", "Rainbow", "Theme")
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Primary overlay colour", -42464));
   public final ColorSetting secondaryColor = this.addSetting(new ColorSetting("Secondary", "Gradient secondary colour", -256));
   public final SliderSetting fillAlpha = this.addSetting(
      new SliderSetting("Fill Alpha", "Fill opacity", 0.35, 0.0, 1.0, 0.05)
   );
   public final SliderSetting lineWidth = this.addSetting(
      new SliderSetting("Line Width", "Outline line width", 2.0, 1.0, 5.0, 0.5, "px")
   );
   public final SliderSetting glowStrength = this.addSetting(
      new SliderSetting("Glow Strength", "Glow halo size (Glow mode)", 1.0, 0.5, 3.0, 0.1)
   );

   public BlockOverlayModule() {
      super("Block Overlay", "Overlay the looked-at block with fill/outline/glow", Category.VISUALS);
   }

   public int primaryRgb() {
      if (this.colorMode.get().equals("Theme")) {
         return themeAccent();
      }
      if (this.colorMode.get().equals("Rainbow")) {
         return rainbowColor();
      }
      return this.color.get() & 16777215;
   }

   public int secondaryRgb() {
      return this.secondaryColor.get() & 16777215;
   }

   public float fillAlphaValue() {
      return this.fillAlpha.getFloat();
   }

   public float lineWidthValue() {
      return this.lineWidth.getFloat();
   }

   public float glowStrengthValue() {
      return this.glowStrength.getFloat();
   }

   private static int themeAccent() {
      dev.aether.theme.Theme theme = dev.aether.AetherClient.themes() != null
         ? dev.aether.AetherClient.themes().current() : null;
      return theme != null ? theme.accent() : 0xFF38BDF8;
   }

   private static int rainbowColor() {
      float hue = (float) ((System.nanoTime() / 1.0E9 * 60.0F) % 360.0F);
      return dev.aether.util.Colors.hsvToRgb(hue, 1.0F, 1.0F) & 16777215;
   }
}
