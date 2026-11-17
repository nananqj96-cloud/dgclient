package dev.aether.gui;

import dev.aether.AetherClient;
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
import dev.aether.module.Modules;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.BlockListSetting;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.IconListSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.Setting;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.StringSetting;
import dev.aether.theme.SoundSettings;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental minimalistic GUI for <b>Aether Client</b>, in the style of the Minecraft "Vape"
 * client's ClickGUI: a flat, monochrome dark/white design with <b>no glow, no blur, and no glass
 * chrome</b>.
 *
 * <p>When the {@code Experimental Minimal GUI} setting on {@link dev.aether.module.Modules.ClickGuiModule}
 * is enabled, {@link ClickGuiScreen} renders through this class instead of the default Dashboard /
 * Columns chrome. It reuses Aether's real settings widgets (toggles, sliders, mode, color, keybind,
 * etc.) so every module — including the Aether Client module that holds the experimental toggle and
 * the GUI keybind — is fully editable here. A flat {@code Themes & Audio} tab mirrors the default
 * GUI's theme/audio studio. The 67port is left untouched and always uses its original GUI.
 *
 * <p>The constructor is intentionally trivial (no widget construction) so opening the default GUI is
 * unaffected. Settings widgets and the themes studio are built lazily on first use.
 *
 * <p><b>Geometry is computed during {@link #render} with the real renderer and cached for input.</b>
 * This avoids the classic stale-height bug where layout math (which needs text metrics) disagrees with
 * what is actually drawn, which made cards overlap and clicks misalign. All drawing and input routing
 * is exception-guarded so a render error can never crash the overlay.
 */
public final class AetherMinimalGui {
   // Flat Vape-style monochrome palette (no glow, no gradients).
   private static final int C_BG = 0xFF0F0F12;
   private static final int C_SURFACE = 0xFF18181B;
   private static final int C_HEADER = 0xFF131316;
   private static final int C_BORDER = 0xFF2A2A2E;
   private static final int C_BORDER_SOFT = 0xFF222226;
   private static final int C_TEXT = 0xFFF5F5F5;
   private static final int C_MUTED = 0xFF8A8A8A;
   private static final int C_DARK = 0xFF161616;
   private static final int C_WHITE = 0xFFFFFFFF;
   private static final int C_ACCENT = 0xFFFFFFFF;

   private static final float HEADER_H = 48.0F;
   private static final float SIDEBAR_W = 190.0F;
   private static final float CARD_H = 34.0F;
   private static final float CARD_GAP = 6.0F;
   private static final float CAT_H = 32.0F;
   private static final float CLOSE_SIZE = 20.0F;
   private static final float SEARCH_H = 30.0F;
   private static final float DOTS_SIZE = 18.0F;

   private final ModuleManager modules;
   private final ThemeManager realThemes;
   // Flat theme manager for the settings widgets; built lazily to keep the ctor trivial.
   private ThemeManager flatThemes;

   // Themes & Audio studio (uses the real theme manager so selections affect the client).
   // Built lazily on first use to keep the constructor trivial.
   private ColorSetting accentProxy;
   private ColorWidget accentWidget;
   private final List<SettingWidget> soundWidgets = new ArrayList<>();

   // Per-module settings widgets (built lazily on first expand), keyed by module.
   private final Map<Module, List<SettingWidget>> moduleWidgets = new LinkedHashMap<>();
   private final Map<Module, Boolean> expanded = new LinkedHashMap<>();

   // Selection / search / scroll state.
   private Category selectedCat; // null == ALL
   private boolean themesTab;
   private String search = "";
   private boolean searchFocused;
   private float scroll;
   private float maxScroll;

   // Header controls (recomputed every render, read by input).
   private float toggleX, toggleY, toggleW, toggleH;
   private float themesBtnX, themesBtnY, themesBtnW, themesBtnH;
   private float closeX, closeY;
   private float searchX, searchY, searchW, searchH;
   private float bodyX, bodyY, bodyW, bodyH;

   // Hit regions + active widgets rebuilt every render for exact input routing.
   private final List<CatHit> catHits = new ArrayList<>();
   private final List<ModuleHit> moduleHits = new ArrayList<>();
   private final List<ThemeHit> themeHits = new ArrayList<>();
   private final List<SettingWidget> activeWidgets = new ArrayList<>();

   private static final class CatHit {
      final float x, y, w, h;
      final Category cat; // null == ALL; THEMES handled via the themes flag
      final boolean themes;
      CatHit(float x, float y, float w, float h, Category cat, boolean themes) {
         this.x = x; this.y = y; this.w = w; this.h = h; this.cat = cat; this.themes = themes;
      }
   }

   private static final class ModuleHit {
      final Module module;
      final float headerX, headerY, headerW, headerH;
      final float dotsX, dotsY, dotsW, dotsH;
      final float settingsTop, settingsBottom;
      ModuleHit(Module module, float headerX, float headerY, float headerW, float headerH,
                float dotsX, float dotsY, float dotsW, float dotsH,
                float settingsTop, float settingsBottom) {
         this.module = module;
         this.headerX = headerX; this.headerY = headerY; this.headerW = headerW; this.headerH = headerH;
         this.dotsX = dotsX; this.dotsY = dotsY; this.dotsW = dotsW; this.dotsH = dotsH;
         this.settingsTop = settingsTop; this.settingsBottom = settingsBottom;
      }
   }

   private static final class ThemeHit {
      final float x, y, w, h;
      final Theme theme; // null == "+ Custom Theme"
      ThemeHit(float x, float y, float w, float h, Theme theme) {
         this.x = x; this.y = y; this.w = w; this.h = h; this.theme = theme;
      }
   }

   public AetherMinimalGui(ModuleManager modules, ThemeManager realThemes) {
      this.modules = modules;
      this.realThemes = realThemes;
   }

   /** Flat theme manager for the settings widgets; built lazily on first use to keep the ctor trivial. */
   private ThemeManager flat() {
      if (this.flatThemes == null) {
         ThemeManager tm = new ThemeManager();
         Theme flat = new Theme("Minimal", C_ACCENT, false);
         tm.getThemes().add(flat);
         tm.select(flat);
         this.flatThemes = tm;
      }
      return this.flatThemes;
   }

   /** Returns (building on first use) the settings widgets for a module, mirroring ModuleEntry. */
   private List<SettingWidget> widgetsFor(Module m) {
      List<SettingWidget> ws = this.moduleWidgets.get(m);
      if (ws != null) {
         return ws;
      }
      ws = new ArrayList<>();
      ThemeManager flat = flat();
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
      // Always expose the module's keybind so it can be bound in-game.
      ws.add(new KeybindWidget(flat, m.getKeybind()));
      this.moduleWidgets.put(m, ws);
      return ws;
   }

   /** Builds the themes & audio studio widgets on first use. */
   private void ensureThemesStudio() {
      if (this.accentProxy != null) {
         return;
      }
      this.accentProxy = new ColorSetting("Custom Accent", "Accent color for the active custom theme",
         this.realThemes.current().accent()) {
         @Override
         public void set(Integer value) {
            super.set(value);
            Theme cur = realThemes.current();
            if (cur.isCustom()) {
               cur.setAccent(value | 0xFF000000);
            }
         }
      };
      ThemeManager flat = flat();
      this.accentWidget = new ColorWidget(flat, this.accentProxy);
      SoundSettings sounds = AetherClient.sounds();
      if (sounds != null) {
         for (Setting<?> s : sounds.all()) {
            if (s instanceof SliderSetting sl) {
               this.soundWidgets.add(new SliderWidget(flat, sl));
            } else if (s instanceof BooleanSetting b) {
               this.soundWidgets.add(new BooleanWidget(flat, b));
            }
         }
      }
   }

   private Modules.ClickGuiModule clickGuiModule() {
      return this.modules.clickGui;
   }

   // =========================================================================
   // RENDER
   // =========================================================================

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.catHits.clear();
      this.moduleHits.clear();
      this.themeHits.clear();
      this.activeWidgets.clear();

      try {
         // Full-screen flat dark background — no glow.
         vg.rect(0.0F, 0.0F, uiW, uiH, 0.0F, C_BG);

         // Header band.
         vg.rect(0.0F, 0.0F, uiW, HEADER_H, 0.0F, C_HEADER);
         vg.rectOutline(0.0F, HEADER_H - 1.0F, uiW, 1.0F, 0.0F, C_BORDER);
         this.renderHeader(vg, mx, my, uiW);

         // Left sidebar.
         vg.rect(0.0F, HEADER_H, SIDEBAR_W, uiH - HEADER_H, 0.0F, C_SURFACE);
         vg.rectOutline(SIDEBAR_W - 1.0F, HEADER_H, 1.0F, uiH - HEADER_H, 0.0F, C_BORDER);
         this.renderSidebar(vg, mx, my, uiH);

         if (this.themesTab) {
            this.renderThemesStudio(vg, mx, my, uiW, uiH);
         } else {
            this.renderModuleList(vg, mx, my, uiW, uiH);
         }
      } catch (Throwable t) {
         AetherClient.LOGGER.error("Aether minimal GUI render failed", t);
      }
   }

   private void renderHeader(NVGRenderer vg, float mx, float my, float uiW) {
      // Title.
      vg.text("AETHER", 22.0F, 30.0F, 15.0F, C_TEXT);
      float subX = 22.0F + vg.textWidth("AETHER", 15.0F) + 8.0F;
      vg.text("minimal", subX, 30.0F, 10.0F, C_MUTED);

      // Experimental-UI toggle pill — prominently reachable from inside the minimal GUI.
      boolean minimalOn = this.clickGuiModule().experimentalMinimalGui.get();
      String toggleLabel = minimalOn ? "MINIMAL  ON" : "MINIMAL  OFF";
      float tw = vg.textWidth(toggleLabel, 11.0F) + 24.0F;
      this.toggleW = tw;
      this.toggleH = 26.0F;
      this.toggleX = 22.0F;
      this.toggleY = (HEADER_H - this.toggleH) * 0.5F;
      boolean tHov = this.hit(mx, my, this.toggleX, this.toggleY, this.toggleW, this.toggleH);
      vg.rect(this.toggleX, this.toggleY, this.toggleW, this.toggleH, 6.0F,
         minimalOn ? Colors.withAlpha(C_WHITE, 0.14F) : Colors.withAlpha(C_WHITE, 0.04F));
      vg.rectOutline(this.toggleX, this.toggleY, this.toggleW, this.toggleH, 6.0F, 1.0F,
         minimalOn ? Colors.withAlpha(C_WHITE, 0.45F) : C_BORDER_SOFT);
      vg.circle(this.toggleX + 13.0F, this.toggleY + this.toggleH * 0.5F, 4.0F, minimalOn ? C_WHITE : C_MUTED);
      vg.text(toggleLabel, this.toggleX + 24.0F, this.toggleY + this.toggleH * 0.5F, 11.0F,
         minimalOn ? C_TEXT : C_MUTED);

      // Themes & Audio button (right of header, left of close).
      String themesLabel = "THEMES & AUDIO";
      float thw = vg.textWidth(themesLabel, 11.0F) + 22.0F;
      this.themesBtnW = thw;
      this.themesBtnH = 26.0F;
      this.themesBtnX = uiW - 30.0F - CLOSE_SIZE - 8.0F - thw;
      this.themesBtnY = (HEADER_H - this.themesBtnH) * 0.5F;
      boolean thHov = this.hit(mx, my, this.themesBtnX, this.themesBtnY, this.themesBtnW, this.themesBtnH);
      vg.rect(this.themesBtnX, this.themesBtnY, this.themesBtnW, this.themesBtnH, 6.0F,
         this.themesTab ? Colors.withAlpha(C_WHITE, 0.14F) : (thHov ? Colors.withAlpha(C_WHITE, 0.06F) : Colors.withAlpha(C_WHITE, 0.03F)));
      vg.rectOutline(this.themesBtnX, this.themesBtnY, this.themesBtnW, this.themesBtnH, 6.0F, 1.0F,
         this.themesTab ? Colors.withAlpha(C_WHITE, 0.45F) : C_BORDER_SOFT);
      vg.text(themesLabel, this.themesBtnX + 11.0F, this.themesBtnY + this.themesBtnH * 0.5F, 11.0F,
         this.themesTab ? C_TEXT : (thHov ? C_TEXT : C_MUTED));

      // Close button (top-right).
      this.closeX = uiW - 30.0F;
      this.closeY = (HEADER_H - CLOSE_SIZE) * 0.5F;
      boolean cHov = this.hit(mx, my, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
      vg.rect(this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE, 5.0F, Colors.withAlpha(C_WHITE, cHov ? 0.14F : 0.05F));
      vg.cross(this.closeX + 5.0F, this.closeY + 5.0F, 10.0F, 1.4F, cHov ? C_TEXT : C_MUTED);
   }

   private void renderSidebar(NVGRenderer vg, float mx, float my, float uiH) {
      // Search box (top of sidebar).
      this.searchH = SEARCH_H;
      this.searchX = 12.0F;
      this.searchY = HEADER_H + 12.0F;
      this.searchW = SIDEBAR_W - 24.0F;
      this.drawSearchBox(vg, mx, my);

      float cy = this.searchY + this.searchH + 12.0F;
      this.addCat(vg, mx, my, cy, null, "ALL");
      cy += CAT_H + 4.0F;
      for (Category cat : Category.values()) {
         this.addCat(vg, mx, my, cy, cat, cat.getDisplayName());
         cy += CAT_H + 4.0F;
      }
      cy += 2.0F;
      this.addCat(vg, mx, my, cy, null, "THEMES & AUDIO");
   }

   private void addCat(NVGRenderer vg, float mx, float my, float y, Category cat, String label) {
      float x = 12.0F;
      float w = SIDEBAR_W - 24.0F;
      boolean isThemes = label.equals("THEMES & AUDIO");
      boolean selected = isThemes ? this.themesTab : (!this.themesTab && ((cat == null && this.selectedCat == null)
         || (cat != null && cat == this.selectedCat)));
      boolean hov = this.hit(mx, my, x, y, w, CAT_H);
      this.catHits.add(new CatHit(x, y, w, CAT_H, cat, isThemes));

      if (selected) {
         vg.rect(x, y, w, CAT_H, 6.0F, Colors.withAlpha(C_WHITE, 0.12F));
         vg.rectOutline(x, y, w, CAT_H, 6.0F, 1.0F, Colors.withAlpha(C_WHITE, 0.30F));
         vg.rect(x + 3.0F, y + 7.0F, 2.6F, CAT_H - 14.0F, 1.3F, C_WHITE);
      } else if (hov) {
         vg.rect(x, y, w, CAT_H, 6.0F, Colors.withAlpha(C_WHITE, 0.05F));
      }
      vg.text(label, x + 20.0F, y + CAT_H * 0.5F, 12.0F, selected ? C_TEXT : (hov ? C_TEXT : C_MUTED));
   }

   private void renderModuleList(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      float pad = 18.0F;
      this.bodyX = SIDEBAR_W + pad;
      this.bodyW = uiW - SIDEBAR_W - pad * 2.0F;
      this.bodyY = HEADER_H + 14.0F;
      this.bodyH = uiH - this.bodyY - 16.0F;

      vg.save();
      vg.scissor(this.bodyX - 1.0F, this.bodyY, this.bodyW + 2.0F, this.bodyH);

      float contentY = this.bodyY + 10.0F - this.scroll;
      float contentH = this.computeContentHeight(vg);
      this.maxScroll = Math.max(0.0F, contentH - this.bodyH);
      this.scroll = Math.clamp(this.scroll, 0.0F, this.maxScroll);

      for (Category cat : Category.values()) {
         if (this.selectedCat != null && this.selectedCat != cat) {
            continue;
         }
         List<Module> inCat = this.modules.inCategory(cat);
         if (inCat.isEmpty()) {
            continue;
         }
         vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT), this.bodyX, contentY + 12.0F, 10.5F, C_MUTED);
         contentY += 22.0F;

         for (Module module : inCat) {
            if (!this.matchesSearch(module)) {
               continue;
            }
            List<SettingWidget> ws = this.widgetsFor(module);
            boolean exp = Boolean.TRUE.equals(this.expanded.get(module));
            // Layout height uses the REAL renderer so wrapped ModeWidgets/expanded ColorWidgets
            // measure exactly as drawn — no stale height(null) divergence.
            float settingsH = exp ? this.settingsHeight(ws, vg) : 0.0F;
            float cardH = CARD_H + settingsH;
            boolean hov = this.hit(mx, my, this.bodyX, contentY, this.bodyW, CARD_H);
            boolean enabled = module.isEnabled();

            // Row card: enabled = paper-white tint, disabled = dark tint (Vape idiom).
            int card = enabled
               ? Colors.withAlpha(C_WHITE, hov ? 0.16F : 0.10F)
               : Colors.withAlpha(C_WHITE, hov ? 0.07F : 0.035F);
            vg.rect(this.bodyX, contentY, this.bodyW, cardH, 6.0F, card);
            vg.rectOutline(this.bodyX, contentY, this.bodyW, cardH, 6.0F, 0.8F,
               enabled ? Colors.withAlpha(C_WHITE, 0.22F) : C_BORDER_SOFT);

            // State dot + name.
            float dotX = this.bodyX + 18.0F;
            float dotY = contentY + CARD_H * 0.5F;
            vg.circle(dotX, dotY, 3.4F, enabled ? C_WHITE : C_MUTED);
            vg.text(module.getName(), this.bodyX + 30.0F, dotY, 12.0F, enabled ? C_DARK : C_MUTED);

            // Three-dots expand control (Vape idiom) at the right of the header.
            float dotsCX = this.bodyX + this.bodyW - 22.0F;
            float dotsCY = contentY + CARD_H * 0.5F;
            this.drawDots(vg, dotsCX, dotsCY, exp ? C_TEXT : C_MUTED);
            float dotsX = dotsCX - DOTS_SIZE * 0.5F;
            float dotsY = dotsCY - DOTS_SIZE * 0.5F;

            this.moduleHits.add(new ModuleHit(module, this.bodyX, contentY, this.bodyW, CARD_H,
               dotsX, dotsY, DOTS_SIZE, DOTS_SIZE, contentY + CARD_H, contentY + cardH));

            // Expanded settings rows.
            if (exp) {
               vg.line(this.bodyX + 10.0F, contentY + CARD_H, this.bodyX + this.bodyW - 10.0F, contentY + CARD_H,
                  1.0F, C_BORDER_SOFT);
               float wy = contentY + CARD_H + 8.0F;
               float innerW = this.bodyW - 24.0F;
               for (SettingWidget w : ws) {
                  if (!w.isVisible()) {
                     continue;
                  }
                  w.setBounds(this.bodyX + 12.0F, wy, innerW);
                  w.render(vg, mx, my);
                  this.activeWidgets.add(w);
                  wy += w.height(vg) + 4.0F;
               }
            }

            contentY += cardH + CARD_GAP;
         }
         contentY += 6.0F;
      }

      if (this.maxScroll > 1.0F) {
         float trackH = this.bodyH - 12.0F;
         float thumbH = Math.max(24.0F, trackH * (this.bodyH / Math.max(1.0F, contentH)));
         float thumbY = this.bodyY + 6.0F + (trackH - thumbH) * (this.scroll / this.maxScroll);
         vg.rect(this.bodyX + this.bodyW - 7.0F, this.bodyY + 6.0F, 2.5F, trackH, 1.2F, C_BORDER_SOFT);
         vg.rect(this.bodyX + this.bodyW - 7.0F, thumbY, 2.5F, thumbH, 1.2F, Colors.withAlpha(C_WHITE, 0.55F));
      }
      vg.restore();
   }

   private void drawDots(NVGRenderer vg, float cx, float cy, int color) {
      float off = 4.0F;
      vg.circle(cx - off, cy, 1.8F, color);
      vg.circle(cx, cy, 1.8F, color);
      vg.circle(cx + off, cy, 1.8F, color);
   }

   private void drawSearchBox(NVGRenderer vg, float mx, float my) {
      boolean hov = this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH);
      vg.rect(this.searchX, this.searchY, this.searchW, this.searchH, 8.0F, Colors.withAlpha(C_HEADER, 0.9F));
      vg.rectOutline(this.searchX, this.searchY, this.searchW, this.searchH, 8.0F, 1.0F,
         this.searchFocused ? Colors.withAlpha(C_WHITE, 0.55F) : C_BORDER);
      float cy = this.searchY + this.searchH * 0.5F;
      if (this.search.isEmpty() && !this.searchFocused) {
         vg.text("Search...", this.searchX + 14.0F, cy, 11.0F, C_MUTED);
      } else {
         vg.textTruncated(this.search, this.searchX + 14.0F, cy, 11.5F, C_TEXT, this.searchW - 40.0F);
      }
      if (!this.search.isEmpty()) {
         vg.cross(this.searchX + this.searchW - 18.0F, cy - 5.0F, 10.0F, 1.5F, C_MUTED);
      }
   }

   private float settingsHeight(List<SettingWidget> ws, NVGRenderer vg) {
      float h = 8.0F;
      for (SettingWidget w : ws) {
         if (w.isVisible()) {
            h += w.height(vg) + 4.0F;
         }
      }
      return h + 4.0F;
   }

   private boolean matchesSearch(Module m) {
      if (this.search.isEmpty()) {
         return true;
      }
      return m.getName().toLowerCase(Locale.ROOT).contains(this.search);
   }

   private float computeContentHeight(NVGRenderer vg) {
      float h = 10.0F;
      for (Category cat : Category.values()) {
         if (this.selectedCat != null && this.selectedCat != cat) {
            continue;
         }
         List<Module> inCat = this.modules.inCategory(cat);
         if (inCat.isEmpty()) {
            continue;
         }
         h += 22.0F;
         for (Module module : inCat) {
            if (!this.matchesSearch(module)) {
               continue;
            }
            boolean exp = Boolean.TRUE.equals(this.expanded.get(module));
            float ws = exp ? this.settingsHeight(this.widgetsFor(module), vg) : 0.0F;
            h += CARD_H + ws + CARD_GAP;
         }
         h += 6.0F;
      }
      return h;
   }

   // =========================================================================
   // THEMES & AUDIO STUDIO (flat)
   // =========================================================================

   private void renderThemesStudio(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.ensureThemesStudio();
      float pad = 18.0F;
      this.bodyX = SIDEBAR_W + pad;
      this.bodyW = uiW - SIDEBAR_W - pad * 2.0F;
      this.bodyY = HEADER_H + 14.0F;
      this.bodyH = uiH - this.bodyY - 16.0F;

      vg.text("THEMES & AUDIO", this.bodyX, this.bodyY + 16.0F, 13.0F, C_TEXT);
      float y = this.bodyY + 38.0F;

      Theme active = this.realThemes.current();
      vg.text("ACCENT PALETTES", this.bodyX, y + 6.0F, 10.5F, C_MUTED);
      y += 22.0F;

      List<Theme> all = this.realThemes.getThemes();
      int cols = 3;
      float gap = 10.0F;
      float cardW = (this.bodyW - gap * (cols - 1)) / (float) cols;
      float cardH = 44.0F;
      int total = all.size() + 1;

      for (int i = 0; i < total; i++) {
         int col = i % cols;
         int row = i / cols;
         float cx = this.bodyX + col * (cardW + gap);
         float cy = y + row * (cardH + gap);
         boolean hov = this.hit(mx, my, cx, cy, cardW, cardH);
         this.themeHits.add(new ThemeHit(cx, cy, cardW, cardH, i < all.size() ? all.get(i) : null));

         if (i < all.size()) {
            Theme t = all.get(i);
            boolean sel = t == active;
            vg.rect(cx, cy, cardW, cardH, 8.0F, sel ? Colors.withAlpha(C_WHITE, 0.12F)
               : (hov ? Colors.withAlpha(C_WHITE, 0.06F) : Colors.withAlpha(C_WHITE, 0.03F)));
            vg.rectOutline(cx, cy, cardW, cardH, 8.0F, 1.0F, sel ? Colors.withAlpha(C_WHITE, 0.45F) : C_BORDER_SOFT);
            vg.circle(cx + 20.0F, cy + cardH * 0.5F, 7.5F, t.accent());
            vg.textTruncated(t.getName(), cx + 36.0F, cy + cardH * 0.5F, 12.0F, sel ? C_TEXT : C_MUTED, cardW - 52.0F);
            if (t.isCustom()) {
               vg.cross(cx + cardW - 20.0F, cy + cardH * 0.5F - 5.0F, 10.0F, 1.5F, C_MUTED);
            }
         } else {
            vg.rect(cx, cy, cardW, cardH, 8.0F, Colors.withAlpha(C_WHITE, hov ? 0.10F : 0.05F));
            vg.rectOutline(cx, cy, cardW, cardH, 8.0F, 1.0F, Colors.withAlpha(C_WHITE, hov ? 0.40F : 0.20F));
            String lbl = "+ Custom Theme";
            float lw = vg.textWidth(lbl, 12.0F);
            vg.text(lbl, cx + (cardW - lw) * 0.5F, cy + cardH * 0.5F, 12.0F, hov ? C_TEXT : C_MUTED);
         }
      }
      int rows = (total + cols - 1) / cols;
      y += rows * (cardH + gap) + 10.0F;

      if (active.isCustom()) {
         float pickH = this.accentWidget.height(vg) + 16.0F;
         vg.rect(this.bodyX, y, this.bodyW, pickH, 8.0F, Colors.withAlpha(C_HEADER, 0.6F));
         vg.rectOutline(this.bodyX, y, this.bodyW, pickH, 8.0F, 1.0F, C_BORDER_SOFT);
         this.accentWidget.setBounds(this.bodyX + 14.0F, y + 8.0F, this.bodyW - 28.0F);
         this.accentWidget.render(vg, mx, my);
         this.activeWidgets.add(this.accentWidget);
         y += pickH + 14.0F;
      }

      vg.text("INTERFACE AUDIO", this.bodyX, y + 6.0F, 10.5F, C_MUTED);
      y += 22.0F;
      float audioY = y;
      float innerY = y + 10.0F;
      for (SettingWidget sw : this.soundWidgets) {
         sw.setBounds(this.bodyX + 14.0F, innerY, this.bodyW - 28.0F);
         innerY += sw.height(vg) + 4.0F;
      }
      float audioH = innerY - audioY + 6.0F;
      vg.rect(this.bodyX, audioY, this.bodyW, audioH, 8.0F, Colors.withAlpha(C_HEADER, 0.6F));
      vg.rectOutline(this.bodyX, audioY, this.bodyW, audioH, 8.0F, 1.0F, C_BORDER_SOFT);
      for (SettingWidget sw : this.soundWidgets) {
         sw.render(vg, mx, my);
         this.activeWidgets.add(sw);
      }
   }

   // =========================================================================
   // INPUT
   // =========================================================================

   public boolean mouseClicked(float mx, float my, int btn) {
      if (btn != 0) {
         return false;
      }
      // Close button.
      if (this.hit(mx, my, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE)) {
         return true;
      }
      // Header: experimental-UI toggle (turn the minimal GUI off).
      if (this.hit(mx, my, this.toggleX, this.toggleY, this.toggleW, this.toggleH)) {
         this.clickGuiModule().experimentalMinimalGui.set(!this.clickGuiModule().experimentalMinimalGui.get());
         UiSounds.select();
         return true;
      }
      // Header: Themes & Audio button.
      if (this.hit(mx, my, this.themesBtnX, this.themesBtnY, this.themesBtnW, this.themesBtnH)) {
         this.themesTab = !this.themesTab;
         UiSounds.select();
         return true;
      }

      if (this.themesTab) {
         this.ensureThemesStudio();
         // Themes studio clicks: accent picker (only when a custom theme is active), audio, cards.
         if (this.realThemes.current().isCustom() && this.accentWidget.mouseClicked(mx, my, btn)) {
            return true;
         }
         for (SettingWidget sw : this.soundWidgets) {
            if (sw.mouseClicked(mx, my, btn)) {
               return true;
            }
         }
         for (int i = this.themeHits.size() - 1; i >= 0; i--) {
            ThemeHit th = this.themeHits.get(i);
            if (this.hit(mx, my, th.x, th.y, th.w, th.h)) {
               if (th.theme == null) {
                  Theme custom = this.realThemes.addCustom(this.realThemes.current().accent());
                  this.realThemes.select(custom);
                  this.accentProxy.set(Integer.valueOf(custom.accent()));
               } else if (th.theme.isCustom() && mx >= th.x + th.w - 26.0F) {
                  this.realThemes.removeCustom(th.theme);
               } else {
                  this.realThemes.select(th.theme);
                  if (th.theme.isCustom()) {
                     this.accentProxy.set(Integer.valueOf(th.theme.accent()));
                  }
               }
               UiSounds.select();
               return true;
            }
         }
         return true;
      }

      // Search box.
      if (this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 22.0F) {
            this.search = "";
         } else {
            this.searchFocused = true;
         }
         return true;
      }
      this.searchFocused = false;

      // Sidebar category selection.
      for (int i = this.catHits.size() - 1; i >= 0; i--) {
         CatHit ch = this.catHits.get(i);
         if (this.hit(mx, my, ch.x, ch.y, ch.w, ch.h)) {
            if (ch.themes) {
               this.themesTab = true;
            } else {
               this.themesTab = false;
               this.selectedCat = ch.cat;
               this.scroll = 0.0F;
            }
            UiSounds.select();
            return true;
         }
      }

      // Module cards: three-dots expand, header toggles, settings widgets capture (hit priority).
      for (int i = this.moduleHits.size() - 1; i >= 0; i--) {
         ModuleHit mh = this.moduleHits.get(i);
         if (this.hit(mx, my, mh.dotsX, mh.dotsY, mh.dotsW, mh.dotsH)) {
            Boolean prev = this.expanded.get(mh.module);
            this.expanded.put(mh.module, prev == null || !prev);
            UiSounds.select();
            return true;
         }
         // Settings rows of an expanded card capture first (so they sit above the header).
         if (this.hit(mx, my, mh.headerX, mh.settingsTop, mh.headerW, mh.settingsBottom - mh.settingsTop)) {
            for (SettingWidget w : this.widgetsFor(mh.module)) {
               if (w.isVisible() && w.mouseClicked(mx, my, btn)) {
                  return true;
               }
            }
         }
         if (this.hit(mx, my, mh.headerX, mh.headerY, mh.headerW, mh.headerH)) {
            mh.module.toggle();
            UiSounds.toggle(mh.module.isEnabled());
            return true;
         }
      }
      return true;
   }

   /** True if the click is on the close button (the caller closes the whole GUI). */
   public boolean hitClose(float mx, float my) {
      return this.hit(mx, my, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
   }

   public void mouseDragged(float mx, float my) {
      for (SettingWidget w : this.activeWidgets) {
         w.mouseDragged(mx, my);
      }
   }

   public void mouseReleased() {
      for (SettingWidget w : this.activeWidgets) {
         w.mouseReleased();
      }
   }

   public boolean mouseScrolled(float mx, float my, double scrollY, float uiW, float uiH) {
      if (this.themesTab) {
         return false;
      }
      if (this.hit(mx, my, this.bodyX, this.bodyY, this.bodyW, this.bodyH) && this.maxScroll > 0.0F) {
         this.scroll = Math.clamp(this.scroll - (float) scrollY * 34.0F, 0.0F, this.maxScroll);
         return true;
      }
      return false;
   }

   public boolean keyPressed(int keyCode) {
      // Route to a listening widget (keybind capture) first.
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
