package dev.aether.gui.widget;

import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.Setting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;

public abstract class SettingWidget {
   protected final ThemeManager themes;
   private final Setting<?> boundSetting;
   protected float x;
   protected float y;
   protected float width;

   protected SettingWidget(ThemeManager themes, Setting<?> boundSetting) {
      this.themes = themes;
      this.boundSetting = boundSetting;
   }

   public Setting<?> boundSetting() {
      return this.boundSetting;
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float width() {
      return this.width;
   }

   public boolean isVisible() {
      return this.boundSetting == null || this.boundSetting.isVisible();
   }

   protected Theme theme() {
      return this.themes.current();
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setBounds(float x, float y, float width) {
      this.x = x;
      this.y = y;
      this.width = width;
   }

   public boolean contains(float mx, float my) {
      return mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + this.height(null);
   }

   public abstract float height(NVGRenderer nVGRenderer);

   public abstract void render(NVGRenderer nVGRenderer, float f, float g);

   public boolean mouseClicked(float mx, float my, int button) {
      return false;
   }

   public void mouseDragged(float mx, float my) {
   }

   public void mouseReleased() {
   }

   public boolean keyPressed(int keyCode) {
      return false;
   }

   public boolean charTyped(int codepoint) {
      return false;
   }

   public boolean isListening() {
      return false;
   }

   public boolean hasPendingKeybind() {
      return false;
   }

   public boolean handlePendingKeybindMouse(int button) {
      return false;
   }

   public void unfocus() {
   }
}
