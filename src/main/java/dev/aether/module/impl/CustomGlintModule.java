package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.Colors;
import java.util.Locale;

public class CustomGlintModule extends Module {
   public final ModeSetting style = this.addSetting(
      new ModeSetting(
         "Style",
         "Glint texture. Default recolors the vanilla foil; the rest are custom patterns",
         "Default",
         "Default",
         "Ender",
         "Void",
         "Galaxy",
         "Toxic",
         "Amethyst",
         "Cyber",
         "Solar",
         "Prismatic",
         "Abyss"
      )
   );
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Where the glint color comes from",
         "Solid",
         "Solid",
         "Rainbow",
         "Theme"
      )
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Glint color", -49508));
   public final SliderSetting strength = this.addSetting(
      new SliderSetting("Strength", "Glint intensity", 60.0, 0.0, 100.0, 5.0, "%")
   );
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Rainbow cycle speed", 100.0, 10.0, 300.0, 10.0, "%")
   );

   public CustomGlintModule() {
      super("CustomGlint", "Recolors or restyles the enchantment glint", Category.MISC);
      this.mode.visibleWhen(() -> this.style.is("Default"));
      this.color.visibleWhen(() -> this.style.is("Default") && this.mode.is("Solid"));
      this.speed.visibleWhen(() -> this.style.is("Default") && this.mode.is("Rainbow"));
   }

   public boolean isActive() {
      return this.isEnabled();
   }

   public boolean usesTexture() {
      return !this.style.is("Default");
   }

   public String textureName() {
      return this.style.get().toLowerCase(Locale.ROOT);
   }

   public float strengthUnit() {
      return this.strength.getFloat() / 100.0F;
   }

   public int glintColor() {
      String modeName = this.mode.get();

      int base = switch (modeName) {
         case "Rainbow" -> this.rainbow();
         case "Theme" -> AetherClient.themes().current().accent();
         default -> this.color.get();
      };
      float s = this.strength.getFloat() / 100.0F;
      int r = Math.round((float)Colors.red(base) * s);
      int g = Math.round((float)Colors.green(base) * s);
      int b = Math.round((float)Colors.blue(base) * s);
      return Colors.rgb(r, g, b);
   }

   private int rainbow() {
      double seconds = (double)(System.nanoTime() % 1000000000000L) / 1.0E9;
      double hue = seconds * 36.0 * (double)(this.speed.getFloat() / 100.0F) % 360.0;
      return Colors.hsvToRgb((float)hue, 0.85F, 1.0F);
   }
}
