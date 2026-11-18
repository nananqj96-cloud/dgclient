package dev.aether.gui;

import dev.aether.corz.CorzModuleHandle;
import dev.aether.corz.CorzPortBus;
import dev.aether.gui.widget.BooleanWidget;
import dev.aether.gui.widget.BlockListWidget;
import dev.aether.gui.widget.ColorWidget;
import dev.aether.gui.widget.IconListWidget;
import dev.aether.gui.widget.KeybindWidget;
import dev.aether.gui.widget.ModeWidget;
import dev.aether.gui.widget.SettingWidget;
import dev.aether.gui.widget.SliderWidget;
import dev.aether.gui.widget.StringWidget;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.render.anim.Animation;
import dev.aether.settings.BlockListSetting;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.IconListSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.Setting;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.StringSetting;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The <b>Larp Recode</b> client GUI — a dashboard / clickgui variant with a frosted-glass,
 * five-column layout, a themed background, and a theme dock. It is a separate variant from the
 * Aether Dashboard / Columns / Experimental Minimal GUIs and does not modify them.
 *
 * <p>Four themes are available (Ocean, Cherry Blossom, Green Forest, Charcoal), each with its own
 * palette and procedurally-drawn background. Modules are toggled by clicking a row; expanding a row
 * (chevron) reveals its settings, reusing Aether's real {@link SettingWidget}s so every module —
 * including keybinds — is fully editable here.
 *
 * <p>All geometry is computed during {@link #render} and cached for input, so layout and click
 * regions always match what is drawn. The constructor is trivial (no widget construction) so opening
 * the GUI is unaffected by lazy initialisation.
 */
public final class LarpRecodeGui {
   // ---- Theme palettes (R, G, B) ----
   private static final int[] OCEAN = {0x08, 0x18, 0x38, 0x0C, 0x24, 0x44, 0x14, 0x34, 0x60, 0x2E, 0x6B, 0xD6, 0xB8, 0xD4, 0xF0, 0x88, 0xB8, 0xE0};
   private static final int[] CHERRY = {0x2A, 0x14, 0x20, 0x3A, 0x1C, 0x2A, 0x4A, 0x24, 0x3A, 0xE8, 0x5B, 0xA8, 0xF0, 0xD0, 0xE0, 0xC8, 0x98, 0xB8};
   private static final int[] FOREST = {0x0E, 0x24, 0x18, 0x14, 0x30, 0x20, 0x1C, 0x40, 0x28, 0x4C, 0xC0, 0x6A, 0xC0, 0xE8, 0xC8, 0x88, 0xC8, 0x98};
   private static final int[] CHARCOAL = {0x0A, 0x0A, 0x0C, 0x16, 0x16, 0x1A, 0x20, 0x20, 0x28, 0x8A, 0x8A, 0x92, 0xD0, 0xD0, 0xD8, 0x9A, 0x9A, 0xA2};
   private static final String[] THEME_NAMES = {"Ocean", "Cherry Blossom", "Green Forest", "Charcoal"};
   private static final int[][] THEMES = {OCEAN, CHERRY, FOREST, CHARCOAL};

   private final ModuleManager modules;
   private int themeIndex;
   private String search = "";
   private boolean searchFocused;

   // Column definition: each column holds one or more categories.
   // The last column is the Corz column (folded-in Corz feature set, tagged "corz").
   private static final Category[][] COLUMNS = {
      {Category.COMBAT, Category.RENDER},
      {Category.MOVEMENT, Category.PLAYER},
      {Category.VISUALS},
      {Category.BASEFINDING},
      {Category.DONUT, Category.MISC, Category.CLIENT},
      {}
   };

   // Lazy widget caches (built on first expand), keyed by module.
   private final Map<Module, List<SettingWidget>> moduleWidgets = new LinkedHashMap<>();
   private final Map<Module, Boolean> expanded = new LinkedHashMap<>();

   // Layout caches (recomputed every render, read by input).
   private float headerH;
   private float searchX, searchY, searchW, searchH;
   private float hudBtnX, hudBtnY, hudBtnR;
   private float gearBtnX, gearBtnY, gearBtnR;
   private float dockX, dockY, dockW, dockH;
   private float bodyTop;
   private final List<PanelHit> panelHits = new ArrayList<>();
   private final List<ModuleHit> moduleHits = new ArrayList<>();
   private final List<ThemeHit> themeHits = new ArrayList<>();
   private final List<SettingWidget> activeWidgets = new ArrayList<>();

   // Corz column scroll state.
   private final Animation corzScroll = new Animation(120.0F, 0.0F);
   private float corzMaxScroll;
   private float corzColX, corzColY, corzColW, corzColH;
   private final List<CorzHit> corzHits = new ArrayList<>();

   private static final class PanelHit {
      final int col;
      final float x, y, w, h;
      PanelHit(int col, float x, float y, float w, float h) {
         this.col = col; this.x = x; this.y = y; this.w = w; this.h = h;
      }
   }

   private static final class ModuleHit {
      final Module module;
      final float x, y, w, rowH;
      final float chevX, chevY, chevW, chevH;
      final float settingsTop, settingsBottom;
      ModuleHit(Module module, float x, float y, float w, float rowH,
                float chevX, float chevY, float chevW, float chevH,
                float settingsTop, float settingsBottom) {
         this.module = module;
         this.x = x; this.y = y; this.w = w; this.rowH = rowH;
         this.chevX = chevX; this.chevY = chevY; this.chevW = chevW; this.chevH = chevH;
         this.settingsTop = settingsTop; this.settingsBottom = settingsBottom;
      }
   }

   private static final class ThemeHit {
      final int index;
      final float x, y, w, h;
      ThemeHit(int index, float x, float y, float w, float h) {
         this.index = index; this.x = x; this.y = y; this.w = w; this.h = h;
      }
   }

   private static final class CorzHit {
      final CorzModuleHandle module;
      final float x, y, w, rowH;
      final float tagX, tagY, tagW, tagH;
      CorzHit(CorzModuleHandle module, float x, float y, float w, float rowH,
              float tagX, float tagY, float tagW, float tagH) {
         this.module = module;
         this.x = x; this.y = y; this.w = w; this.rowH = rowH;
         this.tagX = tagX; this.tagY = tagY; this.tagW = tagW; this.tagH = tagH;
      }
   }

   public LarpRecodeGui(ModuleManager modules) {
      this.modules = modules;
   }

   /** Sets the active theme index (used by tests / screenshots). */
   public void setThemeIndex(int index) {
      this.themeIndex = Math.max(0, Math.min(THEME_NAMES.length - 1, index));
   }

   /** Returns the active theme index. */
   public int themeIndex() {
      return this.themeIndex;
   }

   /** Returns the number of available themes. */
   public int themeCount() {
      return THEME_NAMES.length;
   }

   /** Returns the theme name at the given index. */
   public String themeName(int index) {
      return THEME_NAMES[index];
   }

   /** Toggles the expanded (settings-visible) state of a module (used by tests/screenshots). */
   public void toggleExpanded(Module module) {
      Boolean prev = this.expanded.get(module);
      this.expanded.put(module, prev == null || !prev);
   }

   /** Returns whether a module's settings are expanded. */
   public boolean isExpanded(Module module) {
      return Boolean.TRUE.equals(this.expanded.get(module));
   }

   // =========================================================================
   // THEME PALETTE
   // =========================================================================

   private int rgb(int[] p, int i) {
      return (p[i] << 16) | (p[i + 1] << 8) | p[i + 2];
   }

   private int bg() { return rgb(THEMES[themeIndex], 0); }
   private int surface() { return rgb(THEMES[themeIndex], 3); }
   private int surfaceElev() { return rgb(THEMES[themeIndex], 6); }
   private int accent() { return rgb(THEMES[themeIndex], 9); }
   private int text() { return rgb(THEMES[themeIndex], 12); }
   private int textMuted() { return rgb(THEMES[themeIndex], 15); }

   // =========================================================================
   // RENDER
   // =========================================================================

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.panelHits.clear();
      this.moduleHits.clear();
      this.themeHits.clear();
      this.activeWidgets.clear();

      try {
         this.renderBackground(vg, uiW, uiH);
         this.headerH = 64.0F;
         this.renderHeader(vg, mx, my, uiW);
         this.bodyTop = this.headerH + 12.0F;
         this.renderColumns(vg, mx, my, uiW, uiH);
         this.renderCorzColumn(vg, mx, my, uiW, uiH);
         this.renderThemeDock(vg, mx, my, uiW, uiH);
      } catch (Throwable t) {
         dev.aether.AetherClient.LOGGER.error("Larp Recode GUI render failed", t);
      }
   }

   private void renderBackground(NVGRenderer vg, float uiW, float uiH) {
      int[] p = THEMES[themeIndex];
      // Base vertical gradient.
      vg.rectGradient(0.0F, 0.0F, uiW, uiH, 0.0F,
         rgb(p, 0) | 0xFF000000, rgb(p, 3) | 0xFF000000, true);

      long now = System.nanoTime();
      float t = (float)(now % 10000000000L) / 1.0E9F;

      if (themeIndex == 0) {
         // Ocean: light rays + jellyfish + seaweed.
         for (int i = 0; i < 5; i++) {
            float rx = uiW * (0.15F + 0.7F * ((i + 0.5F) / 5.0F)) + (float)Math.sin(t * 0.3F + i) * 20.0F;
            float rw = 60.0F + i * 18.0F;
            vg.rect(rx, 0.0F, rw, uiH, 0.0F, Colors.withAlpha(rgb(p, 9), 0.05F));
         }
         // Jellyfish.
         for (int i = 0; i < 2; i++) {
            float jx = uiW * (0.55F + 0.25F * i) + (float)Math.sin(t * 0.5F + i * 2.0F) * 30.0F;
            float jy = uiH * 0.18F + (float)Math.sin(t * 0.4F + i) * 14.0F;
            vg.circle(jx, jy, 22.0F, Colors.withAlpha(rgb(p, 12), 0.18F));
            vg.circle(jx, jy, 12.0F, Colors.withAlpha(rgb(p, 12), 0.28F));
            for (int l = 0; l < 5; l++) {
               float ly = jy + 22.0F + l * 10.0F;
               float lx = jx + (float)Math.sin(t * 2.0F + l + i) * 8.0F;
               vg.line(jx, ly - 10.0F, lx, ly, 2.0F, Colors.withAlpha(rgb(p, 12), 0.14F));
            }
         }
         // Seaweed at the bottom.
         for (int i = 0; i < 10; i++) {
            float sx = uiW * (i / 10.0F) + 20.0F;
            float sy = uiH - 40.0F;
            for (int s = 0; s < 6; s++) {
               float yy = sy - s * 18.0F;
               float xx = sx + (float)Math.sin(t * 1.5F + i + s) * 6.0F;
               vg.line(sx, sy - (s - 1) * 18.0F, xx, yy, 3.0F, Colors.withAlpha(rgb(p, 3), 0.5F));
               sx = xx;
            }
         }
      } else if (themeIndex == 1) {
         // Cherry blossom: branches + drifting petals.
         for (int i = 0; i < 6; i++) {
            float bx = uiW * (i * 0.2F);
            float by = 0.0F;
            float ex = bx + 80.0F + (float)Math.sin(i) * 40.0F;
            float ey = 180.0F + (float)Math.cos(i) * 30.0F;
            vg.line(bx, by, ex, ey, 3.0F, Colors.withAlpha(rgb(p, 3), 0.6F));
            for (int b = 0; b < 4; b++) {
               float bx2 = bx + (ex - bx) * (b + 1) / 5.0F;
               float by2 = by + (ey - by) * (b + 1) / 5.0F;
               vg.line(bx2, by2, bx2 + 30.0F, by2 - 20.0F, 2.0F, Colors.withAlpha(rgb(p, 3), 0.4F));
               vg.circle(bx2 + 30.0F, by2 - 20.0F, 4.0F, Colors.withAlpha(rgb(p, 12), 0.7F));
            }
         }
         // Drifting petals.
         for (int i = 0; i < 18; i++) {
            float px = (float)((i * 137.5F + t * 30.0F * (i % 3 + 1)) % uiW);
            float py = (float)((t * 20.0F * (i % 4 + 1) + i * 90.0F) % uiH);
            vg.circle(px, py, 3.5F, Colors.withAlpha(rgb(p, 12), 0.55F));
         }
      } else if (themeIndex == 2) {
         // Green forest: light rays + bokeh.
         for (int i = 0; i < 5; i++) {
            float rx = uiW * (0.1F + 0.8F * (i / 5.0F));
            vg.rect(rx, 0.0F, 50.0F, uiH, 0.0F, Colors.withAlpha(rgb(p, 9), 0.05F));
         }
         for (int i = 0; i < 14; i++) {
            float bx = (i * 73.0F + t * 10.0F) % uiW;
            float by = (i * 113.0F + t * 6.0F) % uiH;
            float r = 10.0F + (i % 4) * 8.0F;
            vg.circleGlow(bx, by, r, r * 2.0F, Colors.withAlpha(rgb(p, 9), 0.12F));
         }
      } else {
         // Charcoal: faint vignette + grid.
         vg.rect(0.0F, 0.0F, uiW, uiH, 0.0F, Colors.withAlpha(0x000000, 0.0F));
         for (int gx = 0; gx < uiW; gx += 48.0F) {
            vg.line(gx, 0.0F, gx, uiH, 1.0F, Colors.withAlpha(rgb(p, 9), 0.04F));
         }
         for (int gy = 0; gy < uiH; gy += 48.0F) {
            vg.line(0.0F, gy, uiW, gy, 1.0F, Colors.withAlpha(rgb(p, 9), 0.04F));
         }
      }
      // Top + bottom vignette for depth.
      vg.rect(0.0F, 0.0F, uiW, 40.0F, 0.0F, Colors.withAlpha(rgb(p, 0), 0.5F));
      vg.rect(0.0F, uiH - 70.0F, uiW, 70.0F, 0.0F, Colors.withAlpha(rgb(p, 0), 0.55F));
   }

   private void renderHeader(NVGRenderer vg, float mx, float my, float uiW) {
      // Header background.
      vg.rect(0.0F, 0.0F, uiW, this.headerH, 0.0F, Colors.withAlpha(bg(), 0.75F));
      vg.rectOutline(0.0F, this.headerH - 1.0F, uiW, 1.0F, 0.0F, 1.0F, Colors.withAlpha(accent(), 0.3F));

      // LR logo tile + title.
      float lx = 18.0F;
      float ly = 16.0F;
      vg.rect(lx, ly, 32.0F, 32.0F, 7.0F, Colors.withAlpha(accent(), 0.22F));
      vg.rectOutline(lx, ly, 32.0F, 32.0F, 7.0F, 1.0F, Colors.withAlpha(accent(), 0.5F));
      vg.text("LR", lx + 16.0F, ly + 22.0F, 13.0F, text());
      vg.text("LARP RECODE", lx + 42.0F, ly + 14.0F, 15.0F, text());
      vg.text("BETA · 1.21.11", lx + 42.0F, ly + 28.0F, 9.0F, textMuted());

      // Search bar (top center).
      this.searchH = 32.0F;
      this.searchW = Math.min(360.0F, uiW * 0.4F);
      this.searchX = (uiW - this.searchW) * 0.5F;
      this.searchY = (this.headerH - this.searchH) * 0.5F;
      boolean sHov = this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH);
      vg.rect(this.searchX, this.searchY, this.searchW, this.searchH, 16.0F, Colors.withAlpha(surfaceElev(), 0.85F));
      vg.rectOutline(this.searchX, this.searchY, this.searchW, this.searchH, 16.0F, 1.0F,
         this.searchFocused ? Colors.withAlpha(accent(), 0.6F) : Colors.withAlpha(accent(), 0.25F));
      float scy = this.searchY + this.searchH * 0.5F;
      vg.text("SEARCH MODULES · CTRL+F", this.searchX + 16.0F, scy, 10.0F,
         (!this.search.isEmpty() || this.searchFocused) ? text() : textMuted());
      if (!this.search.isEmpty()) {
         vg.textTruncated(this.search, this.searchX + 16.0F, scy, 11.0F, text(), this.searchW - 40.0F);
         vg.cross(this.searchX + this.searchW - 18.0F, scy - 5.0F, 10.0F, 1.4F, textMuted());
      }

      // HUD + gear round buttons (top right).
      this.hudBtnR = 14.0F;
      this.hudBtnX = uiW - 30.0F - this.hudBtnR * 2.0F - 12.0F;
      this.hudBtnY = this.headerH * 0.5F;
      boolean hHov = this.hit(mx, my, this.hudBtnX - this.hudBtnR, this.hudBtnY - this.hudBtnR, this.hudBtnR * 2.0F, this.hudBtnR * 2.0F);
      vg.circle(this.hudBtnX, this.hudBtnY, this.hudBtnR, Colors.withAlpha(surfaceElev(), hHov ? 0.95F : 0.7F));
      vg.circleOutline(this.hudBtnX, this.hudBtnY, this.hudBtnR, 1.0F, Colors.withAlpha(accent(), hHov ? 0.6F : 0.3F));
      vg.text("H", this.hudBtnX, this.hudBtnY + 4.0F, 11.0F, hHov ? accent() : text());

      this.gearBtnR = 14.0F;
      this.gearBtnX = uiW - 30.0F;
      this.gearBtnY = this.headerH * 0.5F;
      boolean gHov = this.hit(mx, my, this.gearBtnX - this.gearBtnR, this.gearBtnY - this.gearBtnR, this.gearBtnR * 2.0F, this.gearBtnR * 2.0F);
      vg.circle(this.gearBtnX, this.gearBtnY, this.gearBtnR, Colors.withAlpha(surfaceElev(), gHov ? 0.95F : 0.7F));
      vg.circleOutline(this.gearBtnX, this.gearBtnY, this.gearBtnR, 1.0F, Colors.withAlpha(accent(), gHov ? 0.6F : 0.3F));
      vg.text("G", this.gearBtnX, this.gearBtnY + 4.0F, 11.0F, gHov ? accent() : text());
   }

   private void renderColumns(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      float gap = 12.0F;
      float sidePad = 14.0F;
      float colW = (uiW - sidePad * 2.0F - gap * (COLUMNS.length - 1)) / (float) COLUMNS.length;
      float dockH = 70.0F;
      float availH = uiH - this.bodyTop - dockH - 16.0F;

      for (int c = 0; c < COLUMNS.length; c++) {
         float cx = sidePad + c * (colW + gap);
         float cy = this.bodyTop;
         for (Category cat : COLUMNS[c]) {
            List<Module> inCat = this.modules.inCategory(cat);
            boolean hasVisible = false;
            for (Module m : inCat) {
               if (this.matchesSearch(m)) {
                  hasVisible = true;
                  break;
               }
            }
            if (!hasVisible) {
               continue;
            }
            float panelH = this.panelHeight(cat, colW);
            if (cy + panelH > this.bodyTop + availH) {
               break;
            }
            this.renderPanel(vg, mx, my, c, cx, cy, colW, panelH, cat, inCat);
            cy += panelH + gap;
         }
      }
   }

   private float panelHeight(Category cat, float colW) {
      List<Module> inCat = this.modules.inCategory(cat);
      float h = 44.0F; // header
      int visible = 0;
      for (Module m : inCat) {
         if (!this.matchesSearch(m)) {
            continue;
         }
         visible++;
         boolean exp = Boolean.TRUE.equals(this.expanded.get(m));
         h += 30.0F;
         if (exp) {
            h += this.settingsHeight(this.widgetsFor(m), null) + 4.0F;
         }
      }
      return h + 8.0F;
   }

   // =========================================================================
   // CORZ COLUMN (folded-in Corz feature set, tagged "corz")
   // =========================================================================

   private List<CorzModuleHandle> corzModules() {
      if (!CorzPortBus.isAvailable()) {
         return List.of();
      }
      var provider = CorzPortBus.provider();
      if (provider == null) {
         return List.of();
      }
      List<CorzModuleHandle> all = provider.allModules();
      // Filter by the active search term (matches display name).
      if (this.search.isEmpty()) {
         return all;
      }
      List<CorzModuleHandle> out = new ArrayList<>();
      for (CorzModuleHandle m : all) {
         if (m.displayName().toLowerCase(Locale.ROOT).contains(this.search.toLowerCase(Locale.ROOT))) {
            out.add(m);
         }
      }
      return out;
   }

   private void renderCorzColumn(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.corzHits.clear();
      var provider = CorzPortBus.isAvailable() ? CorzPortBus.provider() : null;
      if (provider == null) {
         return;
      }
      float gap = 12.0F;
      float sidePad = 14.0F;
      float colW = (uiW - sidePad * 2.0F - gap * (COLUMNS.length - 1)) / (float) COLUMNS.length;
      float dockH = 70.0F;
      float availH = uiH - this.bodyTop - dockH - 16.0F;
      float cx = sidePad + (COLUMNS.length - 1) * (colW + gap);
      float cy = this.bodyTop;

      List<CorzModuleHandle> mods = this.corzModules();
      int count = mods.size();
      float headerH = 44.0F;
      float rowH = 30.0F;
      float contentH = headerH + count * (rowH + 4.0F) + 8.0F;
      this.corzMaxScroll = Math.max(0.0F, contentH - availH);
      this.corzScroll.setTarget(Math.clamp(this.corzScroll.getTarget(), 0.0F, this.corzMaxScroll));
      this.corzColX = cx;
      this.corzColY = cy;
      this.corzColW = colW;
      this.corzColH = availH;

      // Frosted-glass panel.
      vg.rect(cx, cy, colW, availH, 12.0F, Colors.withAlpha(surface(), 0.78F));
      vg.rectOutline(cx, cy, colW, availH, 12.0F, 1.0F, Colors.withAlpha(accent(), 0.18F));

      // Header: "CORZ" title + count pill.
      float ix = cx + 12.0F;
      float iy = cy + 10.0F;
      vg.rect(ix, iy, 24.0F, 24.0F, 6.0F, Colors.withAlpha(accent(), 0.25F));
      vg.text("C", ix + 16.0F, iy + 17.0F, 12.0F, accent());
      vg.text("CORZ", ix + 32.0F, iy + 17.0F, 12.0F, text());
      if (count > 0) {
         String cnt = Integer.toString(count);
         float cw = vg.textWidth(cnt, 9.0F) + 12.0F;
         float cpx = cx + colW - cw - 30.0F;
         vg.rect(cpx, iy + 4.0F, cw, 16.0F, 8.0F, Colors.withAlpha(accent(), 0.2F));
         vg.text(cnt, cpx + 6.0F, iy + 15.0F, 9.0F, accent());
      }
      vg.line(cx + 10.0F, cy + headerH, cx + colW - 10.0F, cy + headerH, 1.0F, Colors.withAlpha(accent(), 0.15F));

      // Module rows (scrolled).
      float scrollY = this.corzScroll.value();
      vg.save();
      vg.scissor(cx + 6.0F, cy + headerH, colW - 12.0F, availH - headerH - 6.0F);
      float ry = cy + headerH + 6.0F - scrollY;
      for (CorzModuleHandle mod : mods) {
         if (ry + rowH < cy + headerH) {
            ry += rowH + 4.0F;
            continue;
         }
         if (ry > cy + availH) {
            break;
         }
         boolean enabled = mod.isEnabled();
         boolean hov = this.hit(mx, my, cx + 6.0F, ry, colW - 12.0F, rowH);
         int rowCol = enabled ? Colors.withAlpha(accent(), hov ? 0.32F : 0.22F)
            : Colors.withAlpha(surfaceElev(), hov ? 0.5F : 0.35F);
         vg.rect(cx + 6.0F, ry, colW - 12.0F, rowH, 7.0F, rowCol);
         if (enabled) {
            vg.rect(cx + 6.0F, ry + 4.0F, 3.0F, rowH - 8.0F, 1.5F, Colors.withAlpha(0xFFFFFF, 0.85F));
         }
         // Truncate the display name to fit before the tag.
         float textMaxW = colW - 12.0F - 56.0F;
         vg.textTruncated(mod.displayName().toUpperCase(Locale.ROOT), cx + 18.0F, ry + rowH * 0.5F, 11.0F,
            enabled ? text() : textMuted(), textMaxW);

         // "corz" tag pill on the right.
         float tagW = 38.0F;
         float tagX = cx + colW - 6.0F - tagW;
         float tagY = ry + rowH * 0.5F - 7.0F;
         vg.rect(tagX, tagY, tagW, 14.0F, 7.0F, Colors.withAlpha(accent(), 0.22F));
         vg.text("corz", tagX + tagW * 0.5F - vg.textWidth("corz", 8.0F) * 0.5F, tagY + 10.0F, 8.0F, accent());
         this.corzHits.add(new CorzHit(mod, cx + 6.0F, ry, colW - 12.0F, rowH, tagX, tagY, tagW, 14.0F));

         // Keybind letter on the far right.
         if (mod.getBindKey() >= 0) {
            String k = keyName(mod.getBindKey());
            vg.text(k, cx + colW - 44.0F, ry + rowH * 0.5F, 10.0F, Colors.withAlpha(accent(), 0.8F));
         }
         ry += rowH + 4.0F;
      }
      vg.restore();

      // Scrollbar thumb.
      if (this.corzMaxScroll > 1.0F) {
         float trackH = availH - headerH - 12.0F;
         float thumbH = Math.max(20.0F, trackH * (availH / contentH));
         float thumbY = cy + headerH + 6.0F + (trackH - thumbH) * (scrollY / this.corzMaxScroll);
         vg.rect(cx + colW - 6.0F, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(accent(), 0.45F));
      }
   }

   private static String keyName(int key) {
      if (key < 0) {
         return "";
      }
      if (key >= 290 && key <= 314) {
         return "F" + (key - 290 + 1);
      }
      return Integer.toString(key);
   }

   private void renderPanel(NVGRenderer vg, float mx, float my, int col, float x, float y, float w, float h,
                            Category cat, List<Module> inCat) {
      // Frosted-glass panel.
      vg.rect(x, y, w, h, 12.0F, Colors.withAlpha(surface(), 0.78F));
      vg.rectOutline(x, y, w, h, 12.0F, 1.0F, Colors.withAlpha(accent(), 0.18F));

      // Header: icon tile + title + count pill + chevron.
      float hh = 44.0F;
      float ix = x + 12.0F;
      float iy = y + 10.0F;
      vg.rect(ix, iy, 24.0F, 24.0F, 6.0F, Colors.withAlpha(accent(), 0.25F));
      vg.text(cat.getDisplayName().substring(0, 1), ix + 16.0F, iy + 17.0F, 12.0F, accent());
      vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT), ix + 32.0F, iy + 17.0F, 12.0F, text());

      int count = 0;
      for (Module m : inCat) {
         if (this.matchesSearch(m)) {
            count++;
         }
      }
      if (count > 0) {
         String cnt = Integer.toString(count);
         float cw = vg.textWidth(cnt, 9.0F) + 12.0F;
         float cpx = x + w - cw - 30.0F;
         vg.rect(cpx, iy + 4.0F, cw, 16.0F, 8.0F, Colors.withAlpha(accent(), 0.2F));
         vg.text(cnt, cpx + 6.0F, iy + 15.0F, 9.0F, accent());
      }
      // Chevron (collapse) — right of header.
      float chX = x + w - 18.0F;
      float chY = y + hh * 0.5F;
      vg.chevron(chX, chY, 4.0F, 1.6F, textMuted(), false);
      vg.line(x + 10.0F, y + hh, x + w - 10.0F, y + hh, 1.0F, Colors.withAlpha(accent(), 0.15F));

      this.panelHits.add(new PanelHit(col, x, y, w, hh));

      // Module rows.
      float ry = y + hh + 6.0F;
      for (Module module : inCat) {
         if (!this.matchesSearch(module)) {
            continue;
         }
         boolean exp = Boolean.TRUE.equals(this.expanded.get(module));
         float rowH = 30.0F;
         float settingsH = exp ? this.settingsHeight(this.widgetsFor(module), vg) + 4.0F : 0.0F;
         float totalH = rowH + settingsH;
         if (ry + totalH > y + h) {
            break;
         }
         boolean enabled = module.isEnabled();
         boolean hov = this.hit(mx, my, x + 6.0F, ry, w - 12.0F, rowH);

         // Row: enabled = accent fill + white left bar; disabled = subtle.
         int rowCol = enabled ? Colors.withAlpha(accent(), hov ? 0.32F : 0.22F)
            : Colors.withAlpha(surfaceElev(), hov ? 0.5F : 0.35F);
         vg.rect(x + 6.0F, ry, w - 12.0F, rowH, 7.0F, rowCol);
         if (enabled) {
            vg.rect(x + 6.0F, ry + 4.0F, 3.0F, rowH - 8.0F, 1.5F, Colors.withAlpha(0xFFFFFF, 0.85F));
         }
         vg.text(module.getName().toUpperCase(Locale.ROOT), x + 18.0F, ry + rowH * 0.5F, 11.0F,
            enabled ? text() : textMuted());

         // Chevron to expand settings.
         float chevCX = x + w - 22.0F;
         float chevCY = ry + rowH * 0.5F;
         vg.save();
         vg.translate(chevCX, chevCY);
         vg.rotate(exp ? (float)(Math.PI * 0.5) : 0.0F);
         vg.chevron(0.0F, 0.0F, 4.0F, 1.6F, exp ? text() : textMuted(), false);
         vg.restore();
         float chevX = chevCX - 8.0F;
         float chevY = chevCY - 8.0F;

         // Keybind letter on the right (if bound).
         if (module.getKeybind().isBound()) {
            String k = module.getKeybind().keyName();
            vg.text(k, x + w - 34.0F, ry + rowH * 0.5F, 10.0F, Colors.withAlpha(accent(), 0.8F));
         }

         this.moduleHits.add(new ModuleHit(module, x + 6.0F, ry, w - 12.0F, rowH,
            chevX, chevY, 16.0F, 16.0F, ry + rowH, ry + totalH));

         // Expanded settings.
         if (exp) {
            vg.line(x + 10.0F, ry + rowH, x + w - 10.0F, ry + rowH, 1.0F, Colors.withAlpha(accent(), 0.12F));
            float wy = ry + rowH + 6.0F;
            float innerW = w - 24.0F;
            for (SettingWidget w2 : this.widgetsFor(module)) {
               if (!w2.isVisible()) {
                  continue;
               }
               w2.setBounds(x + 12.0F, wy, innerW);
               w2.render(vg, mx, my);
               this.activeWidgets.add(w2);
               wy += w2.height(vg) + 3.0F;
            }
         }
         ry += totalH + 4.0F;
      }
   }

   private void renderThemeDock(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.dockH = 56.0F;
      this.dockW = Math.min(560.0F, uiW - 80.0F);
      this.dockX = (uiW - this.dockW) * 0.5F;
      this.dockY = uiH - this.dockH - 12.0F;
      vg.rect(this.dockX, this.dockY, this.dockW, this.dockH, 28.0F, Colors.withAlpha(surface(), 0.8F));
      vg.rectOutline(this.dockX, this.dockY, this.dockW, this.dockH, 28.0F, 1.0F, Colors.withAlpha(accent(), 0.25F));

      int n = THEME_NAMES.length;
      float thumbW = (this.dockW - 24.0F - (n - 1) * 10.0F) / (float) n;
      float thumbH = this.dockH - 20.0F;
      float ty = this.dockY + 10.0F;
      for (int i = 0; i < n; i++) {
         float tx = this.dockX + 12.0F + i * (thumbW + 10.0F);
         boolean sel = i == this.themeIndex;
         boolean hov = this.hit(mx, my, tx, ty, thumbW, thumbH);
         int[] tp = THEMES[i];
         vg.rect(tx, ty, thumbW, thumbH, 10.0F,
            sel ? Colors.withAlpha(accent(), 0.25F) : Colors.withAlpha(rgb(tp, 3), 0.6F));
         vg.rectOutline(tx, ty, thumbW, thumbH, 10.0F, 1.0F,
            sel ? Colors.withAlpha(accent(), 0.7F) : Colors.withAlpha(accent(), 0.2F));
         // Thumbnail swatch.
         vg.rect(tx + 6.0F, ty + 6.0F, thumbW - 12.0F, thumbH * 0.55F, 6.0F, Colors.withAlpha(rgb(tp, 6), 0.8F));
         vg.text(THEME_NAMES[i], tx + (thumbW - vg.textWidth(THEME_NAMES[i], 8.0F)) * 0.5F, ty + thumbH - 8.0F, 8.0F,
            sel ? accent() : textMuted());
         if (sel) {
            vg.circle(tx + thumbW * 0.5F, ty + thumbH + 3.0F, 2.4F, accent());
         }
         this.themeHits.add(new ThemeHit(i, tx, ty, thumbW, thumbH));
      }
   }

   // =========================================================================
   // WIDGETS (lazy)
   // =========================================================================

   private List<SettingWidget> widgetsFor(Module m) {
      List<SettingWidget> ws = this.moduleWidgets.get(m);
      if (ws != null) {
         return ws;
      }
      ws = new ArrayList<>();
      dev.aether.theme.ThemeManager flat = flatThemes();
      for (Setting<?> s : m.getSettings()) {
         if (s instanceof BooleanSetting b) ws.add(new BooleanWidget(flat, b));
         else if (s instanceof SliderSetting sl) ws.add(new SliderWidget(flat, sl));
         else if (s instanceof ModeSetting mo) ws.add(new ModeWidget(flat, mo));
         else if (s instanceof ColorSetting c) ws.add(new ColorWidget(flat, c));
         else if (s instanceof BlockListSetting bl) ws.add(new BlockListWidget(flat, bl));
         else if (s instanceof IconListSetting il) ws.add(new IconListWidget(flat, il));
         else if (s instanceof StringSetting st) ws.add(new StringWidget(flat, st));
         else if (s instanceof KeybindSetting kb) ws.add(new KeybindWidget(flat, kb));
      }
      ws.add(new KeybindWidget(flat, m.getKeybind()));
      this.moduleWidgets.put(m, ws);
      return ws;
   }

   private dev.aether.theme.ThemeManager flatThemes;

   private dev.aether.theme.ThemeManager flatThemes() {
      if (this.flatThemes == null) {
         dev.aether.theme.ThemeManager tm = new dev.aether.theme.ThemeManager();
         dev.aether.theme.Theme flat = new dev.aether.theme.Theme("Minimal", 0xFFFFFFFF, false);
         tm.getThemes().add(flat);
         tm.select(flat);
         this.flatThemes = tm;
      }
      return this.flatThemes;
   }

   private float settingsHeight(List<SettingWidget> ws, NVGRenderer vg) {
      float h = 6.0F;
      for (SettingWidget w : ws) {
         if (w.isVisible()) {
            h += w.height(vg) + 3.0F;
         }
      }
      return h;
   }

   private boolean matchesSearch(Module m) {
      if (this.search.isEmpty()) {
         return true;
      }
      return m.getName().toLowerCase(Locale.ROOT).contains(this.search.toLowerCase(Locale.ROOT));
   }

   // =========================================================================
   // INPUT
   // =========================================================================

   public boolean mouseClicked(float mx, float my, int btn) {
      if (btn != 0) {
         return false;
      }
      // Corz column rows (toggle).
      for (int i = this.corzHits.size() - 1; i >= 0; i--) {
         CorzHit ch = this.corzHits.get(i);
         if (this.hit(mx, my, ch.x, ch.y, ch.w, ch.rowH)) {
            ch.module.toggle();
            UiSounds.toggle(ch.module.isEnabled());
            return true;
         }
      }
      // Theme dock.
      for (int i = this.themeHits.size() - 1; i >= 0; i--) {
         ThemeHit th = this.themeHits.get(i);
         if (this.hit(mx, my, th.x, th.y, th.w, th.h)) {
            this.themeIndex = th.index;
            UiSounds.select();
            return true;
         }
      }
      // Search bar.
      if (this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 22.0F) {
            this.search = "";
         } else {
            this.searchFocused = true;
         }
         return true;
      }
      this.searchFocused = false;
      // HUD / gear buttons (no-op placeholders for now).
      if (this.hit(mx, my, this.hudBtnX - this.hudBtnR, this.hudBtnY - this.hudBtnR, this.hudBtnR * 2.0F, this.hudBtnR * 2.0F)) {
         return true;
      }
      if (this.hit(mx, my, this.gearBtnX - this.gearBtnR, this.gearBtnY - this.gearBtnR, this.gearBtnR * 2.0F, this.gearBtnR * 2.0F)) {
         return true;
      }
      // Module rows: chevron expands, body toggles, settings capture.
      for (int i = this.moduleHits.size() - 1; i >= 0; i--) {
         ModuleHit mh = this.moduleHits.get(i);
         if (this.hit(mx, my, mh.chevX, mh.chevY, mh.chevW, mh.chevH)) {
            Boolean prev = this.expanded.get(mh.module);
            this.expanded.put(mh.module, prev == null || !prev);
            UiSounds.select();
            return true;
         }
         if (this.hit(mx, my, mh.x, mh.settingsTop, mh.w, mh.settingsBottom - mh.settingsTop)) {
            for (SettingWidget w : this.widgetsFor(mh.module)) {
               if (w.isVisible() && w.mouseClicked(mx, my, btn)) {
                  return true;
               }
            }
         }
         if (this.hit(mx, my, mh.x, mh.y, mh.w, mh.rowH)) {
            mh.module.toggle();
            UiSounds.toggle(mh.module.isEnabled());
            return true;
         }
      }
      return true;
   }

   public void mouseDragged(float mx, float my) {
      for (SettingWidget w : this.activeWidgets) {
         w.mouseDragged(mx, my);
      }
   }

   /** Handles mouse-wheel scrolling over the Corz column. */
   public void mouseScrolled(float mx, float my, float scrollY) {
      if (this.corzMaxScroll > 0.0F && this.hit(mx, my, this.corzColX, this.corzColY, this.corzColW, this.corzColH)) {
         this.corzScroll.setTarget(Math.clamp(this.corzScroll.getTarget() + scrollY * 22.0F, 0.0F, this.corzMaxScroll));
      }
   }

   public void mouseReleased() {
      for (SettingWidget w : this.activeWidgets) {
         w.mouseReleased();
      }
   }

   public boolean keyPressed(int keyCode) {
      for (SettingWidget w : this.activeWidgets) {
         if (w.isListening() && w.keyPressed(keyCode)) {
            return true;
         }
      }
      if (keyCode == 259 && !this.search.isEmpty()) {
         this.search = this.search.substring(0, this.search.length() - 1);
         return true;
      }
      if (keyCode == 257 || keyCode == 335) {
         this.searchFocused = false;
         return true;
      }
      return false;
   }

   public boolean charTyped(int codepoint) {
      for (SettingWidget w : this.activeWidgets) {
         if (w.isListening() && w.charTyped(codepoint)) {
            return true;
         }
      }
      if (!this.searchFocused) {
         return false;
      }
      if (this.search.length() < 32) {
         this.search += Character.toLowerCase(codepoint);
      }
      return true;
   }

   public boolean isListening() {
      for (SettingWidget w : this.activeWidgets) {
         if (w.isListening()) {
            return true;
         }
      }
      return false;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
