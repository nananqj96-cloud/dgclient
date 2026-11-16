package dev.aether.port67;

import dev.aether.AetherClient;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.render.NvgDrawable;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Full-screen standalone 67Client GUI (choice 1 of the warning).
 *
 * <p>The main Aether client GUI is unloaded; the 67client GUI — ported from the 67port source — is
 * the only GUI shown. It renders the 67client dropdown menu full-screen on Aether's {@link NVGRenderer}
 * and is driven by Aether's shared module/theme state.
 */
public class SixSevenPortScreen extends Screen implements NvgDrawable {
   private final SixSevenPortWindow window;

   public SixSevenPortScreen() {
      super(Component.literal("67Client"));
      this.window = new SixSevenPortWindow(
         AetherClient.themes(),
         ClickGuiScreen.state(),
         AetherClient.modules()
      );
      this.window.setFullScreen(true);
      ClickGuiScreen.state().setSixSevenPortOpen(true);
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
      graphics.fill(0, 0, this.width, this.height, Colors.withAlpha(theme.isBloom() ? 0x120718 : 0x050811, 0.85F));
   }

   @Override
   public void renderNvg(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      try {
         this.window.render(vg, mx, my, uiW, uiH, 0.0F, 0.0F, uiH);
      } catch (Throwable t) {
         AetherClient.LOGGER.error("67Port full-screen render failed", t);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      float mx = OverlayRenderer.guiToUi(event.x());
      float my = OverlayRenderer.guiToUi(event.y());
      if (this.window.mouseClicked(mx, my, event.button())) {
         if (this.window.consumeCloseRequest()) {
            this.minecraft.setScreen(null);
         }
         return true;
      }
      return true;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      this.window.mouseDragged(OverlayRenderer.guiToUi(event.x()), OverlayRenderer.guiToUi(event.y()));
      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.window.mouseReleased();
      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      this.window.mouseScrolled(OverlayRenderer.guiToUi(mouseX), OverlayRenderer.guiToUi(mouseY), scrollY);
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.window.keyPressed(event.key())) {
         return true;
      }
      if (event.isEscape()) {
         ClickGuiScreen.state().setSixSevenPortOpen(false);
         this.minecraft.setScreen(null);
         return true;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      return this.window.charTyped(event.codepoint()) || super.charTyped(event);
   }
}
