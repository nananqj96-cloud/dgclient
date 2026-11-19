package dev.aether.gui;

import dev.aether.accessory.AccessoriesModule;
import dev.aether.accessory.Accessory;
import dev.aether.accessory.AccessoryAvatarRenderer;
import dev.aether.accessory.AccessoryType;
import dev.aether.accessory.AccessoryRegistry;
import dev.aether.render.NvgDrawable;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The big Accessories popup: a large live avatar preview on the left and a catalogue of
 * Aether-themed crowns, wings, feathers and capes on the right. Left-click a variant to equip
 * it, right-click to unequip the current category.
 */
public class AccessoriesScreen extends Screen implements NvgDrawable {
   private static final float HEADER_H = 58.0F;
   private static final float STAGE_W = 300.0F;
   private static final float TAB_H = 32.0F;
   private static final float CELL_W = 150.0F;
   private static final float CELL_H = 96.0F;
   private static final float CELL_GAP = 10.0F;

   private final Screen parent;
   private final AccessoriesModule module;
   private final ThemeManager themes;
   private AccessoryType selectedType = AccessoryType.CROWN;
   private final float[] closeRect = new float[4];
   private final float[][] tabRects = new float[AccessoryType.values().length][4];
   private final float[][] cellRects = new float[4][4];
   private final float[] unequipRect = new float[4];

   public AccessoriesScreen(Screen parent, AccessoriesModule module) {
      super(Component.literal("Accessories"));
      this.parent = parent;
      this.module = module;
      this.themes = dev.aether.AetherClient.themes();
   }

   private float[] layout() {
      float uiW = OverlayRenderer.uiWidth();
      float uiH = OverlayRenderer.uiHeight();
      float pw = Math.min(uiW * 0.88F, 980.0F);
      float ph = Math.min(uiH * 0.88F, 700.0F);
      float px = (uiW - pw) * 0.5F;
      float py = (uiH - ph) * 0.5F;
      return new float[]{px, py, pw, ph};
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.fill(0, 0, this.width, this.height, -67108864);
   }

   @Override
   public void renderNvg(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      if (!vg.hasFont()) {
         return;
      }
      Theme theme = this.themes.current();
      float[] l = this.layout();
      float px = l[0], py = l[1], pw = l[2], ph = l[3];

      vg.glow(px, py + 4.0F, pw, ph, 16.0F, 20.0F, Colors.withAlpha(0xFF000000, 0.5F));
      vg.glow(px - 1.0F, py - 1.0F, pw + 2.0F, ph + 2.0F, 16.0F, 14.0F, Colors.withAlpha(theme.accent(), 0.22F));
      vg.rectGradient(px, py, pw, ph, 16.0F, theme.background(), theme.backgroundTo(), true);
      vg.rectOutline(px, py, pw, ph, 16.0F, 1.4F, Colors.withAlpha(theme.accentBright(), 0.55F));
      vg.rect(px + 20.0F, py, 64.0F, 2.2F, 1.1F, theme.accentBright());

      // Header
      vg.textGradient("ACCESSORIES", px + 20.0F, py + 20.0F, 19.0F, theme.accentBright(), theme.accent());
      vg.text("Aether avatar styling — " + this.module.equippedCount() + " equipped", px + 20.0F, py + 42.0F, 11.5F, theme.textDisabled());
      float cx = px + pw - 20.0F - 3.0F;
      float cy = py + 22.0F;
      boolean cHov = mx >= cx - 10.0F && mx <= cx + 10.0F && my >= cy - 10.0F && my <= cy + 10.0F;
      vg.cross(cx - 6.0F, cy - 6.0F, 12.0F, 1.8F, cHov ? theme.accentBright() : theme.textMuted());
      this.closeRect[0] = cx - 10.0F;
      this.closeRect[1] = cy - 10.0F;
      this.closeRect[2] = cx + 10.0F;
      this.closeRect[3] = cy + 10.0F;

      // Left stage
      float stageX = px + 16.0F;
      float stageY = py + HEADER_H + 12.0F;
      float stageH = ph - HEADER_H - 24.0F;
      vg.rect(stageX, stageY, STAGE_W, stageH, 14.0F, Colors.withAlpha(theme.accent(), 0.07F));
      vg.rectOutline(stageX, stageY, STAGE_W, stageH, 14.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.25F));
      float avatarCx = stageX + STAGE_W * 0.5F;
      float avatarCy = stageY + stageH * 0.46F;
      float avatarScale = Math.min(STAGE_W, stageH) / 240.0F;
      AccessoryAvatarRenderer.render(vg, avatarCx, avatarCy, avatarScale, this.module, System.nanoTime());

