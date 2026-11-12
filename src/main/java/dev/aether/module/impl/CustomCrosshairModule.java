package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.Colors;

public class CustomCrosshairModule extends Module {
   private static final int AETHER_CYAN = 0xFF38BDF8;
   public final ModeSetting style = this.addSetting(
      new ModeSetting(
         "Style",
         "Crosshair shape",
         "Cross",
         "Dot",
         "Cross",
         "Circle",
         "T-Shape",
         "Brackets",
         "Chevron",
         "Aether"
      )
   );
   public final SliderSetting size = this.addSetting(
      new SliderSetting("Size", "Overall crosshair size", 7.0, 2.0, 24.0, 1.0, "px")
   );
   public final SliderSetting thickness = this.addSetting(
      new SliderSetting("Thickness", "Line / dot thickness", 2.0, 1.0, 6.0, 0.5, "px")
   );
   public final SliderSetting gap = this.addSetting(
      new SliderSetting("Gap", "Center gap for line styles", 3.0, 0.0, 12.0, 1.0, "px")
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Crosshair color", AETHER_CYAN));
   public final BooleanSetting rainbow = this.addSetting(
      new BooleanSetting("Rainbow", "Cycle through the rainbow", false)
   );
   public final BooleanSetting centerDot = this.addSetting(
      new BooleanSetting("Center Dot", "Add a filled dot at the very center", false)
   );
   public final BooleanSetting outline = this.addSetting(
      new BooleanSetting("Outline", "Dark border for contrast on any background", true)
   );
   public final BooleanSetting glow = this.addSetting(
      new BooleanSetting("Glow", "Soft glow behind the crosshair", true)
   );
   public final BooleanSetting hideVanilla = this.addSetting(
      new BooleanSetting("Hide Vanilla", "Hide Minecraft's default crosshair", true)
   );

   public CustomCrosshairModule() {
      super("CustomCrosshair", "Draws a custom crosshair", Category.MISC);
   }

   public boolean shouldHideVanilla() {
      return this.isEnabled() && this.hideVanilla.get();
   }

   private int resolveColor() {
      if (this.rainbow.get()) {
         float hue = (float)(System.nanoTime() % 3000000000L) / 3.0E9F;
         return 0xFF000000 | Colors.hsvToRgb(hue, 0.85F, 1.0F) & 0xFFFFFF;
      }
      return this.color.get();
   }

   public void render(NVGRenderer vg, float cx, float cy) {
      int col = this.resolveColor();
      int line = this.outline.get() ? 0xB0000000 : 0;
      float t = this.thickness.getFloat();
      float s = this.size.getFloat();
      float g = this.gap.getFloat();
      if (this.glow.get()) {
         vg.circleGlow(cx, cy, s + 2.0F, 6.0F, Colors.withAlpha(col, 0.42F));
      }

      switch (this.style.get()) {
         case "Dot" -> this.dot(vg, cx, cy, Math.max(1.5F, s * 0.35F), col, line);
         case "T-Shape" -> this.cross(vg, cx, cy, s, t, g, col, line, false, true);
         case "Circle" -> this.ring(vg, cx, cy, s, t, col, line);
         case "Brackets" -> this.brackets(vg, cx, cy, s, t, g, col, line);
         case "Chevron" -> {
            if (line != 0) {
               vg.chevron(cx, cy + s * 0.15F, s + 2.0F, t + 2.0F, line, true);
            }
            vg.chevron(cx, cy + s * 0.15F, s, t, col, true);
         }
         case "Aether", "A", "V" -> vg.aetherMark(cx, cy, Math.max(5.0F, s * 1.15F), col, Colors.lighten(col, 0.25F));
         default -> this.cross(vg, cx, cy, s, t, g, col, line, true, true);
      }

      if (this.centerDot.get() && !this.style.is("Dot") && !this.style.is("Aether")) {
         this.dot(vg, cx, cy, Math.max(1.2F, t * 0.8F), col, line);
      }
   }

   private void dot(NVGRenderer vg, float cx, float cy, float r, int col, int line) {
      if (line != 0) {
         vg.circle(cx, cy, r + 1.0F, line);
      }
      vg.circle(cx, cy, r, col);
   }

   private void ring(NVGRenderer vg, float cx, float cy, float r, float t, int col, int line) {
      if (line != 0) {
         vg.circleOutline(cx, cy, r, t + 2.0F, line);
      }
      vg.circleOutline(cx, cy, r, t, col);
   }

   private void cross(NVGRenderer vg, float cx, float cy, float len, float t, float g, int col, int line, boolean top, boolean bottom) {
      if (line != 0) {
         this.segments(vg, cx, cy, len, t + 2.0F, g, line, top, bottom);
      }
      this.segments(vg, cx, cy, len, t, g, col, top, bottom);
   }

   private void segments(NVGRenderer vg, float cx, float cy, float len, float t, float g, int argb, boolean top, boolean bottom) {
      vg.line(cx + g, cy, cx + g + len, cy, t, argb);
      vg.line(cx - g, cy, cx - g - len, cy, t, argb);
      if (top) {
         vg.line(cx, cy - g, cx, cy - g - len, t, argb);
      }
      if (bottom) {
         vg.line(cx, cy + g, cx, cy + g + len, t, argb);
      }
   }

   private void brackets(NVGRenderer vg, float cx, float cy, float len, float t, float g, int col, int line) {
      float arm = Math.max(2.0F, len * 0.5F);
      float d = len + g * 0.4F;
      if (line != 0) {
         this.drawBrackets(vg, cx, cy, d, arm, t + 2.0F, line);
      }
      this.drawBrackets(vg, cx, cy, d, arm, t, col);
   }

   private void drawBrackets(NVGRenderer vg, float cx, float cy, float d, float arm, float t, int argb) {
      vg.line(cx - d, cy - d, cx - d + arm, cy - d, t, argb);
      vg.line(cx - d, cy - d, cx - d, cy - d + arm, t, argb);
      vg.line(cx + d, cy - d, cx + d - arm, cy - d, t, argb);
      vg.line(cx + d, cy - d, cx + d, cy - d + arm, t, argb);
      vg.line(cx - d, cy + d, cx - d + arm, cy + d, t, argb);
      vg.line(cx - d, cy + d, cx - d, cy + d - arm, t, argb);
      vg.line(cx + d, cy + d, cx + d - arm, cy + d, t, argb);
      vg.line(cx + d, cy + d, cx + d, cy + d - arm, t, argb);
   }
}
