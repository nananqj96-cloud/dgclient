package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;

/**
 * Replaces the vanilla sky with a beautiful animated aurora sky. Draws flowing, coloured aurora
 * curtains across the sky that drift and pulse over time, with adjustable intensity, speed and
 * palette. Purely a visual/render effect — no gameplay impact.
 */
public class SkyShaderModule extends Module {
   public final BooleanSetting aurora = this.addSetting(
      new BooleanSetting("Aurora", "Draw flowing aurora curtains in the sky", true)
   );
   public final BooleanSetting stars = this.addSetting(
      new BooleanSetting("Stars", "Draw a twinkling starfield", true)
   );
   public final BooleanSetting gradient = this.addSetting(
      new BooleanSetting("Gradient", "Animated gradient sky tint", true)
   );
   public final ModeSetting palette = this.addSetting(
      new ModeSetting(
         "Palette",
         "Aurora colour scheme",
         "Aurora",
         "Aurora",
         "North",
         "Sunset",
         "Toxic",
         "Prismatic"
      )
   );
   public final SliderSetting intensity = this.addSetting(
      new SliderSetting("Intensity", "Aurora brightness", 70.0, 20.0, 100.0, 5.0, "%")
   );
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Aurora drift speed", 100.0, 20.0, 300.0, 10.0, "%")
   );
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Aurora elevation above horizon", 60.0, 20.0, 160.0, 5.0)
   );
   public final ColorSetting accent = this.addSetting(new ColorSetting("Accent", "Custom accent tint", -49508));

   public SkyShaderModule() {
      super("SkyShader", "A beautiful animated aurora sky", Category.VISUALS);
   }

   public float intensityUnit() {
      return this.intensity.getFloat() / 100.0F;
   }

   public float speedUnit() {
      return this.speed.getFloat() / 100.0F;
   }

   public float heightValue() {
      return this.height.getFloat();
   }

   public int paletteRgb(int fallback) {
      return this.palette.is("Prismatic") ? fallback : this.accent.get();
   }

   public String paletteName() {
      return this.palette.get();
   }
}
