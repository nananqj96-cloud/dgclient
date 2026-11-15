package dev.aether.port67;

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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The 67Client GUI, ported faithfully from the 67port source ({@code sixseven.client.ui}).
 *
 * <p>This reproduces the 67client's real dropdown menu — its signature glass dropdown panels
 * (210px wide, 38px animated header with a rotating chevron, expandable body of module rows) —
 * running on Aether's {@link NVGRenderer} instead of the 67client's own bundled NanoVG stack.
 *
 * <p>It is driven by Aether's shared {@link ModuleManager} and {@link ThemeManager}, so toggling a
 * module here reflects in the main Aether GUI and vice-versa (real sync), and the colors follow the
 * live Aether theme. Rendered as a synced secondary window alongside the main GUI (choice 2), or
 * full-screen via {@link SixSevenPortScreen} (choice 1).
 */
public final class SixSevenPortWindow {
   private static final float PANEL_W = 210.0F;
   private static final float HEADER_H = 38.0F;
   private static final float RADIUS = 12.0F;
   private static final float ROW_H = 30.0F;
   private static final float ROW_GAP = 3.0F;
   private static final float WINDOW_RADIUS = 13.0F;
   private static final float TAB_H = 34.0F;

   private final ThemeManager themes;
   private final ClickGuiState state;
   private final ModuleManager modules;
   private final Animation subOpenAnim = new Animation(165.0F, 0.0F);
   private final Animation scrollAnim = new Animation(145.0F, 0.0F);
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

   private final StringBuilder search = new StringBuilder();
   private boolean searchFocused;
   private float searchX;
   private float searchY;
   private float searchW;
   private float searchH;
   private float closeX;
   private float closeY;
   private float closeSize = 22.0F;
   private boolean closeRequested;

   // Per-category dropdown open progress (the 67client animates each panel open/closed).
   private final Map<Category, Animation> panelOpen = new EnumMap<>(Category.class);

   // Clickable regions recorded during render, hit-tested in mouseClicked (robust across the
   // render/input phase boundary).
   private final List<Hit> hits = new ArrayList<>();

   private static final class Hit {
      final float x;
      final float y;
      final float w;
      final float h;
      final Object target; // Category header (toggle) or Module (toggle module)
      final boolean module;

