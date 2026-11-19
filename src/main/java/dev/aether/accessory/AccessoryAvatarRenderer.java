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

   /**
    * Draws the equipped accessories as a 2D overlay on top of the real Minecraft player rendered by
    * {@code InventoryScreen.renderEntityInInventoryFollowsMouse}. The entity is drawn inside the
    * {@code (x1, y1) - (x2, y2)} box at a given pixel scale; the accessory overlays are positioned
    * relative to that box and rotate with the entity's yaw/pitch so they stay aligned with the model.
    * When a {@code preview} accessory is supplied it is drawn on top of (or replacing) the equipped
    * accessory of the same type.
    */
   public static void renderEquippedOnPlayer(
      NVGRenderer vg,
      float x1, float y1, float x2, float y2, float scale,
      AccessoriesModule module, Accessory preview, float yawDeg, float pitchDeg
   ) {
      float boxW = x2 - x1;
      float boxH = y2 - y1;
      float unit = Math.min(boxW, boxH) / 2.0F;
      float cx = (x1 + x2) * 0.5F;
      float headCy = (y1 + y2) * 0.5F - unit * 0.55F;
      float shoulderY = (y1 + y2) * 0.5F - unit * 0.12F;
      float feetY = y2 - 8.0F;
      float headR = unit * 0.20F;
      long t = System.nanoTime();
      float tt = (float)(t / 1.0E9);
      float yaw = (float)(yawDeg * (Math.PI / 180.0));
      float cosY = (float)Math.cos(yaw);
      float sinY = (float)Math.sin(yaw);

      Accessory crown = pick(module, preview, AccessoryType.CROWN);
      Accessory halo = pick(module, preview, AccessoryType.HALOS);
      Accessory horns = pick(module, preview, AccessoryType.HORNS);
      Accessory mask = pick(module, preview, AccessoryType.MASKS);
      Accessory wings = pick(module, preview, AccessoryType.WINGS);
      Accessory feathers = pick(module, preview, AccessoryType.FEATHERS);
      Accessory cape = pick(module, preview, AccessoryType.CAPE);
      Accessory pack = pick(module, preview, AccessoryType.BACKPACKS);

      if (wings != null) {
         drawPlayerWings(vg, cx, shoulderY, unit, wings, tt, cosY, sinY);
      }
      if (cape != null) {
         drawPlayerCape(vg, cx, shoulderY, feetY, unit, cape, tt);
      }
      if (pack != null) {
         drawPlayerPack(vg, cx, shoulderY, feetY, unit, pack, tt);
      }
      if (feathers != null) {
         drawPlayerFeathers(vg, cx, (shoulderY + feetY) * 0.5F, unit, feathers, tt);
      }
      if (crown != null) {
         drawPlayerCrown(vg, cx, headCy, headR, unit, crown, tt);
      }
      if (halo != null) {
         drawPlayerHalo(vg, cx, headCy, headR, unit, halo, tt);
      }
      if (horns != null) {
         drawPlayerHorns(vg, cx, headCy, headR, unit, horns, tt);
      }
      if (mask != null) {
         drawPlayerMask(vg, cx, headCy, headR, unit, mask, tt);
      }
   }

   private static Accessory pick(AccessoriesModule module, Accessory preview, AccessoryType type) {
      if (preview != null && preview.type() == type) {
         return preview;
      }
      return module.equippedAccessory(type);
   }

   private static void drawPlayerWings(NVGRenderer vg, float cx, float shoulderY, float unit, Accessory wings, float tt, float cosY, float sinY) {
      float flap = (float)(1.0F + 0.10F * Math.sin(tt * 3.0F));
      for (int side = -1; side <= 1; side += 2) {
         float sx = cx + side * unit * 0.18F;
         vg.circleGlow(sx, shoulderY - unit * 0.02F, unit * 0.40F, unit * 0.18F, Colors.withAlpha(wings.secondary(), 0.4F));
         for (int i = 0; i < 4; i++) {
            float spread = (float)(i + 1) * 0.42F;
            float len = (unit * 0.55F + i * unit * 0.16F) * flap;
            float dx = side * spread * unit * 0.5F;
            float dy = -spread * unit * 0.20F;
            int col = Colors.lerp(wings.primary(), wings.secondary(), i / 4.0F);
            vg.triangle(
               sx, shoulderY - unit * 0.02F,
               sx + dx * 0.55F, shoulderY - unit * 0.02F - len * 0.35F + dy,
               sx + dx, shoulderY - unit * 0.02F + len * 0.05F + dy,
               Colors.withAlpha(col, 0.92F)
            );
            vg.aetherMark(sx + dx * 0.55F, shoulderY - unit * 0.05F + dy, unit * 0.06F, wings.secondary(), 0xFFFFFFFF);
         }
      }
   }

   private static void drawPlayerCape(NVGRenderer vg, float cx, float shoulderY, float feetY, float unit, Accessory cape, float tt) {
      float sway = (float)(Math.sin(tt * 1.2F) * unit * 0.03F);
      float topW = unit * 0.52F;
      float botW = unit * 0.26F;
      float h = feetY - shoulderY;
      vg.glow(cx - topW + sway, shoulderY - unit * 0.04F, topW * 2.0F, h + unit * 0.08F, unit * 0.10F, unit * 0.10F, Colors.withAlpha(cape.secondary(), 0.30F));
      vg.rectVaryingGradient(
         cx - topW + sway, shoulderY,
         topW * 2.0F, h,
         topW, topW, botW, botW,
         cape.primary(), cape.secondary()
      );
      vg.aetherMark(cx + sway, shoulderY + h * 0.30F, unit * 0.09F, cape.secondary(), 0xFFFFFFFF);
   }

   private static void drawPlayerPack(NVGRenderer vg, float cx, float shoulderY, float feetY, float unit, Accessory pack, float tt) {
      float topY = shoulderY + unit * 0.10F;
      float botY = feetY - unit * 0.30F;
      float halfW = unit * 0.22F;
      float bob = (float)(Math.sin(tt * 2.0F) * unit * 0.02F);
      vg.glow(cx - halfW, topY + bob, halfW * 2.0F, (botY - topY) + unit * 0.08F, unit * 0.08F, unit * 0.08F, Colors.withAlpha(pack.secondary(), 0.28F));
      vg.rectGradient(cx - halfW, topY + bob, halfW * 2.0F, botY - topY, unit * 0.10F, pack.primary(), Colors.darken(pack.secondary(), 0.3F), true);
      vg.rectOutline(cx - halfW, topY + bob, halfW * 2.0F, botY - topY, unit * 0.10F, 1.0F, pack.secondary());
      vg.rect(cx - halfW * 0.5F - unit * 0.03F, topY - unit * 0.04F, unit * 0.06F, unit * 0.12F, unit * 0.03F, Colors.withAlpha(pack.secondary(), 0.8F));
      vg.rect(cx + halfW * 0.5F - unit * 0.03F, topY - unit * 0.04F, unit * 0.06F, unit * 0.12F, unit * 0.03F, Colors.withAlpha(pack.secondary(), 0.8F));
      vg.aetherMark(cx, (topY + botY) * 0.5F + bob, unit * 0.10F, pack.secondary(), 0xFFFFFFFF);
   }

   private static void drawPlayerFeathers(NVGRenderer vg, float cx, float midY, float unit, Accessory feathers, float tt) {
      for (int i = 0; i < 7; i++) {
         float a = tt * 0.6F + i * ((float)Math.PI * 2 / 7.0F);
         float r = (float)(unit * (0.60F + 0.20F * Math.sin(tt * 1.2F + i)));
         float dx = (float)(Math.cos(a) * r * unit * 0.6F);
         float dy = (float)(Math.sin(a) * r * unit * 0.8F) - unit * 0.10F;
         float s = unit * 0.07F;
         vg.circleGlow(cx + dx, midY + dy, s * 2.2F, s * 1.4F, Colors.withAlpha(feathers.secondary(), 0.45F));
         vg.triangle(cx + dx, midY + dy - s * 2.0F, cx + dx - s, midY + dy + s, cx + dx + s, midY + dy + s, Colors.lerp(feathers.primary(), feathers.secondary(), 0.5F));
      }
   }

   private static void drawPlayerCrown(NVGRenderer vg, float cx, float headCy, float headR, float unit, Accessory crown, float tt) {
      float baseY = headCy - headR + unit * 0.02F;
      float baseW = headR * 1.6F;
      float pulse = (float)(1.0F + 0.05F * Math.sin(tt * 3.0F));
      vg.glow(cx - baseW, baseY - unit * 0.24F, baseW * 2.0F, unit * 0.28F, unit * 0.06F, unit * 0.08F, Colors.withAlpha(crown.secondary(), 0.45F));
      vg.rectGradient(cx - baseW, baseY, baseW * 2.0F, unit * 0.09F, unit * 0.03F, crown.primary(), crown.secondary(), true);
      float[] heights = {unit * 0.18F, unit * 0.26F, unit * 0.18F};
      for (int i = 0; i < 3; i++) {
         float px = cx - baseW + baseW * (i + 0.5F) / 3.0F;
         float py = baseY - heights[i] * pulse;
         vg.triangle(px - unit * 0.06F, baseY, px + unit * 0.06F, baseY, px, py, Colors.lerp(crown.primary(), crown.secondary(), 0.4F));
      }
      vg.circleGlow(cx, baseY - unit * 0.06F, unit * 0.07F * pulse, unit * 0.04F, Colors.withAlpha(crown.secondary(), 0.7F));
      vg.circle(cx, baseY - unit * 0.06F, unit * 0.045F * pulse, crown.secondary());
      vg.aetherMark(cx, baseY - unit * 0.06F, unit * 0.026F * pulse, crown.primary(), 0xFFFFFFFF);
   }

   private static void drawPlayerHalo(NVGRenderer vg, float cx, float headCy, float headR, float unit, Accessory halo, float tt) {
      float bob = (float)(Math.sin(tt * 1.6F) * unit * 0.03F);
      float hy = headCy - headR - unit * 0.16F + bob;
      float r = headR * 1.7F;
      vg.glow(cx - r, hy - r, r * 2.0F, r * 2.0F, unit * 0.05F, unit * 0.08F, Colors.withAlpha(halo.secondary(), 0.4F));
      vg.circleGlow(cx, hy, r + unit * 0.03F, unit * 0.04F, Colors.withAlpha(halo.secondary(), 0.35F));
      vg.circleOutline(cx, hy, r, unit * 0.035F, halo.secondary());
      vg.aetherMark(cx, hy, unit * 0.07F, halo.primary(), 0xFFFFFFFF);
   }

   private static void drawPlayerHorns(NVGRenderer vg, float cx, float headCy, float headR, float unit, Accessory horns, float tt) {
      float baseY = headCy - unit * 0.04F;
      for (int side = -1; side <= 1; side += 2) {
         float bx = cx + side * headR * 0.70F;
         float tipX = cx + side * headR * 1.50F;
         float tipY = baseY - unit * 0.30F;
         int col = Colors.lerp(horns.primary(), horns.secondary(), 0.4F);
         vg.triangle(bx, baseY, bx + side * headR * 0.10F, baseY - unit * 0.06F, tipX, tipY, col);
         vg.triangle(bx, baseY + unit * 0.02F, bx - side * headR * 0.10F, baseY - unit * 0.04F, tipX - side * headR * 0.10F, tipY + unit * 0.04F, Colors.withAlpha(horns.secondary(), 0.8F));
         vg.aetherMark(bx, baseY + unit * 0.02F, unit * 0.045F, horns.secondary(), 0xFFFFFFFF);
      }
   }

   private static void drawPlayerMask(NVGRenderer vg, float cx, float headCy, float headR, float unit, Accessory mask, float tt) {
      float mY = headCy + unit * 0.02F;
      float halfW = headR * 0.85F;
      float halfH = headR * 0.55F;
      vg.glow(cx - halfW, mY - halfH, halfW * 2.0F, halfH * 2.0F, unit * 0.04F, unit * 0.05F, Colors.withAlpha(mask.secondary(), 0.35F));
      vg.rectGradient(cx - halfW, mY - halfH, halfW * 2.0F, halfH * 2.0F, unit * 0.04F, mask.primary(), Colors.darken(mask.primary(), 0.2F), true);
      vg.rectOutline(cx - halfW, mY - halfH, halfW * 2.0F, halfH * 2.0F, unit * 0.04F, 1.2F, mask.secondary());
      vg.rect(cx - halfW * 0.5F, mY - halfH * 0.35F, halfW, unit * 0.02F, unit * 0.01F, mask.secondary());
      vg.aetherMark(cx, mY, unit * 0.05F, mask.secondary(), 0xFFFFFFFF);
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
      float wingFlap = (float)(1.0F + 0.06F * Math.sin(t * 2.0F));

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
      float sway = (float)(Math.sin(t * 1.2F) * 3.0F * scale);
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
      float pulse = (float)(1.0F + 0.05F * Math.sin(t * 3.0F));
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
         float dx = (float)(o[0] * scale + Math.sin(t * 0.8F + o[0]) * 5.0F * scale);
         float dy = (float)(o[1] * scale + Math.cos(t * 0.7F + o[1]) * 4.0F * scale);
         float s = o[2] * scale;
         vg.circleGlow(cx + dx, cy + dy, 8.0F * s, 5.0F * s, Colors.withAlpha(feathers.secondary(), 0.45F));
         vg.triangle(cx + dx, cy + dy - 12.0F * s, cx + dx - 4.0F * s, cy + dy + 6.0F * s, cx + dx + 4.0F * s, cy + dy + 6.0F * s, Colors.lerp(feathers.primary(), feathers.secondary(), 0.5F));
      }
   }
}
