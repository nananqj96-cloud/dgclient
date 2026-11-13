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

   public void startListening() {
      this.listening = true;
      UiSounds.keybindListen();
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

      String keyLabel = this.listening ? "Press key..." : this.setting.keyName();
      float tw = vg.textWidth(keyLabel, 11.0F);
      float pillW = Math.max(38.0F, tw + 14.0F);
      float pillH = 16.0F;
      float px = this.x + this.width - pillW;
      float py = cy - pillH * 0.5F;

      int fill = this.listening
         ? Colors.withAlpha(theme.accent(), 0.28F)
         : Colors.withAlpha(0xFF090D16, hovered ? 0.88F : 0.72F);
      vg.rect(px, py, pillW, pillH, 5.0F, fill);
      if (this.listening) {
         float pulse = (float)(0.5 + 0.5 * Math.sin((double)System.nanoTime() / 2.2E8));
         vg.rectOutline(px, py, pillW, pillH, 5.0F, 1.2F, Colors.withAlpha(theme.accentBright(), 0.45F + 0.55F * pulse));
      } else {
         vg.rectOutline(px, py, pillW, pillH, 5.0F, 1.0F, Colors.withAlpha(hovered ? theme.accentBright() : theme.accent(), hovered ? 0.52F : 0.22F));
      }
      vg.text(keyLabel, px + (pillW - tw) * 0.5F, cy, 11.0F, this.listening ? theme.accentBright() : theme.textPrimary());
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      if (this.listening) {
         return this.handlePendingKeybindMouse(button);
      }
      if (!this.contains(mx, my)) {
         return false;
      }
      if (button == 0) {
         this.listening = true;
         UiSounds.keybindListen();
         return true;
      }
      if (button == 1 || button == 2) {
         this.setting.set(Integer.valueOf(KeybindSetting.NONE));
         UiSounds.keybindSet();
         return true;
      }
      return false;
   }

   @Override
   public boolean handlePendingKeybindMouse(int button) {
      if (!this.listening) {
         return false;
      }
      this.setting.set(Integer.valueOf(button));
      this.listening = false;
      UiSounds.keybindSet();
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode) {
      if (!this.listening) {
         return false;
      }
      if (keyCode == 256 || keyCode == 259 || keyCode == 261) {
         this.setting.set(Integer.valueOf(KeybindSetting.NONE));
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

   @Override
   public boolean hasPendingKeybind() {
      return this.listening;
   }

   @Override
   public void unfocus() {
      this.listening = false;
   }
}
