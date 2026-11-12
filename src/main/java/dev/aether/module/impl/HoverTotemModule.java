package dev.aether.module.impl;

import dev.aether.mixin.AbstractContainerScreenAccessor;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class HoverTotemModule extends Module {
   public final SliderSetting tickDelay = this.addSetting(new SliderSetting("Tick Delay", "", 0.0, 0.0, 20.0, 1.0));
   public final BooleanSetting hotbarTotem = this.addSetting(new BooleanSetting("Hotbar Totem", "", true));
   public final SliderSetting hotbarSlot = this.addSetting(new SliderSetting("Hotbar Slot", "", 1.0, 1.0, 9.0, 1.0));
   public final BooleanSetting autoSwitchToTotem = this.addSetting(
      new BooleanSetting("Auto Switch To Totem", "", false)
   );
   private int tickCounter;

   public HoverTotemModule() {
      super("Hover Totem", "Equip totem when hovering over one in inventory", Category.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.tickCounter = 0;
   }

   @Override
   protected void onDisable() {
      this.tickCounter = 0;
   }

   @Override
   public void onTick() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gameMode != null) {
         if (this.autoSwitchToTotem.get() && this.hotbarTotem.get()) {
            int slot = this.hotbarSlot.getInt() - 1;
            if (client.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
               InventoryHelper.swap(slot);
            }
         }

         if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
            this.tickCounter++;
            if (this.tickCounter >= this.tickDelay.getInt()) {
               this.tickCounter = 0;
               Slot focusedSlot = this.getFocusedSlot(handledScreen);
               if (focusedSlot != null && focusedSlot.hasItem() && focusedSlot.getItem().is(Items.TOTEM_OF_UNDYING)) {
                  int slotId = focusedSlot.index;
                  client.gameMode.handleInventoryMouseClick(handledScreen.getMenu().containerId, slotId, 0, ClickType.PICKUP, client.player);
                  client.gameMode.handleInventoryMouseClick(handledScreen.getMenu().containerId, 45, 0, ClickType.PICKUP, client.player);
                  client.gameMode.handleInventoryMouseClick(handledScreen.getMenu().containerId, slotId, 0, ClickType.PICKUP, client.player);
               }
            }
         }
      }
   }

   private Slot getFocusedSlot(AbstractContainerScreen<?> screen) {
      return ((AbstractContainerScreenAccessor)screen).getHoveredSlot();
   }
}
