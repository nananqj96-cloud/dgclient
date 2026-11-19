package dev.aether.accessory;

import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.util.Colors;

/**
 * Draws the stylised Aether avatar inside the Accessories popup and layers the equipped
 * accessories on top of it: wings behind, cape behind the torso, crown on the head and drifting
 * feathers around the figure. Every accessory is drawn in its own Aether-themed palette.
 */
public final class AccessoryAvatarRenderer {
   private static final int BODY_TOP = 0xFF33557F;
   private static final int BODY_BOTTOM = 0xFF5A86C9;
   private static final int HEAD = 0xFFD8B48C;
   private static final int HEAD_SHADE = 0xFFB98F68;

   private AccessoryAvatarRenderer() {
   }

   public static void render(NVGRenderer vg, float cx, float cy, float scale, AccessoriesModule module, long timeNanos) {
      float t = (float)(timeNanos / 1.0E9);
      Accessory wings = module.equippedAccessory(AccessoryType.WINGS);
      Accessory cape = module.equippedAccessory(AccessoryType.CAPE);
      Accessory crown = module.equippedAccessory(AccessoryType.CROWN);
      Accessory feathers = module.equippedAccessory(AccessoryType.FEATHERS);

      // Geometry (scaled units, cy = vertical middle of the figure).
      float headR = 26.0F * scale;
      float headCy = cy - 56.0F * scale;
      float shoulderY = cy - 22.0F * scale;
      float hipY = cy + 18.0F * scale;
      float feetY = cy + 72.0F * scale;
      float wingFlap = 1.0F + 0.06F * Math.sin(t * 2.0F);

      if (wings != null) {
         drawWings(vg, cx, shoulderY, scale, wings, wingFlap);
      }
      if (cape != null) {
         drawCape(vg, cx, shoulderY, hipY, feetY, scale, cape, t);
      }

      // Legs
      vg.rectGradient(cx - 16.0F * scale, hipY, 12.0F * scale, feetY - hipY, 4.0F * scale, BODY_TOP, BODY_BOTTOM, true);
      vg.rectGradient(cx + 4.0F * scale, hipY, 12.0F * scale, feetY - hipY, 4.0F * scale, BODY_TOP, BODY_BOTTOM, true);

      // Torso
      vg.rectGradient(cx - 22.0F * scale, shoulderY, 44.0F * scale, hipY - shoulderY, 8.0F * scale, BODY_TOP, BODY_BOTTOM, true);
      // Aether mark on the chest
      vg.aetherMark(cx, (shoulderY + hipY) * 0.5F + 2.0F * scale, 7.0F * scale, BODY_BOTTOM, 0xFFFFFFFF);

      // Arms
      vg.rectGradient(cx - 34.0F * scale, shoulderY + 2.0F * scale, 10.0F * scale, 34.0F * scale, 4.0F * scale, BODY_TOP, BODY_BOTTOM, true);
      vg.rectGradient(cx + 24.0F * scale, shoulderY + 2.0F * scale, 10.0F * scale, 34.0F * scale, 4.0F * scale, BODY_TOP, BODY_BOTTOM, true);

      // Head
      vg.circleGlow(cx, headCy, headR + 4.0F * scale, 8.0F * scale, Colors.withAlpha(BODY_BOTTOM, 0.25F));
      vg.circle(cx, headCy, headR, HEAD);
      vg.circle(cx, headCy + headR * 0.45F, headR * 0.95F, HEAD_SHADE);

      if (crown != null) {
         drawCrown(vg, cx, headCy, headR, scale, crown, t);
      }
      if (feathers != null) {
         drawFeathers(vg, cx, cy, scale, feathers, t);
      }
   }

   private static void drawWings(NVGRenderer vg, float cx, float shoulderY, float scale, Accessory wings, float flap) {
      for (int side = -1; side <= 1; side += 2) {
         float sx = cx + side * 20.0F * scale;
         vg.circleGlow(sx, shoulderY, 26.0F * scale, 14.0F * scale, Colors.withAlpha(wings.secondary(), 0.4F));
         // Four feather triangles fanning out from the shoulder.
         for (int i = 0; i < 4; i++) {
            float spread = (float)(i + 1) * 0.55F;
            float len = (60.0F + i * 14.0F) * scale * flap;
            float dx = side * spread * scale;
            float dy = -spread * 0.5F * scale;
            int col = Colors.lerp(wings.primary(), wings.secondary(), i / 4.0F);
            vg.triangle(
               sx, shoulderY,
               sx + side * len * 0.55F, shoulderY - len * 0.35F + dy,
               sx + side * len * 0.95F, shoulderY + len * 0.15F + dy,
               Colors.withAlpha(col, 0.92F)
            );
         }
      }
   }