      // Equipped chips under the avatar
      float chipY = stageY + stageH - 26.0F;
      int chip = 0;
      for (AccessoryType type : AccessoryType.values()) {
         if (this.module.selected(type) != null) {
            String label = type.getDisplayName().substring(0, type.getDisplayName().length() - 1);
            float cw = vg.textWidth(label, 10.5F) + 16.0F;
            float cxp = stageX + 12.0F + chip * (cw + 6.0F);
            if (cxp + cw > stageX + STAGE_W - 12.0F) {
               break;
            }
            vg.rect(cxp, chipY, cw, 18.0F, 9.0F, Colors.withAlpha(theme.accent(), 0.24F));
            vg.text(label, cxp + 8.0F, chipY + 12.0F, 10.5F, theme.accentBright());
            chip++;
         }
      }

      // Right column
      float rightX = stageX + STAGE_W + 16.0F;
      float rightW = px + pw - 16.0F - rightX;
      float tabY = py + HEADER_H + 8.0F;
      float tabW = (rightW - 3.0F * 8.0F) / AccessoryType.values().length;
      AccessoryType[] types = AccessoryType.values();
      for (int i = 0; i < types.length; i++) {
         AccessoryType type = types[i];
         boolean active = type == this.selectedType;
         boolean equipped = this.module.selected(type) != null;
         float tx = rightX + i * (tabW + 8.0F);
         this.tabRects[i][0] = tx;
         this.tabRects[i][1] = tabY;
         this.tabRects[i][2] = tx + tabW;
         this.tabRects[i][3] = tabY + TAB_H;
         if (active) {
            vg.glow(tx, tabY, tabW, TAB_H, 7.0F, 6.0F, Colors.withAlpha(theme.accent(), 0.25F));
            vg.rect(tx, tabY, tabW, TAB_H, 8.0F, Colors.withAlpha(theme.accent(), 0.26F));
            vg.rectOutline(tx, tabY, tabW, TAB_H, 8.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.6F));
         } else {
            vg.rect(tx, tabY, tabW, TAB_H, 8.0F, Colors.withAlpha(theme.accent(), 0.09F));
            vg.rectOutline(tx, tabY, tabW, TAB_H, 8.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.8F));
         }
         float labelX = tx + 12.0F;
         vg.text(type.getDisplayName(), labelX, tabY + TAB_H * 0.5F + 4.0F, 12.0F, active ? theme.textPrimary() : theme.textMuted());
         if (equipped) {
            vg.circle(tx + tabW - 12.0F, tabY + TAB_H * 0.5F, 3.4F, theme.accentBright());
         }
      }

      // Variant grid for the selected category
      List<Accessory> variants = AccessoryRegistry.forType(this.selectedType);
      float gridX = rightX;
      float gridY = tabY + TAB_H + 14.0F;
      float cols = 2.0F;
      for (int i = 0; i < variants.size(); i++) {
         Accessory acc = variants.get(i);
         int col = i % 2;
         int row = i / 2;
         float cellX = gridX + col * (CELL_W + CELL_GAP);
         float cellY = gridY + row * (CELL_H + CELL_GAP);
         this.cellRects[i][0] = cellX;
         this.cellRects[i][1] = cellY;
         this.cellRects[i][2] = cellX + CELL_W;
         this.cellRects[i][3] = cellY + CELL_H;
         boolean selected = this.module.selected(this.selectedType) != null && this.module.selected(this.selectedType).equals(acc.id());
         boolean hov = mx >= cellX && mx <= cellX + CELL_W && my >= cellY && my <= cellY + CELL_H;
         if (selected) {
            vg.glow(cellX, cellY, CELL_W, CELL_H, 10.0F, 8.0F, Colors.withAlpha(acc.secondary(), 0.4F));
            vg.rectOutline(cellX, cellY, CELL_W, CELL_H, 10.0F, 1.6F, acc.secondary());
         } else if (hov) {
            vg.rectOutline(cellX, cellY, CELL_W, CELL_H, 10.0F, 1.2F, Colors.withAlpha(theme.accentBright(), 0.5F));
         }
         vg.rect(cellX, cellY, CELL_W, CELL_H, 10.0F, Colors.withAlpha(theme.accent(), selected ? 0.18F : (hov ? 0.1F : 0.06F)));

         // Swatch
         float swX = cellX + 12.0F;
         float swY = cellY + 12.0F;
         float swS = 34.0F;
         vg.rectGradient(swX, swY, swS, swS, 8.0F, acc.primary(), acc.secondary(), true);
         vg.aetherMark(swX + swS * 0.5F, swY + swS * 0.5F, 9.0F, acc.primary(), acc.secondary());
         vg.text(acc.name(), cellX + 12.0F, cellY + CELL_H - 26.0F, 12.0F, selected ? acc.secondary() : theme.textPrimary());
         vg.text(acc.description(), cellX + 12.0F, cellY + CELL_H - 12.0F, 9.5F, theme.textMuted());
      }

      // Unequip button for the selected category
      float uY = gridY + 2 * (CELL_H + CELL_GAP) + 4.0F;
      if (uY + 30.0F < py + ph - 12.0F) {
         float uX = gridX;
         this.unequipRect[0] = uX;
         this.unequipRect[1] = uY;
         this.unequipRect[2] = uX + 130.0F;
         this.unequipRect[3] = uY + 30.0F;
         boolean uHov = mx >= uX && mx <= uX + 130.0F && my >= uY && my <= uY + 30.0F;
         boolean has = this.module.selected(this.selectedType) != null;
         vg.rect(uX, uY, 130.0F, 30.0F, 8.0F, Colors.withAlpha(theme.accent(), has ? (uHov ? 0.3F : 0.18F) : 0.07F));
         vg.rectOutline(uX, uY, 130.0F, 30.0F, 8.0F, 1.0F, Colors.withAlpha(theme.accentBright(), has ? 0.6F : 0.3F));
         vg.text("Unequip " + this.selectedType.getDisplayName().substring(0, this.selectedType.getDisplayName().length() - 1), uX + 12.0F, uY + 20.0F, 11.0F, has ? theme.accentBright() : theme.textMuted());
      }
   }

   private float ux(double guiCoord) {
      return OverlayRenderer.guiToUi(guiCoord);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      float mx = this.ux(event.x());
      float my = this.ux(event.y());
      if (this.inRect(this.closeRect, mx, my)) {
         this.onClose();
         return true;
      }
      AccessoryType[] types = AccessoryType.values();
      for (int i = 0; i < types.length; i++) {
         if (this.inRect(this.tabRects[i], mx, my)) {
            this.selectedType = types[i];
            return true;
         }
      }
      if (this.inRect(this.unequipRect, mx, my)) {
         if (this.module.selected(this.selectedType) != null) {
            this.module.unequip(this.selectedType);
            return true;
         }
      }
      List<Accessory> variants = AccessoryRegistry.forType(this.selectedType);
      for (int i = 0; i < variants.size(); i++) {
         if (this.inRect(this.cellRects[i], mx, my)) {
            this.module.equip(this.selectedType, variants.get(i).id());
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      // Right-click on a cell unequips the selected category.
      if (event.button() == 1) {
         float mx = this.ux(event.x());
         float my = this.ux(event.y());
         List<Accessory> variants = AccessoryRegistry.forType(this.selectedType);
         for (int i = 0; i < variants.size(); i++) {
            if (this.inRect(this.cellRects[i], mx, my)) {
               this.module.unequip(this.selectedType);
               return true;
            }
         }
      }
      return super.mouseReleased(event);
   }

   @Override
   public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
      if (event.key() == 256) {
         this.onClose();
         return true;
      }
      return super.keyPressed(event);
   }

   private static boolean inRect(float[] r, float mx, float my) {
      return mx >= r[0] && mx <= r[2] && my >= r[1] && my <= r[3];
   }

   private void onClose() {
      if (this.parent != null) {
         this.minecraft.setScreen(this.parent);
      } else {
         this.minecraft.setScreen(null);
      }
   }
}
