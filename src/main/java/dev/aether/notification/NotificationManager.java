package dev.aether.notification;

import dev.aether.hud.HudComponent;
import dev.aether.hud.HudDragController;
import dev.aether.module.Modules;
import dev.aether.render.anim.Animation;
import dev.aether.render.anim.Easing;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager extends HudComponent {
   private static final float WIDTH_PAD = 12.0F;
   private static final float HEIGHT = 30.0F;
   private static final float HEIGHT_WEATHER = 42.0F;
   private static final float HEIGHT_BIG = 64.0F;
   private static final int BIG_ACCENT = 0xA855F7;
   private static final int BIG_ACCENT_BRIGHT = 0xC084FC;
   private static final float GAP = 6.0F;
   private static final float FONT = 13.5F;
   private static final float FONT_SUB = 11.0F;
   private static final float GLYPH = 26.0F;
   private static final float ANCHOR_W = 168.0F;
   private static final float ANCHOR_H = 30.0F;
   private final ThemeManager themes;
   private final Modules.HudModule hud;
   private final List<NotificationManager.Toast> toasts = new ArrayList<>();

   public NotificationManager(ThemeManager var1, Modules.HudModule var2) {
      super("notifications", 0.99F, 0.71F, () -> var2.isEnabled() && var2.notifications.get());
      this.themes = var1;
      this.hud = var2;
   }

   private boolean enabled() {
      return this.hud.isEnabled() && this.hud.notifications.get();
   }

   private float lifeSeconds() {
      return this.hud.notifyDuration.getFloat();
   }

   public void push(String var1, boolean var2) {
      this.add(
         new NotificationManager.Toast(var1 + (var2 ? " enabled" : " disabled"), null, null, var2)
      );
   }

   public void pushInfo(String var1) {
      this.add(new NotificationManager.Toast(var1, null, null, true, false));
   }

   public void pushBig(String var1, String var2) {
      this.add(new NotificationManager.Toast(var1, var2, null, true, true));
   }

   public void pushWeather(String var1, String var2, NotificationManager.Weather var3, boolean var4) {
      this.add(new NotificationManager.Toast(var1, var2, var3, var4, false));
   }

   private void add(NotificationManager.Toast var1) {
      if (this.enabled()) {
         synchronized (this.toasts) {
            this.toasts.add(var1);
            if (this.toasts.size() > 6) {
               this.toasts.removeFirst();
            }
         }
      }
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 168.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return 30.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
   }

   public void renderToasts(NVGRenderer var1, float var2, float var3) {
      Theme var4 = this.themes.current();
      float var5 = this.lifeSeconds();
      float var6 = var5 * 1.0E9F;
      synchronized (this.toasts) {
         Iterator var9 = this.toasts.iterator();
         ArrayList var10 = new ArrayList();

         while (var9.hasNext()) {
            NotificationManager.Toast var11 = (NotificationManager.Toast)var9.next();
            if ((float)(System.nanoTime() - var11.bornNanos) > var6 + 3.0E8F) {
               var9.remove();
            } else {
               var10.add(var11);
            }
         }

         boolean var27 = HudDragController.isEditing();
         if (!var10.isEmpty() || var27) {
            float var12 = this.getScale();
            float var13 = 168.0F * var12;
            float var14 = 30.0F * var12;
            float var15 = this.getFx() * (var2 - var13);
            float var16 = this.getFy() * (var3 - var14);
            var1.save();
            var1.translate((float)Math.round(var15), (float)Math.round(var16));
            var1.scale(var12);
            float var17 = 168.0F;
            float var18 = 30.0F;

            for (int var19 = var10.size() - 1; var19 >= 0; var19--) {
               NotificationManager.Toast var20 = (NotificationManager.Toast)var10.get(var19);
               float var21 = (float)(System.nanoTime() - var20.bornNanos) / 1.0E9F;
               float var22 = Math.clamp((var5 + 0.3F - var21) / 0.3F, 0.0F, 1.0F);
               float var23 = var20.slide.value();
               float var24 = this.drawToast(var1, var4, var17, var18, var23, var22, var20.title, var20.subtitle, var20.weather, var20.accent, var20.big);
               var18 -= var24 + 6.0F;
            }

            if (var10.isEmpty() && var27) {
               this.drawToast(var1, var4, var17, var18, 1.0F, 0.5F, "Notification", null, null, true, false);
            }

            var1.restore();
         }
      }
   }

   private float drawToast(
      NVGRenderer var1,
      Theme var2,
      float var3,
      float var4,
      float var5,
      float var6,
      String var7,
      String var8,
      NotificationManager.Weather var9,
      boolean var10,
      boolean big
   ) {
      boolean var11 = var8 != null;
      float var12 = big ? HEIGHT_BIG : (var11 ? HEIGHT_WEATHER : HEIGHT);
      float var13 = var9 != null ? 26.0F : 0.0F;
      float titleFont = big ? 16.0F : 13.5F;
      float subFont = big ? 12.0F : 11.0F;
      float var14 = big ? 20.0F + var13 : 16.0F + var13;
      float var15 = Math.max(var1.textWidth(var7, titleFont), var11 ? var1.textWidth(var8, subFont) : 0.0F);
      float var16 = big ? Math.min(360.0F, var14 + var15 + 20.0F) : var14 + var15 + 12.0F;
      float var17 = var3 - var16 + (1.0F - var5) * (var16 + 10.0F);
      float var18 = var4 - var12;
      int accent = big ? BIG_ACCENT : (var10 ? var2.accent() : var2.textDisabled());
      int accentBright = big ? BIG_ACCENT_BRIGHT : var2.accentBright();
      var1.save();
      var1.alpha(var6);
      var1.rectGradient(var17, var18, var16, var12, big ? 5.0F : 3.0F, var2.background(), var2.backgroundTo(), true);
      var1.rectOutline(var17 + 0.5F, var18 + 0.5F, var16 - 1.0F, var12 - 1.0F, big ? 5.0F : 3.0F, 1.0F, Colors.withAlpha(accent, 0.34F * var6));
      var1.rect(var17 + (big ? 12.0F : 9.0F), var18, big ? 36.0F : 28.0F, 2.0F, 1.0F, accent);
      var1.glow(var17 + (big ? 12.0F : 9.0F), var18, big ? 36.0F : 28.0F, 2.0F, 1.0F, 4.0F, Colors.withAlpha(accent, 0.18F * var6));
      var1.save();
      var1.translate(var17 + (big ? 10.0F : 7.0F), var18 + var12 / 2.0F);
      var1.rotate((float) (Math.PI / 4));
      var1.rect(-2.0F, -2.0F, 4.0F, 4.0F, 0.7F, Colors.withAlpha(accent, 0.9F));
      var1.restore();
      var1.line(var17 + var16 - (big ? 12.0F : 9.0F), var18 + var12 - 1.0F, var17 + var16 - 1.0F, var18 + var12 - 1.0F, 1.0F, Colors.withAlpha(accent, 0.45F));
      if (var9 != null) {
         this.drawWeatherGlyph(var1, var17 + 12.0F + 2.0F + var13 / 2.0F - 4.0F, var18 + var12 / 2.0F, var9, accent, accentBright);
      }

      if (var11) {
         if (var10 || big) {
            var1.textGradient(var7, var17 + var14, var18 + var12 / 2.0F - (big ? 12.0F : 8.0F), titleFont, accentBright, accent);
         } else {
            var1.text(var7, var17 + var14, var18 + var12 / 2.0F - (big ? 12.0F : 8.0F), titleFont, var2.textPrimary());
         }

         var1.text(var8, var17 + var14, var18 + var12 / 2.0F + (big ? 12.0F : 8.0F), subFont, var2.textMuted());
      } else if (var10 || big) {
         var1.textGradient(var7, var17 + var14, var18 + var12 / 2.0F, titleFont, accentBright, accent);
      } else {
         var1.text(var7, var17 + var14, var18 + var12 / 2.0F, titleFont, var2.textMuted());
      }

      var1.restore();
      return var12;
   }

   private void drawWeatherGlyph(NVGRenderer var1, float var2, float var3, NotificationManager.Weather var4, int var5, int var6) {
      switch (var4) {
         case RAIN:
            this.drawCloud(var1, var2, var3 - 3.0F, var5);

            for (int var12 = -1; var12 <= 1; var12++) {
               float var13 = var2 + (float)var12 * 4.0F;
               var1.line(var13 + 1.0F, var3 + 4.0F, var13 - 1.0F, var3 + 9.0F, 1.6F, var5);
            }
            break;
         case THUNDER:
            this.drawCloud(var1, var2, var3 - 3.0F, var5);
            var1.line(var2 + 1.5F, var3 + 2.0F, var2 - 2.5F, var3 + 6.0F, 1.9F, var6);
            var1.line(var2 - 2.5F, var3 + 6.0F, var2 + 1.5F, var3 + 6.0F, 1.9F, var6);
            var1.line(var2 + 1.5F, var3 + 6.0F, var2 - 2.5F, var3 + 11.0F, 1.9F, var6);
            break;
         case CLEAR:
            var1.circle(var2, var3, 4.5F, var6);

            for (int var7 = 0; var7 < 8; var7++) {
               double var8 = (double)var7 * Math.PI / 4.0;
               float var10 = (float)Math.cos(var8);
               float var11 = (float)Math.sin(var8);
               var1.line(var2 + var10 * 6.5F, var3 + var11 * 6.5F, var2 + var10 * 9.0F, var3 + var11 * 9.0F, 1.6F, var5);
            }
      }
   }

   private void drawCloud(NVGRenderer var1, float var2, float var3, int var4) {
      var1.rect(var2 - 7.0F, var3 - 1.0F, 14.0F, 5.0F, 2.5F, var4);
      var1.circle(var2 - 4.5F, var3 + 1.0F, 4.0F, var4);
      var1.circle(var2 + 4.5F, var3 + 1.0F, 4.0F, var4);
      var1.circle(var2, var3 - 2.5F, 5.0F, var4);
   }

   private static class Toast {
      final String title;
      final String subtitle;
      final NotificationManager.Weather weather;
      final boolean accent;
      final boolean big;
      final long bornNanos = System.nanoTime();
      final Animation slide = new Animation(220.0F, 0.0F, Easing.EASE_OUT_CUBIC);

      Toast(String var1, String var2, NotificationManager.Weather var3, boolean var4, boolean var5) {
         this.title = var1;
         this.subtitle = var2;
         this.weather = var3;
         this.accent = var4;
         this.big = var5;
         this.slide.setTarget(1.0F);
      }
   }

   public static enum Weather {
      RAIN,
      THUNDER,
      CLEAR;
   }
}