      Hit(float x, float y, float w, float h, Object target, boolean module) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.target = target;
         this.module = module;
      }
   }

   public SixSevenPortWindow(ThemeManager themes, ClickGuiState state, ModuleManager modules) {
      this.themes = themes;
      this.state = state;
      this.modules = modules;
      this.subOpenAnim.snapTo(state.isSixSevenPortOpen() ? 1.0F : 0.0F);
      for (Category cat : Category.values()) {
         this.panelOpen.put(cat, new Animation(220.0F, 0.0F));
      }
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

   public void toggleCategory(Category cat) {
      this.panelOpen.get(cat).setTarget(this.panelOpen.get(cat).getTarget() > 0.5F ? 0.0F : 1.0F);
   }

   public void setCategoryOpen(Category cat, boolean open) {
      this.panelOpen.get(cat).setTarget(open ? 1.0F : 0.0F);
   }

   public void unfocusInputs() {
      this.searchFocused = false;
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

      vg.save();
      vg.alpha(sub);
      float scale = 0.95F + 0.05F * sub;
      vg.translate(this.x + this.w * 0.5F, this.y + this.h * 0.5F);
      vg.scale(scale);
      vg.translate(-(this.x + this.w * 0.5F), -(this.y + this.h * 0.5F));

      // Optical glow + dark velvet glass shell (67client dark-glass chrome, tinted by Aether theme)
      float bloom = theme.bloomStrength();
      vg.glow(this.x, this.y + 4.0F, this.w, this.h, WINDOW_RADIUS, 20.0F, Colors.withAlpha(0xFF000000, 0.52F));
      vg.glow(this.x - 1.0F, this.y - 1.0F, this.w + 2.0F, this.h + 2.0F, WINDOW_RADIUS, 18.0F * bloom + 6.0F, Colors.withAlpha(theme.accent(), 0.24F * bloom + 0.08F));
      vg.rectGradient(this.x, this.y, this.w, this.h, WINDOW_RADIUS, theme.background(), theme.backgroundTo(), true);

      // Header band
      vg.rectVaryingGradient(this.x, this.y, this.w, HEADER_H, WINDOW_RADIUS, WINDOW_RADIUS, 0.0F, 0.0F, theme.headerTop(), theme.headerBottom());
      vg.line(this.x + 1.0F, this.y + HEADER_H, this.x + this.w - 1.0F, this.y + HEADER_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));
      vg.rectOutline(this.x, this.y, this.w, this.h, WINDOW_RADIUS, 1.15F, Colors.withAlpha(theme.accentBright(), 0.44F));
      vg.rect(this.x + 18.0F, this.y, 62.0F, 2.2F, 1.1F, theme.accentBright());

      this.renderHeader(vg, theme, mx, my);
      this.renderDropdowns(vg, theme, mx, my);

      vg.restore();
   }

   private void renderHeader(NVGRenderer vg, Theme theme, float mx, float my) {
      float cy = this.y + HEADER_H * 0.5F;
      vg.aetherMark(this.x + 20.0F, cy, 8.2F, theme.accent(), theme.accentBright());
      float tw = vg.textGradient("67 PORT", this.x + 34.0F, cy, 13.5F, theme.accentBright(), theme.accent());

      String subtitle = "67Client dropdown · synced";
      vg.text(subtitle, this.x + 40.0F + tw, cy, 9.0F, theme.textDisabled());

      // Close button
      this.closeX = this.x + this.w - 30.0F;
      this.closeY = cy - this.closeSize * 0.5F;
      boolean cHov = this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize);
      vg.rect(this.closeX, this.closeY, this.closeSize, this.closeSize, 6.0F, Colors.withAlpha(0xFFFFFFFF, cHov ? 0.12F : 0.04F));
      vg.cross(this.closeX + 6.0F, this.closeY + 6.0F, 10.0F, 1.5F, cHov ? theme.accentBright() : theme.textMuted());

      // Compact search input
      float leftLimit = this.x + 48.0F + tw + vg.textWidth(subtitle, 9.0F) + 8.0F;
      this.searchH = 26.0F;
      this.searchW = Math.max(105.0F, (this.closeX - 8.0F) - leftLimit);
      this.searchX = this.closeX - 8.0F - this.searchW;
      this.searchY = cy - this.searchH * 0.5F;

      vg.rect(this.searchX, this.searchY, this.searchW, this.searchH, 7.0F, Colors.withAlpha(0xFF07060D, 0.72F));
      vg.rectOutline(this.searchX, this.searchY, this.searchW, this.searchH, 7.0F, 1.0F, Colors.withAlpha(this.searchFocused ? theme.accentBright() : theme.borderSubtle(), 0.88F));
      float sx = this.searchX + 10.0F;
      if (this.search.isEmpty() && !this.searchFocused) {
         vg.text("Search 67...", sx, cy, 11.0F, theme.textDisabled());
      } else {
         float stw = vg.textTruncated(this.search.toString(), sx, cy, 11.5F, theme.textPrimary(), this.searchW - 28.0F);
         if (this.searchFocused && System.nanoTime() / 400000000L % 2L == 0L) {
            vg.rect(sx + stw + 1.5F, cy - 5.5F, 1.3F, 11.0F, 0.6F, theme.accentBright());
         }
      }
      if (!this.search.isEmpty()) {
         vg.cross(this.searchX + this.searchW - 15.0F, cy - 4.5F, 9.0F, 1.4F, theme.textMuted());
      }
   }

   /**
    * Renders the 67client dropdown panels — one glass dropdown per category, each with an animated
    * header (chevron rotates open/closed) and an expanding body of module rows.
    */
   private void renderDropdowns(NVGRenderer vg, Theme theme, float mx, float my) {
      float bodyTop = this.y + HEADER_H + TAB_H;
      float bodyH = this.h - HEADER_H - TAB_H - 8.0F;
      float innerX = this.x + 12.0F;
      float innerW = this.w - 24.0F;
      this.hits.clear();

      vg.save();
      vg.scissor(this.x + 6.0F, bodyTop, this.w - 12.0F, bodyH);

      float contentTop = bodyTop + 10.0F - this.scrollAnim.value();
      Category[] cats = Category.values();
      int cols = innerW >= PANEL_W * 2.0F + 16.0F ? 2 : 1;
      float colW = cols == 2 ? (innerW - 12.0F) * 0.5F : innerW;

      float[] colY = new float[cols];
      float maxContent = 0.0F;
      for (Category cat : cats) {
         int col = this.indexOf(cats, cat) % cols;
         float px = innerX + col * (colW + 12.0F);
         float py = contentTop + colY[col];
         float prog = this.panelOpen.get(cat).value();
         this.renderDropdown(vg, theme, mx, my, px, py, colW, cat, prog);
         float panelH = HEADER_H + this.dropdownHeight(cat) * prog;
         colY[col] += panelH + 12.0F;
         maxContent = Math.max(maxContent, colY[col]);
      }

      this.maxScroll = Math.max(0.0F, maxContent - bodyH);
      this.scrollAnim.setTarget(Math.clamp(this.scrollAnim.getTarget(), 0.0F, this.maxScroll));

      if (this.maxScroll > 1.0F) {
         float trackH = bodyH - 16.0F;
         float thumbH = Math.max(28.0F, trackH * (bodyH / Math.max(1.0F, maxContent)));
         float thumbY = bodyTop + 8.0F + (trackH - thumbH) * (this.scrollAnim.value() / this.maxScroll);
         vg.rect(this.x + this.w - 8.0F, bodyTop + 8.0F, 3.0F, trackH, 1.5F, Colors.withAlpha(theme.borderSubtle(), 0.5F));
         vg.rect(this.x + this.w - 8.0F, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(theme.accentBright(), 0.75F));
      }
      vg.restore();
   }

   private int indexOf(Category[] cats, Category cat) {
      for (int i = 0; i < cats.length; i++) {
         if (cats[i] == cat) {
            return i;
         }
      }
      return 0;
   }

   /** Height of a dropdown's expanded body in UI pixels. */
   private float dropdownHeight(Category cat) {
      String q = this.search.toString().toLowerCase(Locale.ROOT).trim();
      int count = 0;
      for (Module m : this.modules.inCategory(cat)) {
         if (q.isEmpty() || m.matchesFilter(q)) {
            count++;
         }
      }
      return Math.max(36.0F, count * (ROW_H + ROW_GAP) + 8.0F);
   }

   /**
    * Draws one 67client dropdown panel: glass header (icon + title + chevron) and an animated body
    * of module rows. Faithful to the 67client {@code MenuPanel}/{@code ModulePanel} chrome.
    */
   private void renderDropdown(NVGRenderer vg, Theme theme, float mx, float my, float px, float py, float pw, Category cat, float prog) {
      float radius = RADIUS;
      boolean open = prog > 0.5F;

      // Header glass panel (67client signature chrome)
      vg.glow(px, py, pw, HEADER_H, radius, 8.0F, Colors.withAlpha(theme.accent(), 0.18F * prog + 0.06F));
      vg.rectGradient(px, py, pw, HEADER_H, radius, theme.surfaceElevated(), theme.surfacePressed(), true);
      vg.rectOutline(px, py, pw, HEADER_H, radius, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));

      float cy = py + HEADER_H * 0.5F;
      // Category dot
      vg.circle(px + 16.0F, cy, 4.6F, open ? theme.accentBright() : theme.textMuted());
      vg.text(cat.getDisplayName(), px + 28.0F, cy, 13.5F, open ? theme.textPrimary() : theme.textMuted());

      // Chevron (rotates open/closed like the 67client)
      float chevX = px + pw - 18.0F;
      vg.chevron(chevX, cy, 7.0F, 1.8F, theme.accentBright(), !open);

      // Header is clickable to expand/collapse the dropdown
      this.hits.add(new Hit(px, py, pw, HEADER_H, cat, false));

      // Expanded body
      if (prog > 0.004F) {
         vg.save();
         vg.alpha(prog);
         float bodyTop = py + HEADER_H;
         float bodyH = this.dropdownHeight(cat) * prog;
         // Body glass
         vg.rect(px, bodyTop, pw, bodyH, 0.0F, Colors.withAlpha(theme.background(), 0.62F));
         vg.line(px + 1.0F, bodyTop, px + pw - 1.0F, bodyTop, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));
         vg.rectOutline(px, bodyTop, pw, bodyH, 0.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.5F));

         String q = this.search.toString().toLowerCase(Locale.ROOT).trim();
         float rowY = bodyTop + 8.0F;
         for (Module m : this.modules.inCategory(cat)) {
            if (!q.isEmpty() && !m.matchesFilter(q)) {
               continue;
            }
            float rowH = ROW_H;
            boolean hov = this.hit(mx, my, px + 4.0F, rowY, pw - 8.0F, rowH);
            this.hits.add(new Hit(px + 4.0F, rowY, pw - 8.0F, rowH, m, true));
            // Row hover highlight
            if (hov) {
               vg.rect(px + 4.0F, rowY, pw - 8.0F, rowH, 6.0F, Colors.withAlpha(theme.accent(), 0.16F));
            }
            // Module state dot
            float dotX = px + 18.0F;
            float dotY = rowY + rowH * 0.5F;
            vg.circle(dotX, dotY, 3.6F, m.isEnabled() ? theme.statusEnabled() : theme.statusDisabled());
            if (m.isEnabled()) {
               vg.circleOutline(dotX, dotY, 6.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.5F));
            }
            vg.text(m.getName(), px + 30.0F, dotY, 12.0F, m.isEnabled() ? theme.textPrimary() : theme.textMuted());

            rowY += rowH + ROW_GAP;
         }
         if (rowY <= bodyTop + 10.0F) {
            vg.text("No matching modules", px + 16.0F, bodyTop + 18.0F, 11.0F, theme.textDisabled());
         }
         vg.restore();
      }
   }

   // =========================================================================
   // INPUT
   // =========================================================================

   public boolean mouseClicked(float mx, float my, int btn) {
      if (btn == 0 && this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize)) {
         this.state.setSixSevenPortOpen(false);
         this.closeRequested = true;
         return true;
      }
      if (this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 22.0F) {
            this.search.setLength(0);
         } else {
            this.searchFocused = true;
         }
         return true;
      }

      // Hit-test recorded regions (topmost last) — module rows first, then category headers.
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         Hit h = this.hits.get(i);
         if (this.hit(mx, my, h.x, h.y, h.w, h.h)) {
            if (h.module) {
               ((Module) h.target).toggle();
            } else {
               this.toggleCategory((Category) h.target);
            }
            return true;
         }
      }
      // Window header drag
      if (this.hit(mx, my, this.x, this.y, this.w, HEADER_H)) {
         this.draggingHeader = true;
         this.dragOffX = mx - this.x;
         this.dragOffY = my - this.y;
         return true;
      }
      return false;
   }

   public void mouseDragged(float mx, float my) {
      if (this.draggingHeader) {
         this.customMoved = true;
         this.customX = mx - this.dragOffX;
         this.customY = my - this.dragOffY;
      }
   }

   public void mouseReleased() {
      this.draggingHeader = false;
   }

   public boolean mouseScrolled(float mx, float my, double scrollY) {
      if (this.hit(mx, my, this.x, this.y + HEADER_H, this.w, this.h - HEADER_H) && this.maxScroll > 0.0F) {
         this.scrollAnim.setTarget(Math.clamp(this.scrollAnim.getTarget() - (float) scrollY * 40.0F, 0.0F, this.maxScroll));
         return true;
      }
      return false;
   }

   public boolean keyPressed(int keyCode) {
      if (this.searchFocused) {
         if (keyCode == 256) {
            this.search.setLength(0);
            this.searchFocused = false;
         } else if (keyCode == 259 && !this.search.isEmpty()) {
            this.search.deleteCharAt(this.search.length() - 1);
         }
         return true;
      }
      return false;
   }

   public boolean charTyped(int codepoint) {
      if (this.searchFocused && this.search.length() < 32) {
         this.search.append((char) codepoint);
         return true;
      }
      return false;
   }

   public boolean isListening() {
      return this.searchFocused;
   }

   /** Returns true once if the user clicked the close button; resets the flag. */
   public boolean consumeCloseRequest() {
      boolean r = this.closeRequested;
      this.closeRequested = false;
      return r;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
