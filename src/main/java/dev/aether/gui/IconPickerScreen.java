package dev.aether.gui;

import dev.aether.AetherClient;
import dev.aether.gui.picker.PickerGrid;
import dev.aether.gui.widget.ColorWidget;
import dev.aether.render.NvgDrawable;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.settings.ColorSetting;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import dev.aether.util.UiSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public class IconPickerScreen extends Screen implements NvgDrawable {
   private static final float HEADER_H = 42.0F;
   private static final float SEARCH_H = 34.0F;
   private static final float PAD = 12.0F;
   private static final float CELL = 34.0F;
   private static final float SEL_CHIP_W = 116.0F;
   private static final float PANEL_RADIUS = 14.0F;
   private final Screen parent;
   private final PickerGrid model;
   private final ThemeManager themes;
   private final StringBuilder search = new StringBuilder();
   private boolean searchFocused;
   private boolean selectedOnly;
   private List<PickerGrid.Cell> filtered = List.of();
   private String lastQuery = null;
   private boolean lastSelectedOnly;
   private boolean filterDirty = true;
   private float scroll;
   private float maxScroll;
   private ColorSetting colorSetting;
   private String colorTitle;
   private ColorWidget colorWidget;
   private final float[] popupRect = new float[4];
   private final float[] closeRect = new float[4];
   private final float[] searchRect = new float[4];
   private final float[] selChipRect = new float[4];

   public IconPickerScreen(Screen var1, PickerGrid var2, ThemeManager var3) {
      super(Component.literal(var2.title()));
      this.parent = var1;
      this.model = var2;
      this.themes = var3;
   }

   private void refreshFilter() {
      String var1 = this.search.toString().trim().toLowerCase(Locale.ROOT);
      if (this.filterDirty || !var1.equals(this.lastQuery) || this.selectedOnly != this.lastSelectedOnly) {
         this.lastQuery = var1;
         this.lastSelectedOnly = this.selectedOnly;
         this.filterDirty = false;
         ArrayList var2 = new ArrayList();

         for (PickerGrid.Cell var4 : this.model.cells()) {
            if ((!this.selectedOnly || var4.selected()) && (var1.isEmpty() || var4.matches(var1))) {
               var2.add(var4);
            }
         }

         this.filtered = var2;
         this.scroll = 0.0F;
      }
   }

   private IconPickerScreen.Layout layout() {
      float var1 = OverlayRenderer.uiWidth();
      float var2 = OverlayRenderer.uiHeight();
      float var3 = Math.min(var1 * 0.82F, 940.0F);
      float var4 = Math.min(var2 * 0.84F, 660.0F);
      float var5 = (var1 - var3) / 2.0F;
      float var6 = (var2 - var4) / 2.0F;
      float var7 = var5 + 12.0F;
      float var8 = var6 + 42.0F + 34.0F;
      float var9 = var3 - 24.0F;
      float var10 = var4 - 42.0F - 34.0F - 12.0F;
      int var11 = Math.max(1, (int)(var9 / 34.0F));
      float var12 = var9 / (float)var11;
      return new IconPickerScreen.Layout(var5, var6, var3, var4, var7, var8, var9, var10, var11, var12, var12 - 11.0F);
   }

   private float contentHeight(IconPickerScreen.Layout var1) {
      int var2 = (this.filtered.size() + var1.cols() - 1) / var1.cols();
      return (float)var2 * var1.cell();
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.refreshFilter();
      IconPickerScreen.Layout var5 = this.layout();
      this.maxScroll = Math.max(0.0F, this.contentHeight(var5) - var5.gridH());
      this.scroll = Math.clamp(this.scroll, 0.0F, this.maxScroll);
      float var6 = OverlayRenderer.uiScale();
      double var7 = (double)this.minecraft.getWindow().getGuiScale();
      float var9 = (float)((double)var6 / var7);
      var1.fill(0, 0, this.width, this.height, -670694391);
      fillRoundedRectV(var1, var5.px() * var9, var5.py() * var9, var5.panelW() * var9, var5.panelH() * var9, 14.0F * var9, -132771297, -133560304);
      var1.enableScissor(gi(var5.gridX() * var9), gi(var5.gridY() * var9), gi((var5.gridX() + var5.gridW()) * var9), gi((var5.gridY() + var5.gridH()) * var9));
      int var10 = Math.max(0, (int)(this.scroll / var5.cell()) * var5.cols());
      int var11 = var5.cols() * ((int)(var5.gridH() / var5.cell()) + 3);
      int var12 = Math.min(this.filtered.size(), var10 + var11);
      Matrix3x2fStack var13 = var1.pose();

      for (int var14 = var10; var14 < var12; var14++) {
         PickerGrid.Cell var15 = this.filtered.get(var14);
         int var16 = var14 % var5.cols();
         int var17 = var14 / var5.cols();
         float var18 = var5.gridX() + (float)var16 * var5.cell() + (var5.cell() - var5.icon()) / 2.0F;
         float var19 = var5.gridY() - this.scroll + (float)var17 * var5.cell() + (var5.cell() - var5.icon()) / 2.0F;
         float var20 = var5.icon() * var9 / 16.0F;
         var13.pushMatrix();
         var13.translate(var18 * var9, var19 * var9);
         var13.scale(var20, var20);
         var1.renderItem(var15.icon(), 0, 0);
         var13.popMatrix();
      }

      var1.disableScissor();
   }

   private static int gi(float var0) {
      return Math.round(var0);
   }

   private static void fillRoundedRectV(GuiGraphics var0, float var1, float var2, float var3, float var4, float var5, int var6, int var7) {
      int var8 = Math.round(var1);
      int var9 = Math.round(var2);
      int var10 = Math.round(var1 + var3);
      int var11 = Math.round(var2 + var4);
      int var12 = var11 - var9;
      int var13 = var10 - var8;
      if (var12 > 0 && var13 > 0) {
         int var14 = Math.min(Math.round(var5), Math.min(var13, var12) / 2);

         for (int var15 = 0; var15 < var12; var15++) {
            int var16 = 0;
            if (var14 > 0) {
               int var17 = var15 < var14 ? var15 : (var15 >= var12 - var14 ? var12 - 1 - var15 : -1);
               if (var17 >= 0) {
                  double var18 = (double)(var14 - var17) - 0.5;
                  var16 = (int)Math.round((double)var14 - Math.sqrt(Math.max(0.0, (double)var14 * (double)var14 - var18 * var18)));
               }
            }

            int var20 = Colors.lerp(var6, var7, var12 <= 1 ? 0.0F : (float)var15 / (float)(var12 - 1));
            var0.fill(var8 + var16, var9 + var15, var10 - var16, var9 + var15 + 1, var20);
         }
      }
   }

   @Override
   public void renderNvg(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      if (var1.hasFont()) {
         Theme var7 = this.themes.current();
         IconPickerScreen.Layout var8 = this.layout();
         boolean var9 = System.nanoTime() / 400000000L % 2L == 0L;
         var1.glow(var8.px(), var8.py(), var8.panelW(), var8.panelH(), 5.0F, 14.0F, Colors.withAlpha(-16777216, 0.45F));
         var1.glow(var8.px(), var8.py(), var8.panelW(), var8.panelH(), 5.0F, 6.0F, Colors.withAlpha(var7.accent(), 0.2F));
         var1.rectOutline(var8.px(), var8.py(), var8.panelW(), var8.panelH(), 5.0F, 1.4F, Colors.withAlpha(var7.accentBright(), 0.65F));
         var1.rectOutline(
            var8.px() + 2.2F, var8.py() + 2.2F, var8.panelW() - 4.4F, var8.panelH() - 4.4F, 3.0F, 1.0F, Colors.withAlpha(var7.accentBright(), 0.1F)
         );
         var1.rect(var8.px() + 18.0F, var8.py(), 48.0F, 2.0F, 1.0F, var7.accentBright());
         var1.textGradient(this.model.title(), var8.px() + 12.0F, var8.py() + 16.0F, 16.0F, var7.accentBright(), var7.accent());
         String var10 = "Left-click: toggle   ·   Right-click: color   ·   " + this.model.activeCount() + " active";
         var1.text(var10, var8.px() + 12.0F, var8.py() + 31.0F, 11.0F, var7.textDisabled());
         float var11 = var8.px() + var8.panelW() - 12.0F - 3.0F;
         float var12 = var8.py() + 18.0F;
         boolean var13 = Math.abs(var2 - var11) < 10.0F && Math.abs(var3 - var12) < 10.0F;
         var1.cross(var11 - 6.0F, var12 - 6.0F, 12.0F, 1.8F, var13 ? var7.accentBright() : var7.textMuted());
         this.closeRect[0] = var11 - 10.0F;
         this.closeRect[1] = var12 - 10.0F;
         this.closeRect[2] = var11 + 10.0F;
         this.closeRect[3] = var12 + 10.0F;
         float var14 = var8.px() + 12.0F;
         float var15 = var8.py() + 42.0F + 4.0F;
         float var16 = 22.0F;
         float var17 = var8.panelW() - 24.0F - 116.0F - 8.0F;
         this.searchRect[0] = var14;
         this.searchRect[1] = var15;
         this.searchRect[2] = var14 + var17;
         this.searchRect[3] = var15 + var16;
         var1.rect(var14, var15, var17, var16, var16 / 2.0F, this.searchFocused ? Colors.withAlpha(var7.accent(), 0.16F) : Colors.withAlpha(-16777216, 0.4F));
         var1.rectOutline(
            var14,
            var15,
            var17,
            var16,
            var16 / 2.0F,
            1.1F,
            Colors.withAlpha(this.searchFocused ? var7.accentBright() : var7.accent(), this.searchFocused ? 0.9F : 0.35F)
         );
         float var18 = var14 + 12.0F;
         float var19 = var15 + var16 / 2.0F;
         var1.circleOutline(var18, var19 - 1.0F, 4.0F, 1.4F, var7.textMuted());
         var1.line(var18 + 3.0F, var19 + 2.0F, var18 + 6.0F, var19 + 5.0F, 1.4F, var7.textMuted());
         if (this.search.length() == 0 && !this.searchFocused) {
            var1.text("Search…  (" + this.filtered.size() + " shown)", var14 + 24.0F, var19, 12.5F, var7.textDisabled());
         } else {
            float var20 = var1.text(this.search.toString(), var14 + 24.0F, var19, 12.5F, var7.textPrimary());
            if (this.searchFocused && var9) {
               var1.rect(var14 + 24.0F + var20 + 1.5F, var19 - 6.0F, 1.4F, 12.0F, 0.7F, var7.accentBright());
            }
         }

         float var6;
         this.selChipRect[0] = var6 = var14 + var17 + 8.0F;
         this.selChipRect[1] = var15;
         this.selChipRect[2] = var6 + 116.0F;
         this.selChipRect[3] = var15 + var16;
         boolean var37 = inRect(var2, var3, this.selChipRect);
         var1.rect(var6, var15, 116.0F, var16, var16 / 2.0F, this.selectedOnly ? Colors.withAlpha(var7.accent(), 0.22F) : Colors.withAlpha(-16777216, 0.4F));
         var1.rectOutline(
            var6,
            var15,
            116.0F,
            var16,
            var16 / 2.0F,
            1.1F,
            Colors.withAlpha(!this.selectedOnly && !var37 ? var7.accent() : var7.accentBright(), this.selectedOnly ? 0.9F : 0.4F)
         );
         float var21 = var6 + 13.0F;
         float var22 = var15 + var16 / 2.0F;
         if (this.selectedOnly) {
            var1.circle(var21, var22, 4.0F, var7.statusEnabled());
            var1.circleGlow(var21, var22, 4.0F, 4.0F, Colors.withAlpha(var7.statusEnabled(), 0.5F));
         } else {
            var1.circleOutline(var21, var22, 4.0F, 1.4F, var7.textMuted());
         }

         var1.text("Selected only", var6 + 24.0F, var22, 11.5F, this.selectedOnly ? var7.textPrimary() : var7.textMuted());
         var1.save();
         var1.scissor(var8.gridX(), var8.gridY(), var8.gridW(), var8.gridH());
         int var23 = Math.max(0, (int)(this.scroll / var8.cell()) * var8.cols());
         int var24 = var8.cols() * ((int)(var8.gridH() / var8.cell()) + 3);
         int var25 = Math.min(this.filtered.size(), var23 + var24);
         int var26 = this.cellAt(var2, var3, var8);

         for (int var27 = var23; var27 < var25; var27++) {
            PickerGrid.Cell var28 = this.filtered.get(var27);
            int var29 = var27 % var8.cols();
            int var30 = var27 / var8.cols();
            float var31 = var8.gridX() + (float)var29 * var8.cell();
            float var32 = var8.gridY() - this.scroll + (float)var30 * var8.cell();
            boolean var33 = var28.tracked();
            boolean var34 = var28.enabled();
            if (var27 == var26) {
               var1.rect(var31 + 1.0F, var32 + 1.0F, var8.cell() - 2.0F, var8.cell() - 2.0F, 6.0F, Colors.withAlpha(var7.accent(), 0.12F));
            }

            if (var33) {
               int var35 = var28.color();
               int var36 = var34 ? var35 : Colors.withAlpha(var35, 0.35F);
               var1.rectOutline(var31 + 1.5F, var32 + 1.5F, var8.cell() - 3.0F, var8.cell() - 3.0F, 6.0F, var34 ? 1.8F : 1.0F, var36 | (var34 ? -16777216 : 0));
               var1.rect(var31 + 4.0F, var32 + var8.cell() - 5.0F, var8.cell() - 8.0F, 2.5F, 1.0F, var35 | 0xFF000000);
               if (var34) {
                  var1.circle(var31 + var8.cell() - 6.0F, var32 + 6.0F, 2.6F, var7.statusEnabled());
               }
            }
         }

         var1.restore();
         if (this.filtered.isEmpty() && this.selectedOnly) {
            var1.text(
               "Nothing selected yet — turn off \"Selected only\" to browse.",
               var8.gridX() + 4.0F,
               var8.gridY() + 16.0F,
               12.5F,
               var7.textDisabled()
            );
         }

         if (this.maxScroll > 0.0F) {
            float var38 = var8.px() + var8.panelW() - 6.0F;
            float var39 = Math.max(24.0F, var8.gridH() * (var8.gridH() / this.contentHeight(var8)));
            float var40 = var8.gridY() + (var8.gridH() - var39) * (this.scroll / this.maxScroll);
            var1.rect(var38, var40, 3.0F, var39, 1.5F, Colors.withAlpha(var7.accent(), 0.55F));
         }

         if (this.colorSetting != null && this.colorWidget != null) {
            this.renderColorPopup(var1, var7);
         } else {
            this.popupRect[3] = 0.0F;
            this.popupRect[2] = 0.0F;
            this.popupRect[1] = 0.0F;
            this.popupRect[0] = 0.0F;
         }
      }
   }

   private void renderColorPopup(NVGRenderer var1, Theme var2) {
      this.colorWidget.setExpanded(true);
      float var3 = 220.0F;
      float var4 = 26.0F;
      float var5 = OverlayRenderer.uiWidth();
      float var6 = OverlayRenderer.uiHeight();
      float var7 = (var5 - var3) / 2.0F + 12.0F;
      float var8 = this.colorWidget.height(var1);
      float var9 = var4 + var8 + 12.0F;
      float var10 = (var5 - var3) / 2.0F;
      float var11 = (var6 - var9) / 2.0F;
      this.popupRect[0] = var10;
      this.popupRect[1] = var11;
      this.popupRect[2] = var10 + var3;
      this.popupRect[3] = var11 + var9;
      var1.glow(var10, var11, var3, var9, 12.0F, 14.0F, Colors.withAlpha(var2.accent(), 0.3F));
      var1.rectGradient(var10, var11, var3, var9, 12.0F, var2.background(), var2.backgroundTo(), true);
      var1.rectOutline(var10, var11, var3, var9, 12.0F, 1.4F, Colors.withAlpha(var2.accentBright(), 0.7F));
      var1.textGradient(this.colorTitle, var10 + 12.0F, var11 + 14.0F, 12.5F, var2.accentBright(), var2.accent());
      this.colorWidget.setBounds(var7, var11 + var4, var3 - 24.0F);
      this.colorWidget.render(var1, OverlayRenderer.uiMouseX(), OverlayRenderer.uiMouseY());
   }

   private int cellAt(float var1, float var2, IconPickerScreen.Layout var3) {
      if (!(var1 < var3.gridX()) && !(var1 > var3.gridX() + var3.gridW()) && !(var2 < var3.gridY()) && !(var2 > var3.gridY() + var3.gridH())) {
         int var4 = (int)((var1 - var3.gridX()) / var3.cell());
         int var5 = (int)((var2 - (var3.gridY() - this.scroll)) / var3.cell());
         if (var4 >= 0 && var4 < var3.cols() && var5 >= 0) {
            int var6 = var5 * var3.cols() + var4;
            return var6 >= 0 && var6 < this.filtered.size() ? var6 : -1;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private float ux(double var1) {
      return OverlayRenderer.guiToUi(var1);
   }

   public boolean mouseClicked(MouseButtonEvent var1, boolean var2) {
      float var3 = this.ux(var1.x());
      float var4 = this.ux(var1.y());
      IconPickerScreen.Layout var5 = this.layout();
      if (this.colorSetting != null) {
         if (inRect(var3, var4, this.popupRect)) {
            this.colorWidget.mouseClicked(var3, var4, var1.button());
         } else {
            this.closeColor();
            UiSounds.select();
         }

         return true;
      } else if (inRect(var3, var4, this.closeRect)) {
         this.onClose();
         return true;
      } else if (inRect(var3, var4, this.selChipRect)) {
         this.selectedOnly = !this.selectedOnly;
         this.filterDirty = true;
         this.searchFocused = false;
         UiSounds.toggle(this.selectedOnly);
         return true;
      } else if (inRect(var3, var4, this.searchRect)) {
         this.searchFocused = true;
         UiSounds.select();
         return true;
      } else {
         this.searchFocused = false;
         if (!(var3 < var5.px()) && !(var3 > var5.px() + var5.panelW()) && !(var4 < var5.py()) && !(var4 > var5.py() + var5.panelH())) {
            int var6 = this.cellAt(var3, var4, var5);
            if (var6 >= 0) {
               PickerGrid.Cell var7 = this.filtered.get(var6);
               if (var1.button() == 1) {
                  this.openColor(var7.colorTarget(), var7.label());
               } else if (var1.button() == 0) {
                  boolean var8 = var7.selected();
                  var7.toggle();
                  UiSounds.toggle(var7.enabled());
                  if (this.selectedOnly && var8 && !var7.selected()) {
                     this.filterDirty = true;
                  }
               }

               return true;
            } else {
               return true;
            }
         } else {
            this.onClose();
            return true;
         }
      }
   }

   private void openColor(ColorSetting var1, String var2) {
      if (var1 != null) {
         this.colorSetting = var1;
         this.colorTitle = var2;
         this.colorWidget = new ColorWidget(this.themes, var1);
         this.colorWidget.setExpanded(true);
         this.searchFocused = false;
         UiSounds.select();
      }
   }

   private void closeColor() {
      this.colorSetting = null;
      this.colorTitle = null;
      this.colorWidget = null;
   }

   public boolean mouseDragged(MouseButtonEvent var1, double var2, double var4) {
      if (this.colorSetting != null && this.colorWidget != null) {
         this.colorWidget.mouseDragged(this.ux(var1.x()), this.ux(var1.y()));
      }

      return true;
   }

   public boolean mouseReleased(MouseButtonEvent var1) {
      if (this.colorWidget != null) {
         this.colorWidget.mouseReleased();
      }

      return true;
   }

   public boolean mouseScrolled(double var1, double var3, double var5, double var7) {
      if (this.colorSetting == null) {
         this.scroll = Math.clamp(this.scroll - (float)(var7 * (double)this.layout().cell()), 0.0F, this.maxScroll);
      }

      return true;
   }

   public boolean keyPressed(KeyEvent var1) {
      int var2 = var1.key();
      if (this.colorSetting != null) {
         if (this.colorWidget != null && this.colorWidget.isListening()) {
            this.colorWidget.keyPressed(var2);
            return true;
         } else if (var2 == 256) {
            this.closeColor();
            return true;
         } else {
            return true;
         }
      } else if (this.searchFocused) {
         switch (var2) {
            case 256:
            case 257:
            case 335:
               this.searchFocused = false;
               break;
            case 259:
               if (this.search.length() > 0) {
                  this.search.deleteCharAt(this.search.length() - 1);
                  this.filterDirty = true;
               }
         }

         return true;
      } else if (var2 == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(var1);
      }
   }

   public boolean charTyped(CharacterEvent var1) {
      if (this.colorSetting != null) {
         return true;
      } else if (!this.searchFocused) {
         return true;
      } else if (this.search.length() >= 48) {
         return true;
      } else {
         char var2 = (char)var1.codepoint();
         if (var2 == ' ' || var2 == '_' || var2 == ':' || var2 == '/' || var2 >= '0' && var2 <= '9' || var2 >= 'a' && var2 <= 'z' || var2 >= 'A' && var2 <= 'Z'
            )
          {
            this.search.append(Character.toLowerCase(var2));
            this.filterDirty = true;
         }

         return true;
      }
   }

   public void onClose() {
      AetherClient.config().save();
      this.minecraft.setScreen(this.parent);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void debugSetSearch(String var1) {
      this.search.setLength(0);
      this.search.append(var1);
      this.searchFocused = true;
      this.filterDirty = true;
      this.refreshFilter();
   }

   public void debugSetSelectedOnly(boolean var1) {
      this.selectedOnly = var1;
      this.filterDirty = true;
      this.refreshFilter();
   }

   public void debugOpenColor(int var1) {
      if (var1 >= 0 && var1 < this.filtered.size()) {
         PickerGrid.Cell var2 = this.filtered.get(var1);
         this.openColor(var2.colorTarget(), var2.label());
      }
   }

   public int debugFilteredCount() {
      return this.filtered.size();
   }

   private static boolean inRect(float var0, float var1, float[] var2) {
      return var0 >= var2[0] && var0 <= var2[2] && var1 >= var2[1] && var1 <= var2[3];
   }

   private static record Layout(
      float px, float py, float panelW, float panelH, float gridX, float gridY, float gridW, float gridH, int cols, float cell, float icon
   ) {
   }
}
