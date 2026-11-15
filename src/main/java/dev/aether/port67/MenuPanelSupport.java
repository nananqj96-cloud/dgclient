package dev.aether.port67;

import dev.aether.module.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported faithfully from the 67port source ({@code sixseven.client.ui.MenuPanelSupport}) — the
 * 67client module row inside a dropdown. Renders the 67client row chrome (glass row, module name,
 * rotating chevron, expandable keybind area). The 67client's {@code GuiModuleEntry}/settings data
 * model is adapted to Aether's {@link Module}; the drawing logic is the real 67client source.
 */
public final class MenuPanelSupport {
   public static final float amount = 30.0F;
   public final Module module;
   public final List<Object> list = new ArrayList<>();
   public final String string;
   public final PanelLayout panelLayout;
   public final TweenValue tweenValue = new TweenValue(180.0F, 0.0F, Easing.OUT_QUART);
   public final TweenValue tweenValue2;
   public final TweenValue tweenValue3;
   public final TweenValue tweenValue4 = new TweenValue(260.0F, 0.0F, Easing.OUT_QUART);
   public float amount2;
   public float amount3;
   public float amount4;
   public float amount5;
   public boolean enabled;

   public MenuPanelSupport(Module module, String categoryName, PanelLayout panelLayout) {
      this.module = module;
      this.panelLayout = panelLayout;
      this.string = module.getName() + "@" + categoryName;
      this.tweenValue2 = new TweenValue(220.0F, module.isEnabled() ? 1.0F : 0.0F, Easing.OUT_QUART);
      this.tweenValue3 = new TweenValue(270.0F, this.test() ? 1.0F : 0.0F, Easing.OUT_QUART);
   }

   public boolean test() {
      return this.panelLayout.test(this.string);
   }

   public boolean test2() {
      return this.tweenValue3.getValue() > 0.999F;
   }

   public void apply(boolean flag) {
      this.panelLayout.apply(this.string, flag);
      this.tweenValue3.animateTo(flag ? 1.0F : 0.0F);
      if (!flag) {
         this.update3();
      }
   }

   public void update() {
      float amount32 = 44.0F;
      this.amount5 = amount32 + 6.0F;
      this.tweenValue3.snapTo(this.tweenValue3.getTarget());
   }

   public float computeFloat() {
      this.update();
      return 30.0F + this.amount5 * this.tweenValue3.getValue();
   }

