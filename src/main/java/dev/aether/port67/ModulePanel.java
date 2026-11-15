package dev.aether.port67;

import dev.aether.module.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported faithfully from the 67port source ({@code sixseven.client.ui.ModulePanel}) — the 67client
 * module-list dropdown. Renders one 67client dropdown panel per category, with an animated expandable
 * body of module rows. The 67client's {@code CollectionData3}/{@code GuiModuleEntry} data model is
 * adapted to Aether's {@link Module} (so toggling here syncs with the main Aether GUI). The drawing
 * logic is the real 67client source.
 */
public final class ModulePanel extends MenuPanel {
   public final String categoryName;
   public final List<MenuPanelSupport> list = new ArrayList<>();
   public String string = "";
   public boolean enabled;
   public float amount = 1.0F;
   public float amount2 = 1.0F;
   public float amount3;

   public ModulePanel(String categoryName, PanelLayout panelLayout, List<Module> modules) {
      super(panelLayout.panel(categoryName));
      this.categoryName = categoryName;
      for (Module module : modules) {
         this.list.add(new MenuPanelSupport(module, categoryName, panelLayout));
      }
      this.enabled = this.panelLayoutSupport.enabled;
      if (this.enabled) {
         this.apply(true);
      }
   }

   @Override
   public void apply(boolean flag) {
      super.apply(flag);
      this.enabled = this.panelLayoutSupport.enabled;
   }

   public void apply(String text) {
      this.string = text.toLowerCase(Locale.ROOT).trim();
   }

   public void apply(float amount4) {
      this.amount = amount4;
      this.amount2 = 0.975F + 0.025F * amount4;
      this.amount3 = 24.0F * (1.0F - amount4);
   }

   public float computeFloat(float floatValue) {
      return (floatValue - this.computeFloat2() - 105.0F) / this.amount2
         + this.computeFloat2() + 105.0F;
   }

   public float computeFloat2(float floatValue) {
      return (floatValue - this.amount3 - this.computeFloat3() - 19.0F) / this.amount2
         + this.computeFloat3() + 19.0F;
   }

   public void render(
      float floatValue,
      float floatValue2,
      float floatValue3,
      float floatValue4,
      float floatValue5,
      float floatValue6) {
      float floatValue7 = floatValue - 105.0F - (floatValue3 - 105.0F) * this.amount2;
      float floatValue8 = floatValue2 - this.amount3 - 19.0F - (floatValue4 - 19.0F) * this.amount2;
      this.apply2(
         Math.clamp(floatValue7, 0.0F, Math.max(0.0F, floatValue5 - 210.0F)),
         Math.clamp(floatValue8, 58.0F, Math.max(58.0F, floatValue6 - 112.0F)));
   }

   @Override
   public void render(
      SixSevenFontManager fontManager,
      float floatValue,
      float floatValue2,
      float floatValue3,
      float floatValue4) {
      fontManager.update2();
      fontManager.apply3(this.amount);
      fontManager.apply(0.0F, this.amount3);
      fontManager.render2(this.computeFloat2() + 105.0F, this.computeFloat3() + 19.0F, this.amount2);
      super.render(
         fontManager,
         this.computeFloat(floatValue),
         this.computeFloat2(floatValue2),
         floatValue3,
         floatValue4);
      fontManager.update3();
   }

   public List<MenuPanelSupport> visible() {
      List<MenuPanelSupport> result = new ArrayList<>();
      for (MenuPanelSupport value : this.list) {
         if (this.string.isEmpty()
            || value.module.getName().toLowerCase(Locale.ROOT).contains(this.string)) {
            result.add(value);
         }
      }
      return result;
   }

   @Override
   public String getString() {
      return this.categoryName;
   }

   @Override
   public int computeInt() {
      return -1;
   }

   @Override
   public float computeFloat(SixSevenFontManager fontManager) {
      if (this.enabled) {
         return 0.0F;
      }
      float floatValue = 12.0F;
      for (MenuPanelSupport menuPanelSupport2 : this.visible()) {
         floatValue += menuPanelSupport2.computeFloat() + 3.0F;
      }
      return Math.max(32.0F, floatValue - 3.0F);
   }

   @Override
   public void render(
      SixSevenFontManager fontManager,
      float floatValue,
      float floatValue2,
      float floatValue3,
      float floatValue4,
      float floatValue5) {
      if (!this.enabled) {
         float amount32 = floatValue;
         for (MenuPanelSupport menuPanelSupport2 : this.visible()) {
            float value = menuPanelSupport2.computeFloat();
            menuPanelSupport2.amount2 = this.computeFloat2() + 6.0F;
            menuPanelSupport2.amount3 = amount32;
            menuPanelSupport2.amount4 = 198.0F;
            if (amount32 + value >= floatValue4 && amount32 <= floatValue5) {
               menuPanelSupport2.render(
                  fontManager,
                  floatValue2,
                  floatValue3,
                  this.computeFloat(amount32, amount32 + value, floatValue4, floatValue5));
            }
            amount32 += value + 3.0F;
         }
         if (this.visible().isEmpty()) {
            fontManager.computeFloat(
               "No matching modules",
               this.computeFloat2() + 16.0F,
               floatValue + 16.0F,
               11.0F,
               this.getColorPalette().computeInt18());
         }
      }
   }

   @Override
   public boolean test(float floatValue, float floatValue2, int intValue) {
      if (!this.enabled && !this.test()) {
         for (MenuPanelSupport menuPanelSupport2 : this.visible()) {
            if (menuPanelSupport2.test(floatValue, floatValue2, intValue)) {
               return true;
            }
         }
         return false;
      }
      return false;
   }

   public void update() {
      for (MenuPanelSupport value : this.list) {
         value.update3();
      }
   }

   @Override
   public void apply(float floatValue, float floatValue2) {
      for (MenuPanelSupport value : this.list) {
         value.apply(floatValue, floatValue2);
      }
   }

   @Override
   public void update2() {
      for (MenuPanelSupport value : this.list) {
         value.update2();
      }
   }

   public boolean key(int intValue, boolean flag) {
      return !this.enabled && !this.test() && this.visible().stream()
         .anyMatch(value -> value.test(intValue, flag));
   }

   @Override
   public boolean test(int intValue) {
      return !this.enabled && !this.test() && this.visible().stream()
         .anyMatch(value -> value.test(intValue));
   }
}
