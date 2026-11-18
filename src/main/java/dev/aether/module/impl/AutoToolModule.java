package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.inventory.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

/**
 * Automatically swaps to the best tool in your hotbar for the block you are mining, then (optionally)
 * swaps back once you stop.
 */
public class AutoToolModule extends Module {
   private int savedSlot = -1;
   public final BooleanSetting swapBack = this.addSetting(
      new BooleanSetting("Swap Back", "Return to your previous slot when you stop mining", true)
   );

   public AutoToolModule() {
      super("Auto Tool", "Swap to the best tool for the block you mine", Category.MISC);
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) {
         this.updateSelectedSlot(player);
         return;
      }
      BlockPos pos = this.getBlockPos(mc, player);
      if (pos == null) {
         this.updateSelectedSlot(player);
         return;
      }
      BlockState state = mc.level.getBlockState(pos);
      if (state.isAir()) {
         this.updateSelectedSlot(player);
         return;
      }
      PlayerInventory inv = player.getInventory();
      int current = inv.getSelectedSlot();
      float[] speeds = new float[PlayerInventory.getHotbarSize()];
      boolean[] suitable = new boolean[PlayerInventory.getHotbarSize()];
      for (int i = 0; i < speeds.length; i++) {
         ItemStack stack = inv.getStack(i);
         speeds[i] = stack.getMiningSpeedMultiplier(state);
         suitable[i] = stack.isSuitableFor(state);
      }
      int best = selectBest(speeds, suitable, current);
      if (best != current) {
         if (this.savedSlot < 0) {
            this.savedSlot = current;
         }
         inv.setSelectedSlot(best);
      }
   }

   private void updateSelectedSlot(LocalPlayer player) {
      if (player != null && this.swapBack.get() && this.savedSlot >= 0) {
         player.getInventory().setSelectedSlot(this.savedSlot);
         this.savedSlot = -1;
      }
   }

   private static BlockPos getBlockPos(Minecraft mc, LocalPlayer player) {
      HitResult hit = player.pick(12.0, 0.0F, false);
      if (hit instanceof BlockHitResult blockHit) {
         return blockHit.getBlockPos();
      }
      return null;
   }

   private static int selectBest(float[] speeds, boolean[] suitable, int current) {
      int best = current;
      float bestSpeed = speeds[current];
      for (int i = 0; i < speeds.length; i++) {
         if (i == current) {
            continue;
         }
         if (suitable[i] && (speeds[i] > bestSpeed || !suitable[best])) {
            best = i;
            bestSpeed = speeds[i];
         }
      }
      return best;
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && this.swapBack.get() && this.savedSlot >= 0) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         this.savedSlot = -1;
      }
   }
}
