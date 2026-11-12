package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.impl.RegionMapModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class RegionMapHud extends HudComponent {
   private static final float PAD = 8.0F;
   private static final float CELL = 12.0F;
   private static final float GRID = 108.0F;
   private static final float HEADER_H = 15.0F;
   private static final float GAP_HEADER = 5.0F;
   private static final float GAP_LEGEND = 8.0F;
   private static final float LEGEND_ROW = 12.0F;
   private static final int LEGEND_COLS = 2;
   private final RegionMapModule module;
   private final ThemeManager themes;

   public RegionMapHud(RegionMapModule var1, ThemeManager var2) {
      super("regionMap", 0.008F, 0.05F, var1::isEnabled);
      this.module = var1;
      this.themes = var2;
   }

   private float legendRows() {
      return (float)Math.ceil((double)this.module.regionTypeCount() / 2.0);
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 124.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      float var2 = 144.0F;
      if (this.module.legend.get()) {
         var2 += 8.0F + this.legendRows() * 12.0F;
      }

      return var2;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      LocalPlayer var7 = Minecraft.getInstance().player;
      if (var7 != null) {
         float var8 = 3.0F;
         var1.glow(var2, var3, var4, var5, 4.0F, 7.0F, Colors.withAlpha(-16777216, 0.3F));
         var1.rectGradient(var2, var3, var4, var5, var8, var6.background(), var6.backgroundTo(), true);
         int var9 = this.module.currentRegionId();
         int var10 = this.module.regionTypeAtWorld(var7.getX(), var7.getZ());
         this.drawHeader(var1, var6, var2, var3, var4, var9);
         float var11 = var2 + 8.0F;
         float var12 = var3 + 8.0F + 15.0F + 5.0F;
         this.drawGrid(var1, var6, var7, var11, var12);
         if (this.module.legend.get()) {
            this.drawLegend(var1, var6, var11, var12 + 108.0F + 8.0F, var10);
         }
      }
   }

   private void drawHeader(NVGRenderer var1, Theme var2, float var3, float var4, float var5, int var6) {
      float var7 = var4 + 8.0F + 7.5F;
      var1.text("REGION MAP", var3 + 8.0F, var7, 9.0F, var2.textPrimary());
      String var8 = var6 >= 0 ? "#" + var6 : "N/A";
      float var9 = var1.textWidth(var8, 8.5F);
      float var10 = var9 + 9.0F;
      float var11 = 12.5F;
      float var12 = var3 + var5 - 8.0F - var10;
      float var13 = var7 - var11 / 2.0F;
      boolean var14 = var6 >= 0;
      var1.rect(var12, var13, var10, var11, 6.0F, Colors.withAlpha(var2.accent(), var14 ? 0.18F : 0.1F));
      var1.rectOutline(var12, var13, var10, var11, 6.0F, 1.0F, Colors.withAlpha(var2.accent(), var14 ? 0.45F : 0.2F));
      var1.text(var8, var12 + 4.5F, var7, 8.5F, var14 ? var2.accentBright() : var2.textMuted());
   }

   private void drawGrid(NVGRenderer var1, Theme var2, LocalPlayer var3, float var4, float var5) {
      int var8 = this.module.mapSize();
      int var9 = (int)(Math.clamp(this.module.opacity.getFloat() / 100.0F, 0.0F, 1.0F) * 255.0F);
      var1.rect(var4, var5, 108.0F, 108.0F, 2.0F, Colors.withAlpha(-16054000, 0.94F));
      var1.save();
      var1.scissor(var4, var5, 108.0F, 108.0F);

      for (int var10 = 0; var10 < var8; var10++) {
         for (int var11 = 0; var11 < var8; var11++) {
            int var12 = this.module.regionTypeAt(var10 * var8 + var11);
            if (var12 >= 0) {
               int var13 = Colors.withAlpha(0xFF000000 | this.module.regionTypeRgb(var12), var9);
               float var14 = var4 + (float)var11 * 12.0F;
               float var15 = var5 + (float)var10 * 12.0F;
               var1.rect(var14 + 1.0F, var15 + 1.0F, 10.0F, 10.0F, 0.8F, var13);
            }
         }
      }

      if (this.module.gridLines.get()) {
         int var19 = Colors.withAlpha(var2.accent(), 0.14F);

         for (int var20 = 0; var20 <= var8; var20++) {
            float var24 = (float)var20 * 12.0F;
            var1.line(var4 + var24, var5, var4 + var24, var5 + 108.0F, 1.0F, var19);
            var1.line(var4, var5 + var24, var4 + 108.0F, var5 + var24, 1.0F, var19);
         }
      }

      int[] var6;
      boolean var7 = (var6 = this.module.worldToGrid(var3.getX(), var3.getZ()))[0] >= 0 && var6[0] < var8 && var6[1] >= 0 && var6[1] < var8;
      if (var7) {
         float var21 = var4 + (float)var6[0] * 12.0F;
         float var25 = var5 + (float)var6[1] * 12.0F;
         var1.glow(var21, var25, 12.0F, 12.0F, 2.0F, 3.0F, Colors.withAlpha(var2.accent(), 0.35F));
         var1.rectOutline(var21 + 0.5F, var25 + 0.5F, 11.0F, 11.0F, 2.0F, 1.2F, var2.accentBright());
      }

      if (this.module.cellNumbers.get()) {
         for (int var22 = 0; var22 < var8; var22++) {
            for (int var26 = 0; var26 < var8; var26++) {
               int var28 = this.module.regionTypeAt(var22 * var8 + var26);
               if (var28 >= 0) {
                  String var30 = String.valueOf(this.module.regionIdAt(var22 * var8 + var26));
                  float var31 = var30.length() >= 3 ? 5.5F : 6.5F;
                  float var16 = var1.textWidth(var30, var31);
                  float var17 = var4 + (float)var26 * 12.0F + (12.0F - var16) / 2.0F;
                  float var18 = var5 + (float)var22 * 12.0F + 6.0F;
                  var1.text(var30, var17 + 0.5F, var18 + 0.5F, var31, Colors.withAlpha(-16777216, 0.55F));
                  var1.text(var30, var17, var18, var31, -790280);
               }
            }
         }
      }

      if (var7) {
         double[] var23 = this.module.worldToCellPosition(var3.getX(), var3.getZ());
         float var27 = var4 + (float)(((double)var6[0] + var23[0]) * 12.0);
         float var29 = var5 + (float)(((double)var6[1] + var23[1]) * 12.0);
         var1.circleGlow(var27, var29, 2.2F, 3.5F, Colors.withAlpha(var2.accent(), 0.75F));
         var1.save();
         var1.translate(var27, var29);
         var1.rotate((float)Math.toRadians((double)(var3.getYRot() + 180.0F)));
         var1.triangle(0.0F, -4.9F, 3.3F, 2.9F, -3.3F, 2.9F, Colors.withAlpha(-16777216, 0.55F));
         var1.triangle(0.0F, -4.0F, 2.6F, 2.2F, -2.6F, 2.2F, var2.accentBright());
         var1.restore();
      }

      var1.restore();
      var1.rectOutline(var4, var5, 108.0F, 108.0F, 2.0F, 1.0F, Colors.withAlpha(var2.accent(), 0.5F));
   }

   private void drawLegend(NVGRenderer var1, Theme var2, float var3, float var4, int var5) {
      float var6 = 54.0F;

      for (int var7 = 0; var7 < this.module.regionTypeCount(); var7++) {
         int var8 = var7 % 2;
         int var9 = var7 / 2;
         float var10 = var3 + (float)var8 * var6;
         float var11 = var4 + (float)var9 * 12.0F + 6.0F;
         var1.rect(var10, var11 - 2.5F, 5.0F, 5.0F, 1.2F, 0xFF000000 | this.module.regionTypeRgb(var7));
         boolean var12 = var7 == var5;
         var1.text(this.module.regionTypeName(var7), var10 + 8.0F, var11, 7.5F, var12 ? var2.accentBright() : var2.textMuted());
      }
   }
}
