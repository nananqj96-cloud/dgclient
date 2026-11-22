package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class TenacityAutoToolModule extends Module {
   private int previous = -1;
   private static TenacityAutoToolModule instance;

   public TenacityAutoToolModule() {
      super("Tenacity Auto Tool", "Selects the best tool while mining (Tenacity port)", Category.PLAYER);
      instance = this;
   }

   public static TenacityAutoToolModule instance() { return instance; }

   public void select(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.world == null || mc.player.isCreative()) return;
      BlockState state = mc.world.getBlockState(pos);
      if (state.isAir()) return;
      var inv = mc.player.getInventory();
      int orig = inv.getSelectedSlot();
      int best = orig;
      float bestSpeed = -1.0F;
      for (int i = 0; i < 9; i++) {
         inv.setSelectedSlot(i);
         float speed = mc.player.getBlockBreakingSpeed(state);
         if (speed > bestSpeed + 0.001F || (i == orig && speed >= bestSpeed - 0.001F)) {
            bestSpeed = speed;
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
      boolean mining = mc.currentScreen == null
         && mc.options.attackKey.isPressed()
         && mc.crosshairTarget instanceof BlockHitResult bhr
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
