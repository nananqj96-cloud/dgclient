package dev.aether.port67;

import dev.aether.module.Module;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from the 67port source ({@code sixseven.client.ui.PanelMenu}) — the 67client dropdown menu
 * orchestrator. Lays out and renders the dropdown panels and handles input. The 67client's
 * dialog/config/anti-cheat subsystems are not carried over (they are not part of the core dropdown
 * GUI); the dropdown rendering, top-bar search, and panel interaction are the real 67client source,
 * driven by Aether's module categories for sync.
 */
public final class SixSevenPanelMenu {
   public static final float amount = 280.0F;
   public static final float amount2 = 32.0F;
   public static final float amount3 = 106.0F;
   public static final float amount4 = 30.0F;

   public final List<ModulePanel> list = new ArrayList<>();
   public final List<ModulePanel> list2 = new ArrayList<>();
   public final PanelLayout panelLayout;
   public final TweenValue tweenValue = new TweenValue(180.0F, 0.0F, Easing.OUT_QUART);
   public final StringBuilder stringBuilder = new StringBuilder();
   public boolean enabled;
   public boolean enabled2;
   public ModulePanel modulePanel;
   public ModulePanel modulePanel2;
   public float amount10;
   public float amount11;
   public float amount12 = 1.0F;
   public int count = -1;

   private float pressX;
   private float pressY;
   private float baseX;
   private float baseY;
   private boolean dragMoved;

   public SixSevenPanelMenu(PanelLayout panelLayout, List<ModulePanel> panels) {
      this.panelLayout = panelLayout;
      for (ModulePanel panel : panels) {
         this.list.add(panel);
      }
      this.list2.addAll(this.list);
   }

   public void render(SixSevenFontManager fontManager, float uiW, float uiH) {
      this.amount10 = uiW;
      this.amount11 = uiH;
      panelLayout.ensureDefaultLayout(uiW, uiH);
      int intValue = Math.max(1, (int) ((uiW - 24.0F) / 222.0F));
      int intValue2 = Math.max(1, (this.list.size() + intValue - 1) / intValue);
      for (ModulePanel modulePanel : this.list) {
         modulePanel.setAmount7((uiH - 122.0F) / intValue2 - 38.0F - 12.0F);
         if (modulePanel.enabled) {
            modulePanel.apply(true);
         }
         modulePanel.apply(this.stringBuilder.toString());
      }
      this.amount12 = this.computeFloat();
      for (int index = 0; index < this.list.size(); index++) {
         this.list.get(index).apply(Math.min(130.0F, index * 26.0F));
      }
   }

   private float computeFloat() {
      return 1.0F;
   }

   public void render2(
      SixSevenFontManager fontManager, float mx, float my, float uiW, float uiH) {
      for (int index = this.list2.size() - 1; index >= 0; index--) {
         ModulePanel modulePanel = this.list2.get(index);
         modulePanel.render(fontManager, mx, my, uiW, uiH);
      }
      this.render(fontManager, mx, my);
   }

   /** Top bar: search field + watermark (the real 67client top-bar drawing). */
   public void render(SixSevenFontManager fontManager, float mx, float my) {
      ColorPalette instance = ColorPalette.getInstance();
      float floatValue3 = (this.amount10 - 280.0F) / 2.0F;
      float float2 = this.computeFloat2();
      this.tweenValue.animateTo(1.0F);
      SixSevenPanelMenu.render(fontManager, floatValue3, float2, 280.0F, 32.0F, this.tweenValue.getValue());
      fontManager.render2(floatValue3 + 16.0F, float2 + 15.0F, 4.5F, 1.5F, instance.computeInt18());
      fontManager.render2(
         floatValue3 + 19.0F,
         float2 + 18.0F,
         floatValue3 + 23.0F,
         float2 + 22.0F,
         1.5F,
         instance.computeInt18());
      fontManager.computeFloat(
         this.stringBuilder.isEmpty() && !this.enabled
            ? "67Port"
            : this.stringBuilder + (this.enabled ? "|" : ""),
         floatValue3 + 32.0F,
         float2 + 16.0F,
         13.0F,
         instance.computeInt16(),
         214.0F);
      if (!this.stringBuilder.isEmpty()) {
         fontManager.render5(
            floatValue3 + 280.0F - 24.0F, float2 + 11.0F, 10.0F, 1.4F, instance.computeInt18());
      }
   }

   private float computeFloat2() {
      return 16.0F - 8.0F * (1.0F - this.amount12);
   }

