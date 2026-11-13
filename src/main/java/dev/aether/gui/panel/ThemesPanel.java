package dev.aether.gui.panel;

import dev.aether.AetherClient;
import dev.aether.gui.ClickGuiState;
import dev.aether.gui.widget.BooleanWidget;
import dev.aether.gui.widget.ColorWidget;
import dev.aether.gui.widget.SettingWidget;
import dev.aether.gui.widget.SliderWidget;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.Setting;
import dev.aether.settings.SliderSetting;
import dev.aether.theme.SoundSettings;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ThemesPanel extends Panel {
   private final ColorWidget accentWidget;
   private final ColorSetting accentProxy;
   private final List<SettingWidget> soundWidgets = new ArrayList<>();
   private int hoveredRow = -1;
   private float lastStartY = Float.MIN_VALUE;

   public ThemesPanel(ThemeManager themes, ClickGuiState state) {
      super(themes, state.panel("__themes__"));
      this.accentProxy = new ColorSetting("Accent", "Custom theme accent color", themes.current().accent()) {
         @Override
         public void set(Integer value) {
            super.set(value);
            Theme current = themes.current();
            if (current.isCustom()) {
               current.setAccent(value | 0xFF000000);
            }
         }
      };
      this.accentWidget = new ColorWidget(themes, this.accentProxy);
      SoundSettings sounds = AetherClient.sounds();
      if (sounds != null) {
         for (Setting<?> setting : sounds.all()) {
            if (setting instanceof SliderSetting slider) {
               this.soundWidgets.add(new SliderWidget(themes, slider));
            } else if (setting instanceof BooleanSetting bool) {
               this.soundWidgets.add(new BooleanWidget(themes, bool));
            }
         }
      }
   }

   @Override
   protected String title() {
      return "Themes";
   }

   @Override
   protected float contentHeight(NVGRenderer vg) {
      float h = 12.0F + (float)this.themes.getThemes().size() * 26.0F + 28.0F;
      if (this.themes.current().isCustom()) {
         h += this.accentWidget.height(vg) + 6.0F;
      }

      h += 48.0F;
      for (SettingWidget widget : this.soundWidgets) {
         h += widget.height(vg) + 3.0F;
      }
      return h;
   }

   @Override
   protected void renderContent(NVGRenderer vg, float startY, float mx, float my, float viewTop, float viewBottom) {
      this.lastStartY = startY;
      Theme active = this.themes.current();
      float rowY = startY;
      int rowIndex = 0;
      int newHoveredRow = -1;

      for (Theme theme : this.themes.getThemes()) {
         float fade = this.edgeFade(rowY, rowY + 26.0F, viewTop, viewBottom);
         vg.save();
         vg.alpha(fade);
         boolean selected = theme == active;
         boolean hovered = my >= rowY && my <= rowY + 26.0F && mx >= this.ps.x + 6.0F && mx <= this.ps.x + WIDTH - 6.0F;
         if (hovered) {
            newHoveredRow = rowIndex;
         }
         rowIndex++;
         if (selected || hovered) {
            vg.rect(this.ps.x + 6.0F, rowY, WIDTH - 12.0F, 26.0F, 7.0F, Colors.withAlpha(this.theme().accent(), selected ? 0.16F : 0.08F));
         }

         float sy = rowY + 13.0F;
         vg.circle(this.ps.x + 20.0F, sy, 6.0F, theme.accent());
         if (selected) {
            vg.rectOutline(this.ps.x + 11.0F, sy - 9.0F, 18.0F, 18.0F, 9.0F, 1.4F, this.theme().accentBright());
         }

         vg.text(theme.getName(), this.ps.x + 36.0F, sy, 13.0F, selected ? this.theme().textPrimary() : this.theme().textMuted());
         if (theme.isCustom()) {
            vg.cross(this.ps.x + WIDTH - 26.0F, sy - 5.5F, 11.0F, 1.6F, this.theme().textDisabled());
         }

         vg.restore();
         rowY += 26.0F;
      }

      if (active.isCustom()) {
         this.accentWidget.setBounds(this.ps.x + 14.0F, rowY + 3.0F, WIDTH - 28.0F);
         this.accentWidget.render(vg, mx, my);
         rowY += this.accentWidget.height(vg) + 6.0F;
      }

      float fadex = this.edgeFade(rowY, rowY + 28.0F, viewTop, viewBottom);
      vg.save();
      vg.alpha(fadex);
      boolean hoveredx = my >= rowY && my <= rowY + 24.0F && mx >= this.ps.x + 6.0F && mx <= this.ps.x + WIDTH - 6.0F;
      vg.rect(this.ps.x + 6.0F, rowY, WIDTH - 12.0F, 24.0F, 7.0F, Colors.withAlpha(this.theme().accent(), hoveredx ? 0.22F : 0.12F));
      String label = "+  Add Custom";
      vg.text(
         label,
         this.ps.x + (WIDTH - vg.textWidth(label, 12.5F)) * 0.5F,
         rowY + 12.0F,
         12.5F,
         hoveredx ? this.theme().accentBright() : this.theme().textPrimary()
      );
      vg.restore();
      if (hoveredx) {
         newHoveredRow = 999;
      }

      rowY += 28.0F;
      rowY = this.sectionHeader(vg, "Sounds", rowY, viewTop, viewBottom);

      for (int i = 0; i < this.soundWidgets.size(); i++) {
         if (i == 5) {
            rowY = this.sectionHeader(vg, "Startup Sound", rowY, viewTop, viewBottom);
         }
         SettingWidget widget = this.soundWidgets.get(i);
         widget.setBounds(this.ps.x + 14.0F, rowY, WIDTH - 28.0F);
         float wFade = this.edgeFade(rowY, rowY + widget.height(vg), viewTop, viewBottom);
         vg.save();
         vg.alpha(wFade);
         widget.render(vg, mx, my);
         vg.restore();
         rowY += widget.height(vg) + 3.0F;
      }

      if (newHoveredRow != this.hoveredRow && newHoveredRow != -1) {
         UiSounds.hover();
      }
      this.hoveredRow = newHoveredRow;
   }

   private float sectionHeader(NVGRenderer vg, String title, float rowY, float viewTop, float viewBottom) {
      float fade = this.edgeFade(rowY, rowY + 24.0F, viewTop, viewBottom);
      vg.save();
      vg.alpha(fade);
      float cy = rowY + 15.0F;
      vg.textGradient(title.toUpperCase(Locale.ROOT), this.ps.x + 14.0F, cy, 11.0F, this.theme().accentBright(), this.theme().accent());
      float lineX = this.ps.x + 14.0F + vg.textWidth(title.toUpperCase(Locale.ROOT), 11.0F) + 8.0F;
      vg.rect(lineX, cy - 0.5F, Math.max(0.0F, this.ps.x + WIDTH - 14.0F - lineX), 1.0F, 0.5F, Colors.withAlpha(this.theme().accent(), 0.3F));
      vg.restore();
      return rowY + 24.0F;
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      if (this.accentWidget.mouseClicked(mx, my, button)) {
         return true;
      }
      for (SettingWidget widget : this.soundWidgets) {
         if (widget.mouseClicked(mx, my, button)) {
            return true;
         }
      }
      if (button != 0) {
         return false;
      }
      float rowY = this.lastStartY;
      if (rowY == Float.MIN_VALUE) {
         return false;
      }
      for (Theme theme : this.themes.getThemes()) {
         if (my >= rowY && my <= rowY + 26.0F && mx >= this.ps.x + 6.0F && mx <= this.ps.x + WIDTH - 6.0F) {
            if (theme.isCustom() && mx >= this.ps.x + WIDTH - 34.0F) {
               this.themes.removeCustom(theme);
            } else {
               this.themes.select(theme);
               if (theme.isCustom()) {
                  this.accentProxy.set(Integer.valueOf(theme.accent()));
               }
            }
            return true;
         }
         rowY += 26.0F;
      }

      if (this.themes.current().isCustom()) {
         rowY += this.accentWidget.height(null) + 6.0F;
      }

      if (my >= rowY && my <= rowY + 24.0F && mx >= this.ps.x + 6.0F && mx <= this.ps.x + WIDTH - 6.0F) {
         Theme custom = this.themes.addCustom(this.themes.current().accent());
         this.themes.select(custom);
         this.accentProxy.set(Integer.valueOf(custom.accent()));
         return true;
      }
      return false;
   }

   @Override
   public void mouseDragged(float mx, float my) {
      this.accentWidget.mouseDragged(mx, my);
      for (SettingWidget widget : this.soundWidgets) {
         widget.mouseDragged(mx, my);
      }
   }

   @Override
   public void mouseReleased() {
      this.accentWidget.mouseReleased();
      for (SettingWidget widget : this.soundWidgets) {
         widget.mouseReleased();
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      return this.accentWidget.keyPressed(keyCode);
   }

   @Override
   public boolean isListening() {
      return this.accentWidget.isListening();
   }

   @Override
   public void unfocusInputs() {
      this.accentWidget.unfocus();
   }
}
