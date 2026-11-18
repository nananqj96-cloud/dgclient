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
 * The <b>Larp Recode v2</b> client GUI — a modern dashboard with a frosted-glass sidebar,
 * category navigation, animated module cards, a live search bar, and a theme dock.
 *
 * <p>Designed after studying real client UIs (Meteor, LiquidBounce, Gloss): clean dark
 * surfaces, subtle glow accents, smooth easing animations, and a left sidebar for category
 * navigation. Modules toggle by clicking a card; expanding reveals settings inline, reusing
 * Aether's real {@link SettingWidget}s so every module — including keybinds — is fully editable.
 *
 * <p>All geometry is computed during {@link #render} and cached for input, so layout and click
 * regions always match what is drawn. The constructor is trivial so opening the GUI is fast.
 */
public final class LarpRecodeGui {
   // ---- Theme palettes: [bg_top, bg_bottom, surface, surface_elev, accent, accent_bright, text, text_muted, header] ----
   // Each color is (R, G, B). 9 colors × 3 = 27 elements.
   private static final int[] OCEAN = {
      0x0A, 0x1E, 0x3F,   // bg_top
      0x08, 0x14, 0x2A,   // bg_bottom
      0x14, 0x28, 0x50,   // surface
      0x1C, 0x38, 0x68,   // surface_elev
      0x48, 0x8E, 0xDA,   // accent
      0x7A, 0xC8, 0xF0,   // accent_bright
      0xE8, 0xF0, 0xFF,   // text
      0x9A, 0xB8, 0xD8,   // text_muted
      0x48, 0x8E, 0xDA    // header
   };
   private static final int[] CHERRY = {
      0x1E, 0x12, 0x1E,   // bg_top
      0x14, 0x0C, 0x16,   // bg_bottom
      0x2A, 0x1C, 0x2E,   // surface
      0x3A, 0x24, 0x3E,   // surface_elev
      0xE8, 0x5B, 0xA8,   // accent
      0xF0, 0x9A, 0xD0,   // accent_bright
      0xFF, 0xF0, 0xF4,   // text
      0xC8, 0x98, 0xB8,   // text_muted
      0xE6, 0xD8, 0xDD    // header
   };
   private static final int[] FOREST = {
      0x0A, 0x1A, 0x10,   // bg_top
      0x06, 0x10, 0x0A,   // bg_bottom
      0x14, 0x28, 0x1C,   // surface
      0x1C, 0x40, 0x28,   // surface_elev
      0x4A, 0xBF, 0x6A,   // accent
      0x88, 0xE8, 0xA8,   // accent_bright
      0xE8, 0xF0, 0xE0,   // text
      0x98, 0xC8, 0x98,   // text_muted
      0x4A, 0xBF, 0x6A    // header
   };
   private static final int[] CHARCOAL = {
      0x0C, 0x0C, 0x10,   // bg_top
      0x08, 0x08, 0x0C,   // bg_bottom
      0x14, 0x14, 0x1A,   // surface
      0x1C, 0x1C, 0x22,   // surface_elev
      0x8A, 0x8A, 0x92,   // accent
      0xC0, 0xC0, 0xD0,   // accent_bright
      0xE0, 0xE0, 0xE8,   // text
      0x9A, 0x9A, 0xA2,   // text_muted
      0x3C, 0x3C, 0x42    // header
   };
   private static final String[] THEME_NAMES = {"Ocean", "Cherry Blossom", "Green Forest", "Charcoal"};
   private static final int[][] THEMES = {OCEAN, CHERRY, FOREST, CHARCOAL};

   // Sidebar category order
   private static final Category[] SIDEBAR_CATEGORIES = {
      Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.VISUALS,
      Category.RENDER, Category.BASEFINDING, Category.DONUT, Category.MISC, Category.CLIENT
   };

   private final ModuleManager modules;
   private int themeIndex;
   private String search = "";
   private boolean searchFocused;
   private int activeCategory = 0; // index into SIDEBAR_CATEGORIES
   private boolean sidebarCollapsed;

   // Lazy widget caches
   private final Map<Module, List<SettingWidget>> moduleWidgets = new LinkedHashMap<>();
   private final Map<Module, Boolean> expanded = new LinkedHashMap<>();
   private final Map<Module, Float> openAnim = new LinkedHashMap<>();
   private final Map<Module, Float> toggleAnim = new LinkedHashMap<>();

   // Layout caches
   private float headerH;
   private float sidebarX, sidebarW, sidebarY, sidebarH;
   private float bodyX, bodyY, bodyW, bodyH;
   private float searchX, searchY, searchW, searchH;
   private float dockX, dockY, dockW, dockH;
   private float bodyTop;
   private float catButtonY;
   private final List<CatHit> catHits = new ArrayList<>();
   private final List<ModuleHit> moduleHits = new ArrayList<>();
   private final List<ThemeHit> themeHits = new ArrayList<>();
   private final List<SettingWidget> activeWidgets = new ArrayList<>();

   // Corz panel state
   private final Animation corzScroll = new Animation(120.0F, 0.0F);
   private float corzMaxScroll;
   private float corzX, corzY, corzW, corzH;
   private final List<CorzHit> corzHits = new ArrayList<>();

   private static final class CatHit {
      final int index;
      final float x, y, w, h;
      CatHit(int index, float x, float y, float w, float h) {
         this.index = index; this.x = x; this.y = y; this.w = w; this.h = h;
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
      CorzHit(CorzModuleHandle module, float x, float y, float w, float rowH) {
         this.module = module; this.x = x; this.y = y; this.w = w; this.rowH = rowH;
      }
   }

   public LarpRecodeGui(ModuleManager modules) {
      this.modules = modules;
   }

   public void setThemeIndex(int index) {
      this.themeIndex = Math.max(0, Math.min(THEME_NAMES.length - 1, index));
   }

   public int themeIndex() { return this.themeIndex; }
   public int themeCount() { return THEME_NAMES.length; }
   public String themeName(int index) { return THEME_NAMES[index]; }

   public void toggleExpanded(Module module) {
      Boolean prev = this.expanded.get(module);
      this.expanded.put(module, prev == null || !prev);
   }

   public boolean isExpanded(Module module) {
      return Boolean.TRUE.equals(this.expanded.get(module));
   }

   // =========================================================================
   // THEME PALETTE
   // =========================================================================

   private int rgb(int[] p, int i) {
      return (p[i] << 16) | (p[i + 1] << 8) | p[i + 2];
   }

   private int bgTop() { return rgb(THEMES[themeIndex], 0); }
   private int bgBottom() { return rgb(THEMES[themeIndex], 1); }
   private int surface() { return rgb(THEMES[themeIndex], 2); }
   private int surfaceElev() { return rgb(THEMES[themeIndex], 3); }
   private int accent() { return rgb(THEMES[themeIndex], 4); }
   private int accentBright() { return rgb(THEMES[themeIndex], 5); }
   private int text() { return rgb(THEMES[themeIndex], 6); }
   private int textMuted() { return rgb(THEMES[themeIndex], 7); }
   private int headerColor() { return rgb(THEMES[themeIndex], 8); }

   // =========================================================================
   // RENDER
   // =========================================================================

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.catHits.clear();
      this.moduleHits.clear();
      this.themeHits.clear();
      this.activeWidgets.clear();

      try {
         this.renderBackground(vg, uiW, uiH);
         this.headerH = 44.0F;
         this.renderHeader(vg, mx, my, uiW);
         this.bodyTop = this.headerH + 8.0F;

         // Layout: sidebar (left) + body (main area)
         this.sidebarW = this.sidebarCollapsed ? 48.0F : 180.0F;
         this.sidebarX = 8.0F;
         this.sidebarY = this.bodyTop;
         this.sidebarH = uiH - this.bodyTop - 72.0F; // leave room for theme dock

         this.bodyX = this.sidebarX + this.sidebarW + 8.0F;
         this.bodyY = this.bodyTop;
         this.bodyW = uiW - this.bodyX - 8.0F;
         this.bodyH = uiH - this.bodyTop - 72.0F;

         this.renderSidebar(vg, mx, my);
         this.renderBody(vg, mx, my, uiW, uiH);
         this.renderCorzPanel(vg, mx, my, uiW, uiH);
         this.renderThemeDock(vg, mx, my, uiW, uiH);
      } catch (Throwable t) {
         dev.aether.AetherClient.LOGGER.error("Larp Recode v2 GUI render failed", t);
      }
   }

   private void renderBackground(NVGRenderer vg, float uiW, float uiH) {
      int[] p = THEMES[themeIndex];
      vg.rectGradient(0.0F, 0.0F, uiW, uiH, 0.0F,
         rgb(p, 0) | 0xFF000000, rgb(p, 1) | 0xFF000000, true);

      long now = System.nanoTime();
      float t = (float)(now % 10000000000L) / 1.0E9F;

      if (themeIndex == 0) {
         // Ocean: subtle light rays + soft glow orbs
         for (int i = 0; i < 4; i++) {
            float rx = uiW * (0.1F + 0.8F * (i / 4.0F)) + (float)Math.sin(t * 0.2F + i) * 30.0F;
            vg.rect(rx, 0.0F, 50.0F, uiH, 0.0F, Colors.withAlpha(rgb(p, 4), 0.035F));
         }
         for (int i = 0; i < 3; i++) {
            float bx = uiW * (0.3F + 0.25F * i) + (float)Math.sin(t * 0.3F + i) * 20.0F;
            float by = uiH * 0.4F + (float)Math.sin(t * 0.4F + i * 2.0F) * 20.0F;
            vg.circleGlow(bx, by, 60.0F, 120.0F, Colors.withAlpha(rgb(p, 5), 0.06F));
         }
      } else if (themeIndex == 1) {
         // Cherry blossom: drifting petals
         for (int i = 0; i < 16; i++) {
            float px = (float)((i * 137.5F + t * 25.0F * (i % 3 + 1)) % uiW);
            float py = (float)((t * 18.0F * (i % 4 + 1) + i * 80.0F) % uiH);
            vg.circle(px, py, 3.0F, Colors.withAlpha(rgb(p, 5), 0.25F));
         }
      } else if (themeIndex == 2) {
         // Forest: subtle bokeh
         for (int i = 0; i < 12; i++) {
            float bx = (i * 73.0F + t * 10.0F) % uiW;
            float by = (i * 113.0F + t * 6.0F) % uiH;
            float r = 8.0F + (i % 4) * 7.0F;
            vg.circleGlow(bx, by, r, r * 2.5F, Colors.withAlpha(rgb(p, 4), 0.07F));
         }
      } else {
         // Charcoal: faint grid
         for (int gx = 0; gx < uiW; gx += 48.0F) {
            vg.line(gx, 0.0F, gx, uiH, 1.0F, Colors.withAlpha(rgb(p, 4), 0.025F));
         }
         for (int gy = 0; gy < uiH; gy += 48.0F) {
            vg.line(0.0F, gy, uiW, gy, 1.0F, Colors.withAlpha(rgb(p, 4), 0.025F));
         }
      }
   }

   private void renderHeader(NVGRenderer vg, float mx, float my, float uiW) {
      // Header background — frosted glass
      vg.rect(0.0F, 0.0F, uiW, this.headerH, 0.0F, Colors.withAlpha(bgTop(), 0.88F));
      vg.rectOutline(0.0F, this.headerH - 1.0F, uiW, 1.0F, 0.0F, 1.0F,
         Colors.withAlpha(accent(), 0.35F));

      // Logo tile + title
      float lx = 14.0F;
      float ly = 8.0F;
      vg.rect(lx, ly, 28.0F, 28.0F, 7.0F, Colors.withAlpha(accent(), 0.28F));
      vg.rectOutline(lx, ly, 28.0F, 28.0F, 7.0F, 1.0F, Colors.withAlpha(accent(), 0.5F));
      vg.text("LR", lx + 14.0F, ly + 20.0F, 12.0F, 0xFFFFFFFF);

      float titleX = lx + 36.0F;
      vg.text("LARP RECODE", titleX, ly + 14.0F, 14.0F, 0xFFFFFFFF);
      vg.text("v2.2.0 · 1.21.11", titleX, ly + 26.0F, 9.0F, Colors.withAlpha(0xFFFFFF, 0.6F));

      // Search bar (top right)
      this.searchH = 30.0F;
      this.searchW = Math.min(340.0F, uiW * 0.35F);
      this.searchX = uiW - this.searchW - 14.0F;
      this.searchY = (this.headerH - this.searchH) * 0.5F;
      boolean sHov = this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH);
      vg.rect(this.searchX, this.searchY, this.searchW, this.searchH, 14.0F,
         Colors.withAlpha(surfaceElev(), this.searchFocused ? 0.95F : 0.8F));
      vg.rectOutline(this.searchX, this.searchY, this.searchW, this.searchH, 14.0F, 1.0F,
         this.searchFocused ? Colors.withAlpha(accent(), 0.6F) : Colors.withAlpha(accent(), 0.25F));
      float scy = this.searchY + this.searchH * 0.5F;
      if (this.searchFocused || !this.search.isEmpty()) {
         vg.text(this.search.isEmpty() ? "Search modules..." : this.search, this.searchX + 14.0F, scy, 10.0F, text());
      } else {
         vg.text("Search modules...", this.searchX + 14.0F, scy, 10.0F, textMuted());
      }
      if (!this.search.isEmpty()) {
         vg.cross(this.searchX + this.searchW - 16.0F, scy - 5.0F, 10.0F, 1.4F, textMuted());
      }
   }

   private void renderSidebar(NVGRenderer vg, float mx, float my) {
      // Sidebar background — frosted glass
      vg.rect(this.sidebarX, this.sidebarY, this.sidebarW, this.sidebarH, 12.0F,
         Colors.withAlpha(surface(), 0.78F));
      vg.rectOutline(this.sidebarX, this.sidebarY, this.sidebarW, this.sidebarH, 12.0F, 1.0F,
         Colors.withAlpha(accent(), 0.15F));

      if (this.sidebarCollapsed) {
         // Collapsed: just show category letter icons
         float cy = this.sidebarY + 12.0F;
         float btnR = 14.0F;
         for (int i = 0; i < SIDEBAR_CATEGORIES.length; i++) {
            Category cat = SIDEBAR_CATEGORIES[i];
            boolean active = i == this.activeCategory;
            boolean hov = this.hit(mx, my, this.sidebarX + 6.0F, cy - btnR, this.sidebarW - 12.0F, btnR * 2.0F);
            int bg = active ? Colors.withAlpha(accent(), 0.3F)
               : (hov ? Colors.withAlpha(surfaceElev(), 0.6F) : Colors.withAlpha(surfaceElev(), 0.35F));
            vg.circle(this.sidebarX + this.sidebarW * 0.5F, cy, btnR, bg);
            if (active) vg.circleOutline(this.sidebarX + this.sidebarW * 0.5F, cy, btnR, 1.0F,
               Colors.withAlpha(accentBright(), 0.6F));
            vg.text(cat.getDisplayName().substring(0, 1), this.sidebarX + this.sidebarW * 0.5F, cy + 4.0F, 10.0F,
               active ? 0xFFFFFFFF : textMuted());
            this.catHits.add(new CatHit(i, this.sidebarX + 6.0F, cy - btnR, this.sidebarW - 12.0F, btnR * 2.0F));
            cy += btnR * 2.0F + 6.0F;
         }
         // Collapse toggle
         float toggleY = this.sidebarY + this.sidebarH - 16.0F;
         vg.circle(this.sidebarX + this.sidebarW * 0.5F, toggleY, 10.0F, Colors.withAlpha(accent(), 0.25F));
         vg.chevron(this.sidebarX + this.sidebarW * 0.5F, toggleY, 4.0F, 1.6F, accent(), true);
         this.catHits.add(new CatHit(-1, this.sidebarX + 6.0F, toggleY - 10.0F, this.sidebarW - 12.0F, 20.0F));
         return;
      }

      // Expanded: show "CATEGORIES" header + category buttons
      float sx = this.sidebarX + 12.0F;
      vg.text("CATEGORIES", sx, this.sidebarY + 24.0F, 9.0F, textMuted());
      float cy = this.sidebarY + 36.0F;
      float rowH = 32.0F;
      for (int i = 0; i < SIDEBAR_CATEGORIES.length; i++) {
         Category cat = SIDEBAR_CATEGORIES[i];
         boolean active = i == this.activeCategory;
         boolean hov = this.hit(mx, my, sx - 4.0F, cy, this.sidebarW - 24.0F, rowH);
         int bg = active ? Colors.withAlpha(accent(), 0.22F)
            : (hov ? Colors.withAlpha(surfaceElev(), 0.45F) : Colors.withAlpha(surfaceElev(), 0.2F));
         vg.rect(sx - 4.0F, cy, this.sidebarW - 24.0F, rowH - 4.0F, 8.0F, bg);
         if (active) {
            vg.rect(sx - 4.0F, cy, 3.0F, rowH - 4.0F, 1.5F, Colors.withAlpha(accentBright(), 0.8F));
         }
         // Icon circle
         vg.circle(sx + 10.0F, cy + (rowH - 4.0F) * 0.5F, 8.0F,
            active ? Colors.withAlpha(accent(), 0.4F) : Colors.withAlpha(surfaceElev(), 0.5F));
         vg.text(cat.getDisplayName().substring(0, 1), sx + 10.0F, cy + (rowH - 4.0F) * 0.5F + 3.0F, 8.0F,
            active ? 0xFFFFFFFF : textMuted());
         vg.text(cat.getDisplayName(), sx + 24.0F, cy + (rowH - 4.0F) * 0.5F + 3.0F, 10.0F,
            active ? 0xFFFFFFFF : text());
         int count = this.modules.inCategory(cat).size();
         if (count > 0) {
            String cnt = Integer.toString(count);
            float cw = vg.textWidth(cnt, 8.0F) + 10.0F;
            vg.rect(this.sidebarX + this.sidebarW - 12.0F - cw, cy + 6.0F, cw, 14.0F, 7.0F,
               Colors.withAlpha(accent(), active ? 0.3F : 0.15F));
            vg.text(cnt, this.sidebarX + this.sidebarW - 12.0F - cw + 5.0F, cy + 15.0F, 8.0F,
               active ? accentBright() : textMuted());
         }
         this.catHits.add(new CatHit(i, sx - 4.0F, cy, this.sidebarW - 24.0F, rowH - 4.0F));
         cy += rowH;
      }

      // Collapse toggle at bottom
      float toggleY = this.sidebarY + this.sidebarH - 12.0F;
      vg.rect(sx - 4.0F, toggleY, this.sidebarW - 24.0F, 24.0F, 8.0F, Colors.withAlpha(surfaceElev(), 0.3F));
      vg.chevron(sx + 8.0F, toggleY + 12.0F, 4.0F, 1.6F, accent(), true);
      vg.text("Collapse", sx + 20.0F, toggleY + 16.0F, 9.0F, textMuted());
      this.catHits.add(new CatHit(-1, sx - 4.0F, toggleY, this.sidebarW - 24.0F, 24.0F));
   }

   private void renderBody(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      // Body background
      vg.rect(this.bodyX, this.bodyY, this.bodyW, this.bodyH, 12.0F,
         Colors.withAlpha(surface(), 0.65F));
      vg.rectOutline(this.bodyX, this.bodyY, this.bodyW, this.bodyH, 12.0F, 1.0F,
         Colors.withAlpha(accent(), 0.12F));

      if (this.sidebarCollapsed || this.activeCategory < 0 || this.activeCategory >= SIDEBAR_CATEGORIES.length) {
         // Empty state
         float ecx = this.bodyX + this.bodyW * 0.5F;
         float ecy = this.bodyY + this.bodyH * 0.5F;
         vg.circleGlow(ecx, ecy, 30.0F, 60.0F, Colors.withAlpha(accent(), 0.15F));
         vg.text("Select a category", ecx, ecy - 8.0F, 14.0F, textMuted());
         return;
      }

      Category cat = SIDEBAR_CATEGORIES[this.activeCategory];
      // Category title
      vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT), this.bodyX + 16.0F, this.bodyY + 24.0F, 13.0F, 0xFFFFFFFF);
      vg.text("MODULES", this.bodyX + 16.0F, this.bodyY + 38.0F, 8.0F, textMuted());

      // Module list
      List<Module> inCat = this.modules.inCategory(cat);
      float ry = this.bodyY + 52.0F;
      float padX = 16.0F;
      float cardW = this.bodyW - padX * 2.0F;

      for (Module module : inCat) {
         if (!this.matchesSearch(module)) continue;

         boolean exp = Boolean.TRUE.equals(this.expanded.get(module));
         // Animation values
         float openAmt = exp ? 1.0F : 0.0F;
         Integer prev = this.openAnim.get(module);
         if (prev == null) { this.openAnim.put(module, openAmt); }
         else {
            float cur = prev;
            float diff = openAmt - cur;
            float step = Math.abs(diff) * 0.25F;
            if (Math.abs(diff) < 0.01F) cur = openAmt;
            else cur += Math.signum(diff) * step;
            this.openAnim.put(module, cur);
            openAmt = cur;
         }

         float rowH = 30.0F;
         float settingsH = this.settingsHeight(this.widgetsFor(module), vg) * openAmt;
         float totalH = rowH + settingsH + 4.0F;
         if (ry + totalH > this.bodyY + this.bodyH - 8.0F) break;

         boolean enabled = module.isEnabled();
         boolean hov = this.hit(mx, my, this.bodyX + padX, ry, cardW, rowH);

         // Card background
         int cardBg = enabled ? Colors.withAlpha(accent(), hov ? 0.22F : 0.14F)
            : Colors.withAlpha(surfaceElev(), hov ? 0.4F : 0.22F);
         vg.rect(this.bodyX + padX, ry, cardW, rowH, 8.0F, cardBg);

         // Status indicator dot
         vg.circle(this.bodyX + padX + 16.0F, ry + rowH * 0.5F, 4.0F,
            enabled ? Colors.withAlpha(accentBright(), 0.9F) : Colors.withAlpha(textMuted(), 0.3F));

         // Module name
         vg.text(module.getName(), this.bodyX + padX + 28.0F, ry + rowH * 0.5F + 4.0F, 11.0F,
            enabled ? 0xFFFFFFFF : textMuted());

         // Description (truncated)
         if (!module.getDescription().isEmpty()) {
            float dw = cardW - 28.0F - 80.0F;
            if (dw > 20.0F) {
               vg.textTruncated(module.getDescription(), this.bodyX + padX + 28.0F, ry + rowH * 0.5F + 4.0F + 12.0F, 8.0F,
                  Colors.withAlpha(textMuted(), 0.7F), dw);
            }
         }

         // Keybind on right
         if (module.getKeybind().isBound()) {
            String k = module.getKeybind().keyName();
            float kw = vg.textWidth(k, 9.0F) + 12.0F;
            vg.rect(this.bodyX + padX + cardW - kw - 56.0F, ry + 7.0F, kw, 16.0F, 8.0F,
               Colors.withAlpha(surfaceElev(), 0.5F));
            vg.text(k, this.bodyX + padX + cardW - kw - 56.0F + 6.0F, ry + 16.0F, 9.0F, accent());
         }

         // Chevron to expand
         float chevCX = this.bodyX + padX + cardW - 22.0F;
         float chevCY = ry + rowH * 0.5F;
         float chevRot = exp ? (float)(Math.PI * 0.5) : 0.0F;
         vg.save();
         vg.translate(chevCX, chevCY);
         vg.rotate(chevRot);
         vg.chevron(0.0F, 0.0F, 4.0F, 1.6F, exp ? 0xFFFFFFFF : textMuted(), false);
         vg.restore();

         this.moduleHits.add(new ModuleHit(module, this.bodyX + padX, ry, cardW, rowH,
            chevCX - 8.0F, chevCY - 8.0F, 16.0F, 16.0F, ry + rowH, ry + rowH + settingsH));

         // Expanded settings with clip
         if (openAmt > 0.01F) {
            vg.save();
            vg.scissor(this.bodyX + padX, ry + rowH, cardW, settingsH);
            float wy = ry + rowH + 4.0F;
            vg.line(this.bodyX + padX + 8.0F, wy - 2.0F, this.bodyX + padX + cardW - 8.0F, wy - 2.0F, 1.0F,
               Colors.withAlpha(accent(), 0.12F));
            for (SettingWidget w2 : this.widgetsFor(module)) {
               if (!w2.isVisible()) continue;
               float wH = w2.height(vg);
               // Fade in widgets based on open progress
               float alpha = Math.min(1.0F, openAmt * 2.0F);
               vg.alpha(alpha);
               w2.setBounds(this.bodyX + padX + 8.0F, wy, cardW - 16.0F);
               w2.render(vg, mx, my);
               vg.alpha(1.0F);
               this.activeWidgets.add(w2);
               wy += wH + 3.0F;
               if (wy > ry + rowH + settingsH) break;
            }
            vg.restore();
         }
         ry += totalH;
      }

      // If no modules match
      if (ry == this.bodyY + 52.0F) {
         float ecx = this.bodyX + this.bodyW * 0.5F;
         float ecy = this.bodyY + this.bodyH * 0.5F;
         vg.text("No modules found", ecx - vg.textWidth("No modules found", 11.0F) * 0.5F, ecy, 11.0F, textMuted());
      }
   }

   private void renderCorzPanel(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.corzHits.clear();
      var provider = CorzPortBus.isAvailable() ? CorzPortBus.provider() : null;
      if (provider == null) return;

      // Position Corz panel to the right of the body
      float cw = Math.min(220.0F, uiW * 0.2F);
      float cx = uiW - cw - 8.0F;
      float cy = this.bodyTop;
      float ch = uiH - this.bodyTop - 72.0F;

      List<CorzModuleHandle> mods = this.corzModules();
      int count = mods.size();
      float headerH = 36.0F;
      float rowH = 28.0F;
      float contentH = headerH + count * (rowH + 4.0F) + 8.0F;
      this.corzMaxScroll = Math.max(0.0F, contentH - ch);
      this.corzScroll.setTarget(Math.clamp(this.corzScroll.getTarget(), 0.0F, this.corzMaxScroll));
      this.corzX = cx;
      this.corzY = cy;
      this.corzW = cw;
      this.corzH = ch;

      // Frosted-glass panel
      vg.rect(cx, cy, cw, ch, 12.0F, Colors.withAlpha(surface(), 0.82F));
      vg.rectOutline(cx, cy, cw, ch, 12.0F, 1.0F, Colors.withAlpha(accent(), 0.2F));

      // Header
      float ix = cx + 12.0F;
      float iy = cy + 8.0F;
      vg.rect(ix, iy, 20.0F, 20.0F, 5.0F, Colors.withAlpha(accent(), 0.3F));
      vg.text("C", ix + 14.0F, iy + 15.0F, 11.0F, 0xFFFFFFFF);
      vg.text("CORZ", ix + 28.0F, iy + 15.0F, 12.0F, text());
      if (count > 0) {
         String cnt = Integer.toString(count);
         float cw2 = vg.textWidth(cnt, 9.0F) + 12.0F;
         vg.rect(cx + cw - cw2 - 12.0F, iy + 2.0F, cw2, 16.0F, 8.0F, Colors.withAlpha(accent(), 0.25F));
         vg.text(cnt, cx + cw - cw2 - 12.0F + 6.0F, iy + 13.0F, 9.0F, accent());
      }
      vg.line(cx + 10.0F, cy + headerH, cx + cw - 10.0F, cy + headerH, 1.0F, Colors.withAlpha(accent(), 0.15F));

      // Module rows (scrolled)
      float scrollY = this.corzScroll.value();
      vg.save();
      vg.scissor(cx + 6.0F, cy + headerH, cw - 12.0F, ch - headerH - 6.0F);
      float ry = cy + headerH + 6.0F - scrollY;
      for (CorzModuleHandle mod : mods) {
         if (ry + rowH < cy + headerH) { ry += rowH + 4.0F; continue; }
         if (ry > cy + ch) break;

         boolean enabled = mod.isEnabled();
         boolean hov = this.hit(mx, my, cx + 6.0F, ry, cw - 12.0F, rowH);
         int rowCol = enabled ? Colors.withAlpha(accent(), hov ? 0.32F : 0.22F)
            : Colors.withAlpha(surfaceElev(), hov ? 0.5F : 0.35F);
         vg.rect(cx + 6.0F, ry, cw - 12.0F, rowH, 7.0F, rowCol);
         if (enabled) {
            vg.rect(cx + 6.0F, ry + 4.0F, 3.0F, rowH - 8.0F, 1.5F, Colors.withAlpha(0xFFFFFF, 0.85F));
         }
         float textMaxW = cw - 24.0F;
         vg.textTruncated(mod.displayName().toUpperCase(Locale.ROOT), cx + 18.0F, ry + rowH * 0.5F, 10.0F,
            enabled ? text() : textMuted(), textMaxW);

         // "corz" tag
         float tagW = 34.0F;
         float tagX = cx + cw - 6.0F - tagW;
         float tagY = ry + rowH * 0.5F - 7.0F;
         vg.rect(tagX, tagY, tagW, 14.0F, 7.0F, Colors.withAlpha(accent(), 0.22F));
         vg.text("corz", tagX + tagW * 0.5F - vg.textWidth("corz", 8.0F) * 0.5F, tagY + 10.0F, 8.0F, accent());

         this.corzHits.add(new CorzHit(mod, cx + 6.0F, ry, cw - 12.0F, rowH));
         ry += rowH + 4.0F;
      }
      vg.restore();

      // Scrollbar
      if (this.corzMaxScroll > 1.0F) {
         float trackH = ch - headerH - 12.0F;
         float thumbH = Math.max(20.0F, trackH * (ch / contentH));
         float thumbY = cy + headerH + 6.0F + (trackH - thumbH) * (scrollY / this.corzMaxScroll);
         vg.rect(cx + cw - 6.0F, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(accent(), 0.45F));
      }
   }

   private List<CorzModuleHandle> corzModules() {
      if (!CorzPortBus.isAvailable()) return List.of();
      var provider = CorzPortBus.provider();
      if (provider == null) return List.of();
      List<CorzModuleHandle> all = provider.allModules();
      if (this.search.isEmpty()) return all;
      List<CorzModuleHandle> out = new ArrayList<>();
      for (CorzModuleHandle m : all) {
         if (m.displayName().toLowerCase(Locale.ROOT).contains(this.search.toLowerCase(Locale.ROOT))) {
            out.add(m);
         }
      }
      return out;
   }

   private void renderThemeDock(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.dockH = 52.0F;
      this.dockW = Math.min(520.0F, uiW - 80.0F);
      this.dockX = (uiW - this.dockW) * 0.5F;
      this.dockY = uiH - this.dockH - 10.0F;
      vg.rect(this.dockX, this.dockY, this.dockW, this.dockH, 14.0F, Colors.withAlpha(surface(), 0.85F));
      vg.rectOutline(this.dockX, this.dockY, this.dockW, this.dockH, 14.0F, 1.0F, Colors.withAlpha(accent(), 0.2F));

      int n = THEME_NAMES.length;
      float thumbW = (this.dockW - 24.0F - (n - 1) * 8.0F) / (float) n;
      float thumbH = this.dockH - 18.0F;
      float ty = this.dockY + 9.0F;
      for (int i = 0; i < n; i++) {
         float tx = this.dockX + 12.0F + i * (thumbW + 8.0F);
         boolean sel = i == this.themeIndex;
         boolean hov = this.hit(mx, my, tx, ty, thumbW, thumbH);
         int[] tp = THEMES[i];
         int swatch = rgb(tp, 4);
         int headerCol = rgb(tp, 8);
         vg.rect(tx, ty, thumbW, thumbH, 8.0F,
            sel ? (headerCol | 0xFF000000) : Colors.withAlpha(swatch, 0.5F));
         vg.rectOutline(tx, ty, thumbW, thumbH, 8.0F, 1.0F,
            sel ? Colors.withAlpha(accentBright(), 0.7F) : Colors.withAlpha(accent(), 0.15F));
         vg.text(THEME_NAMES[i], tx + (thumbW - vg.textWidth(THEME_NAMES[i], 8.0F)) * 0.5F, ty + thumbH - 7.0F, 8.0F,
            sel ? 0xFFFFFFFF : textMuted());
         if (sel) {
            vg.circle(tx + thumbW * 0.5F, ty + thumbH + 3.0F, 2.4F, accentBright());
         }
         this.themeHits.add(new ThemeHit(i, tx, ty, thumbW, thumbH));
      }
   }

   // =========================================================================
   // WIDGETS (lazy)
   // =========================================================================

   private List<SettingWidget> widgetsFor(Module m) {
      List<SettingWidget> ws = this.moduleWidgets.get(m);
      if (ws != null) return ws;
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
         if (w.isVisible()) h += w.height(vg) + 3.0F;
      }
      return h;
   }

   private boolean matchesSearch(Module m) {
      if (this.search.isEmpty()) return true;
      return m.getName().toLowerCase(Locale.ROOT).contains(this.search.toLowerCase(Locale.ROOT));
   }

   // =========================================================================
   // INPUT
   // =========================================================================

   public boolean mouseClicked(float mx, float my, int btn) {
      if (btn != 0) return false;

      // Search bar
      if (this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 22.0F) {
            this.search = "";
         } else {
            this.searchFocused = true;
         }
         return true;
      }
      this.searchFocused = false;

      // Theme dock
      for (int i = this.themeHits.size() - 1; i >= 0; i--) {
         ThemeHit th = this.themeHits.get(i);
         if (this.hit(mx, my, th.x, th.y, th.w, th.h)) {
            this.themeIndex = th.index;
            UiSounds.select();
            return true;
         }
      }

      // Sidebar
      for (int i = this.catHits.size() - 1; i >= 0; i--) {
         CatHit ch = this.catHits.get(i);
         if (this.hit(mx, my, ch.x, ch.y, ch.w, ch.h)) {
            if (ch.index == -1) {
               this.sidebarCollapsed = !this.sidebarCollapsed;
               UiSounds.select();
            } else {
               this.activeCategory = ch.index;
               UiSounds.select();
            }
            return true;
         }
      }

      // Corz rows
      for (int i = this.corzHits.size() - 1; i >= 0; i--) {
         CorzHit ch = this.corzHits.get(i);
         if (this.hit(mx, my, ch.x, ch.y, ch.w, ch.rowH)) {
            ch.module.toggle();
            UiSounds.toggle(ch.module.isEnabled());
            return true;
         }
      }

      // Module rows: chevron expands, body toggles, settings capture
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
               if (w.isVisible() && w.mouseClicked(mx, my, btn)) return true;
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
      for (SettingWidget w : this.activeWidgets) w.mouseDragged(mx, my);
   }

   public void mouseScrolled(float mx, float my, float scrollY) {
      if (this.corzMaxScroll > 0.0F && this.hit(mx, my, this.corzX, this.corzY, this.corzW, this.corzH)) {
         this.corzScroll.setTarget(Math.clamp(this.corzScroll.getTarget() + scrollY * 22.0F, 0.0F, this.corzMaxScroll));
      }
   }

   public void mouseReleased() {
      for (SettingWidget w : this.activeWidgets) w.mouseReleased();
   }

   public boolean keyPressed(int keyCode) {
      for (SettingWidget w : this.activeWidgets) {
         if (w.isListening() && w.keyPressed(keyCode)) return true;
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
         if (w.isListening() && w.charTyped(codepoint)) return true;
      }
      if (!this.searchFocused) return false;
      if (this.search.length() < 32) {
         this.search += Character.toLowerCase(codepoint);
      }
      return true;
   }

   public boolean isListening() {
      for (SettingWidget w : this.activeWidgets) {
         if (w.isListening()) return true;
      }
      return false;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
