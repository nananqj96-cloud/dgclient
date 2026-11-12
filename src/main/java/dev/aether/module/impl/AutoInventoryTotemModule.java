package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

public class AutoInventoryTotemModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "", 4.0, 1.0, 40.0, 1.0));
   public final BooleanSetting forceTotem = this.addSetting(new BooleanSetting("Force Totem", "", false));
   public final BooleanSetting hotbarTotem = this.addSetting(new BooleanSetting("Hotbar Totem", "", false));
   public final SliderSetting hotbarSlot = this.addSetting(new SliderSetting("Hotbar Slot", "", 1.0, 1.0, 9.0, 1.0));
   public final SliderSetting hotbarDelay = this.addSetting(new SliderSetting("Hotbar Delay", "", 4.0, 1.0, 40.0, 1.0));
   private int tickCounter;

   public AutoInventoryTotemModule() {
      super(
         "Inv Totem",
         "Moves totems only while inventory (E) is open — click-based, not world-auto",
         Category.COMBAT
      );
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
      if (client.player != null && client.gameMode != null && client.screen instanceof InventoryScreen) {
         this.tickCounter++;
         int wait = this.hotbarTotem.get() && this.needsHotbarWork(client) && !this.needsOffhandWork(client) ? this.hotbarDelay.getInt() : this.delay.getInt();
         if (this.tickCounter >= wait) {
            this.tickCounter = 0;
            if (this.hotbarTotem.get() && this.needsHotbarWork(client)) {
               int source = findTotemInMainInventory(client);
               if (source != -1) {
                  int hotbarIndex = this.hotbarSlot.getInt() - 1;
                  if (hotbarIndex >= 0 && hotbarIndex <= 8) {
                     this.performHotbarSwap(client, source, hotbarIndex);
                  }
               }
            } else if (this.needsOffhandWork(client)) {
               int totemIdx = findTotemForOffhand(client);
               if (totemIdx != -1) {
                  this.performOffhandSwap(client, totemIdx);
               }
            }
         }
      }
   }

   private boolean needsOffhandWork(Minecraft client) {
      boolean hasTotemOffhand = client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
      return this.forceTotem.get() || !hasTotemOffhand;
   }

   private boolean needsHotbarWork(Minecraft client) {
      if (!this.hotbarTotem.get()) {
         return false;
      } else {
         int slot = this.hotbarSlot.getInt() - 1;
         return !client.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING);
      }
   }

   private static int findTotemInMainInventory(Minecraft client) {
      for (int i = 9; i < 36; i++) {
         if (client.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }

   private static int findTotemInHotbar(Minecraft client) {
      for (int i = 0; i < 9; i++) {
         if (client.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }

   private static int findTotemForOffhand(Minecraft client) {
      int main = findTotemInMainInventory(client);
      return main != -1 ? main : findTotemInHotbar(client);
   }

   private static int toScreenSlot(int invIndex) {
      return invIndex < 9 ? invIndex + 36 : invIndex;
   }

   private void performOffhandSwap(Minecraft client, int totemInvIndex) {
      int syncId = client.player.containerMenu.containerId;
      int screenSlot = toScreenSlot(totemInvIndex);
      client.gameMode.handleInventoryMouseClick(syncId, screenSlot, 0, ClickType.PICKUP, client.player);
      client.gameMode.handleInventoryMouseClick(syncId, 45, 0, ClickType.PICKUP, client.player);
      client.gameMode.handleInventoryMouseClick(syncId, screenSlot, 0, ClickType.PICKUP, client.player);
   }

   private void performHotbarSwap(Minecraft client, int sourceInvIndex, int hotbarIndex) {
      int syncId = client.player.containerMenu.containerId;
      client.gameMode.handleInventoryMouseClick(syncId, sourceInvIndex, hotbarIndex, ClickType.SWAP, client.player);
   }
}
