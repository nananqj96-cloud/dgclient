package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.Modules;
import dev.aether.module.impl.ChunkBypassModule;
import dev.aether.render.nanovg.NVGImages;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.suschunk.SusChunkScanner;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;

public class RadarHud extends HudComponent {
   private static final float SIZE = 110.0F;
   private static final int CHUNK_BYPASS_PURPLE = 0xA855F7;
   private static final int CHUNK_BYPASS_PURPLE_BRIGHT = 0xC084FC;
   private final Modules.HudModule module;
   private final Modules.SusChunkFinderModule susFinder;
   private final ChunkBypassModule chunkBypass;
   private final ThemeManager themes;

   public RadarHud(Modules.HudModule var1, Modules.SusChunkFinderModule var2, ChunkBypassModule var5, ThemeManager var3, BooleanSupplier var4) {
      super("radar", 0.006F, 0.45F, var4);
      this.module = var1;
      this.susFinder = var2;
      this.chunkBypass = var5;
      this.themes = var3;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 110.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return 110.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      LocalPlayer var7 = Minecraft.getInstance().player;
      if (var7 != null) {
         float var8 = var4 / 2.0F;
         float var9 = var2 + var8;
         float var10 = var3 + var8;
         float var11 = 0.55F;
         var1.circle(var9, var10, var8, Colors.withAlpha(-15856621, 0.68F));
         var1.circleOutline(var9, var10, var8 - 2.0F, 1.0F, Colors.withAlpha(var6.accent(), 0.32F));
         var1.circleOutline(var9, var10, var8 * 0.5F, 1.0F, Colors.withAlpha(var6.accent(), 0.22F));
         int var12 = Colors.withAlpha(var6.accent(), 0.18F);
         var1.line(var9 - var8, var10, var9 + var8, var10, 1.0F, var12);
         var1.line(var9, var10 - var8, var9, var10 + var8, 1.0F, var12);
         float var13 = var7.getYRot();
         float var14 = (float)Math.toRadians((double)(var13 + 90.0F));
         String[] var15 = new String[]{"N", "E", "S", "W"};
         float[][] var16 = new float[][]{{0.0F, -1.0F}, {1.0F, 0.0F}, {0.0F, 1.0F}, {-1.0F, 0.0F}};

         for (int var17 = 0; var17 < 4; var17++) {
            float var18 = screenX(var16[var17][0], var16[var17][1], var14);
            float var19 = screenY(var16[var17][0], var16[var17][1], var14);
            float var20 = var9 + var18 * (var8 - 9.0F);
            float var21 = var10 + var19 * (var8 - 9.0F);
            boolean var22 = var17 == 0;
            var1.text(var15[var17], var20 - var1.textWidth(var15[var17], 11.0F) / 2.0F, var21, 11.0F, var22 ? var6.accentBright() : var6.textMuted());
         }

         float var30 = this.module.radarRange.getFloat();
         if (this.susFinder.isEnabled() && this.susFinder.showOnRadar.get()) {
            int var31 = this.susFinder.scanner.threshold();

            for (SusChunkScanner.Zone var35 : this.susFinder.scanner.zones()) {
               this.drawZoneBlip(
                  var1,
                  var9,
                  var10,
                  var8,
                  var30,
                  var14,
                  (float)(var35.centroidX() - var7.getX()),
                  (float)(var35.centroidZ() - var7.getZ()),
                  strength(var35.maxScore(), var31),
                  var6.accent(),
                  var6.accentBright()
               );
            }
         }

         if (this.chunkBypass != null && this.chunkBypass.isEnabled() && this.chunkBypass.showOnRadar.get()) {
            for (ChunkBypassModule.Zone var37 : this.chunkBypass.zones()) {
               this.drawZoneBlip(
                  var1,
                  var9,
                  var10,
                  var8,
                  var30,
                  var14,
                  (float)(var37.centroidX() - var7.getX()),
                  (float)(var37.centroidZ() - var7.getZ()),
                  strength(var37.maxScore(), this.chunkBypass.minHits()),
                  CHUNK_BYPASS_PURPLE,
                  CHUNK_BYPASS_PURPLE_BRIGHT
               );
            }
         }

         var1.circleGlow(var9, var10, 3.0F, 4.0F, Colors.withAlpha(var6.accent(), 0.6F));
         var1.circle(var9, var10, 3.0F, var6.accentBright());
         float var32 = Math.max(10.0F, var8 * 0.17F);

         for (AbstractClientPlayer var36 : Minecraft.getInstance().level.players()) {
            float var38;
            float var23;
            float var24;
            if (var36 != var7
               && !(
                  (
                        var24 = (float)Math.sqrt(
                           (double)((var23 = (float)(var36.getX() - var7.getX())) * var23 + (var38 = (float)(var36.getZ() - var7.getZ())) * var38)
                        )
                     )
                     > var30
               )) {
               float var25 = var24 / var30;
               float var26 = var9 + screenX(var23, var38, var14) / Math.max(var24, 0.001F) * var25 * (var8 - var32 / 2.0F - 3.0F);
               float var27 = var10 + screenY(var23, var38, var14) / Math.max(var24, 0.001F) * var25 * (var8 - var32 / 2.0F - 3.0F);
               int var37;
               if (this.module.radarHeads.get() && (var37 = NVGImages.wrapGlTexture(var36.getSkin().body().texturePath(), 64, 64)) > 0) {
                  float var28 = var26 - var32 / 2.0F;
                  float var29 = var27 - var32 / 2.0F;
                  NVGImages.drawSubImage(var1, var37, 64.0F, 64.0F, 8.0F, 8.0F, 16.0F, 16.0F, var28, var29, var32, var32, 1.0F);
                  NVGImages.drawSubImage(var1, var37, 64.0F, 64.0F, 40.0F, 8.0F, 48.0F, 16.0F, var28, var29, var32, var32, 1.0F);
                  var1.rectOutline(var28 - 1.0F, var29 - 1.0F, var32 + 2.0F, var32 + 2.0F, 3.0F, 1.0F, Colors.withAlpha(-1, 0.35F));
               } else {
                  var1.circleGlow(var26, var27, 2.5F, 3.0F, Colors.withAlpha(-1, 0.4F));
                  var1.circle(var26, var27, 2.5F, -1);
               }
            }
         }
      }
   }

