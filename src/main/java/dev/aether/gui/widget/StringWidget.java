package dev.aether.gui.widget;

import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.StringSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;

public class StringWidget extends SettingWidget {
   private static final float HEIGHT = 34.0F;
   private static final float BOX_H = 18.0F;
   private static final float FONT = 12.0F;
   private final StringSetting setting;
   private boolean focused;

   public StringWidget(ThemeManager var1, StringSetting var2) {
      super(var1, var2);
      this.setting = var2;
   }

   @Override
   public float height(NVGRenderer var1) {
      return 34.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3) {
      Theme var4 = this.theme();
      var1.text(this.setting.getName(), this.x, this.y + 8.0F, 12.5F, var4.textMuted());
      float var5 = this.x;
      float var6 = this.y + 14.0F;
      float var7 = this.width;
      int var8 = Colors.withAlpha(-16777216, 0.45F);
      var1.rect(var5, var6, var7, 18.0F, 2.0F, var8);
      var1.rect(var5, var6 + 4.0F, 2.0F, 10.0F, 1.0F, this.focused ? var4.accentBright() : var4.textDisabled());
      var1.rectOutline(var5, var6, var7, 18.0F, 2.0F, 1.1F, Colors.withAlpha(this.focused ? var4.accentBright() : var4.accent(), this.focused ? 0.9F : 0.25F));
      float var9 = var5 + 8.0F;
      float var10 = var6 + 9.0F;
      String var11 = this.setting.get();
      if (var11.isEmpty() && !this.focused) {
         var1.text(this.setting.getPlaceholder(), var9, var10, 12.0F, var4.textDisabled());
      } else {
         float var12 = var1.text(var11, var9, var10, 12.0F, var4.textPrimary());
         if (this.focused && System.nanoTime() / 400000000L % 2L == 0L) {
            var1.rect(var9 + var12 + 1.5F, var10 - 6.0F, 1.4F, 12.0F, 0.7F, var4.accentBright());
         }
      }
   }

   @Override
   public boolean mouseClicked(float var1, float var2, int var3) {
      if (var3 == 0 && this.contains(var1, var2)) {
         this.focused = true;
         UiSounds.select();
         return true;
      } else {
         this.focused = false;
         return false;
      }
   }

   @Override
   public boolean keyPressed(int var1) {
      if (!this.focused) {
         return false;
      } else {
         switch (var1) {
            case 256:
            case 257:
            case 335:
               this.focused = false;
               break;
            case 259:
               String var2 = this.setting.get();
               if (!var2.isEmpty()) {
                  this.setting.set(var2.substring(0, var2.length() - 1));
               }
         }

         return true;
      }
   }

   @Override
   public boolean charTyped(int var1) {
      if (!this.focused) {
         return false;
      } else if (this.setting.get().length() >= this.setting.getMaxLength()) {
         return true;
      } else if (Character.isValidCodePoint(var1) && !Character.isISOControl(var1)) {
         this.setting.set(this.setting.get() + new String(Character.toChars(var1)));
         return true;
      } else {
         return true;
      }
   }

   @Override
   public boolean isListening() {
      return this.focused;
   }
}
