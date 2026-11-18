package dev.aether.render;

import dev.aether.module.impl.ItemTrailModule;
import dev.aether.util.Colors;
import java.util.Deque;

/**
 * Renders the {@link ItemTrailModule} as a fading 2D overlay trail behind the held item. Draws in
 * the overlay's UI space, using the module's recorded sample points.
 */
public final class ItemTrailRenderer {
   private ItemTrailRenderer() {
   }

   public static void render(dev.aether.render.nanovg.NVGRenderer vg, ItemTrailModule module) {
      if (!module.isEnabled()) {
         return;
      }
      Deque<ItemTrailModule.Point> trail = module.trail();
      if (trail.size() < 2) {
         return;
      }
      int rgb = module.currentRgb();
      float fade = module.fadeAmount();
      float thickness = module.thickness();
      int style = module.styleId();
      long now = System.nanoTime();
      // Oldest sample index (the trail is ordered oldest -> newest).
      int n = trail.size();

      switch (style) {
         case ItemTrailModule.STYLE_GLOW -> renderGlow(vg, trail, rgb, fade, thickness, now);
         case ItemTrailModule.STYLE_RIBBON -> renderRibbon(vg, trail, rgb, fade, thickness, now);
         default -> renderStreak(vg, trail, rgb, fade, thickness, now);
      }
   }

   private static float alphaAt(float ageSeconds, float fade) {
      float t = 1.0F - Math.min(1.0F, ageSeconds * fade);
      return t * t;
   }

   private static void renderStreak(dev.aether.render.nanovg.NVGRenderer vg,
                                    Deque<ItemTrailModule.Point> trail, int rgb, float fade,
                                    float thickness, long now) {
      // Draw connecting segments, fading from head (newest) to tail (oldest).
      ItemTrailModule.Point[] pts = trail.toArray(new ItemTrailModule.Point[0]);
      for (int i = 1; i < pts.length; i++) {
         float a = alphaAt(pts[i].ageSeconds(now), fade);
         if (a <= 0.01F) {
            continue;
         }
         // Newer segments are thicker.
         float w = thickness * (0.4F + 0.6F * ((float)(pts.length - i) / (float)(pts.length - 1)));
         int col = Colors.withAlpha(rgb, a * 0.7F);
         vg.line(pts[i - 1].x, pts[i - 1].y, pts[i].x, pts[i].y, w, col);
      }
      // Bright head dot at the newest sample.
      ItemTrailModule.Point head = pts[pts.length - 1];
      vg.circleGlow(head.x, head.y, thickness * 0.9F, thickness * 1.6F,
         Colors.withAlpha(rgb, 0.55F));
      vg.circle(head.x, head.y, thickness * 0.55F, Colors.withAlpha(rgb, 0.85F));
   }

   private static void renderGlow(dev.aether.render.nanovg.NVGRenderer vg,
                                  Deque<ItemTrailModule.Point> trail, int rgb, float fade,
                                  float thickness, long now) {
      ItemTrailModule.Point[] pts = trail.toArray(new ItemTrailModule.Point[0]);
      for (int i = 0; i < pts.length; i++) {
         float a = alphaAt(pts[i].ageSeconds(now), fade);
         if (a <= 0.01F) {
            continue;
         }
         // Scale and alpha grow toward the head.
         float t = (float)i / (float)(pts.length - 1);
         float r = thickness * (0.3F + 0.7F * t);
         int glow = Colors.withAlpha(rgb, a * 0.5F);
         int core = Colors.withAlpha(rgb, a * 0.8F);
         vg.circleGlow(pts[i].x, pts[i].y, r * 1.8F, r * 3.0F, glow);
         vg.circle(pts[i].x, pts[i].y, r * 0.7F, core);
      }
   }

   private static void renderRibbon(dev.aether.render.nanovg.NVGRenderer vg,
                                    Deque<ItemTrailModule.Point> trail, int rgb, float fade,
                                    float thickness, long now) {
      ItemTrailModule.Point[] pts = trail.toArray(new ItemTrailModule.Point[0]);
      float half = thickness * 0.5F;
      // Build left/right edges of the ribbon, thickest at the head.
      for (int i = 1; i < pts.length; i++) {
         float a = alphaAt(pts[i].ageSeconds(now), fade);
         if (a <= 0.01F) {
            continue;
         }
         float t = (float)i / (float)(pts.length - 1);
         float w = half * (0.35F + 0.65F * t);
         // Perpendicular offset from the segment direction.
         float dx = pts[i].x - pts[i - 1].x;
         float dy = pts[i].y - pts[i - 1].y;
         float len = (float)Math.sqrt(dx * dx + dy * dy);
         if (len < 1.0E-4F) {
            continue;
         }
         float nx = -dy / len;
         float ny = dx / len;
         int col = Colors.withAlpha(rgb, a * 0.55F);
         vg.line(pts[i - 1].x + nx * w, pts[i - 1].y + ny * w,
            pts[i].x + nx * w, pts[i].y + ny * w, thickness * 0.5F, col);
         vg.line(pts[i - 1].x - nx * w, pts[i - 1].y - ny * w,
            pts[i].x - nx * w, pts[i].y - ny * w, thickness * 0.5F, col);
      }
      // Bright head.
      ItemTrailModule.Point head = pts[pts.length - 1];
      vg.circleGlow(head.x, head.y, thickness * 0.8F, thickness * 1.5F,
         Colors.withAlpha(rgb, 0.6F));
   }
}