   private static void drawCape(NVGRenderer vg, float cx, float shoulderY, float hipY, float feetY, float scale, Accessory cape, float t) {
      float sway = Math.sin(t * 1.2F) * 3.0F * scale;
      float topW = 52.0F * scale;
      float botW = 26.0F * scale;
      float h = feetY - shoulderY;
      vg.glow(cx - topW, shoulderY - 2.0F * scale, topW * 2.0F, h + 8.0F * scale, 8.0F * scale, 10.0F * scale, Colors.withAlpha(cape.secondary(), 0.28F));
      vg.rectVaryingGradient(
         cx - topW + sway, shoulderY,
         topW * 2.0F, h,
         topW, topW, botW, botW,
         cape.primary(), cape.secondary()
      );
      vg.rectVaryingGradient(
         cx - topW * 0.6F + sway, shoulderY + 6.0F * scale,
         topW * 1.2F, h - 6.0F * scale,
         topW * 0.6F, topW * 0.6F, botW * 0.6F, botW * 0.6F,
         Colors.withAlpha(cape.secondary(), 0.35F), Colors.withAlpha(cape.primary(), 0.35F)
      );
   }

   private static void drawCrown(NVGRenderer vg, float cx, float headCy, float headR, float scale, Accessory crown, float t) {
      float baseY = headCy - headR + 2.0F * scale;
      float baseW = headR * 1.7F;
      float pulse = 1.0F + 0.05F * Math.sin(t * 3.0F);
      vg.glow(cx - baseW, baseY - 20.0F * scale, baseW * 2.0F, 24.0F * scale, 6.0F * scale, 8.0F * scale, Colors.withAlpha(crown.secondary(), 0.4F));
      // Crown band
      vg.rectGradient(cx - baseW, baseY, baseW * 2.0F, 9.0F * scale, 3.0F * scale, crown.primary(), crown.secondary(), true);
      // Three points
      float[] heights = {16.0F, 22.0F, 16.0F};
      for (int i = 0; i < 3; i++) {
         float px = cx - baseW + baseW * (i + 0.5F) / 3.0F;
         float py = baseY - heights[i] * scale * pulse;
         vg.triangle(px - 6.0F * scale, baseY, px + 6.0F * scale, baseY, px, py, Colors.lerp(crown.primary(), crown.secondary(), 0.4F));
      }
      // Central gem
      vg.circleGlow(cx, baseY - 6.0F * scale, 7.0F * scale * pulse, 4.0F * scale, Colors.withAlpha(crown.secondary(), 0.7F));
      vg.circle(cx, baseY - 6.0F * scale, 4.5F * scale * pulse, crown.secondary());
      vg.aetherMark(cx, baseY - 6.0F * scale, 2.6F * scale * pulse, crown.primary(), 0xFFFFFFFF);
   }

   private static void drawFeathers(NVGRenderer vg, float cx, float cy, float scale, Accessory feathers, float t) {
      float[][] offsets = {
         {-70.0F, -90.0F, 0.9F},
         {80.0F, -70.0F, 1.1F},
         {-95.0F, -30.0F, 0.8F},
         {95.0F, -20.0F, 1.0F},
         {-40.0F, -115.0F, 1.2F},
         {60.0F, -110.0F, 0.95F}
      };
      for (float[] o : offsets) {
         float dx = o[0] * scale + Math.sin(t * 0.8F + o[0]) * 5.0F * scale;
         float dy = o[1] * scale + Math.cos(t * 0.7F + o[1]) * 4.0F * scale;
         float s = o[2] * scale;
         vg.circleGlow(cx + dx, cy + dy, 8.0F * s, 5.0F * s, Colors.withAlpha(feathers.secondary(), 0.45F));
         vg.triangle(cx + dx, cy + dy - 12.0F * s, cx + dx - 4.0F * s, cy + dy + 6.0F * s, cx + dx + 4.0F * s, cy + dy + 6.0F * s, Colors.lerp(feathers.primary(), feathers.secondary(), 0.5F));
      }
   }
}
