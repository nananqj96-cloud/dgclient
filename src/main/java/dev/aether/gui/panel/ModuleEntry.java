package dev.aether.gui.panel;

import dev.aether.gui.ClickGuiState;
import dev.aether.gui.widget.BlockListWidget;
import dev.aether.gui.widget.BooleanWidget;
import dev.aether.gui.widget.ColorWidget;
import dev.aether.gui.widget.IconListWidget;
import dev.aether.gui.widget.KeybindWidget;
import dev.aether.gui.widget.ModeWidget;
import dev.aether.gui.widget.SettingWidget;
import dev.aether.gui.widget.SliderWidget;
import dev.aether.gui.widget.StringWidget;
import dev.aether.module.Module;
import dev.aether.render.anim.Animation;
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
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.List;

public class ModuleEntry {
   public static final float COMPACT_ROW_H = 32.0F;
   public static final float CARD_ROW_H = 44.0F;
   private static final int FAV_GOLD = 0xFFFBBF24;

   private final Module module;
   private final ThemeManager themes;
   private final ClickGuiState state;
   private final String key;
   private final List<SettingWidget> widgets = new ArrayList<>();
   private final Animation hover = new Animation(120.0F, 0.0F);
   private final Animation enable = new Animation(140.0F, 0.0F);
   private final Animation expand;
   private boolean showSubtitle;
   private float x;
   private float y;
   private float width;

   public ModuleEntry(Module module, ThemeManager themes, ClickGuiState state) {
      this.module = module;
      this.themes = themes;
      this.state = state;
      this.key = module.getName() + "@" + module.getCategory().name();
      this.expand = new Animation(160.0F, state.isExpanded(this.key) ? 1.0F : 0.0F);
      this.enable.snapTo(module.isEnabled() ? 1.0F : 0.0F);

      for (Setting<?> s : module.getSettings()) {
         if (s instanceof BooleanSetting b) {
            this.widgets.add(new BooleanWidget(themes, b));
         } else if (s instanceof SliderSetting sl) {
            this.widgets.add(new SliderWidget(themes, sl));
         } else if (s instanceof ModeSetting m) {
            this.widgets.add(new ModeWidget(themes, m));
         } else if (s instanceof ColorSetting c) {
            this.widgets.add(new ColorWidget(themes, c));
         } else if (s instanceof BlockListSetting bl) {
            this.widgets.add(new BlockListWidget(themes, bl));
         } else if (s instanceof IconListSetting il) {
            this.widgets.add(new IconListWidget(themes, il));
         } else if (s instanceof StringSetting st) {
            this.widgets.add(new StringWidget(themes, st));
         } else if (s instanceof KeybindSetting kb) {
            this.widgets.add(new KeybindWidget(themes, kb));
         }
      }
      this.widgets.add(new KeybindWidget(themes, module.getKeybind()));
   }

   public Module getModule() {
      return this.module;
   }

   public String key() {
      return this.key;
   }

   public boolean isFavourite() {
      return this.state.isFavourite(this.key);
   }

   public void setShowSubtitle(boolean show) {
      this.showSubtitle = show && !this.module.getDescription().isEmpty();
   }

   public float headerHeight() {
      return this.showSubtitle ? CARD_ROW_H : COMPACT_ROW_H;
   }

   public void setBounds(float x, float y, float width) {
      this.x = x;
      this.y = y;
      this.width = width;
   }

   private void syncExternalExpand() {
      float desired = this.state.isExpanded(this.key) ? 1.0F : 0.0F;
      if (Math.abs(this.expand.getTarget() - desired) > 0.1F) {
         this.expand.setTarget(desired);
      }
   }

   private float settingsHeight(NVGRenderer vg) {
      float innerW = Math.max(120.0F, this.width - 24.0F);
      float total = 8.0F;
      for (SettingWidget w : this.widgets) {
         if (w.isVisible()) {
            w.setBounds(this.x + 12.0F, this.y, innerW);
            total += w.height(vg) + 4.0F;
         }
      }
      return total + 4.0F;
   }

   public float height(NVGRenderer vg) {
      this.syncExternalExpand();
      float exp = this.expand.value();
      return this.headerHeight() + (exp <= 0.005F ? 0.0F : exp * this.settingsHeight(vg));
   }

