package dev.aether.port67;

import dev.aether.AetherClient;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.gui.ClickGuiState;
import dev.aether.render.NvgDrawable;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The two-choice warning shown before the 67Port GUI loads.
 *
 * <p>Choice 1 — unload the main Aether client and load only the 67port (full-screen
 * {@link SixSevenPortScreen}).
 *
 * <p>Choice 2 — keep both loaded and sync them (the 67port GUI runs as a synced secondary window
 * inside the main Aether GUI). The user is warned that choosing 2 may produce UI bugs, because the
 * 67client's GUI is being driven through Aether's renderer alongside the main GUI.
 */
public class SixSevenPortWarningScreen extends Screen implements NvgDrawable {
   private static final float PANEL_W = 560.0F;
   private static final float PANEL_H = 420.0F;

   private float panelX;
   private float panelY;
   private float btn1X;
   private float btn1Y;
   private float btn1W;
   private float btn1H;
   private float btn2X;
   private float btn2Y;
   private float btn2W;
   private float btn2H;

   public SixSevenPortWarningScreen() {
      super(Component.literal("67 Port"));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.minecraft.level == null) {
         this.renderPanorama(graphics, partialTick);
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      Theme theme = AetherClient.themes().current();
      graphics.fill(0, 0, this.width, this.height, Colors.withAlpha(theme.isBloom() ? 0x120718 : 0x050811, 0.72F));
   }

   @Override
   public void renderNvg(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      if (!vg.hasFont()) {
         return;
      }
      Theme theme = AetherClient.themes().current();
      this.panelX = Math.max(20.0F, (uiW - PANEL_W) * 0.5F);
      this.panelY = Math.max(20.0F, (uiH - PANEL_H) * 0.5F);

      float bloom = theme.bloomStrength();
      vg.glow(this.panelX, this.panelY + 4.0F, PANEL_W, PANEL_H, 16.0F, 22.0F, Colors.withAlpha(0xFF000000, 0.55F));
      vg.glow(this.panelX - 1.0F, this.panelY - 1.0F, PANEL_W + 2.0F, PANEL_H + 2.0F, 16.0F, 18.0F * bloom + 6.0F, Colors.withAlpha(theme.accent(), 0.22F * bloom + 0.08F));
      vg.rectGradient(this.panelX, this.panelY, PANEL_W, PANEL_H, 16.0F, theme.background(), theme.backgroundTo(), true);
      vg.rectOutline(this.panelX, this.panelY, PANEL_W, PANEL_H, 16.0F, 1.2F, Colors.withAlpha(theme.accentBright(), 0.45F));
      vg.rect(this.panelX + 24.0F, this.panelY, 80.0F, 2.4F, 1.2F, theme.accentBright());

      float x = this.panelX + 40.0F;
      float y = this.panelY + 44.0F;

      vg.aetherMark(x + 6.0F, y + 4.0F, 11.0F, theme.accent(), theme.accentBright());
      float titleW = vg.textGradient("67 PORT", x + 26.0F, y, 22.0F, theme.accentBright(), theme.accent());
      y += 34.0F;

      vg.text("You are about to load the 67Client GUI, ported from the", x, y, 13.0F, theme.textPrimary());
      y += 20.0F;
      vg.text("67port source. Choose how it should run:", x, y, 13.0F, theme.textPrimary());
      y += 34.0F;

      vg.line(x, y, x + PANEL_W - 80.0F, y, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.8F));
      y += 20.0F;

      vg.text("1", x, y + 8.0F, 15.0F, theme.accentBright());
      vg.text("Unload the main Aether client and load only the 67port.", x + 26.0F, y + 8.0F, 13.5F, theme.textPrimary());
      y += 34.0F;

      vg.text("2", x, y + 8.0F, 15.0F, theme.accentBright());
      vg.text("Keep both loaded and sync them (67port runs inside the Aether GUI).", x + 26.0F, y + 8.0F, 13.5F, theme.textPrimary());
      y += 30.0F;

