package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.Modules;
import dev.aether.render.nanovg.NVGImages;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.spotify.SpotifyService;
import dev.aether.spotify.SpotifyState;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public class SpotifyHud extends HudComponent {
   public static final float WIDTH = 252.0F;
   public static final float HEIGHT = 74.0F;
   private static final float ART = 54.0F;
   private static final float TEXT_X = 76.0F;
   private static final float VOL_ROW = 18.0F;
   private static final float VBAR_X = 36.0F;
   private static final float VBAR_TRIM = 46.0F;
   private static final float VOL_CY = 80.0F;
   private final Modules.SpotifyModule module;
   private final SpotifyService service;
   private final ThemeManager themes;
   private static final long DEMO_START = System.nanoTime();

   public SpotifyHud(Modules.SpotifyModule var1, SpotifyService var2, ThemeManager var3) {
      super("spotify", 0.5F, 0.965F, var1::isEnabled);
      this.module = var1;
      this.service = var2;
      this.themes = var3;
   }

   private boolean demo() {
      return this.module.source.is("Demo");
   }

   private SpotifyState state() {
      if (this.demo()) {
         long var1 = (System.nanoTime() - DEMO_START) / 1000000L % 227000L;
         return new SpotifyState(true, "Neon Nights", "Aether Preview", var1, 227000L, true, true, 0, 70, System.nanoTime());
      } else {
         return this.service.state();
      }
   }

   private boolean showVolume() {
      return this.module.volume.get() && this.state().active() && this.state().volume() >= 0;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 252.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return this.showVolume() ? 92.0F : 74.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      SpotifyState var7 = this.state();
      Theme var8 = this.themes.current();
      if (var7.active() || !this.module.hideWhenIdle.get()) {
         var1.glow(var2, var3, var4, var5, 4.0F, 7.0F, Colors.withAlpha(-16777216, 0.3F));
         var1.rectGradient(var2, var3, var4, var5, 3.0F, Colors.withAlpha(-15264995, 0.94F), Colors.withAlpha(-15856878, 0.94F), true);
         float var9 = var2 + 10.0F;
         float var10 = var3 + 10.0F;
         int var6 = !this.demo() && var7.artVersion() > 0 ? NVGImages.fromFile(this.service.artPath(), var7.artVersion()) : -1;
         if (!this.demo() && var6 > 0) {
            this.drawRoundedImage(var1, var6, var9, var10, 54.0F, 8.0F);
         } else {
            this.drawVinyl(var1, var8, var9, var10, 54.0F);
         }

         if (!var7.active()) {
            var1.textTruncated(this.service.status(), var2 + 76.0F, var3 + var5 / 2.0F, 11.5F, var8.textMuted(), var4 - 88.0F);
         } else {
            float var12 = this.module.controls.get() ? var2 + var4 - 84.0F : var2 + var4 - 12.0F;
            var1.textTruncated(var7.title(), var2 + 76.0F, var3 + 20.0F, 14.0F, var8.textPrimary(), var12 - (var2 + 76.0F) - 6.0F);
            var1.textTruncated(var7.artist(), var2 + 76.0F, var3 + 38.0F, 11.5F, var8.textMuted(), var12 - (var2 + 76.0F) - 6.0F);
            if (this.module.controls.get()) {
               float var13 = var3 + 22.0F;
               int var14 = var8.textMuted();
               this.drawPrev(var1, var2 + var4 - 76.0F, var13, var14);
               this.drawPlayPause(var1, var2 + var4 - 52.0F, var13, var8.accentBright(), var7.playing());
               this.drawNext(var1, var2 + var4 - 28.0F, var13, var14);
            }

            float var22 = var2 + 76.0F;
            float var23 = var4 - 76.0F - 12.0F;
            float var15 = var3 + 58.0F;
            if (var7.durMs() > 0L) {
               float var16 = var7.durMs() > 0L ? Math.clamp((float)var7.livePosMs() / (float)var7.durMs(), 0.0F, 1.0F) : 0.0F;
               var1.text(time(var7.livePosMs()), var22, var3 + 49.0F, 9.5F, var8.textDisabled());
               String var17 = time(var7.durMs());
               var1.text(var17, var22 + var23 - var1.textWidth(var17, 9.5F), var3 + 49.0F, 9.5F, var8.textDisabled());
               var1.rect(var22, var15, var23, 4.0F, 2.0F, Colors.withAlpha(-16777216, 0.5F));
               var1.rectGradient(var22, var15, Math.max(4.0F, var23 * var16), 4.0F, 2.0F, var8.accent(), var8.accentBright(), false);
               var1.circle(var22 + var23 * var16, var15 + 2.0F, 4.0F, -1);
            } else {
               var1.text("SPOTIFY DESKTOP", var22, var3 + 54.0F, 9.5F, var8.textDisabled());
               var1.rect(var22, var15 + 3.0F, var23, 2.0F, 1.0F, Colors.withAlpha(var8.accent(), 0.3F));
            }

            if (this.showVolume()) {
               int var24 = var7.volume();
               float var25 = Math.clamp((float)(var24 < 0 ? 0 : var24) / 100.0F, 0.0F, 1.0F);
               float var18 = var2 + 36.0F;
               float var19 = var4 - 36.0F - 46.0F;
               float var20 = var3 + 80.0F - 2.0F;
               this.drawSpeaker(var1, var2 + 18.0F, var3 + 80.0F, var8.textMuted(), var24 == 0);
               var1.rect(var18, var20, var19, 4.0F, 2.0F, Colors.withAlpha(-16777216, 0.5F));
               var1.rectGradient(var18, var20, Math.max(4.0F, var19 * var25), 4.0F, 2.0F, var8.accent(), var8.accentBright(), false);
               var1.circle(var18 + var19 * var25, var3 + 80.0F, 4.0F, -1);
               String var21 = var24 < 0 ? "--" : var24 + "%";
               var1.text(var21, var2 + var4 - var1.textWidth(var21, 9.5F) - 12.0F, var3 + 80.0F, 9.5F, var8.textDisabled());
            }
         }
      }
   }

   private void drawSpeaker(NVGRenderer var1, float var2, float var3, int var4, boolean var5) {
      long var6 = var1.ctx();
      MemoryStack var8 = MemoryStack.stackPush();

      try {
         NVGColor var9 = NanoVG.nvgRGBA(
            (byte)Colors.red(var4), (byte)Colors.green(var4), (byte)Colors.blue(var4), (byte)Colors.alpha(var4), NVGColor.malloc(var8)
         );
         var1.rect(var2 - 7.0F, var3 - 3.0F, 5.0F, 6.0F, 1.0F, var4);
         NanoVG.nvgFillColor(var6, var9);
         NanoVG.nvgBeginPath(var6);
         NanoVG.nvgMoveTo(var6, var2 - 6.0F, var3);
         NanoVG.nvgLineTo(var6, var2 + 2.0F, var3 - 7.0F);
         NanoVG.nvgLineTo(var6, var2 + 2.0F, var3 + 7.0F);
         NanoVG.nvgClosePath(var6);
         NanoVG.nvgFill(var6);
         NanoVG.nvgStrokeColor(var6, var9);
         NanoVG.nvgStrokeWidth(var6, 1.7F);
         NanoVG.nvgLineCap(var6, 1);
         if (var5) {
            NanoVG.nvgBeginPath(var6);
            NanoVG.nvgMoveTo(var6, var2 + 5.0F, var3 - 3.5F);
            NanoVG.nvgLineTo(var6, var2 + 10.0F, var3 + 3.5F);
            NanoVG.nvgMoveTo(var6, var2 + 10.0F, var3 - 3.5F);
            NanoVG.nvgLineTo(var6, var2 + 5.0F, var3 + 3.5F);
            NanoVG.nvgStroke(var6);
         } else {
            for (float var13 : new float[]{4.5F, 8.0F}) {
               NanoVG.nvgBeginPath(var6);
               NanoVG.nvgArc(var6, var2 + 2.0F, var3, var13, -0.6F, 0.6F, 2);
               NanoVG.nvgStroke(var6);
            }
         }
      } catch (Throwable var151) {
         if (var8 != null) {
            try {
               var8.close();
            } catch (Throwable var14) {
               var151.addSuppressed(var14);
            }
         }

         throw var151;
      }

      if (var8 != null) {
         var8.close();
      }
   }

   private static String time(long var0) {
      long var2 = var0 / 1000L;
      return String.format("%d:%02d", var2 / 60L, var2 % 60L);
   }

   private void drawRoundedImage(NVGRenderer var1, int var2, float var3, float var4, float var5, float var6) {
      MemoryStack var7 = MemoryStack.stackPush();

      try {
         long var8 = var1.ctx();
         NVGPaint var10 = NVGPaint.malloc(var7);
         NanoVG.nvgImagePattern(var8, var3, var4, var5, var5, 0.0F, var2, 1.0F, var10);
         NanoVG.nvgBeginPath(var8);
         NanoVG.nvgRoundedRect(var8, var3, var4, var5, var5, var6);
         NanoVG.nvgFillPaint(var8, var10);
         NanoVG.nvgFill(var8);
      } catch (Throwable var121) {
         if (var7 != null) {
            try {
               var7.close();
            } catch (Throwable var11) {
               var121.addSuppressed(var11);
            }
         }

         throw var121;
      }

      if (var7 != null) {
         var7.close();
      }
   }

   private void drawVinyl(NVGRenderer var1, Theme var2, float var3, float var4, float var5) {
      float var6 = var3 + var5 / 2.0F;
      float var7 = var4 + var5 / 2.0F;
      var1.rect(var3, var4, var5, var5, 8.0F, Colors.withAlpha(-16119795, 0.9F));
      var1.circle(var6, var7, var5 * 0.4F, -15330789);
      var1.circleOutline(var6, var7, var5 * 0.3F, 1.0F, Colors.withAlpha(var2.accent(), 0.35F));
      var1.circleOutline(var6, var7, var5 * 0.22F, 1.0F, Colors.withAlpha(var2.accent(), 0.25F));
      var1.circle(var6, var7, var5 * 0.12F, var2.accent());
      var1.circle(var6, var7, 1.6F, var2.textPrimary());
   }

   private void drawPrev(NVGRenderer var1, float var2, float var3, int var4) {
      this.triangle(var1, var2 + 4.0F, var3, -7.0F, var4);
      var1.rect(var2 - 7.0F, var3 - 5.5F, 2.0F, 11.0F, 1.0F, var4);
   }

   private void drawNext(NVGRenderer var1, float var2, float var3, int var4) {
      this.triangle(var1, var2 - 4.0F, var3, 7.0F, var4);
      var1.rect(var2 + 5.0F, var3 - 5.5F, 2.0F, 11.0F, 1.0F, var4);
   }

   private void drawPlayPause(NVGRenderer var1, float var2, float var3, int var4, boolean var5) {
      var1.circleOutline(var2, var3, 10.0F, 1.4F, var4);
      if (var5) {
         var1.rect(var2 - 3.5F, var3 - 4.5F, 2.4F, 9.0F, 1.2F, var4);
         var1.rect(var2 + 1.1F, var3 - 4.5F, 2.4F, 9.0F, 1.2F, var4);
      } else {
         this.triangle(var1, var2 - 2.5F, var3, 7.0F, var4);
      }
   }

   private void triangle(NVGRenderer var1, float var2, float var3, float var4, int var5) {
      MemoryStack var6 = MemoryStack.stackPush();

      try {
         long var7 = var1.ctx();
         NanoVG.nvgBeginPath(var7);
         NanoVG.nvgMoveTo(var7, var2, var3 - 5.5F);
         NanoVG.nvgLineTo(var7, var2, var3 + 5.5F);
         NanoVG.nvgLineTo(var7, var2 + var4, var3);
         NanoVG.nvgClosePath(var7);
         NanoVG.nvgFillColor(
            var7, NanoVG.nvgRGBA((byte)Colors.red(var5), (byte)Colors.green(var5), (byte)Colors.blue(var5), (byte)Colors.alpha(var5), NVGColor.malloc(var6))
         );
         NanoVG.nvgFill(var7);
      } catch (Throwable var101) {
         if (var6 != null) {
            try {
               var6.close();
            } catch (Throwable var9) {
               var101.addSuppressed(var9);
            }
         }

         throw var101;
      }

      if (var6 != null) {
         var6.close();
      }
   }

   @Override
   public boolean onEditClick(float var1, float var2) {
      SpotifyState var3 = this.state();
      if (!var3.active()) {
         return false;
      } else {
         if (this.module.controls.get() && var2 >= 10.0F && var2 <= 34.0F) {
            if (hit(var1, 176.0F)) {
               if (this.demo()) {
                  return true;
               }

               this.service.previous();
               return true;
            }

            if (hit(var1, 200.0F)) {
               if (this.demo()) {
                  return true;
               }

               this.service.togglePlay();
               return true;
            }

            if (hit(var1, 224.0F)) {
               if (this.demo()) {
                  return true;
               }

               this.service.next();
               return true;
            }
         }

         float var4 = 76.0F;
         float var5 = 164.0F;
         if (var2 >= 52.0F && var2 <= 66.0F && var1 >= var4 && var1 <= var4 + var5 && var3.canSeek()) {
            long var9 = (long)((var1 - var4) / var5 * (float)var3.durMs());
            if (!this.demo()) {
               this.service.seekTo(var9);
            }

            return true;
         } else {
            float var6 = 36.0F;
            float var7 = 170.0F;
            if (this.showVolume() && var2 >= 73.0F && var2 <= 87.0F && var1 >= var6 && var1 <= var6 + var7) {
               int var8 = Math.round((var1 - var6) / var7 * 100.0F);
               if (!this.demo()) {
                  this.service.setVolume(var8);
               }

               return true;
            } else {
               return false;
            }
         }
      }
   }

   private static boolean hit(float var0, float var1) {
      return Math.abs(var0 - var1) <= 11.0F;
   }
}
