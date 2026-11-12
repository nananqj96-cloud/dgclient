package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.lwjgl.glfw.GLFW;

public class ElytraSwapModule extends Module {
   public final KeybindSetting activateKey = this.addSetting(new KeybindSetting("Activate Key", "", 71));
   public final SliderSetting swapDelay = this.addSetting(new SliderSetting("Swap Delay", "", 0.0, 0.0, 20.0, 1.0));
   public final BooleanSetting switchBack = this.addSetting(new BooleanSetting("Switch Back", "", true));
   public final SliderSetting switchDelay = this.addSetting(new SliderSetting("Switch Delay", "", 0.0, 0.0, 20.0, 1.0));
   public final BooleanSetting moveToSlot = this.addSetting(new BooleanSetting("Move To Slot", "", true));
   public final SliderSetting elytraSlot = this.addSetting(new SliderSetting("Elytra Slot", "", 9.0, 1.0, 9.0, 1.0));
   private boolean keyWasDown;
   private boolean swappedToElytra;
   private int tickCounter;
   private boolean waitingForSwap;
   private boolean waitingForSwitchBack;

   public ElytraSwapModule() {
      super("Elytra Swap", "Swap between Elytra and Chestplate with a keybind", Category.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @Override
   public void onTick() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gameMode != null) {
         if (this.waitingForSwap) {
            this.tickCounter++;
            if (this.tickCounter >= this.swapDelay.getInt()) {
               this.performSwap(client);
               this.waitingForSwap = false;
               if (this.switchBack.get()) {
                  this.waitingForSwitchBack = true;
                  this.tickCounter = 0;
               }
            }
         } else if (this.waitingForSwitchBack) {
            this.tickCounter++;
            if (this.tickCounter >= this.switchDelay.getInt()) {
               this.performSwap(client);
               this.waitingForSwitchBack = false;
            }
         } else {
            int key = this.activateKey.get();
            if (key != -1) {
               long handle = client.getWindow().handle();
               boolean pressed = key <= 7 ? GLFW.glfwGetMouseButton(handle, key) == 1 : GLFW.glfwGetKey(handle, key) == 1;
               if (pressed && !this.keyWasDown) {
                  this.waitingForSwap = true;
                  this.tickCounter = 0;
               }

               this.keyWasDown = pressed;
            }
         }
      }
   }

   private void performSwap(Minecraft client) {
      ItemStack chestSlot = client.player.getItemBySlot(EquipmentSlot.CHEST);
      boolean wearingElytra = chestSlot.is(Items.ELYTRA);
      int targetSlot = -1;
      if (wearingElytra) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST && !stack.is(Items.ELYTRA)) {
               targetSlot = i;
               break;
            }
         }
      } else {
         if (this.moveToSlot.get()) {
            int slotIdx = this.elytraSlot.getInt() - 1;
            ItemStack stack = client.player.getInventory().getItem(slotIdx);
            if (stack.is(Items.ELYTRA)) {
               targetSlot = slotIdx;
            }
         }

         if (targetSlot == -1) {
            for (int ix = 0; ix < 36; ix++) {
               if (client.player.getInventory().getItem(ix).is(Items.ELYTRA)) {
                  targetSlot = ix;
                  break;
               }
            }
         }
      }

      if (targetSlot != -1) {
         int screenSlot = targetSlot < 9 ? targetSlot + 36 : targetSlot;
         int armorScreenSlot = 6;
         client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, screenSlot, 0, ClickType.PICKUP, client.player);
         client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, armorScreenSlot, 0, ClickType.PICKUP, client.player);
         client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, screenSlot, 0, ClickType.PICKUP, client.player);
         this.swappedToElytra = !wearingElytra;
      }
   }

   private void resetState() {
      this.keyWasDown = false;
      this.swappedToElytra = false;
      this.tickCounter = 0;
      this.waitingForSwap = false;
      this.waitingForSwitchBack = false;
   }
}
