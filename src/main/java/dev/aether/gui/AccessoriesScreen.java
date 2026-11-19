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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The big Accessories popup: a live preview of the real Minecraft player (with their actual skin,
 * draggable to rotate and scrollable to zoom) on the left, and a catalogue of Aether-themed
 * crowns, halos, horns, masks, wings, feathers, capes and backpacks on the right. Hovering or
 * selecting a variant previews it on the player; left-click equips, right-click unequips.
 */
public class AccessoriesScreen extends Screen implements NvgDrawable {
   private static final float HEADER_H = 58.0F;
   private static final float STAGE_W = 320.0F;
   private static final float TAB_H = 32.0F;
   private static final float CELL_W = 150.0F;
   private static final float CELL_H = 96.0F;
   private static final float CELL_GAP = 10.0F;

   private final Screen parent;
   private final AccessoriesModule module;
   private final ThemeManager themes;
   private AccessoryType selectedType = AccessoryType.CROWN;
   private Accessory previewAccessory;
   private float yaw = 35.0F;
   private float pitch = 8.0F;
   private float zoom = 1.0F;
   private boolean dragging;
   private float dragStartX;
   private float dragStartY;
   private float dragStartYaw;
   private float dragStartPitch;

   private final float[] closeRect = new float[4];
   private final float[] unequipAllRect = new float[4];
   private final float[][] tabRects = new float[AccessoryType.values().length][4];
   private final float[][] cellRects = new float[4][4];

   public AccessoriesScreen(Screen parent, AccessoriesModule module) {
      super(Component.literal("Accessories"));
      this.parent = parent;
      this.module = module;
      this.themes = dev.aether.AetherClient.themes();
   }

   private float[] layout() {
      float uiW = OverlayRenderer.uiWidth();
      float uiH = OverlayRenderer.uiHeight();
      float pw = Math.min(uiW * 0.90F, 1040.0F);
      float ph = Math.min(uiH * 0.90F, 720.0F);
      float px = (uiW - pw) * 0.5F;
      float py = (uiH - ph) * 0.5F;
      return new float[]{px, py, pw, ph};
   }

