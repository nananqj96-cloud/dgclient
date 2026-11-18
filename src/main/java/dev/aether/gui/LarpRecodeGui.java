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
 * The <b>Vulxts Client</b> ClickGUI — a modern, polished interface with a frosted-glass
 * category sidebar, animated module cards, a live search bar, a Corz integration panel,
 * and a theme dock. Designed for clarity and visual appeal: clean dark surfaces, smooth
 * easing animations, hover glow effects, and a clear visual hierarchy.
 *
 * <p>Layout: left sidebar (categories) → main module area → right Corz panel, with a
 * top header bar and bottom theme dock. All geometry is computed during {@link #render}
 * and cached for input, so layout and click regions always match what is drawn.
 */
public final class LarpRecodeGui {
   // ---- Theme palettes: [bg_top, bg_bottom, surface, surface_elev, surface_elev2, accent, accent_bright, text, text_muted, header] ----
   // Each color is (R, G, B). 10 colors × 3 = 30 elements.
   private static final int[] OCEAN = {
      0x0A, 0x1E, 0x3F,   // bg_top
      0x08, 0x14, 0x2A,   // bg_bottom
      0x14, 0x28, 0x50,   // surface
      0x1C, 0x38, 0x68,   // surface_elev
      0x24, 0x48, 0x80,   // surface_elev2
      0x48, 0x8E, 0xDA,   // accent
      0x7A, 0xC8, 0xF0,   // accent_bright
      0xE8, 0xF0, 0xFF,   // text
      0x9A, 0xB8, 0xD8,   // text_muted
      0x48, 0x8E, 0xDA    // header
   };
   private static final int[] CHERRY = {
      0x1E, 0x12, 0x1E,
      0x14, 0x0C, 0x16,
      0x2A, 0x1C, 0x2E,
      0x3A, 0x24, 0x3E,
      0x4A, 0x2E, 0x4A,
      0xE8, 0x5B, 0xA8,
      0xF0, 0x9A, 0xD0,
      0xFF, 0xF0, 0xF4,
      0xC8, 0x98, 0xB8,
      0xE6, 0xD8, 0xDD
   };
   private static final int[] FOREST = {
      0x0A, 0x1A, 0x10,
      0x06, 0x10, 0x0A,
      0x14, 0x28, 0x1C,
      0x1C, 0x40, 0x28,
      0x24, 0x50, 0x34,
      0x4A, 0xBF, 0x6A,
      0x88, 0xE8, 0xA8,
      0xE8, 0xF0, 0xE0,
      0x98, 0xC8, 0x98,
      0x4A, 0xBF, 0x6A
   };
   private static final int[] CHARCOAL = {
      0x0C, 0x0C, 0x10,
      0x08, 0x08, 0x0C,
      0x14, 0x14, 0x1A,
      0x1C, 0x1C, 0x22,
      0x28, 0x28, 0x30,
      0x8A, 0x8A, 0x92,
      0xC0, 0xC0, 0xD0,
      0xE0, 0xE0, 0xE8,
      0x9A, 0x9A, 0xA2,
      0x3C, 0x3C, 0x42
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
   private int activeCategory = 0;
   private boolean sidebarCollapsed;

   // Lazy widget caches
   private final Map<Module, List<SettingWidget>> moduleWidgets = new LinkedHashMap<>();
   private final Map<Module, Boolean> expanded = new LinkedHashMap<>();
   private final Map<Module, Float> openAnim = new LinkedHashMap<>();

   // Layout caches
   private float headerH;
   private float sidebarX, sidebarW, sidebarY, sidebarH;
   private float bodyX, bodyY, bodyW, bodyH;
   private float searchX, searchY, searchW, searchH;
   private float dockX, dockY, dockW, dockH;
   private float bodyTop;
   private float bodyContentY;
   private float bodyContentH;
   private float scrollY;
   private float maxScroll;
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
   private int surfaceElev2() { return rgb(THEMES[themeIndex], 4); }
   private int accent() { return rgb(THEMES[themeIndex], 5); }
   private int accentBright() { return rgb(THEMES[themeIndex], 6); }
   private int text() { return rgb(THEMES[themeIndex], 7); }
   private int textMuted() { return rgb(THEMES[themeIndex], 8); }
   private int headerColor() { return rgb(THEMES[themeIndex], 9); }

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

         // ---- Header ----
         this.headerH = 56.0F;
         this.renderHeader(vg, mx, my, uiW);

         // ---- Layout ----
         this.bodyTop = this.headerH + 10.0F;
         this.sidebarW = this.sidebarCollapsed ? 52.0F : 190.0F;
         this.sidebarX = 10.0F;
         this.sidebarY = this.bodyTop;
         this.sidebarH = uiH - this.bodyTop - 76.0F;

         this.bodyX = this.sidebarX + this.sidebarW + 10.0F;
         this.bodyY = this.bodyTop;
         this.bodyW = uiW - this.bodyX - 14.0F;
         this.bodyH = uiH - this.bodyTop - 76.0F;

         this.bodyContentY = this.bodyY + 8.0F;
         this.bodyContentH = this.bodyH - 16.0F;

         this.renderSidebar(vg, mx, my);
         this.renderBody(vg, mx, my);
         this.renderCorzPanel(vg, mx, my, uiW, uiH);
         this.renderThemeDock(vg, mx, my, uiW, uiH);
      } catch (Throwable t) {
         dev.aether.AetherClient.LOGGER.error("Vulxts Client GUI render failed", t);
      }
   }

   private void renderBackground(NVGRenderer vg, float uiW, float uiH) {
      int[] p = THEMES[themeIndex];
      vg.rectGradient(0.0F, 0.0F, uiW, uiH, 0.0F,
         rgb(p, 0) | 0xFF000000, rgb(p, 1) | 0xFF000000, true);

      long now = System.nanoTime();
      float t = (float)(now % 10000000000L) / 1.0E9F;

      // Animated ambient glow orbs
      for (int i = 0; i < 3; i++) {
         float ox = uiW * (0.2F + 0.3F * i) + (float)Math.sin(t * 0.15F + i * 2.0F) * 40.0F;
         float oy = uiH * (0.3F + 0.2F * i) + (float)Math.cos(t * 0.1F + i) * 30.0F;
         vg.circleGlow(ox, oy, 30.0F, 80.0F, Colors.withAlpha(rgb(p, 5), 0.04F));
      }
   }

   private void renderHeader(NVGRenderer vg, float mx, float my, float uiW) {
      // Header bar with frosted glass
      vg.rect(0.0F, 0.0F, uiW, this.headerH, 0.0F,
         Colors.withAlpha(surface(), 0.85F));
      vg.rectOutline(0.0F, 0.0F, uiW, this.headerH, 0.0F, 1.0F,
         Colors.withAlpha(accent(), 0.12F));
      vg.line(0.0F, this.headerH - 1.0F, uiW, this.headerH - 1.0F, 1.0F,
         Colors.withAlpha(accent(), 0.1F));

      // Logo block
      float lx = 16.0F;
      float ly = this.headerH * 0.5F;
      vg.rect(lx, ly - 14.0F, 28.0F, 28.0F, 7.0F, Colors.withAlpha(accent(), 0.25F));
      vg.text("V", lx + 19.0F, ly + 5.0F, 16.0F, accentBright());

      // Title
      vg.text("VULXTS CLIENT", lx + 38.0F, ly + 6.0F, 14.0F, 0xFFFFFFFF);
      vg.text("v2.2.0 · 1.21.11", lx + 38.0F, ly + 20.0F, 8.0F, textMuted());

      // Search bar
      this.searchX = uiW - 240.0F;
      this.searchY = 12.0F;
      this.searchW = 220.0F;
      this.searchH = 32.0F;

      float sx = this.searchX;
      float sy = this.searchY;
      boolean sf = this.searchFocused;
      vg.rect(sx, sy, this.searchW, this.searchH, 8.0F,
         sf ? Colors.withAlpha(surfaceElev2(), 0.9F) : Colors.withAlpha(surfaceElev(), 0.6F));
      vg.rectOutline(sx, sy, this.searchW, this.searchH, 8.0F, 1.0F,
         sf ? Colors.withAlpha(accent(), 0.5F) : Colors.withAlpha(accent(), 0.15F));
      // Search icon
      vg.circle(sx + 18.0F, sy + this.searchH * 0.5F, 3.0F, textMuted());
      vg.line(sx + 18.0F + 2.0F, sy + this.searchH * 0.5F + 2.0F,
         sx + 18.0F + 5.0F, sy + this.searchH * 0.5F + 5.0F, 1.0F, textMuted());
      if (this.search.isEmpty()) {
         vg.text("Search modules…", sx + 30.0F, sy + this.searchH * 0.5F + 4.0F, 10.0F, textMuted());
      } else {
         vg.text(this.search, sx + 30.0F, sy + this.searchH * 0.5F + 4.0F, 10.0F, text());
      }
      // Clear button
      if (!this.search.isEmpty()) {
         float cw = 20.0F;
         vg.rect(sx + this.searchW - cw - 6.0F, sy + 4.0F, cw, this.searchH - 8.0F, 6.0F,
            Colors.withAlpha(surfaceElev2(), 0.5F));
         vg.text("×", sx + this.searchW - cw - 6.0F + 13.0F, sy + this.searchH * 0.5F + 4.0F, 12.0F, textMuted());
      }
   }

   private void renderSidebar(NVGRenderer vg, float mx, float my) {
      // Sidebar background
      vg.rect(this.sidebarX, this.sidebarY, this.sidebarW, this.sidebarH, 12.0F,
         Colors.withAlpha(surface(), 0.75F));
      vg.rectOutline(this.sidebarX, this.sidebarY, this.sidebarW, this.sidebarH, 12.0F, 1.0F,
         Colors.withAlpha(accent(), 0.15F));

      if (this.sidebarCollapsed) {
         // Collapsed: just show dots
         float cy = this.sidebarY + 20.0F;
         for (int i = 0; i < SIDEBAR_CATEGORIES.length; i++) {
            boolean active = i == this.activeCategory;
            vg.circle(this.sidebarX + this.sidebarW * 0.5F, cy, 4.0F,
               active ? accentBright() : textMuted());
            this.catHits.add(new CatHit(i, this.sidebarX, cy - 8.0F, this.sidebarW, 16.0F));
            cy += 28.0F;
         }
         // Expand button
         vg.chevron(this.sidebarX + this.sidebarW * 0.5F, this.sidebarY + this.sidebarH - 16.0F,
            3.5F, 1.4F, accent(), true);
         return;
      }

      // Category buttons
      float cy = this.sidebarY + 12.0F;
      float btnH = 40.0F;
      for (int i = 0; i < SIDEBAR_CATEGORIES.length; i++) {
         Category cat = SIDEBAR_CATEGORIES[i];
         boolean active = i == this.activeCategory;
         boolean hov = this.hit(mx, my, this.sidebarX + 6.0F, cy, this.sidebarW - 12.0F, btnH);

         // Active indicator bar
         if (active) {
            vg.rect(this.sidebarX, cy + 4.0F, 3.0F, btnH - 8.0F, 1.5F, accent());
         }

         // Button background
         int btnCol = active ? Colors.withAlpha(accent(), 0.12F)
            : (hov ? Colors.withAlpha(surfaceElev(), 0.4F) : Colors.withAlpha(surfaceElev(), 0.15F));
         vg.rect(this.sidebarX + 6.0F, cy, this.sidebarW - 12.0F, btnH, 8.0F, btnCol);

         // Category icon tile
         vg.rect(this.sidebarX + 12.0F, cy + 8.0F, 24.0F, 24.0F, 6.0F,
            Colors.withAlpha(accent(), active ? 0.3F : 0.15F));
         vg.text(cat.getDisplayName().substring(0, 1).toUpperCase(Locale.ROOT),
            this.sidebarX + 24.0F, cy + 22.0F, 11.0F, active ? accentBright() : text());

         // Category name
         vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT),
            this.sidebarX + 42.0F, cy + 18.0F, 10.0F, active ? 0xFFFFFFFF : text());

         // Module count
         int count = 0;
         for (Module mod : this.modules.inCategory(cat)) {
            if (this.matchesSearch(mod)) count++;
         }
         if (count > 0) {
            String cnt = Integer.toString(count);
            float cw = vg.textWidth(cnt, 8.0F) + 10.0F;
            vg.rect(this.sidebarX + this.sidebarW - 12.0F - cw, cy + btnH * 0.5F - 8.0F, cw, 16.0F, 8.0F,
               Colors.withAlpha(accent(), active ? 0.3F : 0.15F));
            vg.text(cnt, this.sidebarX + this.sidebarW - 12.0F - cw + 5.0F, cy + btnH * 0.5F + 2.0F, 8.0F,
               active ? 0xFFFFFFFF : textMuted());
         }

         this.catHits.add(new CatHit(i, this.sidebarX + 6.0F, cy, this.sidebarW - 12.0F, btnH));
         cy += btnH + 4.0F;
      }

      // Collapse button at bottom
      float cbY = this.sidebarY + this.sidebarH - 36.0F;
      vg.rect(this.sidebarX + 6.0F, cbY, this.sidebarW - 12.0F, 30.0F, 8.0F,
         Colors.withAlpha(surfaceElev(), 0.3F));
      vg.chevron(this.sidebarX + this.sidebarW * 0.5F, cbY + 15.0F, 3.5F, 1.4F, accent(), true);
      this.catHits.add(new CatHit(-1, this.sidebarX + 6.0F, cbY, this.sidebarW - 12.0F, 30.0F));
   }

   private void renderBody(NVGRenderer vg, float mx, float my) {
      // Body background
      vg.rect(this.bodyX, this.bodyY, this.bodyW, this.bodyH, 12.0F,
         Colors.withAlpha(surface(), 0.55F));
      vg.rectOutline(this.bodyX, this.bodyY, this.bodyW, this.bodyH, 12.0F, 1.0F,
         Colors.withAlpha(accent(), 0.1F));

      if (this.sidebarCollapsed || this.activeCategory < 0 || this.activeCategory >= SIDEBAR_CATEGORIES.length) {
         // Empty state
         float ecx = this.bodyX + this.bodyW * 0.5F;
         float ecy = this.bodyY + this.bodyH * 0.5F;
         vg.circleGlow(ecx, ecy, 25.0F, 50.0F, Colors.withAlpha(accent(), 0.1F));
         vg.text("Select a category", ecx - vg.textWidth("Select a category", 12.0F) * 0.5F, ecy, 12.0F, textMuted());
         return;
      }

      Category cat = SIDEBAR_CATEGORIES[this.activeCategory];

      // Category title
      vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT), this.bodyX + 20.0F, this.bodyY + 24.0F, 13.0F, 0xFFFFFFFF);
      vg.text("MODULES", this.bodyX + 20.0F, this.bodyY + 38.0F, 8.0F, textMuted());

      // Scrollbar
      float scrollbarX = this.bodyX + this.bodyW - 8.0F;
      float scrollbarY = this.bodyContentY;
      float scrollbarH = this.bodyContentH;
      if (this.maxScroll > 0.0F) {
         float thumbH = Math.max(20.0F, scrollbarH * (scrollbarH / (scrollbarH + this.maxScroll)));
         float thumbY = scrollbarY + (scrollbarH - thumbH) * (this.scrollY / this.maxScroll);
         vg.rect(scrollbarX, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(accent(), 0.3F));
      }

      // Module list with scissor clip
      vg.save();
      vg.scissor(this.bodyX + 4.0F, this.bodyY + 4.0F, this.bodyW - 8.0F, this.bodyH - 8.0F);

      float ry = this.bodyContentY - this.scrollY;
      float padX = 16.0F;
      float cardW = this.bodyW - padX * 2.0F;

      List<Module> inCat = this.modules.inCategory(cat);
      for (Module module : inCat) {
         if (!this.matchesSearch(module)) continue;

         boolean exp = Boolean.TRUE.equals(this.expanded.get(module));
         float openAmt = exp ? 1.0F : 0.0F;
         Float prev = this.openAnim.get(module);
         if (prev == null) {
            this.openAnim.put(module, openAmt);
         } else {
            float cur = prev;
            float diff = openAmt - cur;
            float step = Math.abs(diff) * 0.22F;
            if (Math.abs(diff) < 0.01F) cur = openAmt;
            else cur += Math.signum(diff) * step;
            this.openAnim.put(module, cur);
            openAmt = cur;
         }

         float rowH = 38.0F;
         float settingsH = this.settingsHeight(this.widgetsFor(module), vg) * openAmt;
         float totalH = rowH + settingsH + 6.0F;
         if (ry + totalH > this.bodyY + this.bodyH - 8.0F) break;
         if (ry + rowH < this.bodyContentY - this.scrollY - 200.0F) { ry += totalH; continue; }

         boolean enabled = module.isEnabled();
         boolean hov = this.hit(mx, my, this.bodyX + padX, ry, cardW, rowH);

         // Card background with hover glow
         int cardBg = enabled
            ? Colors.withAlpha(accent(), hov ? 0.18F : 0.10F)
            : Colors.withAlpha(surfaceElev(), hov ? 0.35F : 0.18F);
         vg.rect(this.bodyX + padX, ry, cardW, rowH, 10.0F, cardBg);

         // Left accent bar
         vg.rect(this.bodyX + padX, ry + 6.0F, 3.0F, rowH - 12.0F, 1.5F,
            enabled ? Colors.withAlpha(accentBright(), 0.8F) : Colors.withAlpha(textMuted(), 0.3F));

         // Status dot
         vg.circle(this.bodyX + padX + 20.0F, ry + rowH * 0.5F, 4.0F,
            enabled ? Colors.withAlpha(accentBright(), 0.95F) : Colors.withAlpha(textMuted(), 0.3F));

         // Module name
         vg.text(module.getName(), this.bodyX + padX + 32.0F, ry + rowH * 0.5F + 4.0F, 12.0F,
            enabled ? 0xFFFFFFFF : textMuted());

         // Description (truncated)
         if (!module.getDescription().isEmpty()) {
            float dw = cardW - 32.0F - 100.0F;
            if (dw > 20.0F) {
               vg.textTruncated(module.getDescription(), this.bodyX + padX + 32.0F, ry + rowH * 0.5F + 18.0F, 8.0F,
                  Colors.withAlpha(textMuted(), 0.65F), dw);
            }
         }

         // Keybind pill
         if (module.getKeybind().isBound()) {
            String k = module.getKeybind().keyName();
            float kw = vg.textWidth(k, 9.0F) + 14.0F;
            vg.rect(this.bodyX + padX + cardW - kw - 50.0F, ry + 9.0F, kw, 20.0F, 10.0F,
               Colors.withAlpha(surfaceElev2(), 0.5F));
            vg.text(k, this.bodyX + padX + cardW - kw - 50.0F + 7.0F, ry + 21.0F, 9.0F, accent());
         }

         // Chevron
         float chevCX = this.bodyX + padX + cardW - 26.0F;
         float chevCY = ry + rowH * 0.5F;
         float chevRot = exp ? (float)(Math.PI * 0.5) : 0.0F;
         vg.save();
         vg.translate(chevCX, chevCY);
         vg.rotate(chevRot);
         vg.chevron(0.0F, 0.0F, 4.0F, 1.6F, exp ? 0xFFFFFFFF : textMuted(), false);
         vg.restore();

         this.moduleHits.add(new ModuleHit(module, this.bodyX + padX, ry, cardW, rowH,
            chevCX - 8.0F, chevCY - 8.0F, 16.0F, 16.0F, ry + rowH, ry + rowH + settingsH));

         // Expanded settings
         if (openAmt > 0.01F) {
            vg.save();
            vg.scissor(this.bodyX + padX, ry + rowH, cardW, settingsH);
            float wy = ry + rowH + 8.0F;
            vg.line(this.bodyX + padX + 12.0F, wy - 4.0F, this.bodyX + padX + cardW - 12.0F, wy - 4.0F, 1.0F,
               Colors.withAlpha(accent(), 0.1F));
            for (SettingWidget w2 : this.widgetsFor(module)) {
               if (!w2.isVisible()) continue;
               float wH = w2.height(vg);
               float alpha = Math.min(1.0F, openAmt * 2.0F);
               vg.alpha(alpha);
               w2.setBounds(this.bodyX + padX + 12.0F, wy, cardW - 24.0F);
               w2.render(vg, mx, my);
               vg.alpha(1.0F);
               this.activeWidgets.add(w2);
               wy += wH + 4.0F;
               if (wy > ry + rowH + settingsH) break;
            }
            vg.restore();
         }
         ry += totalH;
      }

      // Empty state for category
      if (ry == this.bodyContentY - this.scrollY) {
         float ecx = this.bodyX + this.bodyW * 0.5F;
         float ecy = this.bodyY + this.bodyH * 0.5F;
         String msg = "No modules in this category";
         if (!this.search.isEmpty()) msg = "No modules match your search";
         vg.text(msg, ecx - vg.textWidth(msg, 11.0F) * 0.5F, ecy, 11.0F, textMuted());
      }

      // Update max scroll
      this.maxScroll = Math.max(0.0F, ry - (this.bodyContentY - this.scrollY) - this.bodyContentH + 8.0F);

      vg.restore();
   }

   private void renderCorzPanel(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.corzHits.clear();
      var provider = CorzPortBus.isAvailable() ? CorzPortBus.provider() : null;
      if (provider == null) return;

      float cw = Math.min(220.0F, uiW * 0.2F);
      float cx = uiW - cw - 10.0F;
      float cy = this.bodyTop;
      float ch = uiH - this.bodyTop - 76.0F;

      List<CorzModuleHandle> mods = this.corzModules();
      int count = mods.size();
      float headerH = 40.0F;
      float rowH = 30.0F;
      float contentH = headerH + count * (rowH + 4.0F) + 8.0F;
      this.corzMaxScroll = Math.max(0.0F, contentH - ch);
      this.corzScroll.setTarget(Math.clamp(this.corzScroll.getTarget(), 0.0F, this.corzMaxScroll));
      this.corzX = cx;
      this.corzY = cy;
      this.corzW = cw;
      this.corzH = ch;

      vg.rect(cx, cy, cw, ch, 12.0F, Colors.withAlpha(surface(), 0.78F));
      vg.rectOutline(cx, cy, cw, ch, 12.0F, 1.0F, Colors.withAlpha(accent(), 0.18F));

      // Header
      float ix = cx + 14.0F;
      float iy = cy + 10.0F;
      vg.rect(ix, iy, 22.0F, 22.0F, 6.0F, Colors.withAlpha(accent(), 0.3F));
      vg.text("C", ix + 15.0F, iy + 16.0F, 12.0F, accentBright());
      vg.text("CORZ", ix + 30.0F, iy + 16.0F, 12.0F, text());
      if (count > 0) {
         String cnt = Integer.toString(count);
         float cw2 = vg.textWidth(cnt, 9.0F) + 12.0F;
         vg.rect(cx + cw - cw2 - 14.0F, iy + 4.0F, cw2, 16.0F, 8.0F, Colors.withAlpha(accent(), 0.25F));
         vg.text(cnt, cx + cw - cw2 - 14.0F + 6.0F, iy + 15.0F, 9.0F, accent());
      }
      vg.line(cx + 10.0F, cy + headerH, cx + cw - 10.0F, cy + headerH, 1.0F, Colors.withAlpha(accent(), 0.12F));

      // Module rows
      float scrollY = this.corzScroll.value();
      vg.save();
      vg.scissor(cx + 6.0F, cy + headerH, cw - 12.0F, ch - headerH - 6.0F);
      float ry = cy + headerH + 6.0F - scrollY;
      for (CorzModuleHandle mod : mods) {
         if (ry + rowH < cy + headerH) { ry += rowH + 4.0F; continue; }
         if (ry > cy + ch) break;

         boolean enabled = mod.isEnabled();
         boolean hov = this.hit(mx, my, cx + 6.0F, ry, cw - 12.0F, rowH);
         int rowCol = enabled ? Colors.withAlpha(accent(), hov ? 0.28F : 0.18F)
            : Colors.withAlpha(surfaceElev(), hov ? 0.45F : 0.3F);
         vg.rect(cx + 6.0F, ry, cw - 12.0F, rowH, 7.0F, rowCol);
         if (enabled) {
            vg.rect(cx + 6.0F, ry + 5.0F, 3.0F, rowH - 10.0F, 1.5F, Colors.withAlpha(0xFFFFFF, 0.85F));
         }
         vg.textTruncated(mod.displayName().toUpperCase(Locale.ROOT), cx + 18.0F, ry + rowH * 0.5F + 4.0F, 10.0F,
            enabled ? text() : textMuted(), cw - 24.0F);

         // "corz" tag
         float tagW = 34.0F;
         float tagX = cx + cw - 6.0F - tagW;
         float tagY = ry + rowH * 0.5F - 7.0F;
         vg.rect(tagX, tagY, tagW, 14.0F, 7.0F, Colors.withAlpha(accent(), 0.2F));
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
         vg.rect(cx + cw - 6.0F, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(accent(), 0.4F));
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
      this.dockH = 56.0F;
      this.dockW = Math.min(560.0F, uiW - 80.0F);
      this.dockX = (uiW - this.dockW) * 0.5F;
      this.dockY = uiH - this.dockH - 10.0F;
      vg.rect(this.dockX, this.dockY, this.dockW, this.dockH, 14.0F, Colors.withAlpha(surface(), 0.82F));
      vg.rectOutline(this.dockX, this.dockY, this.dockW, this.dockH, 14.0F, 1.0F, Colors.withAlpha(accent(), 0.18F));

      int n = THEME_NAMES.length;
      float thumbW = (this.dockW - 28.0F - (n - 1) * 8.0F) / (float) n;
      float thumbH = this.dockH - 18.0F;
      float ty = this.dockY + 9.0F;
      for (int i = 0; i < n; i++) {
         float tx = this.dockX + 14.0F + i * (thumbW + 8.0F);
         boolean sel = i == this.themeIndex;
         boolean hov = this.hit(mx, my, tx, ty, thumbW, thumbH);
         int[] tp = THEMES[i];
         int swatch = rgb(tp, 5);
         int headerCol = rgb(tp, 9);

         vg.rect(tx, ty, thumbW, thumbH, 8.0F,
            sel ? (headerCol | 0xFF000000) : Colors.withAlpha(swatch, 0.45F));
         vg.rectOutline(tx, ty, thumbW, thumbH, 8.0F, 1.0F,
            sel ? Colors.withAlpha(accentBright(), 0.7F) : Colors.withAlpha(accent(), 0.12F));

         // Theme name
         String name = THEME_NAMES[i];
         vg.textTruncated(name, tx + (thumbW - vg.textWidth(name, 8.0F)) * 0.5F, ty + thumbH - 8.0F, 8.0F,
            sel ? 0xFFFFFFFF : textMuted(), thumbW - 6.0F);

         // Selection dot
         if (sel) {
            vg.circle(tx + thumbW * 0.5F, ty + thumbH + 3.0F, 2.4F, accentBright());
         } else if (hov) {
            vg.circle(tx + thumbW * 0.5F, ty + thumbH + 3.0F, 2.0F, accent());
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
         if (w.isVisible()) h += w.height(vg) + 4.0F;
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
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 30.0F) {
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
               this.scrollY = 0.0F;
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

      // Module rows
      for (int i = this.moduleHits.size() - 1; i >= 0; i--) {
         ModuleHit mh = this.moduleHits.get(i);
         if (this.hit(mx, my, mh.chevX, mh.chevY, mh.chevW, mh.chevH)) {
            Boolean prev = this.expanded.get(mh.module);
            this.expanded.put(mh.module, prev == null || !prev);
            UiSounds.select();
            return true;
         }
         if (this.hit(mx, my, mx, mh.settingsTop, mh.w, mh.settingsBottom - mh.settingsTop)) {
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
      // Body scroll
      if (this.hit(mx, my, this.bodyX, this.bodyY, this.bodyW, this.bodyH) && this.maxScroll > 0.0F) {
         this.scrollY = Math.clamp(this.scrollY + scrollY * 30.0F, 0.0F, this.maxScroll);
      }
      // Corz scroll
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
