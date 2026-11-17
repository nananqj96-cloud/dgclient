package dev.aether.port67;

import dev.aether.AetherClient;
import dev.aether.gui.ClickGuiState;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.List;

/**
 * The 67Client GUI window, ported from the 67port source ({@code sixseven.client.ui}).
 *
 * <p>This is the <em>real</em> 67client dropdown menu, not a remake: it delegates all dropdown
 * chrome, the search top-bar, module rows, and their interaction to the ported 67client classes
 * ({@link SixSevenPanelMenu}, {@link ModulePanel}, {@link MenuPanelSupport}), which render through
 * the {@link SixSevenFontManager} NanoVG→NVGRenderer adapter. Only the outer floating-window shell
 * (glass frame, open animation, drag, close) is Aether-side, mirroring the synced-secondary-UI
 * pattern used by the other ported windows.
 *
 * <p>It is driven by Aether's shared {@link ModuleManager} and {@link ThemeManager}, so toggling a
 * module here reflects in the main Aether GUI and vice-versa (real sync), and the colors follow the
 * live Aether theme. Rendered as a synced secondary window alongside the main GUI (choice 2), or
 * full-screen via {@link SixSevenPortScreen} (choice 1).
 */
public final class SixSevenPortWindow {
   private static final float WINDOW_RADIUS = 13.0F;
   private static final float HEADER_H = 38.0F;

   private final ThemeManager themes;
   private final ClickGuiState state;
   private final ModuleManager modules;

   private final Animation subOpenAnim = new Animation(165.0F, 0.0F);
   private final PanelLayout panelLayout;
   private final SixSevenPanelMenu menu;
   private float maxScroll;

   private float x;
   private float y;
   private float w = 430.0F;
   private float h = 560.0F;
   private boolean fullScreen;
   private boolean customMoved;
   private float customX;
   private float customY;
   private boolean draggingHeader;
   private float dragOffX;
   private float dragOffY;
   private boolean closeRequested;

   private final List<ModulePanel> panels = new ArrayList<>();
   private final SixSevenMinimalGui minimalGui;

   public SixSevenPortWindow(ThemeManager themes, ClickGuiState state, ModuleManager modules) {
      this.themes = themes;
      this.state = state;
      this.modules = modules;
      this.subOpenAnim.snapTo(state.isSixSevenPortOpen() ? 1.0F : 0.0F);

      List<String> categories = new ArrayList<>();
      for (Category cat : Category.values()) {
         categories.add(cat.getDisplayName());
      }
      this.panelLayout = new PanelLayout(categories);
      for (Category cat : Category.values()) {
         List<Module> inCategory = this.modules.inCategory(cat);
         this.panels.add(new ModulePanel(cat.getDisplayName(), this.panelLayout, inCategory));
      }
      this.menu = new SixSevenPanelMenu(this.panelLayout, this.panels);
      this.minimalGui = new SixSevenMinimalGui(this.modules, this.themes);
   }

   /** True when the experimental minimalistic (Vape-style) GUI override is enabled. */
   public boolean isMinimal() {
      return this.modules.sixSevenPort != null && this.modules.sixSevenPort.experimentalMinimalGui.get();
   }

   public boolean isVisible() {
      this.subOpenAnim.setTarget(this.state.isSixSevenPortOpen() ? 1.0F : 0.0F);
      return this.subOpenAnim.value() > 0.005F || this.state.isSixSevenPortOpen();
   }

   public float openProgress() {
      this.subOpenAnim.setTarget(this.state.isSixSevenPortOpen() ? 1.0F : 0.0F);
      return this.subOpenAnim.value();
   }

   public float preferredWidth(float uiW) {
      return Math.clamp(uiW * 0.36F, 360.0F, 460.0F);
   }

