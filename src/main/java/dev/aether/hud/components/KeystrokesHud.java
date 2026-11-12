package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.render.anim.Animation;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.CpsTracker;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class KeystrokesHud extends HudComponent {
   private static final float KEY = 26.0F;
   private static final float GAP = 3.0F;
   private static final float FONT = 12.5F;
   private final ThemeManager themes;
   private final Map<String, Animation> press = new HashMap<>();

   public KeystrokesHud(ThemeManager var1, BooleanSupplier var2) {
      super("keystrokes", 0.03F, 0.72F, var2);
      this.themes = var1;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 84.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      float var2 = 55.0F;
      var2 += 29.0F;
      float var4;
      return var4 = var2 + 18.6F;
   }

   private float pressT(String var1, boolean var2) {
      Animation var3 = this.press.computeIfAbsent(var1, var0 -> new Animation(110.0F, 0.0F));
      var3.setTarget(var2 ? 1.0F : 0.0F);
      return var3.value();
   }

   private void key(NVGRenderer var1, Theme var2, String var3, String var4, boolean var5, float var6, float var7, float var8, float var9) {
      float var10 = this.pressT(var3, var5);
      int var11 = Colors.lerp(Colors.withAlpha(-15462118, 0.78F), Colors.withAlpha(var2.accent(), 0.85F), var10);
      var1.rect(var6, var7, var8, var9, 2.0F, var11);
      var1.rectOutline(var6, var7, var8, var9, 2.0F, 1.0F, Colors.withAlpha(var2.accentBright(), 0.16F + 0.38F * var10));
      int var12 = Colors.lerp(var2.textMuted(), -1, var10);
      var1.text(var4, var6 + (var8 - var1.textWidth(var4, 12.5F)) / 2.0F, var7 + var9 / 2.0F, 12.5F, var12);
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      Theme var6 = this.themes.current();
      Options var7 = Minecraft.getInstance().options;
      this.key(var1, var6, "w", "W", isDown(var7.keyUp), var2 + 26.0F + 3.0F, var3, 26.0F, 26.0F);
      float var13;
      this.key(var1, var6, "a", "A", isDown(var7.keyLeft), var2, var13 = var3 + 29.0F, 26.0F, 26.0F);
      this.key(var1, var6, "s", "S", isDown(var7.keyDown), var2 + 26.0F + 3.0F, var13, 26.0F, 26.0F);
      this.key(var1, var6, "d", "D", isDown(var7.keyRight), var2 + 58.0F, var13, 26.0F, 26.0F);
      float var9 = (var4 - 3.0F) / 2.0F;
      float var8;
      this.key(var1, var6, "lmb", "LMB " + CpsTracker.get(0), isDown(var7.keyAttack), var2, var8 = var13 + 29.0F, var9, 26.0F);
      this.key(var1, var6, "rmb", "RMB " + CpsTracker.get(1), isDown(var7.keyUse), var2 + var9 + 3.0F, var8, var9, 26.0F);
      float var10 = this.pressT("space", isDown(var7.keyJump));
      float var11 = 15.6F;
      int var12 = Colors.lerp(Colors.withAlpha(-15462118, 0.78F), Colors.withAlpha(var6.accent(), 0.85F), var10);
      float var15;
      var1.rect(var2, var15 = var8 + 29.0F, var4, var11, 2.0F, var12);
      var1.rectOutline(var2, var15, var4, var11, 2.0F, 1.0F, Colors.withAlpha(var6.accentBright(), 0.16F + 0.38F * var10));
      var1.rect(var2 + var4 * 0.25F, var15 + var11 / 2.0F - 1.25F, var4 * 0.5F, 2.5F, 1.25F, Colors.lerp(var6.textMuted(), -1, var10));
   }

   private static boolean isDown(KeyMapping var0) {
      return var0.isDown();
   }
}