   public void render(NVGRenderer vg, float mx, float my, float alphaMul) {
      this.syncExternalExpand();
      Theme theme = this.theme();
      float headH = this.headerHeight();
      float exp = this.expand.value();
      float fullH = headH + (exp <= 0.005F ? 0.0F : exp * this.settingsHeight(vg));

      boolean rowHovered = mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + headH;
      if (rowHovered && this.hover.getTarget() < 0.5F) {
         UiSounds.hover();
      }
      this.hover.setTarget(rowHovered ? 1.0F : 0.0F);
      this.enable.setTarget(this.module.isEnabled() ? 1.0F : 0.0F);

      float hov = this.hover.value();
      float en = this.enable.value();

      vg.save();
      vg.alpha(alphaMul);

      // Card surface + subtle border + PinkBloom active glow
      if (en > 0.02F && theme.isBloom()) {
         vg.glow(this.x, this.y, this.width, fullH, 7.0F, 8.0F, Colors.withAlpha(theme.accent(), 0.20F * en));
      }
      int baseFill = Colors.lerp(theme.surface(), theme.moduleActiveFill(), en);
      if (hov > 0.01F) {
         baseFill = Colors.lerp(baseFill, theme.surfaceElevated(), hov * 0.65F);
      }
      vg.rect(this.x, this.y, this.width, fullH, 7.0F, baseFill);
      int borderCol = Colors.lerp(
         Colors.withAlpha(theme.borderSubtle(), 0.75F),
         Colors.withAlpha(theme.accent(), 0.52F),
         Math.max(en, hov * 0.45F)
      );
      vg.rectOutline(this.x, this.y, this.width, fullH, 7.0F, 1.0F, borderCol);

      // Left active indicator bar
      if (en > 0.01F) {
         float barH = (headH - 14.0F) * en;
         float barY = this.y + (headH - barH) * 0.5F;
         vg.rect(this.x + 2.5F, barY, 3.0F, barH, 1.5F, Colors.withAlpha(theme.accentBright(), en));
      }

      // Title + optional concise subtitle (single text pass — no double render!)
      float textLeft = this.x + 12.0F;
      float rightControlsW = 78.0F;
      float maxTitleW = Math.max(40.0F, this.width - rightControlsW - 16.0F);
      int titleCol = Colors.lerp(Colors.lerp(theme.textMuted(), theme.textPrimary(), hov * 0.75F), theme.textPrimary(), en);

      if (this.showSubtitle) {
         vg.textTruncated(this.module.getName(), textLeft, this.y + 15.0F, 13.0F, titleCol, maxTitleW);
         vg.textTruncated(this.module.getDescription(), textLeft, this.y + 31.0F, 10.5F, theme.textDisabled(), maxTitleW);
      } else {
         vg.textTruncated(this.module.getName(), textLeft, this.y + headH * 0.5F, 12.5F, titleCol, maxTitleW);
      }

      float midY = this.y + headH * 0.5F;

      // Favourite star (single draw call!)
      float starCx = this.x + this.width - 62.0F;
      boolean fav = this.isFavourite();
      vg.star(starCx, midY, 5.2F, 2.4F, fav ? FAV_GOLD : Colors.withAlpha(theme.textDisabled(), 0.75F), fav);

      // Expand settings chevron
      float chevCx = this.x + this.width - 44.0F;
      vg.save();
      vg.translate(chevCx, midY);
      vg.rotate(exp * (float)(Math.PI * 0.5));
      vg.chevron(0.0F, 0.0F, 3.8F, 1.6F, exp > 0.1F ? theme.accentBright() : theme.textMuted(), false);
      vg.restore();

      // Pill toggle switch
      float pillW = 26.0F;
      float pillH = 14.0F;
      float pillX = this.x + this.width - pillW - 9.0F;
      float pillY = midY - pillH * 0.5F;
      int trackCol = Colors.lerp(Colors.withAlpha(theme.statusDisabled(), 0.80F), Colors.withAlpha(theme.accent(), 0.90F), en);
      vg.rect(pillX, pillY, pillW, pillH, 7.0F, trackCol);
      float knobX = pillX + 7.0F + en * (pillW - 14.0F);
      vg.circle(knobX, midY, 4.6F, Colors.lerp(theme.textMuted(), 0xFFFFFFFF, en));

      // Expandable settings drawer
      if (exp > 0.005F) {
         float sh = this.settingsHeight(vg) * exp;
         vg.save();
         vg.scissor(this.x, this.y + headH, this.width, sh);
         vg.alpha(exp);
         vg.line(this.x + 10.0F, this.y + headH, this.x + this.width - 10.0F, this.y + headH, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.85F));

         float wy = this.y + headH + 8.0F;
         float innerW = this.width - 24.0F;
         for (SettingWidget widget : this.widgets) {
            if (widget.isVisible()) {
               widget.setBounds(this.x + 12.0F, wy, innerW);
               widget.render(vg, mx, my);
               wy += widget.height(vg) + 4.0F;
            }
         }
         vg.restore();
      }

      vg.restore();
   }

   private Theme theme() {
      return this.themes.current();
   }

   private void toggleExpand() {
      boolean next = !(this.expand.getTarget() > 0.5F);
      this.expand.setTarget(next ? 1.0F : 0.0F);
      this.state.setExpanded(this.key, next);
      UiSounds.select();
   }

   public boolean mouseClicked(float mx, float my, int button) {
      float headH = this.headerHeight();
      if (mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + headH) {
         float starLeft = this.x + this.width - 71.0F;
         float starRight = this.x + this.width - 53.0F;
         float chevRight = this.x + this.width - 36.0F;
         if (button == 0 && mx >= starLeft && mx < starRight) {
            this.state.toggleFavourite(this.key);
            UiSounds.select();
            return true;
         }
         if (button == 1 || (button == 0 && mx >= starRight && mx < chevRight)) {
            this.toggleExpand();
            return true;
         }
         if (button == 0) {
            this.module.toggle();
            UiSounds.toggle(this.module.isEnabled());
            return true;
         }
         return true;
      } else if (this.expand.getTarget() > 0.5F && my >= this.y + headH && my <= this.y + this.height(null)) {
         for (SettingWidget w : this.widgets) {
            if (w.isVisible() && w.mouseClicked(mx, my, button)) {
               return true;
            }
         }
         return mx >= this.x && mx <= this.x + this.width;
      }
      return false;
   }

   public void mouseDragged(float mx, float my) {
      for (SettingWidget w : this.widgets) {
         w.mouseDragged(mx, my);
      }
   }

   public void mouseReleased() {
      for (SettingWidget w : this.widgets) {
         w.mouseReleased();
      }
   }

   public boolean keyPressed(int keyCode) {
      for (SettingWidget w : this.widgets) {
         if (w.keyPressed(keyCode)) {
            return true;
         }
      }
      return false;
   }

   public boolean charTyped(int codepoint) {
      for (SettingWidget w : this.widgets) {
         if (w.charTyped(codepoint)) {
            return true;
         }
      }
      return false;
   }

   public boolean isListening() {
      for (SettingWidget w : this.widgets) {
         if (w.isListening()) {
            return true;
         }
      }
      return false;
   }
}