      // UI-bug warning for choice 2
      float warnW = vg.textWidth("⚠  Choosing 2 may cause some UI bugs.", 11.5F) + 18.0F;
      vg.rect(x + 26.0F, y - 12.0F, warnW, 19.0F, 6.0F, Colors.withAlpha(0xFFFBBF24, 0.14F));
      vg.rectOutline(x + 26.0F, y - 12.0F, warnW, 19.0F, 6.0F, 1.0F, Colors.withAlpha(0xFFFBBF24, 0.55F));
      vg.text("⚠  Choosing 2 may cause some UI bugs.", x + 35.0F, y, 11.5F, 0xFFFBBF24);
      y += 30.0F;

      // Buttons
      float btnW = (PANEL_W - 80.0F - 16.0F) * 0.5F;
      this.btn1H = 48.0F;
      this.btn1Y = this.panelY + PANEL_H - 76.0F;
      this.btn1X = this.panelX + 40.0F;
      this.btn1W = btnW;
      this.btn2X = this.btn1X + btnW + 16.0F;
      this.btn2W = btnW;
      this.btn2Y = this.btn1Y;
      this.btn2H = this.btn1H;

      boolean hov1 = this.hit(mx, my, this.btn1X, this.btn1Y, this.btn1W, this.btn1H);
      boolean hov2 = this.hit(mx, my, this.btn2X, this.btn2Y, this.btn2W, this.btn2H);

      vg.rect(this.btn1X, this.btn1Y, this.btn1W, this.btn1H, 10.0F, Colors.withAlpha(theme.accent(), hov1 ? 0.30F : 0.18F));
      vg.rectOutline(this.btn1X, this.btn1Y, this.btn1W, this.btn1H, 10.0F, 1.1F, Colors.withAlpha(theme.accentBright(), hov1 ? 0.80F : 0.55F));
      this.centerText("Unload Aether · 67port only", this.btn1X, this.btn1Y, this.btn1W, this.btn1H, vg, theme.textPrimary());

      vg.rect(this.btn2X, this.btn2Y, this.btn2W, this.btn2H, 10.0F, Colors.withAlpha(0xFFFFFFFF, hov2 ? 0.14F : 0.07F));
      vg.rectOutline(this.btn2X, this.btn2Y, this.btn2W, this.btn2H, 10.0F, 1.1F, Colors.withAlpha(hov2 ? theme.accentBright() : theme.borderSubtle(), hov2 ? 0.70F : 0.9F));
      this.centerText("Keep both · sync", this.btn2X, this.btn2Y, this.btn2W, this.btn2H, vg, theme.textPrimary());
   }

   private void centerText(String text, float x, float y, float w, float h, NVGRenderer vg, int color) {
      float tw = vg.textWidth(text, 13.0F);
      vg.text(text, x + (w - tw) * 0.5F, y + h * 0.5F, 13.0F, color);
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      float mx = OverlayRenderer.guiToUi(event.x());
      float my = OverlayRenderer.guiToUi(event.y());
      if (event.button() != 0) {
         return false;
      }
      if (this.hit(mx, my, this.btn1X, this.btn1Y, this.btn1W, this.btn1H)) {
         // Choice 1: unload main Aether GUI, load only the 67port.
         this.minecraft.setScreen(new SixSevenPortScreen());
         return true;
      }
      if (this.hit(mx, my, this.btn2X, this.btn2Y, this.btn2W, this.btn2H)) {
         // Choice 2: keep both loaded and sync — open the 67port window inside the main GUI.
         ClickGuiState state = ClickGuiScreen.state();
         state.setSixSevenPortOpen(true);
         this.minecraft.setScreen(new ClickGuiScreen(this.minecraft.screen));
         return true;
      }
      return false;
   }

   @Override
   public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
      if (event.isEscape()) {
         this.minecraft.setScreen(null);
         return true;
      }
      return super.keyPressed(event);
   }
}
