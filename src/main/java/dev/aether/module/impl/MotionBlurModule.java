package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.MotionBlurRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;

public class MotionBlurModule extends Module {
   public final SliderSetting strength = this.addSetting(
      new SliderSetting("Strength", "Blur strength", 30.0, 5.0, 100.0, 5.0, "%")
         .withLabel(
            v -> {
               int pct = (int)Math.round(v);
               String tier = pct <= 20
                  ? "Subtle"
                  : (
                     pct <= 45
                        ? "Balanced"
                        : (
                           pct <= 70
                              ? "Smooth"
                              : (pct <= 90 ? "Heavy" : "Cinematic")
                        )
                  );
               return pct + "% · " + tier;
            }
         )
   );
   public final BooleanSetting pinkTrails = this.addSetting(
      new BooleanSetting(
         "Pink Trails", "Tint the trails with your theme accent (Aether look)", true
      )
   );
   public final SliderSetting tint = this.addSetting(
      new SliderSetting(
         "Tint", "How strongly trails take the theme color", 30.0, 0.0, 100.0, 5.0, "%"
      )
   );
   public final BooleanSetting fpsCompensated = this.addSetting(
      new BooleanSetting("FPS Compensated", "Keep the blur consistent across framerates", true)
   );

   public MotionBlurModule() {
      super("MotionBlur", "Cinematic motion blur", Category.VISUALS);
      this.tint.visibleWhen(this.pinkTrails::get);
   }

   @Override
   protected void onDisable() {
      MotionBlurRenderer.reset();
   }

   public double retention() {
      return Math.clamp((double)this.strength.getFloat() / 100.0, 0.05, 0.95);
   }

   public float tintAmount() {
      return this.pinkTrails.get() ? (float)Math.clamp((double)this.tint.getFloat() / 100.0, 0.0, 1.0) : 0.0F;
   }

   public int accentColor() {
      return AetherClient.themes().current().accent();
   }
}