   private static float strength(double var0, int var2) {
      return var2 <= 0 ? 1.0F : Math.clamp((float)((var0 - (double)var2) / ((double)var2 * 2.0)), 0.0F, 1.0F);
   }

   private void drawZoneBlip(NVGRenderer var1, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, int accent, int accentBright) {
      float var11 = (float)Math.sqrt((double)(var8 * var8 + var9 * var9));
      if (!(var11 < 0.5F)) {
         boolean var12 = var11 > var6;
         float var13 = Math.min(var11 / var6, 1.0F);
         float var14 = var12 ? 6.0F : 9.0F;
         float var15 = var3 + screenX(var8, var9, var7) / var11 * var13 * (var5 - var14);
         float var16 = var4 + screenY(var8, var9, var7) / var11 * var13 * (var5 - var14);
         double var17 = (double)(System.nanoTime() % 1000000000000L) / 1.0E9;
         float var19 = (float)(0.5 + 0.5 * Math.sin(var17 * Math.PI * 2.0 / 1.8));
         float var20 = (var12 ? 2.4F : 3.0F + 1.5F * var10) * (1.0F + 0.1F * var19);
         float var21 = (0.72F + 0.28F * var10) * (0.88F + 0.12F * var19);
         var1.circleGlow(var15, var16, var20, 3.5F + 2.5F * var19, Colors.withAlpha(accent, (0.3F + 0.25F * var10) * var21));
         var1.circle(var15, var16, var20, Colors.withAlpha(accent, var21));
         var1.circleOutline(var15, var16, var20 + 1.5F, 1.0F, Colors.withAlpha(accentBright, 0.55F * var21));
         if (var12 && var11 <= var6 * 2.5F) {
            String var23 = (int)var11 + "m";
            float var24 = var1.textWidth(var23, 8.5F);
            float var25 = var15 + (var3 - var15) * 0.24F;
            float var26 = var16 + (var4 - var16) * 0.24F;
            var1.rect(var25 - var24 / 2.0F - 3.0F, var26 - 5.5F, var24 + 6.0F, 11.0F, 5.5F, Colors.withAlpha(-15856621, 0.72F));
            var1.text(var23, var25 - var24 / 2.0F, var26, 8.5F, Colors.withAlpha(accentBright, 0.95F));
         }
      }
   }

   private static float screenX(float var0, float var1, float var2) {
      return (float)((double)(-var0) * Math.sin((double)var2) + (double)var1 * Math.cos((double)var2));
   }

   private static float screenY(float var0, float var1, float var2) {
      return (float)(-((double)var0 * Math.cos((double)var2) + (double)var1 * Math.sin((double)var2)));
   }
}