   /** The accessory shown on the player right now: the preview if one is active, else equipped. */
   private Accessory displayAccessory(AccessoryType type) {
      if (this.previewAccessory != null && this.previewAccessory.type() == type) {
         return this.previewAccessory;
      }
      return this.module.equippedAccessory(type);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      guiGraphics.fill(0, 0, this.width, this.height, -67108864);
      Minecraft mc = this.minecraft;
      if (mc != null && mc.player != null) {
         float[] l = this.layout();
         float stageX = l[0] + 16.0F;
         float stageY = l[1] + HEADER_H + 12.0F;
         float stageH = l[3] - HEADER_H - 24.0F;
         int x1 = (int)stageX;
         int y1 = (int)stageY;
         int x2 = (int)(stageX + STAGE_W);
         int y2 = (int)(stageY + stageH);
         int size = (int)(Math.min(STAGE_W, stageH) * 0.85F * this.zoom);
         int scale = Math.max(20, size);
         float mx = (float)mouseX;
         float my = (float)mouseY;
         InventoryScreen.renderEntityInInventoryFollowsMouse(
            guiGraphics, x1, y1, x2, y2, scale, 0.5F, mx, my, mc.player
         );
      }
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

      // Left stage frame (the real player is rendered in render())
      float stageX = px + 16.0F;
      float stageY = py + HEADER_H + 12.0F;
      float stageH = ph - HEADER_H - 24.0F;
      vg.rect(stageX, stageY, STAGE_W, stageH, 14.0F, Colors.withAlpha(theme.accent(), 0.07F));
      vg.rectOutline(stageX, stageY, STAGE_W, stageH, 14.0F, 1.0F, Colors.withAlpha(theme.accentBright(), 0.25F));
      vg.text("Drag to rotate · Scroll to zoom", stageX + STAGE_W * 0.5F - vg.textWidth("Drag to rotate · Scroll to zoom", 10.0F) * 0.5F, stageY + stageH - 16.0F, 10.0F, theme.textDisabled());

      // Overlay the equipped/previewed accessories on the real player model (drawn in render()).
      AccessoryAvatarRenderer.renderEquippedOnPlayer(
         vg, stageX, stageY, stageX + STAGE_W, stageY + stageH, 1.0F,
         this.module, this.previewAccessory, this.yaw, this.pitch
      );

      // Equipped chips under the stage
      float chipY = stageY + stageH - 34.0F;
      int chip = 0;
      for (AccessoryType type : AccessoryType.values()) {
         if (this.displayAccessory(type) != null) {
            String label = type.getSingular();
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
         float labelX = tx + 10.0F;
         vg.text(type.getDisplayName(), labelX, tabY + TAB_H * 0.5F + 4.0F, 11.5F, active ? theme.textPrimary() : theme.textMuted());
         if (equipped) {
            vg.circle(tx + tabW - 12.0F, tabY + TAB_H * 0.5F, 3.4F, theme.accentBright());
         }
      }

      // Variant grid for the selected category (with thumbnails)
      List<Accessory> variants = AccessoryRegistry.forType(this.selectedType);
      float gridX = rightX;
      float gridY = tabY + TAB_H + 14.0F;
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
         boolean previewing = this.previewAccessory != null && this.previewAccessory.id().equals(acc.id());
         boolean hov = mx >= cellX && mx <= cellX + CELL_W && my >= cellY && my <= cellY + CELL_H;
         if (previewing) {
            vg.glow(cellX, cellY, CELL_W, CELL_H, 10.0F, 9.0F, Colors.withAlpha(acc.secondary(), 0.5F));
            vg.rectOutline(cellX, cellY, CELL_W, CELL_H, 10.0F, 1.8F, acc.secondary());
         } else if (selected) {
            vg.glow(cellX, cellY, CELL_W, CELL_H, 10.0F, 8.0F, Colors.withAlpha(acc.secondary(), 0.4F));
            vg.rectOutline(cellX, cellY, CELL_W, CELL_H, 10.0F, 1.6F, acc.secondary());
         } else if (hov) {
            vg.rectOutline(cellX, cellY, CELL_W, CELL_H, 10.0F, 1.2F, Colors.withAlpha(theme.accentBright(), 0.5F));
         }
         vg.rect(cellX, cellY, CELL_W, CELL_H, 10.0F, Colors.withAlpha(theme.accent(), previewing ? 0.20F : (selected ? 0.18F : (hov ? 0.1F : 0.06F))));

         // Thumbnail: swatch with the Aether mark.
         float swX = cellX + 12.0F;
         float swY = cellY + 12.0F;
         float swS = 34.0F;
         vg.rectGradient(swX, swY, swS, swS, 8.0F, acc.primary(), acc.secondary(), true);
         vg.aetherMark(swX + swS * 0.5F, swY + swS * 0.5F, 9.0F, acc.primary(), acc.secondary());
         vg.text(acc.name(), cellX + 12.0F, cellY + CELL_H - 26.0F, 12.0F, (previewing || selected) ? acc.secondary() : theme.textPrimary());
         vg.text(acc.description, cellX + 12.0F, cellY + CELL_H - 12.0F, 9.5F, theme.textMuted());
         if (previewing) {
            float pw2 = vg.textWidth("PREVIEW", 8.5F) + 10.0F;
            vg.rect(cellX + CELL_W - pw2 - 8.0F, cellY + 8.0F, pw2, 14.0F, 7.0F, Colors.withAlpha(acc.secondary(), 0.85F));
            vg.text("PREVIEW", cellX + CELL_W - pw2 - 8.0F + 5.0F, cellY + 8.0F + 10.0F, 8.5F, 0xFF1A1A1A);
         } else if (selected) {
            float pw2 = vg.textWidth("ON", 8.5F) + 10.0F;
            vg.rect(cellX + CELL_W - pw2 - 8.0F, cellY + 8.0F, pw2, 14.0F, 7.0F, Colors.withAlpha(theme.accentBright(), 0.85F));
            vg.text("ON", cellX + CELL_W - pw2 - 8.0F + 5.0F, cellY + 8.0F + 10.0F, 8.5F, 0xFF1A1A1A);
         }
      }

      // Preview panel
      float panelY = gridY + 2 * (CELL_H + CELL_GAP) + 4.0F;
      if (panelY + 86.0F < py + ph - 12.0F) {
         float panelX = gridX;
         float panelW = rightW;
         vg.rect(panelX, panelY, panelW, 86.0F, 10.0F, Colors.withAlpha(theme.accent(), 0.08F));
         vg.rectOutline(panelX, panelY, panelW, 86.0F, 10.0F, 1.0F, Colors.withAlpha(theme.borderSubtle(), 0.8F));
         Accessory shown = this.previewAccessory != null ? this.previewAccessory : this.module.equippedAccessory(this.selectedType);
         if (shown != null) {
            vg.text("PREVIEW PANEL", panelX + 12.0F, panelY + 16.0F, 10.0F, theme.textDisabled());
            vg.text(shown.name(), panelX + 12.0F, panelY + 34.0F, 13.0F, theme.textPrimary());
            vg.text(shown.description, panelX + 12.0F, panelY + 50.0F, 10.0F, theme.textMuted());
            String state = this.previewAccessory != null ? "PREVIEW — click to equip" : "EQUIPPED";
            vg.text(state, panelX + 12.0F, panelY + 70.0F, 10.0F, this.previewAccessory != null ? shown.secondary() : theme.accentBright());
         } else {
             vg.text("PREVIEW PANEL", panelX + 12.0F, panelY + 16.0F, 10.0F, theme.textDisabled());
             vg.text("Hover or select an accessory to preview it here.", panelX + 12.0F, panelY + 34.0F, 11.0F, theme.textMuted());
             vg.text("Left-click to equip · Right-click to unequip", panelX + 12.0F, panelY + 54.0F, 10.0F, theme.textDisabled());
         }
      }

      // Unequip-all button
      float uY = panelY + 86.0F + 8.0F;
      if (uY + 30.0F < py + ph - 12.0F) {
         float uX = gridX;
         this.unequipAllRect[0] = uX;
         this.unequipAllRect[1] = uY;
         this.unequipAllRect[2] = uX + 130.0F;
         this.unequipAllRect[3] = uY + 30.0F;
         boolean uHov = mx >= uX && mx <= uX + 130.0F && my >= uY && my <= uY + 30.0F;
         boolean has = this.module.equippedCount() > 0;
         vg.rect(uX, uY, 130.0F, 30.0F, 8.0F, Colors.withAlpha(theme.accent(), has ? (uHov ? 0.3F : 0.18F) : 0.07F));
         vg.rectOutline(uX, uY, 130.0F, 30.0F, 8.0F, 1.0F, Colors.withAlpha(theme.accentBright(), has ? 0.6F : 0.3F));
         vg.text("Unequip All", uX + 12.0F, uY + 20.0F, 11.0F, has ? theme.accentBright() : theme.textMuted());
      }
   }

