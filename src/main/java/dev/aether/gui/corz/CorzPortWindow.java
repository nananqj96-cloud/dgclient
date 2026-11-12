package dev.aether.gui.corz;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aether.corz.CorzModuleHandle;
import dev.aether.corz.CorzPortBus;
import dev.aether.corz.CorzPortProvider;
import dev.aether.corz.CorzSettingHandle;
import dev.aether.gui.ClickGuiState;
import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CorzPortWindow {
   private static final float RADIUS = 13.0F;
   private static final float HEADER_H = 50.0F;
   private static final float TABS_H = 34.0F;

   private final ThemeManager themes;
   private final ClickGuiState state;
   private final Animation subOpenAnim = new Animation(165.0F, 0.0F);
   private final Animation scrollAnim = new Animation(145.0F, 0.0F);
   private float maxScroll;

   private float x;
   private float y;
   private float w = 390.0F;
   private float h = 520.0F;
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

   private final Set<String> expandedIds = new HashSet<>();
   private final List<CorzModuleHandle> visibleScratch = new ArrayList<>(160);

   // Active drag/input state for settings
   private CorzSettingHandle activeSlider;
   private float sliderTrackX;
   private float sliderTrackW;
   private boolean draggingRangeMax;
   private CorzSettingHandle activeHueColor;
   private float hueTrackX;
   private float hueTrackW;
   private CorzSettingHandle listeningSettingKey;
   private CorzModuleHandle listeningModuleKey;
   private CorzSettingHandle focusedStringSetting;

   public CorzPortWindow(ThemeManager themes, ClickGuiState state) {
      this.themes = themes;
      this.state = state;
      this.subOpenAnim.snapTo(state.isCorzPortOpen() ? 1.0F : 0.0F);
   }

   public boolean isVisible() {
      this.subOpenAnim.setTarget(this.state.isCorzPortOpen() ? 1.0F : 0.0F);
      return this.subOpenAnim.value() > 0.005F || this.state.isCorzPortOpen();
   }

   public float openProgress() {
      this.subOpenAnim.setTarget(this.state.isCorzPortOpen() ? 1.0F : 0.0F);
      return this.subOpenAnim.value();
   }

   public float preferredWidth(float uiW) {
      return Math.clamp(uiW * 0.34F, 340.0F, 430.0F);
   }

   public void expandModule(String moduleId) {
      if (moduleId != null && !moduleId.isEmpty()) {
         this.expandedIds.add(moduleId);
      }
   }

   public boolean isListening() {
      return this.listeningSettingKey != null || this.listeningModuleKey != null || this.focusedStringSetting != null || this.searchFocused;
   }

   public void syncThemeToCorz() {
      CorzPortProvider provider = CorzPortBus.provider();
      if (provider != null) {
         Theme t = this.themes.current();
         provider.syncTheme(t.accent(), t.accentBright(), t.background());
      }
   }

   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH, float defaultX, float defaultY, float targetH) {
      this.syncThemeToCorz();
      float sub = this.openProgress();
      if (sub <= 0.004F) {
         return;
      }

      Theme theme = this.themes.current();
      this.w = this.preferredWidth(uiW);
      this.h = targetH;
      if (this.customMoved) {
         this.x = Math.clamp(this.customX, 10.0F, Math.max(10.0F, uiW - this.w - 10.0F));
         this.y = Math.clamp(this.customY, 10.0F, Math.max(10.0F, uiH - this.h - 10.0F));
      } else {
         this.x = Math.clamp(defaultX, 10.0F, Math.max(10.0F, uiW - this.w - 10.0F));
         this.y = Math.clamp(defaultY, 10.0F, Math.max(10.0F, uiH - this.h - 10.0F));
      }

      vg.save();
      vg.alpha(sub);
      float scale = 0.95F + 0.05F * sub;
      vg.translate(this.x + this.w * 0.5F, this.y + this.h * 0.5F);
      vg.scale(scale);
      vg.translate(-(this.x + this.w * 0.5F), -(this.y + this.h * 0.5F));

      // PinkBloom optical glow layers + dark velvet glass shell
      float bloom = theme.bloomStrength();
      vg.glow(this.x, this.y + 4.0F, this.w, this.h, RADIUS, 20.0F, Colors.withAlpha(0xFF000000, 0.52F));
      vg.glow(this.x - 1.0F, this.y - 1.0F, this.w + 2.0F, this.h + 2.0F, RADIUS, 18.0F * bloom + 6.0F, Colors.withAlpha(theme.accent(), 0.24F * bloom + 0.08F));
      vg.rectGradient(this.x, this.y, this.w, this.h, RADIUS, theme.background(), theme.backgroundTo(), true);

      // Header band
      vg.rectVaryingGradient(this.x, this.y, this.w, HEADER_H, RADIUS, RADIUS, 0.0F, 0.0F, theme.headerTop(), theme.headerBottom());
      vg.line(this.x + 1.0F, this.y + HEADER_H, this.x + this.w - 1.0F, this.y + HEADER_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.9F));
      vg.rectOutline(this.x, this.y, this.w, this.h, RADIUS, 1.15F, Colors.withAlpha(theme.accentBright(), 0.44F));
      vg.rect(this.x + 18.0F, this.y, 62.0F, 2.2F, 1.1F, theme.accentBright());

      this.renderHeader(vg, theme, mx, my);
      this.renderCategoryPills(vg, theme, mx, my);
      this.renderModulesBody(vg, theme, mx, my);

      vg.restore();
   }

   private void renderHeader(NVGRenderer vg, Theme theme, float mx, float my) {
      float cy = this.y + HEADER_H * 0.5F;
      vg.aetherMark(this.x + 20.0F, cy, 8.2F, theme.accent(), theme.accentBright());
      float tw = vg.textGradient("CORZ PORT", this.x + 34.0F, cy, 13.5F, theme.accentBright(), theme.accent());

      CorzPortProvider provider = CorzPortBus.provider();
      int totalCount = provider != null ? provider.allModules().size() : 0;
      String badge = Integer.toString(totalCount);
      float bw = vg.textWidth(badge, 9.5F) + 10.0F;
      vg.rect(this.x + 40.0F + tw, cy - 7.5F, bw, 15.0F, 7.5F, Colors.withAlpha(theme.accent(), 0.20F));
      vg.text(badge, this.x + 45.0F + tw, cy, 9.5F, theme.accentBright());

      // Close button
      this.closeX = this.x + this.w - 30.0F;
      this.closeY = cy - this.closeSize * 0.5F;
      boolean cHov = this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize);
      vg.rect(this.closeX, this.closeY, this.closeSize, this.closeSize, 6.0F, Colors.withAlpha(0xFFFFFFFF, cHov ? 0.12F : 0.04F));
      vg.cross(this.closeX + 6.0F, this.closeY + 6.0F, 10.0F, 1.5F, cHov ? theme.accentBright() : theme.textMuted());

      // Compact search input
      float leftLimit = this.x + 48.0F + tw + bw;
      this.searchH = 26.0F;
      this.searchW = Math.max(105.0F, (this.closeX - 8.0F) - leftLimit);
      this.searchX = this.closeX - 8.0F - this.searchW;
      this.searchY = cy - this.searchH * 0.5F;

      vg.rect(this.searchX, this.searchY, this.searchW, this.searchH, 7.0F, Colors.withAlpha(0xFF07060D, 0.72F));
      vg.rectOutline(this.searchX, this.searchY, this.searchW, this.searchH, 7.0F, 1.0F, Colors.withAlpha(this.searchFocused ? theme.accentBright() : theme.borderSubtle(), 0.88F));
      float sx = this.searchX + 10.0F;
      if (this.search.isEmpty() && !this.searchFocused) {
         vg.text("Search Corz...", sx, cy, 11.0F, theme.textDisabled());
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

   private List<String> tabNames() {
      CorzPortProvider p = CorzPortBus.provider();
      if (p == null) {
         return List.of("All");
      }
      List<String> list = new ArrayList<>(p.categories().size() + 1);
      list.add("All");
      list.addAll(p.categories());
      return list;
   }

   private void renderCategoryPills(NVGRenderer vg, Theme theme, float mx, float my) {
      float stripY = this.y + HEADER_H;
      vg.rect(this.x + 1.0F, stripY, this.w - 2.0F, TABS_H, 0.0F, Colors.withAlpha(0xFF090610, 0.38F));
      vg.line(this.x + 1.0F, stripY + TABS_H, this.x + this.w - 1.0F, stripY + TABS_H, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.75F));

      vg.save();
      vg.scissor(this.x + 6.0F, stripY, this.w - 12.0F, TABS_H);
      float px = this.x + 10.0F;
      float py = stripY + 6.0F;
      float ph = 22.0F;
      String activeCat = this.state.corzSelectedCategory();
      for (String tab : this.tabNames()) {
         float pw = vg.textWidth(tab, 10.5F) + 14.0F;
         boolean sel = tab.equalsIgnoreCase(activeCat) && this.search.isEmpty();
         boolean hov = this.hit(mx, my, px, py, pw, ph);
         if (sel) {
            vg.glow(px, py, pw, ph, 6.0F, 6.0F, Colors.withAlpha(theme.accent(), 0.25F));
            vg.rect(px, py, pw, ph, 6.0F, Colors.withAlpha(theme.accent(), 0.24F));
            vg.rectOutline(px, py, pw, ph, 6.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.70F));
         } else if (hov) {
            vg.rect(px, py, pw, ph, 6.0F, Colors.withAlpha(0xFFFFFFFF, 0.07F));
         }
         vg.text(tab, px + 7.0F, py + ph * 0.5F, 10.5F, sel ? theme.textPrimary() : (hov ? theme.textPrimary() : theme.textMuted()));
         px += pw + 4.0F;
      }
      vg.restore();
   }

   private List<CorzModuleHandle> collectVisible() {
      this.visibleScratch.clear();
      CorzPortProvider p = CorzPortBus.provider();
      if (p == null) {
         return this.visibleScratch;
      }
      String q = this.search.toString().toLowerCase(Locale.ROOT).trim();
      String cat = this.state.corzSelectedCategory();
      List<CorzModuleHandle> source = (!q.isEmpty() || "All".equalsIgnoreCase(cat))
         ? p.allModules()
         : p.modulesInCategory(cat);
      for (CorzModuleHandle m : source) {
         if (q.isEmpty()
            || m.displayName().toLowerCase(Locale.ROOT).contains(q)
            || m.id().toLowerCase(Locale.ROOT).contains(q)
            || m.category().toLowerCase(Locale.ROOT).contains(q)) {
            this.visibleScratch.add(m);
         }
      }
      return this.visibleScratch;
   }

   private void renderModulesBody(NVGRenderer vg, Theme theme, float mx, float my) {
      float bodyX = this.x;
      float bodyY = this.y + HEADER_H + TABS_H;
      float bodyW = this.w;
      float bodyH = this.h - HEADER_H - TABS_H;

      vg.save();
      vg.scissor(bodyX + 1.0F, bodyY, bodyW - 2.0F, bodyH - 1.0F);

      CorzPortProvider provider = CorzPortBus.provider();
      if (provider == null) {
         String msg = "Corz Port engine initializing...";
         vg.text(msg, bodyX + (bodyW - vg.textWidth(msg, 12.5F)) * 0.5F, bodyY + bodyH * 0.45F, 12.5F, theme.textMuted());
         vg.restore();
         return;
      }

      List<CorzModuleHandle> mods = this.collectVisible();
      boolean showCatTag = "All".equalsIgnoreCase(this.state.corzSelectedCategory()) || !this.search.isEmpty();
      float pad = 10.0F;
      float cardW = bodyW - pad * 2.0F - 4.0F;
      float curY = bodyY + pad - this.scrollAnim.value();

      if (mods.isEmpty()) {
         String empty = "No Corz modules match \"" + this.search + "\".";
         vg.text(empty, bodyX + (bodyW - vg.textWidth(empty, 12.0F)) * 0.5F, bodyY + bodyH * 0.45F, 12.0F, theme.textDisabled());
      }

      for (CorzModuleHandle mod : mods) {
         boolean expanded = this.expandedIds.contains(mod.id());
         float cardH = this.measureModuleCard(mod, expanded);
         if (curY + cardH >= bodyY - 20.0F && curY <= bodyY + bodyH + 20.0F) {
            this.renderModuleCard(vg, theme, mod, expanded, showCatTag, bodyX + pad, curY, cardW, cardH, mx, my);
         }
         curY += cardH + 6.0F;
      }

      float contentH = (curY - (bodyY + pad - this.scrollAnim.value())) + pad;
      this.maxScroll = Math.max(0.0F, contentH - bodyH);
      this.scrollAnim.setTarget(Math.clamp(this.scrollAnim.getTarget(), 0.0F, this.maxScroll));

      if (this.maxScroll > 1.0F) {
         float trackH = bodyH - 16.0F;
         float thumbH = Math.max(24.0F, trackH * (bodyH / contentH));
         float thumbY = bodyY + 8.0F + (trackH - thumbH) * (this.scrollAnim.value() / this.maxScroll);
         vg.rect(bodyX + bodyW - 5.5F, bodyY + 8.0F, 3.0F, trackH, 1.5F, Colors.withAlpha(theme.borderSubtle(), 0.5F));
         vg.rect(bodyX + bodyW - 5.5F, thumbY, 3.0F, thumbH, 1.5F, Colors.withAlpha(theme.accentBright(), 0.78F));
      }

      vg.restore();
   }

   private float measureModuleCard(CorzModuleHandle mod, boolean expanded) {
      float h = 34.0F;
      if (!expanded) {
         return h;
      }
      h += 8.0F;
      for (CorzSettingHandle s : mod.settings()) {
         h += this.settingRowHeight(s) + 4.0F;
      }
      // Built-in Module Keybind row + optional Studio button
      h += 24.0F;
      if (mod.hasCustomStudio()) {
         h += 28.0F;
      }
      return h + 6.0F;
   }

   private float settingRowHeight(CorzSettingHandle s) {
      return switch (s.kind()) {
         case BOOLEAN, KEYBIND, BUTTON -> 24.0F;
         case INT, DOUBLE, RANGE, COLOR -> 34.0F;
         case ENUM, STRING -> 28.0F;
      };
   }

   private void renderModuleCard(
      NVGRenderer vg,
      Theme theme,
      CorzModuleHandle mod,
      boolean expanded,
      boolean showCatTag,
      float cx,
      float cy,
      float cw,
      float ch,
      float mx,
      float my
   ) {
      boolean enabled = mod.isEnabled();
      boolean rowHov = this.hit(mx, my, cx, cy, cw, 34.0F);
      int bg = enabled ? theme.moduleActiveFill() : (rowHov ? theme.surfaceElevated() : theme.surface());
      if (enabled && theme.isBloom()) {
         vg.glow(cx, cy, cw, ch, 8.0F, 8.0F, Colors.withAlpha(theme.accent(), 0.16F));
      }
      vg.rect(cx, cy, cw, ch, 8.0F, bg);
      vg.rectOutline(cx, cy, cw, ch, 8.0F, 1.0F, Colors.withAlpha(enabled ? theme.accentBright() : theme.borderSubtle(), enabled ? 0.52F : 0.85F));

      float midY = cy + 17.0F;
      if (enabled) {
         vg.rect(cx + 1.5F, cy + 7.0F, 3.0F, 20.0F, 1.5F, theme.accentBright());
      }

      // Pill switch on right + chevron
      float swW = 26.0F;
      float swH = 14.0F;
      float swX = cx + cw - 42.0F;
      float swY = midY - swH * 0.5F;
      vg.rect(swX, swY, swW, swH, 7.0F, enabled ? theme.accent() : theme.surfacePressed());
      vg.rectOutline(swX, swY, swW, swH, 7.0F, 1.0F, Colors.withAlpha(enabled ? theme.accentBright() : theme.borderSubtle(), 0.8F));
      vg.circle(swX + (enabled ? swW - 7.0F : 7.0F), midY, 4.8F, enabled ? 0xFFFFFFFF : theme.textMuted());

      vg.chevron(cx + cw - 12.0F, midY, 3.8F, 1.5F, expanded ? theme.accentBright() : theme.textMuted(), expanded);

      float textMaxW = (swX - 8.0F) - (cx + 12.0F);
      if (showCatTag) {
         String cat = mod.category();
         float catW = vg.textWidth(cat, 9.0F) + 10.0F;
         float tagX = swX - 8.0F - catW;
         vg.rect(tagX, midY - 7.0F, catW, 14.0F, 5.0F, Colors.withAlpha(theme.accent(), 0.14F));
         vg.text(cat, tagX + 5.0F, midY, 9.0F, theme.textMuted());
         textMaxW -= (catW + 6.0F);
      }
      vg.textTruncated(mod.displayName(), cx + 12.0F, midY, 12.5F, enabled ? theme.textPrimary() : (rowHov ? theme.textPrimary() : theme.textMuted()), Math.max(40.0F, textMaxW));

      if (!expanded) {
         return;
      }

      vg.line(cx + 10.0F, cy + 34.0F, cx + cw - 10.0F, cy + 34.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.8F));
      float sy = cy + 40.0F;
      float sx = cx + 12.0F;
      float sw = cw - 24.0F;

      for (CorzSettingHandle s : mod.settings()) {
         float rh = this.settingRowHeight(s);
         this.renderSettingRow(vg, theme, s, sx, sy, sw, rh, mx, my);
         sy += rh + 4.0F;
      }

      // Built-in Module Keybind row
      boolean listeningMod = this.listeningModuleKey == mod;
      vg.text("Module Keybind", sx, sy + 12.0F, 11.5F, theme.textMuted());
      String kbTxt = listeningMod ? "Press key..." : formatKey(mod.getBindKey());
      float kbw = vg.textWidth(kbTxt, 10.5F) + 14.0F;
      float kbx = sx + sw - kbw;
      vg.rect(kbx, sy + 3.0F, kbw, 18.0F, 5.0F, listeningMod ? Colors.withAlpha(theme.accent(), 0.28F) : theme.surfacePressed());
      vg.rectOutline(kbx, sy + 3.0F, kbw, 18.0F, 5.0F, 1.0F, Colors.withAlpha(listeningMod ? theme.accentBright() : theme.borderSubtle(), 0.85F));
      vg.text(kbTxt, kbx + 7.0F, sy + 12.0F, 10.5F, listeningMod ? theme.accentBright() : theme.textPrimary());
      sy += 24.0F;

      if (mod.hasCustomStudio()) {
         boolean bHov = this.hit(mx, my, sx, sy + 2.0F, sw, 24.0F);
         vg.rect(sx, sy + 2.0F, sw, 24.0F, 6.0F, Colors.withAlpha(theme.accent(), bHov ? 0.26F : 0.16F));
         vg.rectOutline(sx, sy + 2.0F, sw, 24.0F, 6.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.62F));
         String lbl = "Open Corz Studio \u2197";
         vg.text(lbl, sx + (sw - vg.textWidth(lbl, 11.0F)) * 0.5F, sy + 14.0F, 11.0F, theme.accentBright());
      }
   }

   private void renderSettingRow(NVGRenderer vg, Theme theme, CorzSettingHandle s, float x, float y, float w, float h, float mx, float my) {
      switch (s.kind()) {
         case BOOLEAN -> {
            float cy = y + h * 0.5F;
            vg.textTruncated(s.name(), x, cy, 11.5F, s.getBool() ? theme.textPrimary() : theme.textMuted(), w - 34.0F);
            boolean on = s.getBool();
            float tx = x + w - 24.0F;
            float ty = cy - 6.5F;
            vg.rect(tx, ty, 24.0F, 13.0F, 6.5F, on ? theme.accent() : theme.surfacePressed());
            vg.circle(tx + (on ? 17.5F : 6.5F), cy, 4.5F, on ? 0xFFFFFFFF : theme.textMuted());
         }
         case INT, DOUBLE -> {
            vg.textTruncated(s.name(), x, y + 10.0F, 11.5F, theme.textPrimary(), w - 64.0F);
            String val = s.formatValue();
            float vw = vg.textWidth(val, 10.5F);
            vg.text(val, x + w - vw, y + 10.0F, 10.5F, theme.accentBright());
            float ty = y + 23.0F;
            vg.rect(x, ty, w, 5.0F, 2.5F, theme.surfacePressed());
            double span = Math.max(1.0E-6, s.max() - s.min());
            float frac = (float)Math.clamp((s.getNumber() - s.min()) / span, 0.0, 1.0);
            float fw = w * frac;
            if (fw > 0.5F) {
               vg.rectGradient(x, ty, Math.max(4.0F, fw), 5.0F, 2.5F, theme.accent(), theme.accentBright(), false);
            }
            vg.circle(x + fw, ty + 2.5F, 5.2F, 0xFFFFFFFF);
            vg.circle(x + fw, ty + 2.5F, 2.6F, theme.accent());
         }
         case RANGE -> {
            vg.textTruncated(s.name(), x, y + 10.0F, 11.5F, theme.textPrimary(), w - 80.0F);
            String val = s.formatValue();
            vg.text(val, x + w - vg.textWidth(val, 10.5F), y + 10.0F, 10.5F, theme.accentBright());
            float ty = y + 23.0F;
            vg.rect(x, ty, w, 5.0F, 2.5F, theme.surfacePressed());
            double span = Math.max(1.0E-6, s.max() - s.min());
            float fLo = (float)Math.clamp((s.getRangeMin() - s.min()) / span, 0.0, 1.0);
            float fHi = (float)Math.clamp((s.getRangeMax() - s.min()) / span, 0.0, 1.0);
            float xLo = x + w * fLo;
            float xHi = x + w * fHi;
            vg.rectGradient(xLo, ty, Math.max(4.0F, xHi - xLo), 5.0F, 2.5F, theme.accent(), theme.accentBright(), false);
            vg.circle(xLo, ty + 2.5F, 4.8F, 0xFFFFFFFF);
            vg.circle(xHi, ty + 2.5F, 4.8F, theme.accentBright());
         }
         case ENUM -> {
            float cy = y + h * 0.5F;
            vg.textTruncated(s.name(), x, cy, 11.5F, theme.textMuted(), w * 0.52F);
            String cur = "< " + s.currentOption() + " >";
            float pw = Math.min(w * 0.46F, vg.textWidth(cur, 10.5F) + 14.0F);
            float px = x + w - pw;
            vg.rect(px, cy - 10.0F, pw, 20.0F, 5.5F, Colors.withAlpha(theme.accent(), 0.18F));
            vg.rectOutline(px, cy - 10.0F, pw, 20.0F, 5.5F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.55F));
            vg.textTruncated(cur, px + 7.0F, cy, 10.5F, theme.textPrimary(), pw - 14.0F);
         }
         case COLOR -> {
            vg.textTruncated(s.name(), x, y + 10.0F, 11.5F, theme.textPrimary(), w - 40.0F);
            vg.circle(x + w - 8.0F, y + 10.0F, 6.0F, s.getColor() | 0xFF000000);
            vg.circleOutline(x + w - 8.0F, y + 10.0F, 6.5F, 1.0F, 0x99FFFFFF);
            // Rainbow hue strip
            float ty = y + 23.0F;
            int segs = 6;
            float segW = w / segs;
            int[] hues = {0xFFFF0040, 0xFFFFFF00, 0xFF00FF40, 0xFF00FFFF, 0xFF0040FF, 0xFFFF00FF, 0xFFFF0040};
            for (int i = 0; i < segs; i++) {
               vg.rectGradient(x + i * segW, ty, segW + 0.8F, 5.5F, i == 0 || i == segs - 1 ? 2.5F : 0.0F, hues[i], hues[i + 1], false);
            }
            float[] hsb = Color.RGBtoHSB((s.getColor() >> 16) & 0xFF, (s.getColor() >> 8) & 0xFF, s.getColor() & 0xFF, null);
            vg.circle(x + w * hsb[0], ty + 2.75F, 5.0F, 0xFFFFFFFF);
         }
         case STRING -> {
            float cy = y + h * 0.5F;
            vg.textTruncated(s.name(), x, cy, 11.5F, theme.textMuted(), w * 0.42F);
            float bx = x + w * 0.45F;
            float bw = w * 0.55F;
            boolean foc = this.focusedStringSetting == s;
            vg.rect(bx, cy - 10.0F, bw, 20.0F, 5.0F, theme.surfacePressed());
            vg.rectOutline(bx, cy - 10.0F, bw, 20.0F, 5.0F, 1.0F, Colors.withAlpha(foc ? theme.accentBright() : theme.borderSubtle(), 0.9F));
            vg.textTruncated(s.getString(), bx + 6.0F, cy, 10.5F, theme.textPrimary(), bw - 12.0F);
         }
         case KEYBIND -> {
            float cy = y + h * 0.5F;
            boolean lis = this.listeningSettingKey == s;
            vg.textTruncated(s.name(), x, cy, 11.5F, theme.textMuted(), w - 85.0F);
            String kt = lis ? "Press key..." : formatKey(s.getKey());
            float kw = vg.textWidth(kt, 10.5F) + 14.0F;
            float kx = x + w - kw;
            vg.rect(kx, cy - 9.0F, kw, 18.0F, 5.0F, lis ? Colors.withAlpha(theme.accent(), 0.28F) : theme.surfacePressed());
            vg.rectOutline(kx, cy - 9.0F, kw, 18.0F, 5.0F, 1.0F, Colors.withAlpha(lis ? theme.accentBright() : theme.borderSubtle(), 0.85F));
            vg.text(kt, kx + 7.0F, cy, 10.5F, lis ? theme.accentBright() : theme.textPrimary());
         }
         case BUTTON -> {
            float cy = y + h * 0.5F;
            vg.textTruncated(s.name(), x, cy, 11.5F, theme.textMuted(), w * 0.55F);
            String bl = s.buttonLabel().isEmpty() ? "Run" : s.buttonLabel();
            float bw = Math.min(w * 0.42F, vg.textWidth(bl, 10.5F) + 16.0F);
            float bx = x + w - bw;
            boolean hov = this.hit(mx, my, bx, cy - 9.5F, bw, 19.0F);
            vg.rect(bx, cy - 9.5F, bw, 19.0F, 5.5F, Colors.withAlpha(theme.accent(), hov ? 0.30F : 0.18F));
            vg.rectOutline(bx, cy - 9.5F, bw, 19.0F, 5.5F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.62F));
            vg.textTruncated(bl, bx + 8.0F, cy, 10.5F, theme.accentBright(), bw - 14.0F);
         }
      }
   }

   public boolean mouseClicked(NVGRenderer vg, float mx, float my, int btn) {
      if (!this.state.isCorzPortOpen() || !this.hit(mx, my, this.x, this.y, this.w, this.h)) {
         this.searchFocused = false;
         this.focusedStringSetting = null;
         return false;
      }

      // Close button
      if (btn == 0 && this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize)) {
         this.state.setCorzPortOpen(false);
         UiSounds.guiClose();
         return true;
      }

      // Search pill
      if (this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 18.0F) {
            this.search.setLength(0);
         } else {
            this.searchFocused = true;
         }
         this.focusedStringSetting = null;
         UiSounds.select();
         return true;
      }
      this.searchFocused = false;
      this.focusedStringSetting = null;

      // Dragging header
      if (btn == 0 && this.hit(mx, my, this.x, this.y, this.w, HEADER_H)) {
         this.draggingHeader = true;
         this.dragOffX = mx - this.x;
         this.dragOffY = my - this.y;
         return true;
      }

      // Category pills
      float stripY = this.y + HEADER_H;
      if (btn == 0 && this.hit(mx, my, this.x, stripY, this.w, TABS_H)) {
         float px = this.x + 10.0F;
         for (String tab : this.tabNames()) {
            float pw = vg.textWidth(tab, 10.5F) + 14.0F;
            if (this.hit(mx, my, px, stripY + 6.0F, pw, 22.0F)) {
               this.state.setCorzSelectedCategory(tab);
               this.search.setLength(0);
               this.scrollAnim.snapTo(0.0F);
               UiSounds.select();
               return true;
            }
            px += pw + 4.0F;
         }
         return true;
      }

      // Modules list
      float bodyY = this.y + HEADER_H + TABS_H;
      float bodyH = this.h - HEADER_H - TABS_H;
      if (this.hit(mx, my, this.x, bodyY, this.w, bodyH)) {
         float pad = 10.0F;
         float cardW = this.w - pad * 2.0F - 4.0F;
         float cx = this.x + pad;
         float curY = bodyY + pad - this.scrollAnim.value();

         for (CorzModuleHandle mod : this.collectVisible()) {
            boolean exp = this.expandedIds.contains(mod.id());
            float ch = this.measureModuleCard(mod, exp);
            if (this.hit(mx, my, cx, curY, cardW, ch)) {
               if (my <= curY + 34.0F) {
                  if (btn == 1 || mx >= cx + cardW - 20.0F) {
                     if (!this.expandedIds.remove(mod.id())) {
                        this.expandedIds.add(mod.id());
                     }
                     UiSounds.panelCollapse();
                  } else if (btn == 0) {
                     mod.toggle();
                     UiSounds.toggle(mod.isEnabled());
                  }
                  return true;
               }
               if (exp) {
                  this.clickExpandedSettings(mod, cx + 12.0F, curY + 40.0F, cardW - 24.0F, mx, my, btn);
                  return true;
               }
            }
            curY += ch + 6.0F;
         }
      }
      return true;
   }

   private void clickExpandedSettings(CorzModuleHandle mod, float sx, float sy, float sw, float mx, float my, int btn) {
      for (CorzSettingHandle s : mod.settings()) {
         float rh = this.settingRowHeight(s);
         if (this.hit(mx, my, sx, sy, sw, rh)) {
            switch (s.kind()) {
               case BOOLEAN -> {
                  s.setBool(!s.getBool());
                  UiSounds.select();
               }
               case INT, DOUBLE -> {
                  this.activeSlider = s;
                  this.sliderTrackX = sx;
                  this.sliderTrackW = sw;
                  this.applySliderDrag(mx);
               }
               case RANGE -> {
                  this.activeSlider = s;
                  this.sliderTrackX = sx;
                  this.sliderTrackW = sw;
                  double span = Math.max(1.0E-6, s.max() - s.min());
                  float xLo = sx + sw * (float)((s.getRangeMin() - s.min()) / span);
                  float xHi = sx + sw * (float)((s.getRangeMax() - s.min()) / span);
                  this.draggingRangeMax = Math.abs(mx - xHi) <= Math.abs(mx - xLo);
                  this.applySliderDrag(mx);
               }
               case ENUM -> {
                  List<String> opts = s.options();
                  if (!opts.isEmpty()) {
                     int delta = btn == 1 ? -1 : 1;
                     s.setOptionIndex((s.optionIndex() + delta + opts.size()) % opts.size());
                     UiSounds.select();
                  }
               }
               case COLOR -> {
                  this.activeHueColor = s;
                  this.hueTrackX = sx;
                  this.hueTrackW = sw;
                  this.applyHueDrag(mx);
               }
               case STRING -> {
                  this.focusedStringSetting = s;
                  UiSounds.select();
               }
               case KEYBIND -> {
                  if (btn == 1) {
                     s.setKey(-1);
                     UiSounds.keybindSet();
                  } else {
                     this.listeningSettingKey = s;
                     UiSounds.keybindListen();
                  }
               }
               case BUTTON -> {
                  s.pressButton();
                  UiSounds.select();
               }
            }
            return;
         }
         sy += rh + 4.0F;
      }

      if (this.hit(mx, my, sx, sy, sw, 24.0F)) {
         if (btn == 1) {
            mod.setBindKey(-1);
            UiSounds.keybindSet();
         } else {
            this.listeningModuleKey = mod;
            UiSounds.keybindListen();
         }
         return;
      }
      sy += 24.0F;

      if (mod.hasCustomStudio() && this.hit(mx, my, sx, sy, sw, 26.0F)) {
         CorzPortProvider p = CorzPortBus.provider();
         if (p != null) {
            p.openNativeStudio(mod.id());
         }
      }
   }

   private void applySliderDrag(float mx) {
      if (this.activeSlider == null) {
         return;
      }
      float frac = Math.clamp((mx - this.sliderTrackX) / Math.max(1.0F, this.sliderTrackW), 0.0F, 1.0F);
      double val = this.activeSlider.min() + frac * (this.activeSlider.max() - this.activeSlider.min());
      if (this.activeSlider.kind() == CorzSettingHandle.Kind.RANGE) {
         if (this.draggingRangeMax) {
            this.activeSlider.setRange(this.activeSlider.getRangeMin(), Math.max(val, this.activeSlider.getRangeMin()));
         } else {
            this.activeSlider.setRange(Math.min(val, this.activeSlider.getRangeMax()), this.activeSlider.getRangeMax());
         }
      } else {
         this.activeSlider.setNumber(val);
      }
   }

   private void applyHueDrag(float mx) {
      if (this.activeHueColor == null) {
         return;
      }
      float hue = Math.clamp((mx - this.hueTrackX) / Math.max(1.0F, this.hueTrackW), 0.0F, 1.0F);
      int rgb = Color.HSBtoRGB(hue, 0.78F, 1.0F);
      this.activeHueColor.setColor(0xFF000000 | (rgb & 0xFFFFFF));
   }

   public boolean mouseDragged(float mx, float my) {
      if (this.draggingHeader) {
         this.customMoved = true;
         this.customX = mx - this.dragOffX;
         this.customY = my - this.dragOffY;
         return true;
      }
      if (this.activeSlider != null) {
         this.applySliderDrag(mx);
         return true;
      }
      if (this.activeHueColor != null) {
         this.applyHueDrag(mx);
         return true;
      }
      return false;
   }

   public void mouseReleased() {
      this.draggingHeader = false;
      this.activeSlider = null;
      this.activeHueColor = null;
   }

   public boolean mouseScrolled(float mx, float my, double scrollY) {
      if (!this.state.isCorzPortOpen() || !this.hit(mx, my, this.x, this.y, this.w, this.h)) {
         return false;
      }
      if (this.maxScroll > 0.0F) {
         this.scrollAnim.setTarget(Math.clamp(this.scrollAnim.getTarget() - (float)scrollY * 42.0F, 0.0F, this.maxScroll));
      }
      return true;
   }

   public boolean keyPressed(int keyCode) {
      if (this.listeningSettingKey != null) {
         if (keyCode == 256 || keyCode == 259 || keyCode == 261) {
            this.listeningSettingKey.setKey(-1);
         } else {
            this.listeningSettingKey.setKey(keyCode);
         }
         this.listeningSettingKey = null;
         return true;
      }
      if (this.listeningModuleKey != null) {
         if (keyCode == 256 || keyCode == 259 || keyCode == 261) {
            this.listeningModuleKey.setBindKey(-1);
         } else {
            this.listeningModuleKey.setBindKey(keyCode);
         }
         this.listeningModuleKey = null;
         return true;
      }
      if (this.focusedStringSetting != null) {
         if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
            this.focusedStringSetting = null;
         } else if (keyCode == 259) {
            String cur = this.focusedStringSetting.getString();
            if (!cur.isEmpty()) {
               this.focusedStringSetting.setString(cur.substring(0, cur.length() - 1));
            }
         }
         return true;
      }
      if (this.searchFocused) {
         if (keyCode == 256) {
            this.search.setLength(0);
            this.searchFocused = false;
         } else if (keyCode == 257 || keyCode == 335) {
            this.searchFocused = false;
         } else if (keyCode == 259 && !this.search.isEmpty()) {
            this.search.deleteCharAt(this.search.length() - 1);
         }
         return true;
      }
      return false;
   }

   public boolean charTyped(int codepoint) {
      String ch = new String(Character.toChars(codepoint));
      if (this.focusedStringSetting != null) {
         String cur = this.focusedStringSetting.getString();
         if (cur.length() < 128) {
            this.focusedStringSetting.setString(cur + ch);
         }
         return true;
      }
      if (this.searchFocused) {
         if (this.search.length() < 32) {
            this.search.append(ch);
            this.scrollAnim.snapTo(0.0F);
         }
         return true;
      }
      return false;
   }

   private boolean hit(float mx, float my, float bx, float by, float bw, float bh) {
      return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
   }

   private static String formatKey(int keyCode) {
      if (keyCode < 0) {
         return "None";
      }
      try {
         String s = InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
         return (s == null || s.isBlank()) ? "Key " + keyCode : s.toUpperCase(Locale.ROOT);
      } catch (Throwable t) {
         return "Key " + keyCode;
      }
   }
}