   /** Shared dropdown-chrome renderer (the real 67client panel glow/glass drawing). */
   public static void render(
      SixSevenFontManager fontManager,
      float floatValue,
      float floatValue2,
      float floatValue3,
      float floatValue4,
      float floatValue5) {
      ColorPalette instance = ColorPalette.getInstance();
      fontManager.render2(
         floatValue,
         floatValue2,
         floatValue3,
         floatValue4,
         floatValue4 / 2.0F,
         4.0F + 3.0F * floatValue5,
         instance.computeInt(0.1F + 0.12F * floatValue5));
      fontManager.render(
         floatValue,
         floatValue2,
         floatValue3,
         floatValue4,
         floatValue4 / 2.0F,
         instance.computeInt5(),
         instance.computeInt6(),
         true);
      fontManager.render(
         floatValue,
         floatValue2,
         floatValue3,
         floatValue4,
         floatValue4 / 2.0F,
         instance.computeInt(0.045F * floatValue5));
      fontManager.render(
         floatValue,
         floatValue2,
         floatValue3,
         floatValue4,
         floatValue4 / 2.0F,
         1.2F,
         instance.computeInt(0.4F + 0.45F * floatValue5));
   }

   public ModulePanel getModulePanel(float floatValue, float floatValue2) {
      for (ModulePanel modulePanel : this.list2) {
         float value = modulePanel.computeFloat(floatValue);
         float float2 = modulePanel.computeFloat2(floatValue2);
         if (modulePanel.test(value, float2)) {
            return modulePanel;
         }
      }
      return null;
   }

   public void click(float mx, float my, int btn) {
      if (this.enabled2) {
         return;
      }
      ModulePanel modulePanel = this.getModulePanel(mx, my);
      if (modulePanel == null) {
         this.update2();
         return;
      }
      float amount62 = mx;
      float amount72 = my;
      mx = modulePanel.computeFloat(mx);
      my = modulePanel.computeFloat2(my);
      this.apply(modulePanel);
      if (modulePanel.test(mx, my)) {
         this.update2();
         if (btn == 0) {
            if (modulePanel.enabled) {
               // collapsed -> open
               modulePanel.apply(false);
            } else {
               // open -> start drag
               this.modulePanel = modulePanel;
               this.pressX = amount62;
               this.pressY = amount72;
               this.baseX = modulePanel.computeFloat2();
               this.baseY = modulePanel.computeFloat3();
               this.dragMoved = false;
            }
         } else {
            modulePanel.apply(!modulePanel.enabled);
         }
      } else {
         this.enabled = false;
         this.modulePanel2 = modulePanel;
         modulePanel.test(mx, my, btn);
      }
   }

   public void apply(float mx, float my) {
      if (this.modulePanel != null) {
         if (!this.dragMoved && Math.hypot(mx - this.pressX, my - this.pressY) > 4.0F) {
            this.dragMoved = true;
         }
         if (this.dragMoved) {
            this.modulePanel.render(
               mx, my,
               this.baseX + (mx - this.pressX),
               this.baseY + (my - this.pressY),
               this.amount10, this.amount11);
            this.panelLayout.update();
         }
      } else if (this.modulePanel2 != null) {
         this.modulePanel2.apply(
            this.modulePanel2.computeFloat(mx),
            this.modulePanel2.computeFloat2(my));
      }
   }

   public void update3() {
      if (this.modulePanel != null && !this.dragMoved) {
         this.modulePanel.update3();
      }
      this.modulePanel = this.modulePanel2 = null;
      for (ModulePanel panel : this.list) {
         panel.update2();
      }
   }

   public void render(float mx, float my, double scroll) {
      if (this.modulePanel2 != null) {
         this.modulePanel2.apply(scroll);
      }
   }

   public boolean test(int key, boolean shift) {
      if (this.enabled) {
         if (key == 256 || key == 257) {
            this.enabled = false;
         } else if (key == 259 && !this.stringBuilder.isEmpty()) {
            this.stringBuilder.delete(
               this.stringBuilder.offsetByCodePoints(this.stringBuilder.length(), -1),
               this.stringBuilder.length());
         } else if (key == 261) {
            this.stringBuilder.setLength(0);
         }
         return true;
      }
      if (key == 256) {
         this.update();
         return true;
      }
      for (ModulePanel panel : this.list) {
         if (panel.key(key, shift)) {
            return true;
         }
      }
      return false;
   }

   public boolean charTyped(int codepoint) {
      if (this.enabled) {
         if (Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint)) {
            this.stringBuilder.appendCodePoint(codepoint);
         }
         return true;
      }
      return this.list.stream().anyMatch(panel -> panel.test(codepoint));
   }

   private void update2() {
      this.enabled = false;
      for (ModulePanel panel : this.list) {
         panel.update();
      }
   }

   private void update() {
      if (!this.enabled2) {
         this.enabled2 = true;
         this.update3();
         this.update2();
      }
   }

   public void apply(ModulePanel modulePanel) {
      this.list2.remove(modulePanel);
      this.list2.addFirst(modulePanel);
   }

   public void close() {
      this.update3();
      this.update2();
   }
}
