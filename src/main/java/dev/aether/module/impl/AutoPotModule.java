package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * Throws a potion automatically. In Auto mode it throws once health drops to the threshold; in
 * While Holding mode it throws the currently held potion.
 */
public class AutoPotModule extends Module {
   public final ModeSetting mode = this.addSetting(
      new ModeSetting("Mode", "When to throw a potion", "Auto", "Auto", "While Holding")
   );
   public final SliderSetting health = this.addSetting(
      new SliderSetting("Health", "Throw once health is at or below this (Auto mode)", 10.0, 0.1, 20.0, 0.1)
   );
   public final SliderSetting pitch = this.addSetting(
      new SliderSetting("Pitch", "Downward pitch to throw at", 70.0, 50.0, 90.0, 1.0)
   );

   private long lastThrowNanos;

   public AutoPotModule() {
      super("Auto Pot", "Automatically throw a potion", Category.MISC);
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.screen != null || !mc.isWindowFocused()) {
         return;
      }
      long now = System.nanoTime();
      if (now - this.lastThrowNanos < 300_000_000L) {
         return;
      }
      boolean auto = this.mode.get().equals("Auto");
      if (auto && player.getHealth() > this.health.getFloat()) {
         return;
      }
      int slot;
      if (auto) {
         slot = findPotionSlot(player);
         if (slot == -1) {
            return;
         }
      } else {
         slot = player.getInventory().getSelectedSlot();
      }
      int prevSlot = player.getInventory().getSelectedSlot();
      float prevPitch = player.getPitch();
      player.getInventory().setSelectedSlot(slot);
      player.setPitch((float) this.pitch.getFloat() - 1.0F + ThreadLocalRandom.current().nextFloat());
      mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
      player.setPitch(prevPitch);
      player.getInventory().setSelectedSlot(prevSlot);
      this.lastThrowNanos = now;
   }

   private static int findPotionSlot(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getStack(i).getItem() == Items.SPLASH_POTION) {
            return i;
         }
      }
      return -1;
   }

   @Override
   protected void onDisable() {
      this.lastThrowNanos = 0L;
   }
}
