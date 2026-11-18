package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.SlotActionType;

/**
 * Automatically moves Instant Health potions from your inventory into empty hotbar slots, so your
 * healing potions are always ready to use.
 */
public class PotionRefillModule extends Module {
   public final SliderSetting delay = this.addSetting(
      new SliderSetting("Delay", "Random milliseconds between each potion move", 30.0, 50.0, 0.0, 300.0, 1.0)
   );
   public final BooleanSetting healthPotOnly = this.addSetting(
      new BooleanSetting("Health Pot Only", "Only refill Instant Health potions", false)
   );

   private long lastMoveNanos;
   private double nextDelay;

   public PotionRefillModule() {
      super("Potion Refill", "Automatically refill health potions into your hotbar", Category.MISC);
      this.nextDelay = 30.0;
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         return;
      }
      long now = System.nanoTime();
      if (now - this.lastMoveNanos < (long) this.nextDelay * 1_000_000L) {
         return;
      }
      int emptyHotbar = -1;
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getStack(i).getItem() == Items.AIR) {
            emptyHotbar = i;
            break;
         }
      }
      if (emptyHotbar == -1) {
         this.scheduleNext();
         return;
      }
      int potionSlot = findPotionSlot(player);
      if (potionSlot == -1) {
         this.scheduleNext();
         return;
      }
      mc.interactionManager.clickSlot(0, potionSlot, emptyHotbar, SlotActionType.QUICK_MOVE, player);
      this.lastMoveNanos = now;
      this.scheduleNext();
   }

   private void scheduleNext() {
      double min = this.delay.getMin();
      double max = this.delay.getMax();
      this.nextDelay = min + ThreadLocalRandom.current().nextDouble() * (max - min);
   }

   private int findPotionSlot(LocalPlayer player) {
      for (int i = 9; i < player.getInventory().size(); i++) {
         if (player.getInventory().getStack(i).getItem() == Items.SPLASH_POTION) {
            return i;
         }
      }
      return -1;
   }

   @Override
   protected void onDisable() {
      this.lastMoveNanos = 0L;
   }
}
