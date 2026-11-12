package dev.aether.gui.widget;

import dev.aether.gui.ClickGuiScreen;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.IconListSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import net.minecraft.client.Minecraft;

public class IconListWidget extends SettingWidget {
   private static final float ROW = 26.0F;
   private final IconListSetting setting;

   public IconListWidget(ThemeManager var1, IconListSetting var2) {
      super(var1, var2);
      this.setting = var2;
   }

   @Override
   public float height(NVGRenderer var1) {
      return 26.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3) {
      Theme var4 = this.theme();
      float var5 = this.y + 13.0F;
      var1.text(this.setting.getName(), this.x, var5, 12.5F, var4.textMuted());
      float var6 = 88.0F;
      float var7 = 18.0F;
      float var8 = this.x + this.width - var6;
      float var9 = var5 - var7 / 2.0F;
      boolean var10 = var2 >= var8 && var2 <= var8 + var6 && var3 >= var9 && var3 <= var9 + var7;
      var1.rectGradient(var8, var9, var6, var7, 4.0F, var4.headerTop(), var4.headerBottom(), true);
      var1.rect(var8, var9 + 4.0F, 2.0F, var7 - 8.0F, 1.0F, var10 ? var4.accentBright() : var4.accent());
      var1.rectOutline(var8, var9, var6, var7, 2.0F, 1.1F, Colors.withAlpha(var10 ? var4.accentBright() : var4.accent(), var10 ? 0.9F : 0.32F));
      String var11 = "Pick";
      float var12 = var1.textWidth(var11, 12.0F);
      var1.textGradient(var11, var8 + (var6 - var12) / 2.0F, var9 + var7 / 2.0F, 12.0F, var4.accentBright(), var4.accent());
      String var13 = this.setting.enabledCount() + "/" + this.setting.size();
      var1.text(var13, var8 - 8.0F - var1.textWidth(var13, 11.5F), var5, 11.5F, var4.textDisabled());
   }

   @Override
   public boolean mouseClicked(float var1, float var2, int var3) {
      if (var3 == 0 && this.contains(var1, var2)) {
         if (Minecraft.getInstance().screen instanceof ClickGuiScreen var5) {
            var5.openIconPicker(this.setting);
            UiSounds.select();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
