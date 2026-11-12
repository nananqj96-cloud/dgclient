package dev.aether.util;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryHelper {
   private InventoryHelper() {
   }

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   public static int toScreenSlot(int invIndex) {
      return invIndex < 9 ? invIndex + 36 : invIndex;
   }

   public static void selectHotbarSlot(int slot) {
      Minecraft mc = mc();
      if (slot >= 0 && slot <= 8 && mc.player != null && mc.player.getInventory().getSelectedSlot() != slot) {
         mc.player.getInventory().setSelectedSlot(slot);
         if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
         }
      }
   }

   public static void swapOffhand() {
      Minecraft mc = mc();
      if (mc.player != null && mc.getConnection() != null) {
         mc.getConnection().send(new ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
      }
   }

   public static void swapInventoryToHotbar(int invIndex, int hotbarIndex) {
      Minecraft mc = mc();
      if (mc.player != null && mc.gameMode != null && invIndex >= 9 && invIndex <= 35 && hotbarIndex >= 0 && hotbarIndex <= 8) {
         mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, toScreenSlot(invIndex), hotbarIndex, ClickType.SWAP, mc.player);
      }
   }

   public static void swap(int slot) {
      selectHotbarSlot(slot);
   }

   public static InteractionHand handHolding(Item item) {
      LocalPlayer player = mc().player;
      if (player == null) {
         return null;
      } else if (player.getMainHandItem().is(item)) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().is(item) ? InteractionHand.OFF_HAND : null;
      }
   }

   public static int getHotbarSlot(Item item) {
      LocalPlayer player = mc().player;
      if (player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   public static int findItemSlot(Item item) {
      LocalPlayer player = mc().player;
      if (player == null) {
         return -1;
      } else {
         for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).is(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   public static boolean swapToItem(Item item) {
      int slot = getHotbarSlot(item);
      if (slot < 0) {
         return false;
      } else {
         selectHotbarSlot(slot);
         return true;
      }
   }

   public static boolean swapToStack(Predicate<ItemStack> predicate) {
      LocalPlayer player = mc().player;
      if (player == null) {
         return false;
      } else {
         for (int i = 0; i < 9; i++) {
            if (predicate.test(player.getInventory().getItem(i))) {
               selectHotbarSlot(i);
               return true;
            }
         }

         return false;
      }
   }

   public static int findEmptyHotbarSlot() {
      LocalPlayer player = mc().player;
      if (player == null) {
         return -1;
      } else {
         for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
               return i;
            }
         }

         return -1;
      }
   }
}
