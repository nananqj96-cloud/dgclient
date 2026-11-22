package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class TenacityAutoToolModule extends Module {
   private int previous = -1;
   private static TenacityAutoToolModule instance;

   public TenacityAutoToolModule() {
      super("Tenacity Auto Tool", "Selects the best tool while mining (Tenacity port)", Category.MISC);
      instance = this;
   }

   public static TenacityAutoToolModule instance() { return instance; }

   public void select(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      BlockState state = mc.level.getBlockState(pos);
      if (state.isAir()) return;
      Inventory inv = mc.player.getInventory();
      int orig = inv.getSelectedSlot();
      int best = orig;
      float bestSpeed = -1.0F;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = inv.getItem(i);
         float speed = stack.getDestroySpeed(state);
         boolean suitable = stack.isCorrectToolForDrops(state);
         if (suitable && (speed > bestSpeed + 0.001F || (i == orig && speed >= bestSpeed - 0.001F))) {
            bestSpeed = speed;
            best = i;
         } else if (i == orig && bestSpeed < 0.0F) {
            best = i;
         }
      }
      inv.setSelectedSlot(best);
      if (best != orig && previous < 0) previous = orig;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      boolean mining = mc.screen == null
         && mc.options.keyAttack.isDown()
         && mc.hitResult instanceof BlockHitResult bhr
         && bhr.getType() == HitResult.Type.BLOCK;
      if (!mining && previous >= 0) {
         mc.player.getInventory().setSelectedSlot(previous);
         previous = -1;
      }
   }

   @Override
   protected void onDisable() {
      if (instance == this) instance = null;
   }
}
