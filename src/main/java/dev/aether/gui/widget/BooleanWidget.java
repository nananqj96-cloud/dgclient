package dev.aether.gui.widget;

import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;

public class BooleanWidget extends SettingWidget {
   public static final float HEIGHT = 23.0F;
   private final BooleanSetting setting;
   private final Animation check = new Animation(140.0F, 0.0F);

   public BooleanWidget(ThemeManager themes, BooleanSetting setting) {
      super(themes, setting);
      this.setting = setting;
      this.check.snapTo(setting.get() ? 1.0F : 0.0F);
   }

   @Override
   public float height(NVGRenderer vg) {
      return HEIGHT;
   }

   @Override
   public void render(NVGRenderer vg, float mx, float my) {
      Theme theme = this.theme();
      float cy = this.y + HEIGHT * 0.5F;
      boolean hovered = this.contains(mx, my);
      vg.text(this.setting.getName(), this.x, cy, 12.0F, hovered ? theme.textPrimary() : theme.textMuted());

      this.check.setTarget(this.setting.get() ? 1.0F : 0.0F);
      float t = this.check.value();
      float pillW = 28.0F;
      float pillH = 14.0F;
      float px = this.x + this.width - pillW;
      float py = cy - pillH * 0.5F;

      int trackCol = Colors.lerp(Colors.withAlpha(theme.statusDisabled(), 0.72F), Colors.withAlpha(theme.accent(), 0.88F), t);
      vg.rect(px, py, pillW, pillH, 7.0F, trackCol);
      vg.rectOutline(px, py, pillW, pillH, 7.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.14F + 0.48F * t));

      float knobX = px + 7.0F + t * (pillW - 14.0F);
      vg.circle(knobX, cy, 4.6F, Colors.lerp(theme.textMuted(), 0xFFFFFFFF, t));
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         this.setting.toggle();
         UiSounds.checkbox(this.setting.get());
         return true;
      }
      return false;
   }
}