   public void setFullScreen(boolean fullScreen) {
      this.fullScreen = fullScreen;
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float w() {
      return this.w;
   }

   public float h() {
      return this.h;
   }

   public void unfocusInputs() {
      this.menu.enabled = false;
   }

   /** Sync the ported 67client accent to the live Aether theme accent. */
   public void syncTheme(Theme theme) {
      ColorPalette.getInstance().syncAccent(theme.accent());
   }

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH, float defaultX, float defaultY, float targetH) {
      float sub = this.openProgress();
      if (sub <= 0.004F) {
         return;
      }
      Theme theme = this.themes.current();
      if (this.fullScreen) {
         this.w = uiW;
         this.h = uiH;
         this.x = 0.0F;
         this.y = 0.0F;
      } else {
         this.w = this.preferredWidth(uiW);
         this.h = targetH;
         if (this.customMoved) {
            this.x = Math.clamp(this.customX, 10.0F, Math.max(10.0F, uiW - this.w - 10.0F));
            this.y = Math.clamp(this.customY, 10.0F, Math.max(10.0F, uiH - this.h - 10.0F));
         } else {
            this.x = Math.clamp(defaultX, 10.0F, Math.max(10.0F, uiW - this.w - 10.0F));
            this.y = Math.clamp(defaultY, 10.0F, Math.max(10.0F, uiH - this.h - 10.0F));
         }
      }

      // Experimental minimalistic (Vape-style) override: flat dark/white, no glow.
      // Renders both the panel and floating versions when the setting is enabled.
      if (this.isMinimal()) {
         this.minimalGui.render(vg, mx, my, this.x, this.y, this.w, this.h);
         return;
      }

      this.syncTheme(theme);

      // Floating-window glass shell (Aether-side chrome, mirroring the synced-UI window pattern)
      float bloom = theme.bloomStrength();
      vg.glow(this.x, this.y + 4.0F, this.w, this.h, WINDOW_RADIUS, 20.0F, Colors.withAlpha(0xFF000000, 0.52F));
      vg.glow(this.x - 1.0F, this.y - 1.0F, this.w + 2.0F, this.h + 2.0F, WINDOW_RADIUS, 18.0F * bloom + 6.0F, Colors.withAlpha(theme.accent(), 0.24F * bloom + 0.08F));
      vg.rectGradient(this.x, this.y, this.w, this.h, WINDOW_RADIUS, theme.background(), theme.backgroundTo(), true);
      vg.rectVaryingGradient(this.x, this.y, this.w, HEADER_H, WINDOW_RADIUS, WINDOW_RADIUS, 0.0F, 0.0F, theme.headerTop(), theme.headerBottom());
      vg.line(this.x + 1.0F, this.y + HEADER_H, this.x + this.w - 1.0F, this.y + HEADER_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));
      vg.rectOutline(this.x, this.y, this.w, this.h, WINDOW_RADIUS, 1.15F, Colors.withAlpha(theme.accentBright(), 0.44F));

      // Window header (drag handle + title + close)
      float cy = this.y + HEADER_H * 0.5F;
      float closeX = this.x + this.w - 30.0F;
      float closeY = cy - 11.0F;
      float closeSize = 22.0F;
      boolean cHov = this.hit(mx, my, closeX, closeY, closeSize, closeSize);
      vg.rect(closeX, closeY, closeSize, closeSize, 6.0F, Colors.withAlpha(0xFFFFFFFF, cHov ? 0.12F : 0.04F));
      vg.cross(closeX + 6.0F, closeY + 6.0F, 10.0F, 1.5F, cHov ? theme.accentBright() : theme.textMuted());
      vg.text("67 PORT", this.x + 18.0F, cy, 13.5F, theme.accentBright());
      vg.text("67Client dropdown · synced", this.x + 74.0F, cy, 9.0F, theme.textDisabled());

      this.draggingHeader = this.hit(mx, my, this.x, this.y, this.w, HEADER_H);

