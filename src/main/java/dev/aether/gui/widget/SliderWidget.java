package dev.aether.gui.widget;

import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.SliderSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;

public class SliderWidget extends SettingWidget {
   public static final float HEIGHT = 28.0F;
   private final SliderSetting setting;
   private final Animation fill = new Animation(85.0F, 0.0F);
   private boolean dragging;

   public SliderWidget(ThemeManager themes, SliderSetting setting) {
      super(themes, setting);
      this.setting = setting;
      this.fill.snapTo((float)setting.getNormalized());
   }

   @Override
   public float height(NVGRenderer vg) {
      return HEIGHT;
   }

   @Override
   public void render(NVGRenderer vg, float mx, float my) {
      Theme theme = this.theme();
      float labelY = this.y + 8.0F;
      vg.text(this.setting.getName(), this.x, labelY, 12.0F, theme.textMuted());

      String valStr = this.setting.formatValue();
      float badgeW = vg.textWidth(valStr, 11.0F) + 10.0F;
      float badgeX = this.x + this.width - badgeW;
      vg.rect(badgeX, labelY - 7.0F, badgeW, 14.0F, 5.0F, Colors.withAlpha(theme.accent(), 0.14F));
      vg.text(valStr, badgeX + 5.0F, labelY, 11.0F, theme.accentBright());

      float barY = this.y + HEIGHT - 8.5F;
      this.fill.setTarget((float)this.setting.getNormalized());
      float norm = Math.clamp(this.fill.value(), 0.0F, 1.0F);

      vg.rect(this.x, barY, this.width, 4.0F, 2.0F, Colors.withAlpha(theme.statusDisabled(), 0.72F));
      float fillW = Math.max(4.0F, norm * this.width);
      vg.rectGradient(this.x, barY, fillW, 4.0F, 2.0F, theme.accent(), theme.accentBright(), false);

      float knobX = this.x + norm * this.width;
      float knobY = barY + 2.0F;
      if (this.dragging) {
         vg.circleGlow(knobX, knobY, 4.5F, 5.5F, theme.accentHover());
      }
      vg.circle(knobX, knobY, 4.8F, theme.accentBright());
      vg.circle(knobX, knobY, 2.2F, 0xFFFFFFFF);
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my) && my >= this.y + 9.0F) {
         this.dragging = true;
         this.applyMouse(mx);
         return true;
      }
      return false;
   }

   @Override
   public void mouseDragged(float mx, float my) {
      if (this.dragging) {
         this.applyMouse(mx);
      }
   }

   @Override
   public void mouseReleased() {
      this.dragging = false;
   }

   private void applyMouse(float mx) {
      double prev = this.setting.getNormalized();
      this.setting.setNormalized((double)((mx - this.x) / Math.max(1.0F, this.width)));
      if (this.setting.getNormalized() != prev) {
         UiSounds.sliderTick((float)this.setting.getNormalized());
      }
   }
}
