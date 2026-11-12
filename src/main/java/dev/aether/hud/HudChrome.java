package dev.aether.hud;

import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.util.Colors;

public final class HudChrome {
   private HudChrome() {
   }

   public static void underlay(NVGRenderer vg, Theme theme, float x, float y, float w, float h) {
      if (isRound(w, h)) {
         vg.circleGlow(x + w * 0.5F, y + h * 0.5F, w * 0.5F - 1.0F, 5.0F, Colors.withAlpha(theme.accent(), 0.12F));
      } else {
         vg.glow(x, y + 1.0F, w, h, 6.0F, 6.0F, Colors.withAlpha(0xFF000000, 0.30F));
      }
   }

   public static void overlay(NVGRenderer vg, Theme theme, float x, float y, float w, float h) {
      if (w >= 12.0F && h >= 10.0F) {
         if (isRound(w, h)) {
            float cx = x + w * 0.5F;
            float cy = y + h * 0.5F;
            vg.circleOutline(cx, cy, w * 0.5F - 0.6F, 1.0F, Colors.withAlpha(theme.accent(), 0.36F));
         } else {
            vg.rectOutline(x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, 6.0F, 1.0F, Colors.withAlpha(theme.accent(), 0.28F));
         }
      }
   }

   private static boolean isRound(float w, float h) {
      return w >= 56.0F && Math.abs(w - h) < 1.5F;
   }
}