   public void render(SixSevenFontManager fontManager, float floatValue, float floatValue2, float floatValue3) {
      ColorPalette instance = ColorPalette.getInstance();
      this.tweenValue.animateTo(
         test(floatValue, floatValue2, this.amount2, this.amount3, this.amount4, 30.0F) ? 1.0F : 0.0F);
      this.tweenValue2.animateTo(this.module.isEnabled() ? 1.0F : 0.0F);
      float value = this.tweenValue2.getValue();
      float value2 = this.tweenValue.getValue();
      float value3 = this.tweenValue3.getValue();
      fontManager.update2();
      fontManager.apply3(floatValue3);
      fontManager.render(this.amount2, this.amount3, this.amount4, 30.0F, 8.0F, instance.computeInt(value * 0.14F));
      fontManager.render(this.amount2, this.amount3, this.amount4, 30.0F, 8.0F, instance.computeInt(value2 * 0.1F));
      fontManager.render(
         this.amount2 + 1.0F,
         this.amount3 + 1.0F,
         this.amount4 - 2.0F,
         28.0F,
         7.0F,
         1.0F,
         instance.computeInt(value3 * 0.45F));
      fontManager.render(
         this.amount2 + 1.0F,
         this.amount3 + 1.0F,
         this.amount4 - 2.0F,
         28.0F,
         7.0F,
         instance.computeInt(this.tweenValue4.getValue() * 0.16F));
      fontManager.render(
         this.amount2 + 3.0F,
         this.amount3 + 15.0F - 5.0F * value,
         2.0F,
         Math.max(0.01F, 10.0F * value),
         1.0F,
         instance.computeInt(0.8F * value));
      fontManager.computeFloat(
         this.module.getName(),
         this.amount2 + 10.0F + 2.0F * value2,
         this.amount3 + 15.0F,
         14.0F,
         ArgbColors.lerp(instance.computeInt16(), instance.computeInt(), value),
         this.amount4 - 42.0F);
      fontManager.update2();
      fontManager.apply(this.amount2 + this.amount4 - 14.0F, this.amount3 + 15.0F);
      fontManager.apply2(value3 * (float) Math.PI / 2.0F);
      fontManager.render(
         0.0F,
         0.0F,
         5.0F,
         1.4F,
         ArgbColors.lerp(instance.computeInt18(), instance.computeInt(), value2),
         false);
      fontManager.update3();
      if (value3 > 0.005F) {
         fontManager.update2();
         fontManager.render(this.amount2, this.amount3 + 30.0F, this.amount4, this.amount5 * value3);
         fontManager.apply3(value3);
         fontManager.apply(this.amount2, this.amount3 + 30.0F);
         fontManager.render(0.0F, 0.0F, this.amount4, this.amount5, 8.0F, instance.computeInt4());
         fontManager.render(
            0.5F,
            0.5F,
            this.amount4 - 1.0F,
            this.amount5 - 1.0F,
            8.0F,
            1.0F,
            instance.computeInt14());
         fontManager.computeFloat("Keybind", 12.0F, 20.0F, 12.0F, instance.computeInt17());
         String text = this.enabled ? "Press key…" : keyName(this.module);
         float floatValue4 =
            Math.min(this.amount4 - 82.0F, Math.max(54.0F, fontManager.computeFloat(text, 12.0F) + 20.0F));
         fontManager.render(
            this.amount4 - 12.0F - floatValue4,
            7.0F,
            floatValue4,
            26.0F,
            5.0F,
            instance.computeInt10());
         fontManager.render(
            this.amount4 - 12.0F - floatValue4,
            7.0F,
            floatValue4,
            26.0F,
            5.0F,
            1.0F,
            this.enabled ? instance.computeInt() : instance.computeInt14());
         fontManager.computeFloat(
            text, this.amount4 - 20.0F, 20.0F, 12.0F, instance.computeInt(), floatValue4 - 16.0F, 4);
         fontManager.render2(12.0F, 40.0F, this.amount4 - 12.0F, 40.0F, 1.0F, instance.computeInt14());
         fontManager.update3();
      }
      fontManager.update3();
   }

   private static String keyName(Module module) {
      int key = module.getKeybind().get();
      if (key <= 0) {
         return "Unbound";
      }
      return "Key " + key;
   }

   public boolean test(float amount6, float amount22, int intValue) {
      if (test(amount6, amount22, this.amount2, this.amount3, this.amount4, 30.0F)) {
         this.update3();
         if (intValue == 1 || intValue == 0 && amount6 > this.amount2 + this.amount4 - 30.0F) {
            this.apply(!this.test());
         } else if (intValue == 0) {
            this.module.toggle();
         } else if (intValue == 2) {
            this.apply(true);
            this.enabled = true;
         }
         return true;
      } else if (this.test() && !(this.tweenValue3.getValue() < 0.98F)) {
         float floatValue = amount6 - this.amount2;
         float floatValue2 = amount22 - this.amount3 - 30.0F;
         if (test(floatValue, floatValue2, 0.0F, 0.0F, this.amount4, 40.0F)) {
            this.update3();
            this.enabled = true;
            return true;
         }
         return false;
      } else {
         return false;
      }
   }

   public void apply(float floatValue, float floatValue2) {
   }

   public void update2() {
   }

   public void update3() {
      this.enabled = false;
   }

   public boolean test(int intValue, boolean flag) {
      if (this.enabled) {
         if (intValue != 256) {
         }
         this.enabled = false;
         return true;
      }
      return false;
   }

   public boolean test(int intValue) {
      return false;
   }

   public static boolean test(
      float floatValue, float floatValue2, float floatValue3, float floatValue4, float floatValue5, float floatValue6) {
      return floatValue >= floatValue3
         && floatValue <= floatValue3 + floatValue5
         && floatValue2 >= floatValue4
         && floatValue2 <= floatValue4 + floatValue6;
   }
}
