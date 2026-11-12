package dev.aether.gui;

import dev.aether.AetherClient;
import dev.aether.corz.CorzPortBus;
import dev.aether.corz.CorzPortProvider;
import dev.aether.gui.config.ConfigPanel;
import dev.aether.gui.corz.CorzPortWindow;
import dev.aether.gui.panel.CategoryPanel;
import dev.aether.gui.panel.ModuleEntry;
import dev.aether.gui.panel.Panel;
import dev.aether.gui.panel.ThemesPanel;
import dev.aether.gui.picker.BlockGridModel;
import dev.aether.gui.picker.IconListGridModel;
import dev.aether.gui.widget.BooleanWidget;
import dev.aether.gui.widget.ColorWidget;
import dev.aether.gui.widget.SettingWidget;
import dev.aether.gui.widget.SliderWidget;
import dev.aether.module.Category;
import dev.aether.module.ModuleManager;
import dev.aether.module.Modules;
import dev.aether.module.impl.BlockEspModule;
import dev.aether.render.BlurHook;
import dev.aether.render.NvgDrawable;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGIcons;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.BlockListSetting;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.IconListSetting;
import dev.aether.settings.Setting;
import dev.aether.settings.SliderSetting;
import dev.aether.theme.SoundSettings;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen implements NvgDrawable {
   private static final ClickGuiState STATE = new ClickGuiState();
   private static final float SIDEBAR_W = 186.0F;
   private static final float TOPBAR_H = 54.0F;
   private static final float WIN_RADIUS = 13.0F;

   private final List<Panel> panels = new ArrayList<>();
   private final Map<Category, CategoryPanel> categoryPanels = new EnumMap<>(Category.class);
   private final List<ModuleEntry> dashVisibleScratch = new ArrayList<>(64);
   private final Animation openAnim = new Animation(160.0F, 0.0F);
   private final Animation dashScroll = new Animation(150.0F, 0.0F);
   private float dashMaxScroll;
   private boolean closing;
   private final ConfigPanel configPanel = new ConfigPanel();
   private final CorzPortWindow corzPortWindow;

   // Themes & Audio studio widgets for Dashboard mode
   private final ColorSetting dashAccentProxy;
   private final ColorWidget dashAccentWidget;
   private final List<SettingWidget> dashSoundWidgets = new ArrayList<>();

   // Search & Filter state
   private final StringBuilder search = new StringBuilder();
   private boolean searchFocused;
   private boolean favouritesOnly;

   // Cached interactive bounds (updated on render for exact hit testing)
   private float winX;
   private float winY;
   private float winW;
   private float winH;
   private float searchBoxX;
   private float searchBoxY;
   private float searchBoxW;
   private float searchBoxH;
   private float favBtnX;
   private float favBtnY;
   private float favBtnW;
   private float favBtnH;
   private float modeBtnX;
   private float modeBtnY;
   private float modeBtnW;
   private float modeBtnH;
   private float cfgBtnX;
   private float cfgBtnY;
   private float cfgBtnW;
   private float cfgBtnH;
   private float corzTabX;
   private float corzTabY;
   private float corzTabW;
   private float corzTabH;

   // Window drag (Dashboard header) & Column panel drag
   private boolean draggingWindow;
   private float winOffsetX;
   private float winOffsetY;
   private boolean windowMoved;
   private float customWinX;
   private float customWinY;
   private Panel dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private float pressX;
   private float pressY;
   private boolean dragMoved;
   private Panel pressedContentPanel;
   private ModuleEntry pressedDashEntry;
   private boolean pressedInThemesStudio;
   private boolean pressedInCorzPort;
   private final Screen parent;

   public ClickGuiScreen() {
      this(null);
   }

   public ClickGuiScreen(Screen parent) {
      super(Component.literal("Aether Client"));
      this.parent = parent;
      STATE.ensureDefaultLayout(OverlayRenderer.uiWidth(), OverlayRenderer.uiHeight());
      ModuleManager modules = AetherClient.modules();
      ThemeManager themes = AetherClient.themes();
      this.corzPortWindow = new CorzPortWindow(themes, STATE);

      for (Category cat : Category.values()) {
         CategoryPanel cp = new CategoryPanel(cat, modules, themes, STATE);
         this.categoryPanels.put(cat, cp);
         this.panels.add(cp);
      }
      this.panels.add(new ThemesPanel(themes, STATE));

      for (String expKey : STATE.expandedKeys()) {
         int at = expKey.lastIndexOf('@');
         if (at > 0 && at + 1 < expKey.length()) {
            try {
               STATE.selectCategory(Category.valueOf(expKey.substring(at + 1)));
               break;
            } catch (IllegalArgumentException ignored) {
            }
         }
      }

      this.dashAccentProxy = new ColorSetting("Custom Accent", "Accent color for active custom theme", themes.current().accent()) {
         @Override
         public void set(Integer value) {
            super.set(value);
            Theme cur = themes.current();
            if (cur.isCustom()) {
               cur.setAccent(value | 0xFF000000);
            }
         }
      };
      this.dashAccentWidget = new ColorWidget(themes, this.dashAccentProxy);
      SoundSettings sounds = AetherClient.sounds();
      if (sounds != null) {
         for (Setting<?> s : sounds.all()) {
            if (s instanceof SliderSetting sl) {
               this.dashSoundWidgets.add(new SliderWidget(themes, sl));
            } else if (s instanceof BooleanSetting b) {
               this.dashSoundWidgets.add(new BooleanWidget(themes, b));
            }
         }
      }

      this.openAnim.setTarget(1.0F);
   }

   @Override
   public void added() {
      super.added();
      UiSounds.guiOpen();
   }

   public static ClickGuiState state() {
      return STATE;
   }

   public CorzPortWindow corzPortWindow() {
      return this.corzPortWindow;
   }

   public void toggleCorzPort() {
      STATE.toggleCorzPort();
      if (STATE.isCorzPortOpen()) {
         UiSounds.guiOpen();
      } else {
         UiSounds.guiClose();
      }
   }

   private Modules.ClickGuiModule guiModule() {
      return AetherClient.modules().clickGui;
   }

   public boolean isDashboardMode() {
      return !"Columns".equals(this.guiModule().layout.get());
   }

   public void toggleLayoutMode() {
      this.guiModule().layout.set(this.isDashboardMode() ? "Columns" : "Dashboard");
      this.dashScroll.snapTo(0.0F);
      UiSounds.select();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void onClose() {
      if (!this.closing) {
         this.closing = true;
         this.openAnim.setTarget(0.0F);
         UiSounds.guiClose();
      }
   }

   @Override
   public void removed() {
      BlurHook.clear();
      AetherClient.config().save();
      CorzPortProvider corz = CorzPortBus.provider();
      if (corz != null) {
         corz.saveConfig();
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      Theme t = AetherClient.themes().current();
      int alpha = (int)(92.0F * this.openAnim.value());
      int tint = t.isBloom() ? 0x120718 : 0x050811;
      graphics.fill(0, 0, this.width, this.height, (alpha << 24) | tint);
      this.finishCloseIfDone();
   }

   @Override
   public void tick() {
      this.finishCloseIfDone();
   }

   private void finishCloseIfDone() {
      if (this.closing && this.openAnim.isDone()) {
         this.minecraft.setScreen(this.parent);
      }
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.minecraft.level == null) {
         this.renderPanorama(graphics, partialTick);
      }
      if (this.guiModule().blur.get()) {
         BlurHook.set(this.guiModule().blurStrength.getFloat() * this.openAnim.value());
         graphics.blurBeforeThisStratum();
      } else {
         BlurHook.clear();
      }
   }

   @Override
   public void renderNvg(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      vg.setFontMode(this.guiModule().font.get());
      if (!vg.hasFont()) {
         return;
      }
      this.corzPortWindow.syncThemeToCorz();
      float anim = this.openAnim.value();
      if (anim <= 0.002F && this.closing) {
         return;
      }

      Theme theme = AetherClient.themes().current();
      vg.save();
      vg.alpha(anim);
      vg.rect(0.0F, 0.0F, uiW, uiH, 0.0F, Colors.withAlpha(theme.isBloom() ? 0xFF100616 : 0xFF060912, 0.48F * anim));

      if (this.isDashboardMode()) {
         this.renderDashboardMode(vg, mx, my, uiW, uiH, anim);
      } else {
         this.renderColumnsMode(vg, mx, my, uiW, uiH, anim);
      }

      this.configPanel.render(vg, mx, my, uiW, uiH);
      vg.restore();
   }

   // =========================================================================
   // MODE 1: UNIFIED SIDEBAR DASHBOARD WINDOW (+ SYNCED CORZ PORT WINDOW)
   // =========================================================================

   private void renderDashboardMode(NVGRenderer vg, float mx, float my, float uiW, float uiH, float anim) {
      Theme theme = AetherClient.themes().current();
      float corzProg = this.corzPortWindow.openProgress();
      float corzW = this.corzPortWindow.preferredWidth(uiW);
      float gap = 14.0F * corzProg;

      float maxDashW = uiW - 40.0F - (corzW + 14.0F) * corzProg;
      this.winW = Math.clamp(maxDashW, 560.0F, 880.0F);
      this.winH = Math.clamp(uiH - 64.0F, 400.0F, 580.0F);

      float combinedW = this.winW + gap + corzW * corzProg;
      float defaultDashX = Math.max(12.0F, (uiW - combinedW) * 0.5F);
      float defaultDashY = (uiH - this.winH) * 0.5F;

      if (this.windowMoved) {
         this.winX = Math.clamp(this.customWinX, 12.0F, Math.max(12.0F, uiW - this.winW - 12.0F));
         this.winY = Math.clamp(this.customWinY, 12.0F, Math.max(12.0F, uiH - this.winH - 12.0F));
      } else {
         this.winX = defaultDashX;
         this.winY = defaultDashY;
      }

      vg.save();
      float scale = 0.96F + 0.04F * anim;
      vg.translate(this.winX + this.winW * 0.5F, this.winY + this.winH * 0.5F);
      vg.scale(scale);
      vg.translate(-(this.winX + this.winW * 0.5F), -(this.winY + this.winH * 0.5F));

      // PinkBloom optical glow layers + dark velvet glass shell
      float bloom = theme.bloomStrength();
      vg.glow(this.winX, this.winY + 4.0F, this.winW, this.winH, WIN_RADIUS, 20.0F, Colors.withAlpha(0xFF000000, 0.52F));
      vg.glow(this.winX - 1.0F, this.winY - 1.0F, this.winW + 2.0F, this.winH + 2.0F, WIN_RADIUS, 20.0F * bloom + 6.0F, Colors.withAlpha(theme.accent(), 0.25F * bloom + 0.08F));
      vg.rectGradient(this.winX, this.winY, this.winW, this.winH, WIN_RADIUS, theme.background(), theme.backgroundTo(), true);

      // Left Sidebar surface
      vg.rectVaryingGradient(this.winX, this.winY, SIDEBAR_W, this.winH, WIN_RADIUS, 0.0F, 0.0F, WIN_RADIUS, theme.headerTop(), theme.headerBottom());
      vg.line(this.winX + SIDEBAR_W, this.winY + 1.0F, this.winX + SIDEBAR_W, this.winY + this.winH - 1.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));

      float mainX = this.winX + SIDEBAR_W;
      float mainW = this.winW - SIDEBAR_W;
      vg.line(mainX, this.winY + TOPBAR_H, this.winX + this.winW - 1.0F, this.winY + TOPBAR_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));

      // Outer crisp border + top bloom highlight
      vg.rectOutline(this.winX, this.winY, this.winW, this.winH, WIN_RADIUS, 1.15F, Colors.withAlpha(theme.accentBright(), 0.42F));
      vg.rect(this.winX + 20.0F, this.winY, 58.0F, 2.2F, 1.1F, theme.accentBright());

      this.renderSidebar(vg, theme, mx, my);
      this.renderDashboardTopBar(vg, theme, mx, my, mainX, mainW);
      this.renderDashboardBody(vg, theme, mx, my, mainX, this.winY + TOPBAR_H, mainW, this.winH - TOPBAR_H);

      vg.restore();

      // Render Synced Second UI ("Corz Port") alongside the main window
      if (this.corzPortWindow.isVisible()) {
         float corzDefaultX = this.winX + this.winW + 14.0F;
         this.corzPortWindow.render(vg, mx, my, uiW, uiH, corzDefaultX, this.winY, this.winH);
      }
   }

   private void renderSidebar(NVGRenderer vg, Theme theme, float mx, float my) {
      float bx = this.winX + 16.0F;
      float by = this.winY + 27.0F;
      if (theme.isBloom()) {
         vg.circleGlow(bx + 10.0F, by, 10.0F, 8.0F, Colors.withAlpha(theme.accent(), 0.30F));
      }
      vg.aetherMark(bx + 10.0F, by, 10.2F, theme.accent(), theme.accentBright());
      vg.textGradient("AETHER", bx + 28.0F, by - 5.0F, 15.5F, theme.accentBright(), theme.accent());
      vg.text("PINKBLOOM  v" + AetherClient.VERSION, bx + 28.0F, by + 9.5F, 9.0F, theme.textDisabled());

      vg.line(this.winX + 14.0F, this.winY + TOPBAR_H, this.winX + SIDEBAR_W - 14.0F, this.winY + TOPBAR_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.7F));

      float navY = this.winY + TOPBAR_H + 10.0F;
      vg.text("CATEGORIES", this.winX + 16.0F, navY + 4.0F, 9.5F, theme.textDisabled());
      navY += 15.0F;

      boolean globalFilterActive = !this.search.isEmpty() || this.favouritesOnly;
      Category activeCat = STATE.selectedCategory();
      boolean themesActive = STATE.isThemesTabSelected() && !globalFilterActive;

      for (Category cat : Category.values()) {
         boolean selected = !themesActive && !globalFilterActive && cat == activeCat;
         float rx = this.winX + 10.0F;
         float rw = SIDEBAR_W - 20.0F;
         float rh = 30.0F;
         boolean hov = mx >= rx && mx <= rx + rw && my >= navY && my <= navY + rh;

         if (selected) {
            if (theme.isBloom()) {
               vg.glow(rx, navY, rw, rh, 7.0F, 7.0F, Colors.withAlpha(theme.accent(), 0.22F));
            }
            vg.rect(rx, navY, rw, rh, 7.0F, Colors.withAlpha(theme.accent(), 0.20F));
            vg.rectOutline(rx, navY, rw, rh, 7.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.52F));
            vg.rect(rx + 2.0F, navY + 6.5F, 3.0F, rh - 13.0F, 1.5F, theme.accentBright());
         } else if (hov) {
            vg.rect(rx, navY, rw, rh, 7.0F, Colors.withAlpha(0xFFFFFFFF, 0.05F));
         }

         int icon = NVGIcons.get(cat);
         float cy = navY + rh * 0.5F;
         if (icon > 0) {
            vg.image(icon, rx + 11.0F, cy - 7.0F, 14.0F, 14.0F, selected ? theme.accentBright() : (hov ? theme.textPrimary() : theme.textMuted()));
         }
         vg.text(cat.getDisplayName(), rx + 32.0F, cy, 12.2F, selected ? theme.textPrimary() : (hov ? theme.textPrimary() : theme.textMuted()));

         CategoryPanel cp = this.categoryPanels.get(cat);
         int activeCount = 0;
         for (ModuleEntry me : cp.allEntries()) {
            if (me.getModule().isEnabled()) {
               activeCount++;
            }
         }
         if (activeCount > 0) {
            String cnt = Integer.toString(activeCount);
            float cw = vg.textWidth(cnt, 9.5F) + 10.0F;
            float cx = rx + rw - cw - 6.0F;
            vg.rect(cx, cy - 6.5F, cw, 13.0F, 6.5F, Colors.withAlpha(theme.accent(), selected ? 0.32F : 0.16F));
            vg.text(cnt, cx + 5.0F, cy, 9.5F, theme.accentBright());
         }
         navY += rh + 3.0F;
      }

      // SUBTABS section -> "Corz Port" synced secondary UI launcher!
      navY += 6.0F;
      vg.text("SUBTABS", this.winX + 16.0F, navY + 4.0F, 9.5F, theme.textDisabled());
      navY += 15.0F;
      this.corzTabX = this.winX + 10.0F;
      this.corzTabY = navY;
      this.corzTabW = SIDEBAR_W - 20.0F;
      this.corzTabH = 31.0F;
      boolean corzOpen = STATE.isCorzPortOpen();
      boolean corzHov = this.hit(mx, my, this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH);
      if (corzOpen) {
         vg.glow(this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH, 7.5F, 8.0F, Colors.withAlpha(theme.accent(), 0.28F));
         vg.rect(this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH, 7.5F, Colors.withAlpha(theme.accent(), 0.24F));
         vg.rectOutline(this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH, 7.5F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.72F));
         vg.rect(this.corzTabX + 2.0F, this.corzTabY + 6.5F, 3.0F, this.corzTabH - 13.0F, 1.5F, theme.accentBright());
      } else {
         vg.rect(this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH, 7.5F, Colors.withAlpha(theme.accent(), corzHov ? 0.15F : 0.08F));
         vg.rectOutline(this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH, 7.5F, 1.0F, Colors.withAlpha(theme.accentBright(), corzHov ? 0.45F : 0.24F));
      }
      float ccy = this.corzTabY + this.corzTabH * 0.5F;
      vg.aetherMark(this.corzTabX + 18.0F, ccy, 6.2F, theme.accent(), theme.accentBright());
      vg.text("Corz Port", this.corzTabX + 32.0F, ccy, 12.2F, corzOpen || corzHov ? theme.textPrimary() : theme.textMuted());
      String stateBadge = corzOpen ? "OPEN" : "2ND UI";
      float sbw = vg.textWidth(stateBadge, 8.5F) + 9.0F;
      float sbx = this.corzTabX + this.corzTabW - sbw - 6.0F;
      vg.rect(sbx, ccy - 6.5F, sbw, 13.0F, 6.5F, Colors.withAlpha(theme.accent(), corzOpen ? 0.38F : 0.18F));
      vg.text(stateBadge, sbx + 4.5F, ccy, 8.5F, theme.accentBright());
      navY += this.corzTabH + 6.0F;

      // Themes & Audio tab
      vg.text("CUSTOMIZE", this.winX + 16.0F, navY + 4.0F, 9.5F, theme.textDisabled());
      navY += 15.0F;
      float tx = this.winX + 10.0F;
      float tw = SIDEBAR_W - 20.0F;
      float th = 30.0F;
      boolean tHov = mx >= tx && mx <= tx + tw && my >= navY && my <= navY + th;
      if (themesActive) {
         vg.rect(tx, navY, tw, th, 7.0F, Colors.withAlpha(theme.accent(), 0.20F));
         vg.rectOutline(tx, navY, tw, th, 7.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.52F));
         vg.rect(tx + 2.0F, navY + 6.5F, 3.0F, th - 13.0F, 1.5F, theme.accentBright());
      } else if (tHov) {
         vg.rect(tx, navY, tw, th, 7.0F, Colors.withAlpha(0xFFFFFFFF, 0.05F));
      }
      vg.circle(tx + 18.0F, navY + th * 0.5F, 5.2F, theme.accent());
      vg.circleOutline(tx + 18.0F, navY + th * 0.5F, 7.2F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.65F));
      vg.text("Themes & Audio", tx + 32.0F, navY + th * 0.5F, 12.2F, themesActive ? theme.textPrimary() : (tHov ? theme.textPrimary() : theme.textMuted()));

      // Bottom Config Profiles button
      this.cfgBtnW = SIDEBAR_W - 24.0F;
      this.cfgBtnH = 33.0F;
      this.cfgBtnX = this.winX + 12.0F;
      this.cfgBtnY = this.winY + this.winH - this.cfgBtnH - 12.0F;
      boolean cHov = this.hit(mx, my, this.cfgBtnX, this.cfgBtnY, this.cfgBtnW, this.cfgBtnH);
      vg.rect(this.cfgBtnX, this.cfgBtnY, this.cfgBtnW, this.cfgBtnH, 8.0F, Colors.withAlpha(theme.accent(), cHov ? 0.24F : 0.14F));
      vg.rectOutline(this.cfgBtnX, this.cfgBtnY, this.cfgBtnW, this.cfgBtnH, 8.0F, 1.0F, Colors.withAlpha(theme.accentBright(), cHov ? 0.70F : 0.34F));
      String cfgLabel = "Config Profiles";
      float clw = vg.textWidth(cfgLabel, 11.8F);
      vg.text(cfgLabel, this.cfgBtnX + (this.cfgBtnW - clw) * 0.5F, this.cfgBtnY + this.cfgBtnH * 0.5F, 11.8F, cHov ? theme.accentBright() : theme.textPrimary());
   }

   private void renderDashboardTopBar(NVGRenderer vg, Theme theme, float mx, float my, float mainX, float mainW) {
      float cy = this.winY + TOPBAR_H * 0.5F;
      List<ModuleEntry> vis = this.collectDashboardEntries();
      String heading;
      if (!this.search.isEmpty()) {
         heading = "Search Results";
      } else if (this.favouritesOnly) {
         heading = "Favourites";
      } else if (STATE.isThemesTabSelected()) {
         heading = "Themes & Audio";
      } else {
         heading = STATE.selectedCategory().getDisplayName();
      }
      float hw = vg.text(heading, mainX + 18.0F, cy, 15.0F, theme.textPrimary());

      if (!STATE.isThemesTabSelected() || !this.search.isEmpty() || this.favouritesOnly) {
         String countPill = Integer.toString(vis.size());
         float cpw = vg.textWidth(countPill, 10.5F) + 12.0F;
         vg.rect(mainX + 24.0F + hw, cy - 8.0F, cpw, 16.0F, 8.0F, Colors.withAlpha(theme.accent(), 0.16F));
         vg.text(countPill, mainX + 30.0F + hw, cy, 10.5F, theme.accentBright());
      }

      float rightEdge = mainX + mainW - 16.0F;

      this.modeBtnH = 28.0F;
      String modeLabel = "Columns";
      this.modeBtnW = vg.textWidth(modeLabel, 11.5F) + 28.0F;
      this.modeBtnX = rightEdge - this.modeBtnW;
      this.modeBtnY = cy - this.modeBtnH * 0.5F;
      this.drawPillButton(vg, theme, this.modeBtnX, this.modeBtnY, this.modeBtnW, this.modeBtnH, modeLabel, false, this.hit(mx, my, this.modeBtnX, this.modeBtnY, this.modeBtnW, this.modeBtnH), true);

      this.favBtnH = 28.0F;
      String favLabel = "Favs";
      this.favBtnW = vg.textWidth(favLabel, 11.5F) + 28.0F;
      this.favBtnX = this.modeBtnX - 7.0F - this.favBtnW;
      this.favBtnY = cy - this.favBtnH * 0.5F;
      boolean favHov = this.hit(mx, my, this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH);
      vg.rect(this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH, 7.0F, Colors.withAlpha(this.favouritesOnly ? theme.accent() : 0xFFFFFFFF, this.favouritesOnly ? 0.24F : (favHov ? 0.09F : 0.04F)));
      vg.rectOutline(this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH, 7.0F, 1.0F, Colors.withAlpha(this.favouritesOnly ? theme.accentBright() : theme.borderSubtle(), this.favouritesOnly ? 0.68F : 0.9F));
      vg.star(this.favBtnX + 11.5F, cy, 4.5F, 2.0F, this.favouritesOnly ? 0xFFFBBF24 : theme.textMuted(), this.favouritesOnly);
      vg.text(favLabel, this.favBtnX + 20.5F, cy, 11.5F, this.favouritesOnly ? theme.textPrimary() : (favHov ? theme.textPrimary() : theme.textMuted()));

      this.searchBoxH = 28.0F;
      this.searchBoxW = Math.min(200.0F, Math.max(105.0F, this.favBtnX - (mainX + 145.0F)));
      this.searchBoxX = this.favBtnX - 7.0F - this.searchBoxW;
      this.searchBoxY = cy - this.searchBoxH * 0.5F;
      this.renderSearchPill(vg, theme, this.searchBoxX, this.searchBoxY, this.searchBoxW, this.searchBoxH);
   }

   private List<ModuleEntry> collectDashboardEntries() {
      this.dashVisibleScratch.clear();
      String raw = this.search.toString().toLowerCase(Locale.ROOT).trim();
      boolean allCategories = !raw.isEmpty() || this.favouritesOnly;
      if (allCategories) {
         for (Category cat : Category.values()) {
            CategoryPanel cp = this.categoryPanels.get(cat);
            for (ModuleEntry e : cp.allEntries()) {
               if ((!this.favouritesOnly || e.isFavourite()) && e.getModule().matchesFilter(raw)) {
                  this.dashVisibleScratch.add(e);
               }
            }
         }
      } else if (!STATE.isThemesTabSelected()) {
         CategoryPanel cp = this.categoryPanels.get(STATE.selectedCategory());
         cp.setFilter("");
         cp.setFavouritesOnly(false);
         this.dashVisibleScratch.addAll(cp.visibleEntries());
      }
      return this.dashVisibleScratch;
   }

   private void renderDashboardBody(NVGRenderer vg, Theme theme, float mx, float my, float bx, float by, float bw, float bh) {
      boolean showThemes = STATE.isThemesTabSelected() && this.search.isEmpty() && !this.favouritesOnly;
      vg.save();
      vg.scissor(bx, by, bw, bh);

      float pad = 14.0F;
      float contentTop = by + pad - this.dashScroll.value();
      float totalContentH;

      if (showThemes) {
         totalContentH = this.renderThemesStudio(vg, theme, mx, my, bx + pad, contentTop, bw - pad * 2.0F);
      } else {
         List<ModuleEntry> entries = this.collectDashboardEntries();
         if (entries.isEmpty()) {
            String emptyMsg = this.favouritesOnly ? "Star any module to pin it to Favourites." : "No modules match \"" + this.search + "\".";
            float ew = vg.textWidth(emptyMsg, 13.0F);
            vg.text(emptyMsg, bx + (bw - ew) * 0.5F, by + bh * 0.45F, 13.0F, theme.textDisabled());
            totalContentH = bh;
         } else {
            boolean showDesc = this.guiModule().descriptions.get();
            float gap = 10.0F;
            float colW = (bw - pad * 2.0F - gap) * 0.5F;
            float col0Y = contentTop;
            float col1Y = contentTop;

            for (ModuleEntry entry : entries) {
               entry.setShowSubtitle(showDesc);
               boolean placeLeft = col0Y <= col1Y;
               float ex = placeLeft ? (bx + pad) : (bx + pad + colW + gap);
               float ey = placeLeft ? col0Y : col1Y;
               entry.setBounds(ex, ey, colW);
               float eh = entry.height(vg);
               if (ey + eh >= by - 24.0F && ey <= by + bh + 24.0F) {
                  entry.render(vg, mx, my, 1.0F);
               }
               if (placeLeft) {
                  col0Y += eh + 8.0F;
               } else {
                  col1Y += eh + 8.0F;
               }
            }
            totalContentH = Math.max(col0Y, col1Y) - contentTop + pad * 2.0F;
         }
      }

      this.dashMaxScroll = Math.max(0.0F, totalContentH - bh);
      this.dashScroll.setTarget(Math.clamp(this.dashScroll.getTarget(), 0.0F, this.dashMaxScroll));

      if (this.dashMaxScroll > 1.0F) {
         float trackH = bh - 16.0F;
         float thumbH = Math.max(28.0F, trackH * (bh / totalContentH));
         float thumbY = by + 8.0F + (trackH - thumbH) * (this.dashScroll.value() / this.dashMaxScroll);
         vg.rect(bx + bw - 6.0F, by + 8.0F, 3.0F, trackH, 1.5F, Colors.withAlpha(theme.borderSubtle(), 0.5F));
         vg.rect(bx + bw - 6.0F, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(theme.accentBright(), 0.75F));
      }

      vg.restore();
   }

   private float renderThemesStudio(NVGRenderer vg, Theme active, float mx, float my, float x, float startY, float w) {
      ThemeManager tm = AetherClient.themes();
      float y = startY;
      vg.text("ACCENT & BLOOM PALETTES", x, y + 6.0F, 10.5F, active.textDisabled());
      y += 20.0F;

      List<Theme> all = tm.getThemes();
      int cols = 3;
      float gap = 10.0F;
      float cardW = (w - gap * (cols - 1)) / (float)cols;
      float cardH = 44.0F;
      int totalItems = all.size() + 1;

      for (int i = 0; i < totalItems; i++) {
         int col = i % cols;
         int row = i / cols;
         float cx = x + col * (cardW + gap);
         float cy = y + row * (cardH + gap);
         boolean hov = this.hit(mx, my, cx, cy, cardW, cardH);

         if (i < all.size()) {
            Theme t = all.get(i);
            boolean sel = t == active;
            if (sel && t.isBloom()) {
               vg.glow(cx, cy, cardW, cardH, 8.0F, 8.0F, Colors.withAlpha(t.accent(), 0.25F));
            }
            vg.rect(cx, cy, cardW, cardH, 8.0F, sel ? active.moduleActiveFill() : (hov ? active.surfaceElevated() : active.surface()));
            vg.rectOutline(cx, cy, cardW, cardH, 8.0F, 1.0F, Colors.withAlpha(sel ? t.accentBright() : active.borderSubtle(), sel ? 0.78F : 0.85F));
            vg.circle(cx + 18.0F, cy + cardH * 0.5F, 7.5F, t.accent());
            if (sel) {
               vg.circleOutline(cx + 18.0F, cy + cardH * 0.5F, 10.5F, 1.3F, t.accentBright());
            }
            vg.textTruncated(t.getName(), cx + 34.0F, cy + cardH * 0.5F, 12.2F, sel ? active.textPrimary() : active.textMuted(), cardW - 56.0F);
            if (t.isCustom()) {
               vg.cross(cx + cardW - 20.0F, cy + cardH * 0.5F - 5.0F, 10.0F, 1.5F, active.textDisabled());
            }
         } else {
            vg.rect(cx, cy, cardW, cardH, 8.0F, Colors.withAlpha(active.accent(), hov ? 0.22F : 0.12F));
            vg.rectOutline(cx, cy, cardW, cardH, 8.0F, 1.0F, Colors.withAlpha(active.accentBright(), hov ? 0.65F : 0.32F));
            String addLbl = "+ Custom Theme";
            float aw = vg.textWidth(addLbl, 12.0F);
            vg.text(addLbl, cx + (cardW - aw) * 0.5F, cy + cardH * 0.5F, 12.0F, hov ? active.accentBright() : active.textPrimary());
         }
      }

      int rows = (totalItems + cols - 1) / cols;
      y += rows * (cardH + gap) + 8.0F;

      if (active.isCustom()) {
         float pickH = this.dashAccentWidget.height(vg) + 16.0F;
         vg.rect(x, y, w, pickH, 8.0F, active.surface());
         vg.rectOutline(x, y, w, pickH, 8.0F, 1.0F, Colors.withAlpha(active.borderSubtle(), 0.85F));
         this.dashAccentWidget.setBounds(x + 14.0F, y + 8.0F, w - 28.0F);
         this.dashAccentWidget.render(vg, mx, my);
         y += pickH + 14.0F;
      }

      vg.text("INTERFACE AUDIO", x, y + 6.0F, 10.5F, active.textDisabled());
      y += 20.0F;
      float audioBoxY = y;
      float innerY = y + 10.0F;
      for (SettingWidget sw : this.dashSoundWidgets) {
         sw.setBounds(x + 14.0F, innerY, w - 28.0F);
         innerY += sw.height(vg) + 4.0F;
      }
      float audioBoxH = innerY - audioBoxY + 6.0F;
      vg.rect(x, audioBoxY, w, audioBoxH, 8.0F, active.surface());
      vg.rectOutline(x, audioBoxY, w, audioBoxH, 8.0F, 1.0F, Colors.withAlpha(active.borderSubtle(), 0.85F));
      for (SettingWidget sw : this.dashSoundWidgets) {
         sw.render(vg, mx, my);
      }
      y += audioBoxH + 20.0F;
      return y - startY + 24.0F;
   }

   // =========================================================================
   // MODE 2: REDESIGNED DRAGGABLE CATEGORY COLUMNS (+ SYNCED CORZ PORT)
   // =========================================================================

   private void renderColumnsMode(NVGRenderer vg, float mx, float my, float uiW, float uiH, float anim) {
      Theme theme = AetherClient.themes().current();
      String query = this.search.toString();
      for (Panel p : this.panels) {
         if (p instanceof CategoryPanel cp) {
            cp.setFilter(query);
            cp.setFavouritesOnly(this.favouritesOnly);
         }
      }
      for (int i = this.panels.size() - 1; i >= 0; i--) {
         this.panels.get(i).render(vg, mx, my, uiW, uiH);
      }

      // Floating Top Glass Dock
      float dockW = Math.min(uiW - 32.0F, 790.0F);
      float dockH = 44.0F;
      float dockX = (uiW - dockW) * 0.5F;
      float dockY = 12.0F;

      vg.glow(dockX, dockY + 2.0F, dockW, dockH, 11.0F, 14.0F, Colors.withAlpha(0xFF000000, 0.45F));
      vg.glow(dockX, dockY, dockW, dockH, 11.0F, 12.0F * theme.bloomStrength() + 4.0F, Colors.withAlpha(theme.accent(), 0.22F * theme.bloomStrength() + 0.06F));
      vg.rectGradient(dockX, dockY, dockW, dockH, 11.0F, theme.headerTop(), theme.headerBottom(), true);
      vg.rectOutline(dockX, dockY, dockW, dockH, 11.0F, 1.05F, Colors.withAlpha(theme.accentBright(), 0.44F));

      float cy = dockY + dockH * 0.5F;
      vg.aetherMark(dockX + 20.0F, cy, 8.5F, theme.accent(), theme.accentBright());
      vg.textGradient("AETHER", dockX + 36.0F, cy, 14.0F, theme.accentBright(), theme.accent());

      float rightEdge = dockX + dockW - 10.0F;

      this.modeBtnH = 28.0F;
      String modeLbl = "Dashboard";
      this.modeBtnW = vg.textWidth(modeLbl, 11.5F) + 24.0F;
      this.modeBtnX = rightEdge - this.modeBtnW;
      this.modeBtnY = cy - this.modeBtnH * 0.5F;
      this.drawPillButton(vg, theme, this.modeBtnX, this.modeBtnY, this.modeBtnW, this.modeBtnH, modeLbl, true, this.hit(mx, my, this.modeBtnX, this.modeBtnY, this.modeBtnW, this.modeBtnH), false);

      this.corzTabH = 28.0F;
      String corzLbl = "Corz Port";
      this.corzTabW = vg.textWidth(corzLbl, 11.5F) + 24.0F;
      this.corzTabX = this.modeBtnX - 6.0F - this.corzTabW;
      this.corzTabY = cy - this.corzTabH * 0.5F;
      this.drawPillButton(vg, theme, this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH, corzLbl, STATE.isCorzPortOpen(), this.hit(mx, my, this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH), false);

      this.cfgBtnH = 28.0F;
      String cfgLbl = "Configs";
      this.cfgBtnW = vg.textWidth(cfgLbl, 11.5F) + 20.0F;
      this.cfgBtnX = this.corzTabX - 6.0F - this.cfgBtnW;
      this.cfgBtnY = cy - this.cfgBtnH * 0.5F;
      this.drawPillButton(vg, theme, this.cfgBtnX, this.cfgBtnY, this.cfgBtnW, this.cfgBtnH, cfgLbl, false, this.hit(mx, my, this.cfgBtnX, this.cfgBtnY, this.cfgBtnW, this.cfgBtnH), false);

      this.favBtnH = 28.0F;
      String favLbl = "Favs";
      this.favBtnW = vg.textWidth(favLbl, 11.5F) + 26.0F;
      this.favBtnX = this.cfgBtnX - 6.0F - this.favBtnW;
      this.favBtnY = cy - this.favBtnH * 0.5F;
      boolean favHov = this.hit(mx, my, this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH);
      vg.rect(this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH, 7.0F, Colors.withAlpha(this.favouritesOnly ? theme.accent() : 0xFFFFFFFF, this.favouritesOnly ? 0.24F : (favHov ? 0.09F : 0.04F)));
      vg.rectOutline(this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH, 7.0F, 1.0F, Colors.withAlpha(this.favouritesOnly ? theme.accentBright() : theme.borderSubtle(), this.favouritesOnly ? 0.65F : 0.9F));
      vg.star(this.favBtnX + 11.0F, cy, 4.5F, 2.0F, this.favouritesOnly ? 0xFFFBBF24 : theme.textMuted(), this.favouritesOnly);
      vg.text(favLbl, this.favBtnX + 20.0F, cy, 11.5F, this.favouritesOnly ? theme.textPrimary() : theme.textMuted());

      this.searchBoxH = 28.0F;
      this.searchBoxX = dockX + 106.0F;
      this.searchBoxW = Math.max(110.0F, this.favBtnX - 8.0F - this.searchBoxX);
      this.searchBoxY = cy - this.searchBoxH * 0.5F;
      this.renderSearchPill(vg, theme, this.searchBoxX, this.searchBoxY, this.searchBoxW, this.searchBoxH);

      if (this.corzPortWindow.isVisible()) {
         float cw = this.corzPortWindow.preferredWidth(uiW);
         float ch = Math.clamp(uiH - 96.0F, 380.0F, 540.0F);
         this.corzPortWindow.render(vg, mx, my, uiW, uiH, uiW - cw - 18.0F, 68.0F, ch);
      }
   }

   private void renderSearchPill(NVGRenderer vg, Theme theme, float x, float y, float w, float h) {
      vg.rect(x, y, w, h, 7.0F, Colors.withAlpha(0xFF07060D, 0.72F));
      vg.rectOutline(x, y, w, h, 7.0F, 1.0F, Colors.withAlpha(this.searchFocused ? theme.accentBright() : theme.borderSubtle(), this.searchFocused ? 0.85F : 0.9F));
      float cy = y + h * 0.5F;
      float ix = x + 13.0F;
      vg.circleOutline(ix, cy - 1.0F, 4.0F, 1.4F, this.searchFocused ? theme.accentBright() : theme.textMuted());
      vg.line(ix + 2.9F, cy + 1.9F, ix + 5.8F, cy + 4.8F, 1.4F, this.searchFocused ? theme.accentBright() : theme.textMuted());

      float tx = x + 26.0F;
      if (this.search.isEmpty() && !this.searchFocused) {
         vg.text("Search modules...", tx, cy, 11.5F, theme.textDisabled());
      } else {
         float tw = vg.textTruncated(this.search.toString(), tx, cy, 12.0F, theme.textPrimary(), w - 46.0F);
         if (this.searchFocused && System.nanoTime() / 400000000L % 2L == 0L) {
            vg.rect(tx + tw + 1.5F, cy - 6.0F, 1.4F, 12.0F, 0.7F, theme.accentBright());
         }
      }
      if (!this.search.isEmpty()) {
         vg.cross(x + w - 18.0F, cy - 5.0F, 10.0F, 1.5F, theme.textMuted());
      }
   }

   private void drawPillButton(NVGRenderer vg, Theme theme, float x, float y, float w, float h, String label, boolean accentTint, boolean hovered, boolean gridIcon) {
      int fill = accentTint
         ? Colors.withAlpha(theme.accent(), hovered ? 0.30F : 0.20F)
         : Colors.withAlpha(0xFFFFFFFF, hovered ? 0.10F : 0.05F);
      vg.rect(x, y, w, h, 7.0F, fill);
      vg.rectOutline(x, y, w, h, 7.0F, 1.0F, Colors.withAlpha(accentTint || hovered ? theme.accentBright() : theme.borderSubtle(), accentTint ? 0.65F : (hovered ? 0.5F : 0.9F)));
      float cy = y + h * 0.5F;
      if (gridIcon) {
         float gx = x + 9.0F;
         float gy = cy - 4.5F;
         int ic = hovered ? theme.accentBright() : theme.textMuted();
         vg.rect(gx, gy, 3.6F, 3.6F, 0.8F, ic);
         vg.rect(gx + 5.2F, gy, 3.6F, 3.6F, 0.8F, ic);
         vg.rect(gx, gy + 5.2F, 3.6F, 3.6F, 0.8F, ic);
         vg.rect(gx + 5.2F, gy + 5.2F, 3.6F, 3.6F, 0.8F, ic);
         vg.text(label, x + 21.0F, cy, 11.5F, hovered ? theme.textPrimary() : theme.textMuted());
      } else {
         float tw = vg.textWidth(label, 11.5F);
         vg.text(label, x + (w - tw) * 0.5F, cy, 11.5F, accentTint ? theme.accentBright() : (hovered ? theme.textPrimary() : theme.textMuted()));
      }
   }

   // =========================================================================
   // INPUT HANDLING
   // =========================================================================

   public void openSearch(String query) {
      this.search.setLength(0);
      this.search.append(query);
      this.searchFocused = true;
   }

   public ConfigPanel configPanel() {
      return this.configPanel;
   }

   private float uiX(double x) {
      return OverlayRenderer.guiToUi(x);
   }

   private float uiY(double y) {
      return OverlayRenderer.guiToUi(y);
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      float mx = this.uiX(event.x());
      float my = this.uiY(event.y());
      int btn = event.button();
      NVGRenderer vg = NVGRenderer.get();
      float uiH = OverlayRenderer.uiHeight();

      if (this.configPanel.isOpen()) {
         this.configPanel.mouseClicked(mx, my, btn);
         return true;
      }

      // Route clicks inside synced Corz Port secondary UI first when open
      if (this.corzPortWindow.mouseClicked(vg, mx, my, btn)) {
         this.pressedInCorzPort = true;
         this.searchFocused = false;
         return true;
      }

      // Corz Port subtab toggle button (works in both Dashboard sidebar and Columns top dock)
      if (btn == 0 && this.hit(mx, my, this.corzTabX, this.corzTabY, this.corzTabW, this.corzTabH)) {
         this.toggleCorzPort();
         return true;
      }

      if (btn == 0 && this.hit(mx, my, this.modeBtnX, this.modeBtnY, this.modeBtnW, this.modeBtnH)) {
         this.toggleLayoutMode();
         return true;
      }
      if (btn == 0 && this.hit(mx, my, this.favBtnX, this.favBtnY, this.favBtnW, this.favBtnH)) {
         this.favouritesOnly = !this.favouritesOnly;
         this.dashScroll.snapTo(0.0F);
         UiSounds.select();
         return true;
      }
      if (btn == 0 && this.hit(mx, my, this.cfgBtnX, this.cfgBtnY, this.cfgBtnW, this.cfgBtnH)) {
         this.configPanel.open();
         UiSounds.guiOpen();
         return true;
      }
      if (this.hit(mx, my, this.searchBoxX, this.searchBoxY, this.searchBoxW, this.searchBoxH)) {
         if (!this.search.isEmpty() && mx >= this.searchBoxX + this.searchBoxW - 22.0F) {
            this.search.setLength(0);
         } else {
            this.searchFocused = true;
         }
         UiSounds.select();
         return true;
      }
      this.searchFocused = false;

      if (this.isDashboardMode()) {
         return this.mouseClickedDashboard(mx, my, btn);
      }

      for (Panel p : this.panels) {
         if (p.headerHit(mx, my)) {
            this.bringToFront(p);
            if (btn == 0) {
               this.dragging = p;
               this.dragOffsetX = mx - p.x();
               this.dragOffsetY = my - p.y();
               this.pressX = mx;
               this.pressY = my;
               this.dragMoved = false;
            } else {
               p.toggleCollapsed();
               UiSounds.panelCollapse();
            }
            return true;
         }
         if (p.bodyHit(vg, mx, my, uiH)) {
            this.bringToFront(p);
            this.pressedContentPanel = p;
            p.mouseClicked(mx, my, btn);
            return true;
         }
      }
      return true;
   }

   private boolean mouseClickedDashboard(float mx, float my, int btn) {
      if (btn == 0 && mx >= this.winX && mx <= this.winX + SIDEBAR_W && my >= this.winY + TOPBAR_H && my < this.cfgBtnY) {
         float navY = this.winY + TOPBAR_H + 25.0F;
         for (Category cat : Category.values()) {
            if (my >= navY && my <= navY + 30.0F) {
               STATE.selectCategory(cat);
               this.favouritesOnly = false;
               this.search.setLength(0);
               this.dashScroll.snapTo(0.0F);
               UiSounds.select();
               return true;
            }
            navY += 33.0F;
         }
         // Advance past SUBTABS header (21) + Corz Port row (37) + CUSTOMIZE header (15)
         navY += 21.0F + 37.0F + 15.0F;
         if (my >= navY && my <= navY + 30.0F) {
            STATE.selectThemesTab();
            this.favouritesOnly = false;
            this.search.setLength(0);
            this.dashScroll.snapTo(0.0F);
            UiSounds.select();
            return true;
         }
         return true;
      }

      if (btn == 0 && this.hit(mx, my, this.winX, this.winY, this.winW, TOPBAR_H)) {
         this.draggingWindow = true;
         this.winOffsetX = mx - this.winX;
         this.winOffsetY = my - this.winY;
         return true;
      }

      float bodyX = this.winX + SIDEBAR_W;
      float bodyY = this.winY + TOPBAR_H;
      float bodyW = this.winW - SIDEBAR_W;
      float bodyH = this.winH - TOPBAR_H;
      if (this.hit(mx, my, bodyX, bodyY, bodyW, bodyH)) {
         boolean showThemes = STATE.isThemesTabSelected() && this.search.isEmpty() && !this.favouritesOnly;
         if (showThemes) {
            this.pressedInThemesStudio = true;
            this.clickThemesStudio(mx, my, btn, bodyX + 14.0F, bodyY + 14.0F - this.dashScroll.value(), bodyW - 28.0F);
            return true;
         }
         for (ModuleEntry entry : this.collectDashboardEntries()) {
            if (entry.mouseClicked(mx, my, btn)) {
               this.pressedDashEntry = entry;
               return true;
            }
         }
      }
      return true;
   }

   private void clickThemesStudio(float mx, float my, int btn, float x, float startY, float w) {
      ThemeManager tm = AetherClient.themes();
      if (tm.current().isCustom() && this.dashAccentWidget.mouseClicked(mx, my, btn)) {
         return;
      }
      for (SettingWidget sw : this.dashSoundWidgets) {
         if (sw.mouseClicked(mx, my, btn)) {
            return;
         }
      }
      if (btn != 0) {
         return;
      }
      float y = startY + 20.0F;
      List<Theme> all = tm.getThemes();
      int cols = 3;
      float gap = 10.0F;
      float cardW = (w - gap * (cols - 1)) / (float)cols;
      float cardH = 44.0F;
      int totalItems = all.size() + 1;
      for (int i = 0; i < totalItems; i++) {
         int col = i % cols;
         int row = i / cols;
         float cx = x + col * (cardW + gap);
         float cy = y + row * (cardH + gap);
         if (this.hit(mx, my, cx, cy, cardW, cardH)) {
            if (i < all.size()) {
               Theme t = all.get(i);
               if (t.isCustom() && mx >= cx + cardW - 26.0F) {
                  tm.removeCustom(t);
               } else {
                  tm.select(t);
                  if (t.isCustom()) {
                     this.dashAccentProxy.set(Integer.valueOf(t.accent()));
                  }
               }
            } else {
               Theme custom = tm.addCustom(tm.current().accent());
               tm.select(custom);
               this.dashAccentProxy.set(Integer.valueOf(custom.accent()));
            }
            UiSounds.select();
            return;
         }
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.configPanel.isOpen()) {
         return true;
      }
      float mx = this.uiX(event.x());
      float my = this.uiY(event.y());
      if (this.pressedInCorzPort) {
         this.corzPortWindow.mouseDragged(mx, my);
         return true;
      }
      if (this.draggingWindow) {
         this.windowMoved = true;
         this.customWinX = mx - this.winOffsetX;
         this.customWinY = my - this.winOffsetY;
         return true;
      }
      if (this.pressedInThemesStudio) {
         this.dashAccentWidget.mouseDragged(mx, my);
         for (SettingWidget sw : this.dashSoundWidgets) {
            sw.mouseDragged(mx, my);
         }
         return true;
      }
      if (this.pressedDashEntry != null) {
         this.pressedDashEntry.mouseDragged(mx, my);
         return true;
      }
      if (this.dragging != null) {
         if (Math.abs(mx - this.pressX) + Math.abs(my - this.pressY) > 3.0F) {
            this.dragMoved = true;
         }
         if (this.dragMoved) {
            this.dragging.moveTo(mx - this.dragOffsetX, my - this.dragOffsetY);
         }
         return true;
      }
      if (this.pressedContentPanel != null) {
         this.pressedContentPanel.mouseDragged(mx, my);
      }
      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.configPanel.isOpen()) {
         return true;
      }
      if (this.pressedInCorzPort) {
         this.pressedInCorzPort = false;
         this.corzPortWindow.mouseReleased();
      }
      this.draggingWindow = false;
      if (this.pressedInThemesStudio) {
         this.pressedInThemesStudio = false;
         this.dashAccentWidget.mouseReleased();
         for (SettingWidget sw : this.dashSoundWidgets) {
            sw.mouseReleased();
         }
      }
      if (this.pressedDashEntry != null) {
         this.pressedDashEntry.mouseReleased();
         this.pressedDashEntry = null;
      }
      if (this.dragging != null) {
         if (!this.dragMoved && event.button() == 0) {
            this.dragging.toggleCollapsed();
            UiSounds.panelCollapse();
         } else if (this.dragMoved) {
            STATE.markCustomized();
         }
         this.dragging = null;
         return true;
      }
      if (this.pressedContentPanel != null) {
         this.pressedContentPanel.mouseReleased();
         this.pressedContentPanel = null;
      }
      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.configPanel.isOpen()) {
         return true;
      }
      float mx = this.uiX(mouseX);
      float my = this.uiY(mouseY);
      if (this.corzPortWindow.mouseScrolled(mx, my, scrollY)) {
         return true;
      }
      if (this.isDashboardMode()) {
         if (this.dashMaxScroll > 0.0F && this.hit(mx, my, this.winX + SIDEBAR_W, this.winY + TOPBAR_H, this.winW - SIDEBAR_W, this.winH - TOPBAR_H)) {
            this.dashScroll.setTarget(Math.clamp(this.dashScroll.getTarget() - (float)scrollY * 44.0F, 0.0F, this.dashMaxScroll));
         }
         return true;
      }
      NVGRenderer vg = NVGRenderer.get();
      float uiH = OverlayRenderer.uiHeight();
      for (Panel p : this.panels) {
         if (p.bodyHit(vg, mx, my, uiH) || p.headerHit(mx, my)) {
            p.onScroll(scrollY);
            return true;
         }
      }
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.configPanel.isOpen()) {
         this.configPanel.keyPressed(event.key());
         return true;
      }
      if (this.corzPortWindow.isListening() && this.corzPortWindow.keyPressed(event.key())) {
         return true;
      }
      if (this.dashAccentWidget.isListening()) {
         this.dashAccentWidget.keyPressed(event.key());
         return true;
      }
      for (Panel p : this.panels) {
         if (p.isListening()) {
            p.keyPressed(event.key());
            return true;
         }
      }
      if (this.searchFocused) {
         switch (event.key()) {
            case 256 -> {
               this.search.setLength(0);
               this.searchFocused = false;
            }
            case 257, 335 -> this.searchFocused = false;
            case 259 -> {
               if (!this.search.isEmpty()) {
                  this.search.deleteCharAt(this.search.length() - 1);
               }
            }
         }
         return true;
      }
      if (event.isEscape() || this.guiModule().getKeybind().matches(event.key())) {
         this.onClose();
         return true;
      }
      return super.keyPressed(event);
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.configPanel.isOpen()) {
         this.configPanel.charTyped(event.codepoint());
         return true;
      }
      if (this.corzPortWindow.isListening() && this.corzPortWindow.charTyped(event.codepoint())) {
         return true;
      }
      if (this.dashAccentWidget.isListening()) {
         this.dashAccentWidget.charTyped(event.codepoint());
         return true;
      }
      for (Panel p : this.panels) {
         if (p.isListening()) {
            p.charTyped(event.codepoint());
            return true;
         }
      }
      if (this.searchFocused && event.isAllowedChatCharacter()) {
         if (this.search.length() < 32) {
            this.search.append(event.codepointAsString());
            this.dashScroll.snapTo(0.0F);
         }
         return true;
      }
      return super.charTyped(event);
   }

   private void bringToFront(Panel panel) {
      if (this.panels.remove(panel)) {
         this.panels.addFirst(panel);
      }
   }

   public void openBlockPicker(BlockListSetting setting) {
      BlockGridModel model = new BlockGridModel(setting, () -> {
         BlockEspModule mod = AetherClient.modules().blockEsp;
         return mod != null ? mod.lineColor.get() : 0xFFFF66C4;
      }, "Aether Client");
      this.minecraft.setScreen(new IconPickerScreen(this, model, AetherClient.themes()));
   }

   public void openIconPicker(IconListSetting setting) {
      IconListGridModel model = new IconListGridModel(setting);
      this.minecraft.setScreen(new IconPickerScreen(this, model, AetherClient.themes()));
   }
}
