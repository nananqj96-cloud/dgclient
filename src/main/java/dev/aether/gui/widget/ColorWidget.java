package dev.aether.gui.widget;

import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.ColorSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;

public class ColorWidget extends SettingWidget {
   private static final float ROW = 22.0F;
   private static final float SV_H = 74.0F;
   private static final float HUE_H = 10.0F;
   private static final float BOTTOM_H = 22.0F;
   private static final float GAP = 6.0F;
   private final ColorSetting setting;
   private final Animation expand = new Animation(170.0F, 0.0F);
   private boolean expanded;
   private float hue;
   private float sat;
   private float val;
   private boolean draggingSv;
   private boolean draggingHue;
   private boolean hexFocused;
   private final StringBuilder hexBuffer = new StringBuilder();

   public ColorWidget(ThemeManager var1, ColorSetting var2) {
      super(var1, var2);
      this.setting = var2;
      this.syncFromSetting();
   }

   private void syncFromSetting() {
      float[] var1 = Colors.rgbToHsv(this.setting.get());
      this.hue = var1[0];
      this.sat = var1[1];
      this.val = var1[2];
   }

   public void setExpanded(boolean var1) {
      if (var1 && !this.expanded) {
         this.syncFromSetting();
      }

      this.expanded = var1;
      this.expand.setTarget(var1 ? 1.0F : 0.0F);
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   private void apply() {
      this.setting.set(Integer.valueOf(Colors.hsvToRgb(this.hue, this.sat, this.val)));
   }

   @Override
   public float height(NVGRenderer var1) {
      return 22.0F + this.expand.value() * 124.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3) {
      Theme var4 = this.theme();
      float var5 = this.y + 11.0F;
      var1.text(this.setting.getName(), this.x, var5, 12.5F, var4.textMuted());
      float var6 = 26.0F;
      float var7 = 14.0F;
      var1.rect(this.x + this.width - var6, var5 - var7 / 2.0F, var6, var7, 3.0F, this.setting.get() | 0xFF000000);
      var1.rectOutline(this.x + this.width - var6, var5 - var7 / 2.0F, var6, var7, 3.0F, 1.0F, Colors.withAlpha(-1, 0.35F));
      this.expand.setTarget(this.expanded ? 1.0F : 0.0F);
      float var8 = this.expand.value();
      if (!(var8 < 0.01F)) {
         var1.save();
         var1.scissor(this.x - 4.0F, this.y + 22.0F, this.width + 8.0F, var8 * 124.0F);
         var1.alpha(var8);
         float var9 = this.svTop();
         int var10 = Colors.hsvToRgb(this.hue, 1.0F, 1.0F);
         var1.rect(this.x, var9, this.width, 74.0F, 2.0F, var10);
         var1.rectGradient(this.x, var9, this.width, 74.0F, 6.0F, -1, Colors.withAlpha(-1, 0), false);
         var1.rectGradient(this.x, var9, this.width, 74.0F, 6.0F, Colors.withAlpha(-16777216, 0), -16777216, true);
         float var11 = this.x + this.sat * this.width;
         float var12 = var9 + (1.0F - this.val) * 74.0F;
         var1.circle(var11, var12, 6.0F, -1);
         var1.circle(var11, var12, 4.2F, Colors.hsvToRgb(this.hue, this.sat, this.val));
         float var13 = this.hueTop();
         float var14 = this.width / 6.0F;

         for (int var15 = 0; var15 < 6; var15++) {
            int var16 = Colors.hsvToRgb((float)(var15 * 60), 1.0F, 1.0F);
            int var17 = Colors.hsvToRgb((var15 + 1) * 60 % 360 == 0 ? 359.9F : (float)((var15 + 1) * 60), 1.0F, 1.0F);
            var1.rectGradient(this.x + (float)var15 * var14, var13, var14 + 0.5F, 10.0F, 0.0F, var16, var17, false);
         }

         var1.rectOutline(this.x, var13, this.width, 10.0F, 2.0F, 1.5F, Colors.withAlpha(-15988208, 0.9F));
         float var21 = this.x + this.hue / 360.0F * this.width;
         var1.circle(var21, var13 + 5.0F, 6.0F, -1);
         var1.circle(var21, var13 + 5.0F, 4.2F, var10);
         float var22 = this.bottomTop();
         var1.rect(this.x, var22, 30.0F, 18.0F, 2.0F, this.setting.get() | 0xFF000000);
         var1.rectOutline(this.x, var22, 30.0F, 18.0F, 2.0F, 1.0F, Colors.withAlpha(-1, 0.25F));
         float var23 = this.x + 38.0F;
         float var18 = this.width - 38.0F;
         int var19 = this.hexFocused ? Colors.withAlpha(var4.accent(), 0.18F) : Colors.withAlpha(-16777216, 0.45F);
         var1.rect(var23, var22, var18, 18.0F, 2.0F, var19);
         if (this.hexFocused) {
            var1.rectOutline(var23, var22, var18, 18.0F, 2.0F, 1.2F, var4.accentBright());
         }

         String var20 = this.hexFocused
            ? "#" + this.hexBuffer + (System.nanoTime() / 400000000L % 2L == 0L ? "_" : "")
            : this.setting.hex();
         var1.text(var20, var23 + 8.0F, var22 + 9.0F, 12.0F, this.hexFocused ? var4.textPrimary() : var4.textMuted());
         var1.restore();
      }
   }

   private float svTop() {
      return this.y + 22.0F + 2.0F;
   }

   private float hueTop() {
      return this.svTop() + 74.0F + 6.0F;
   }

   private float bottomTop() {
      return this.hueTop() + 10.0F + 6.0F;
   }

   @Override
   public boolean mouseClicked(float var1, float var2, int var3) {
      if (var3 != 0) {
         return false;
      } else if (var2 >= this.y && var2 <= this.y + 22.0F && var1 >= this.x && var1 <= this.x + this.width) {
         this.expanded = !this.expanded;
         this.expand.setTarget(this.expanded ? 1.0F : 0.0F);
         if (this.expanded) {
            this.syncFromSetting();
         }

         this.hexFocused = false;
         UiSounds.select();
         return true;
      } else if (!this.expanded) {
         return false;
      } else if (inRect(var1, var2, this.x, this.svTop(), this.width, 74.0F)) {
         this.draggingSv = true;
         this.applySv(var1, var2);
         return true;
      } else if (inRect(var1, var2, this.x, this.hueTop() - 3.0F, this.width, 16.0F)) {
         this.draggingHue = true;
         this.applyHue(var1);
         return true;
      } else if (inRect(var1, var2, this.x + 38.0F, this.bottomTop(), this.width - 38.0F, 18.0F)) {
         this.hexFocused = true;
         this.hexBuffer.setLength(0);
         UiSounds.select();
         return true;
      } else {
         if (this.hexFocused) {
            this.commitHex();
         }

         return false;
      }
   }

   private static boolean inRect(float var0, float var1, float var2, float var3, float var4, float var5) {
      return var0 >= var2 && var0 <= var2 + var4 && var1 >= var3 && var1 <= var3 + var5;
   }

   private void applySv(float var1, float var2) {
      this.sat = Math.clamp((var1 - this.x) / this.width, 0.0F, 1.0F);
      this.val = 1.0F - Math.clamp((var2 - this.svTop()) / 74.0F, 0.0F, 1.0F);
      this.apply();
   }

   private void applyHue(float var1) {
      this.hue = Math.clamp((var1 - this.x) / this.width, 0.0F, 1.0F) * 359.9F;
      this.apply();
   }

   @Override
   public void mouseDragged(float var1, float var2) {
      if (this.draggingSv) {
         this.applySv(var1, var2);
      }

      if (this.draggingHue) {
         this.applyHue(var1);
      }
   }

   @Override
   public void mouseReleased() {
      this.draggingSv = false;
      this.draggingHue = false;
   }

   @Override
   public boolean keyPressed(int var1) {
      if (!this.hexFocused) {
         return false;
      } else if (var1 == 256) {
         this.hexFocused = false;
         return true;
      } else if (var1 == 257 || var1 == 335) {
         this.commitHex();
         return true;
      } else if (var1 == 259) {
         if (!this.hexBuffer.isEmpty()) {
            this.hexBuffer.deleteCharAt(this.hexBuffer.length() - 1);
         }

         return true;
      } else {
         char var2 = hexChar(var1);
         if (var2 != 0 && this.hexBuffer.length() < 6) {
            this.hexBuffer.append(var2);
            if (this.hexBuffer.length() == 6) {
               this.commitHex();
            }
         }

         return true;
      }
   }

   private static char hexChar(int var0) {
      if (var0 >= 48 && var0 <= 57) {
         return (char)(48 + var0 - 48);
      } else if (var0 >= 320 && var0 <= 329) {
         return (char)(48 + var0 - 320);
      } else {
         return var0 >= 65 && var0 <= 70 ? (char)(65 + var0 - 65) : '\u0000';
      }
   }

   private void commitHex() {
      this.hexFocused = false;
      if (this.hexBuffer.length() == 6) {
         try {
            this.setting.set(Integer.valueOf(0xFF000000 | Integer.parseInt(this.hexBuffer.toString(), 16)));
            this.syncFromSetting();
            UiSounds.keybindSet();
         } catch (NumberFormatException var2) {
         }
      }

      this.hexBuffer.setLength(0);
   }

   @Override
   public boolean isListening() {
      return this.hexFocused;
   }

   @Override
   public void unfocus() {
      if (this.hexFocused) {
         this.commitHex();
      }
   }
}
