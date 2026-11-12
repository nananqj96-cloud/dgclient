package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class InfoHud extends HudComponent {
   private static final float HEIGHT = 22.0F;
   private static final float FONT_SIZE = 13.0F;
   private static final float PAD_X = 9.0F;
   private final ThemeManager themes;
   private final String label;
   private final Supplier<String> value;

   public InfoHud(String var1, ThemeManager var2, String var3, Supplier<String> var4, float var5, float var6, BooleanSupplier var7) {
      super(var1, var5, var6, var7);
      this.themes = var2;
      this.label = var3;
      this.value = var4;
   }

   private String currentValue() {
      try {
         return this.value.get();
      } catch (Exception var2) {
         return "?";
      }
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 13.0F + var1.textWidth(this.label, 13.0F) + 5.0F + var1.textWidth(this.currentValue(), 13.0F) + 9.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return 22.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      float var7 = var3 + var5 / 2.0F;
      var1.rectGradient(var2, var3, var4, var5, 3.0F, var6.background(), var6.backgroundTo(), true);
      var1.rect(var2 + 5.0F, var7 - 2.0F, 4.0F, 4.0F, 1.0F, Colors.withAlpha(var6.accentBright(), 0.9F));
      float var8 = var2 + 13.0F;
      var8 += var1.textGradient(this.label, var8, var7, 13.0F, var6.accentBright(), var6.accent());
      var1.text(this.currentValue(), var8 + 5.0F, var7, 13.0F, var6.textPrimary());
   }
}
