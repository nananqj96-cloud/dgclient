package dev.aether.hud.components;

import dev.aether.AetherClient;
import dev.aether.hud.HudComponent;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;

public class WatermarkHud extends HudComponent {
   private static final float HEIGHT = 30.0F;
   private final ThemeManager themes;

   public WatermarkHud(ThemeManager themes, BooleanSupplier visible) {
      super("Aether Client", 0.008F, 0.012F, visible);
      this.themes = themes;
   }

   private static String fpsLabel() {
      return Minecraft.getInstance().getFps() + " FPS";
   }

   @Override
   public float measureWidth(NVGRenderer vg) {
      return 56.0F + vg.textWidth("AETHER", 14.5F) + vg.textWidth("v" + AetherClient.VERSION, 10.5F) + vg.textWidth(fpsLabel(), 10.5F);
   }

   @Override
   public float measureHeight(NVGRenderer vg) {
      return HEIGHT;
   }

   @Override
   public void render(NVGRenderer vg, float x, float y, float w, float h) {
      Theme theme = this.themes.current();
      float cy = y + h * 0.5F;
      vg.rectGradient(x, y, w, h, 6.0F, theme.background(), theme.backgroundTo(), true);
      vg.rect(x + 12.0F, y, 28.0F, 2.0F, 1.0F, theme.accentBright());

      vg.aetherMark(x + 17.0F, cy, 7.8F, theme.accent(), theme.accentBright());
      float tx = x + 31.0F;
      tx += vg.textGradient("AETHER", tx, cy, 14.5F, theme.accentBright(), theme.accent()) + 6.0F;
      tx += vg.text("v" + AetherClient.VERSION, tx, cy + 0.5F, 10.5F, theme.textDisabled()) + 8.0F;

      vg.line(tx, y + 8.0F, tx, y + h - 8.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));
      vg.text(fpsLabel(), tx + 8.0F, cy + 0.5F, 10.5F, theme.textPrimary());
   }
}
