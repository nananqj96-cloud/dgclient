package dev.aether.gui.widget;

import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.KeybindSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;

public class KeybindWidget extends SettingWidget {
   public static final float HEIGHT = 23.0F;
   private final KeybindSetting setting;
   private boolean listening;

   public KeybindWidget(ThemeManager themes, KeybindSetting setting) {
      super(themes, setting);
      this.setting = setting;
   }

   @Override
   public float height(NVGRenderer vg) {
      return HEIGHT;
   }

   @Override
   public void render(NVGRenderer vg, float mx, float my) {
      Theme theme = this.theme();
      float cy = this.y + HEIGHT * 0.5F;
      vg.text(this.setting.getName(), this.x, cy, 12.0F, theme.textMuted());

      String keyLabel = this.listening ? "Press key..." : this.setting.keyName();
      float tw = vg.textWidth(keyLabel, 11.0F);
      float pillW = Math.max(36.0F, tw + 14.0F);
      float pillH = 16.0F;
      float px = this.x + this.width - pillW;
      float py = cy - pillH * 0.5F;

      int fill = this.listening ? Colors.withAlpha(theme.accent(), 0.28F) : Colors.withAlpha(0xFF090D16, 0.72F);
      vg.rect(px, py, pillW, pillH, 5.0F, fill);
      if (this.listening) {
         float pulse = (float)(0.5 + 0.5 * Math.sin((double)System.nanoTime() / 2.2E8));
         vg.rectOutline(px, py, pillW, pillH, 5.0F, 1.2F, Colors.withAlpha(theme.accentBright(), 0.45F + 0.55F * pulse));
      } else {
         vg.rectOutline(px, py, pillW, pillH, 5.0F, 1.0F, Colors.withAlpha(theme.accent(), 0.22F));
      }
      vg.text(keyLabel, px + (pillW - tw) * 0.5F, cy, 11.0F, this.listening ? theme.accentBright() : theme.textPrimary());
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      if (this.listening) {
         this.setting.set(Integer.valueOf(button));
         this.listening = false;
         UiSounds.keybindSet();
         return true;
      } else if (button == 0 && this.contains(mx, my)) {
         this.listening = true;
         UiSounds.keybindListen();
         return true;
      }
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.listening) {
         return false;
      }
      if (keyCode == 256) {
         this.setting.set(Integer.valueOf(-1));
      } else {
         this.setting.set(Integer.valueOf(keyCode));
      }
      this.listening = false;
      UiSounds.keybindSet();
      return true;
   }

   @Override
   public boolean isListening() {
      return this.listening;
   }
}