   private float ux(double guiCoord) {
      return OverlayRenderer.guiToUi(guiCoord);
   }

   /** True when the point is inside the real-player stage area. */
   private boolean overStage(float mx, float my) {
      float[] l = this.layout();
      float stageX = l[0] + 16.0F;
      float stageY = l[1] + HEADER_H + 12.0F;
      float stageH = l[3] - HEADER_H - 24.0F;
      return mx >= stageX && mx <= stageX + STAGE_W && my >= stageY && my <= stageY + stageH;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      float mx = this.ux(event.x());
      float my = this.ux(event.y());
      if (this.inRect(this.closeRect, mx, my)) {
         this.onClose();
         return true;
      }
      if (this.overStage(mx, my)) {
         if (event.button() == 0) {
            this.dragging = true;
            this.dragStartX = mx;
            this.dragStartY = my;
            this.dragStartYaw = this.yaw;
            this.dragStartPitch = this.pitch;
            return true;
         }
      }
      AccessoryType[] types = AccessoryType.values();
      for (int i = 0; i < types.length; i++) {
         if (this.inRect(this.tabRects[i], mx, my)) {
            this.selectedType = types[i];
            this.previewAccessory = null;
            return true;
         }
      }
      if (this.inRect(this.unequipAllRect, mx, my)) {
         if (this.module.equippedCount() > 0) {
            this.module.unequipAll();
            this.previewAccessory = null;
            return true;
         }
      }
      List<Accessory> variants = AccessoryRegistry.forType(this.selectedType);
      for (int i = 0; i < variants.size(); i++) {
         if (this.inRect(this.cellRects[i], mx, my)) {
            Accessory acc = variants.get(i);
            if (event.button() == 0) {
               this.module.equip(this.selectedType, acc.id());
               this.previewAccessory = null;
            } else if (event.button() == 1) {
               this.module.unequip(this.selectedType);
               this.previewAccessory = null;
            }
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.dragging = false;
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.dragging && event.button() == 0) {
         this.yaw = this.dragStartYaw + (float)(dragX * 0.5);
         this.pitch = Math.max(-60.0F, Math.min(60.0F, this.dragStartPitch - (float)(dragY * 0.4)));
         return true;
      }
      return super.mouseDragged(event, dragX, dragY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      this.zoom = Math.max(0.5F, Math.min(2.0F, this.zoom - (float)scrollY * 0.002F));
      return true;
   }

   /** Hover tracking: preview the accessory under the cursor. */
   @Override
   public boolean mouseMoved(MouseButtonEvent event, double mouseX, double mouseY) {
      float mx = this.ux(mouseX);
      float my = this.ux(mouseY);
      boolean overCell = false;
      List<Accessory> variants = AccessoryRegistry.forType(this.selectedType);
      for (int i = 0; i < variants.size(); i++) {
         if (this.inRect(this.cellRects[i], mx, my)) {
            this.previewAccessory = variants.get(i);
            overCell = true;
            break;
         }
      }
      if (!overCell && this.previewAccessory != null && this.previewAccessory.type() != this.selectedType) {
         this.previewAccessory = null;
      }
      return super.mouseMoved(event, mouseX, mouseY);
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

   @Override
   public void onClose() {
      if (this.parent != null) {
         this.minecraft.setScreen(this.parent);
      } else {
         this.minecraft.setScreen(null);
      }
   }
}
