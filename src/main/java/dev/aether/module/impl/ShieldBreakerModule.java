package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class ShieldBreakerModule extends Module {
   public final BooleanSetting switchBack = this.addSetting(new BooleanSetting("Switch Back", "", true));
   public final ModeSetting axePriority = this.addSetting(
      new ModeSetting(
         "Axe Priority",
         "",
         "Best",
         "Best",
         "Nearest"
      )
   );
   public final SliderSetting switchDelay = this.addSetting(
      new SliderSetting("Switch Delay", "", 150.0, 0.0, 500.0, 10.0)
   );
   private int previousSlot = -1;
   private int tickCounter;
   private boolean waitingToSwapBack;

   public ShieldBreakerModule() {
      super("Shield Breaker", "Auto-switch to axe when target is blocking", Category.COMBAT);
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
         if (this.waitingToSwapBack) {
            this.tickCounter++;
            int delayTicks = Math.max(1, this.switchDelay.getInt() / 50);
            if (this.tickCounter >= delayTicks) {
               InventoryHelper.swap(this.previousSlot);
               this.resetState();
            }
         } else if (client.hitResult != null
            && client.hitResult.getType() == Type.ENTITY
            && ((EntityHitResult)client.hitResult).getEntity() instanceof Player targetPlayer
            && targetPlayer.isBlocking()
            && !(client.player.getMainHandItem().getItem() instanceof AxeItem)) {
            int axeSlot = this.findAxeSlot(client);
            if (axeSlot != -1) {
               this.previousSlot = client.player.getInventory().getSelectedSlot();
               InventoryHelper.swap(axeSlot);
               client.gameMode.attack(client.player, targetPlayer);
               client.player.swing(InteractionHand.MAIN_HAND);
               if (this.switchBack.get()) {
                  this.waitingToSwapBack = true;
                  this.tickCounter = 0;
               } else {
                  this.previousSlot = -1;
               }
            }
         }
      }
   }

   private int findAxeSlot(Minecraft client) {
      if (this.axePriority.is("Nearest")) {
         for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).getItem() instanceof AxeItem) {
               return i;
            }
         }

         return -1;
      } else {
         int bestSlot = -1;
         float bestDamage = -1.0F;

         for (int ix = 0; ix < 9; ix++) {
            ItemStack stack = client.player.getInventory().getItem(ix);
            if (stack.getItem() instanceof AxeItem) {
               float damage = this.getAxeTier(stack);
               if (damage > bestDamage) {
                  bestDamage = damage;
                  bestSlot = ix;
               }
            }
         }

         return bestSlot;
      }
   }

   private float getAxeTier(ItemStack stack) {
      if (stack.is(Items.NETHERITE_AXE)) {
         return 5.0F;
      } else if (stack.is(Items.DIAMOND_AXE)) {
         return 4.0F;
      } else if (stack.is(Items.IRON_AXE)) {
         return 3.0F;
      } else if (stack.is(Items.GOLDEN_AXE)) {
         return 2.0F;
      } else if (stack.is(Items.STONE_AXE)) {
         return 1.0F;
      } else {
         return stack.is(Items.WOODEN_AXE) ? 0.0F : -1.0F;
      }
   }

   private void resetState() {
      this.previousSlot = -1;
      this.tickCounter = 0;
      this.waitingToSwapBack = false;
   }
}