      // The real 67client dropdown menu, drawn through the ported source.
      // Wrapped in exception-safe save/restore so a porting bug can never crash
      // Aether's whole overlay (which would freeze/break the screen).
      float bodyTop = this.y + HEADER_H;
      float bodyH = this.h - HEADER_H;
      float menuX = this.x + 8.0F;
      float menuY = bodyTop + 8.0F;
      float menuW = this.w - 16.0F;
      float menuH = bodyH - 16.0F;
      SixSevenFontManager fontManager = new SixSevenFontManager(vg);
      try {
         this.menu.render(fontManager, menuW, menuH);
      } catch (Throwable t) {
         AetherClient.LOGGER.error("67Port menu layout failed", t);
         return;
      }
      vg.save();
      try {
         vg.scissor(this.x + 6.0F, bodyTop, this.w - 12.0F, bodyH);
         fontManager.save();
         try {
            fontManager.translate(menuX, menuY);
            this.menu.render2(fontManager, mx - menuX, my - menuY, menuW, menuH);
         } finally {
            fontManager.restore();
         }
      } catch (Throwable t) {
         AetherClient.LOGGER.error("67Port menu render failed", t);
      } finally {
         vg.restore();
      }
   }

   // =========================================================================
   // INPUT
   // =========================================================================

   public boolean mouseClicked(float mx, float my, int btn) {
      if (this.isMinimal()) {
         if (this.minimalGui.mouseClicked(mx, my, btn)) {
            if (this.minimalGui.hitClose(mx, my)) {
               this.state.setSixSevenPortOpen(false);
               this.closeRequested = true;
            }
            return true;
         }
         if (this.minimalGui.hitHeader(mx, my)) {
            this.draggingHeader = true;
            this.dragOffX = mx - this.x;
            this.dragOffY = my - this.y;
            return true;
         }
         return false;
      }
      if (btn == 0 && this.hit(mx, my, this.x + this.w - 30.0F, this.y + (HEADER_H * 0.5F) - 11.0F, 22.0F, 22.0F)) {
         this.state.setSixSevenPortOpen(false);
         this.closeRequested = true;
         return true;
      }
      // The dropdown header drag begins only if the click isn't consumed by the menu.
      float menuX = this.x + 8.0F;
      float menuY = this.y + HEADER_H + 8.0F;
      if (this.menu.click(mx - menuX, my - menuY, btn)) {
         return true;
      }
      if (this.hit(mx, my, this.x, this.y, this.w, HEADER_H)) {
         this.draggingHeader = true;
         this.dragOffX = mx - this.x;
         this.dragOffY = my - this.y;
         return true;
      }
      return false;
   }

   public void mouseDragged(float mx, float my) {
      if (!this.isMinimal()) {
         this.menu.apply(mx - (this.x + 8.0F), my - (this.y + HEADER_H + 8.0F));
      }
      if (this.draggingHeader) {
         this.customMoved = true;
         this.customX = mx - this.dragOffX;
         this.customY = my - this.dragOffY;
      }
   }

   public void mouseReleased() {
      if (!this.isMinimal()) {
         this.menu.update3();
      }
      this.draggingHeader = false;
   }

   public boolean mouseScrolled(float mx, float my, double scrollY) {
      if (this.isMinimal()) {
         return this.minimalGui.mouseScrolled(mx, my, scrollY, this.x, this.y, this.w, this.h);
      }
      for (ModulePanel panel : this.panels) {
         panel.apply((float) scrollY);
      }
      return this.hit(mx, my, this.x, this.y + HEADER_H, this.w, this.h - HEADER_H);
   }

   /** Returns true once if the user clicked the close button; resets the flag. */
   public boolean consumeCloseRequest() {
      boolean r = this.closeRequested;
      this.closeRequested = false;
      return r;
   }

   public boolean keyPressed(int keyCode) {
      return !this.isMinimal() && this.menu.test(keyCode, false);
   }

   public boolean charTyped(int codepoint) {
      return !this.isMinimal() && this.menu.charTyped(codepoint);
   }

   public boolean isListening() {
      return !this.isMinimal() && this.menu.enabled;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
