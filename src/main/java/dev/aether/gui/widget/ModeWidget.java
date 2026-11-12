package dev.aether.gui.widget;

import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.ModeSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.List;

public class ModeWidget extends SettingWidget {
   private static final float CHIP_H = 16.0F;
   private static final float CHIP_ROW_STEP = 19.0F;
   private final ModeSetting setting;
   private float[] boundsFlat;
   private int boundCount;
   private int lines = 1;

   public ModeWidget(ThemeManager themes, ModeSetting setting) {
      super(themes, setting);
      this.setting = setting;
      this.boundsFlat = new float[Math.max(4, setting.getModes().size()) * 3];
   }

   @Override
   public float height(NVGRenderer vg) {
      if (vg != null) {
         float cx = 0.0F;
         int l = 1;
         float maxW = Math.max(120.0F, this.width);
         for (String mode : this.setting.getModes()) {
            float chipW = vg.textWidth(mode, 11.0F) + 14.0F;
            if (cx + chipW > maxW && cx > 0.0F) {
               cx = 0.0F;
               l++;
            }
            cx += chipW + 5.0F;
         }
         this.lines = l;
      }
      return 18.0F + (float)this.lines * CHIP_ROW_STEP;
   }

   @Override
   public void render(NVGRenderer vg, float mx, float my) {
      Theme theme = this.theme();
      vg.text(this.setting.getName(), this.x, this.y + 8.0F, 12.0F, theme.textMuted());

      List<String> modes = this.setting.getModes();
      if (this.boundsFlat.length < modes.size() * 3) {
         this.boundsFlat = new float[modes.size() * 3];
      }
      this.boundCount = 0;

      float cx = this.x;
      float cy = this.y + 18.0F + 8.0F;
      this.lines = 1;

      for (String mode : modes) {
         float textW = vg.textWidth(mode, 11.0F);
         float chipW = textW + 14.0F;
         if (cx + chipW > this.x + this.width && cx > this.x) {
            cx = this.x;
            cy += CHIP_ROW_STEP;
            this.lines++;
         }

         boolean selected = this.setting.is(mode);
         boolean hovered = mx >= cx && mx <= cx + chipW && my >= cy - CHIP_H * 0.5F && my <= cy + CHIP_H * 0.5F;
         int fill = selected
            ? Colors.withAlpha(theme.accent(), 0.24F)
            : Colors.withAlpha(theme.statusDisabled(), hovered ? 0.55F : 0.30F);
         vg.rect(cx, cy - CHIP_H * 0.5F, chipW, CHIP_H, 5.0F, fill);
         vg.rectOutline(
            cx,
            cy - CHIP_H * 0.5F,
            chipW,
            CHIP_H,
            5.0F,
            1.0F,
            Colors.withAlpha(selected ? theme.accentBright() : theme.accent(), selected ? 0.65F : 0.14F)
         );
         vg.text(mode, cx + 7.0F, cy, 11.0F, selected ? theme.accentBright() : (hovered ? theme.textPrimary() : theme.textMuted()));

         int base = this.boundCount * 3;
         this.boundsFlat[base] = cx;
         this.boundsFlat[base + 1] = cy - CHIP_H * 0.5F;
         this.boundsFlat[base + 2] = chipW;
         this.boundCount++;
         cx += chipW + 5.0F;
      }
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      if (button != 0) {
         return false;
      }
      List<String> modes = this.setting.getModes();
      for (int i = 0; i < this.boundCount && i < modes.size(); i++) {
         int base = i * 3;
         float bx = this.boundsFlat[base];
         float by = this.boundsFlat[base + 1];
         float bw = this.boundsFlat[base + 2];
         if (mx >= bx && mx <= bx + bw && my >= by && my <= by + CHIP_H) {
            this.setting.set(modes.get(i));
            UiSounds.select();
            return true;
         }
      }
      return false;
   }
}
