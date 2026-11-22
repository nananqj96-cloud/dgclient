package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class TenacityGhostHandModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Reach distance", 5.0, 1.0, 15.0, 0.5, "m"));

   public TenacityGhostHandModule() {
      super("GhostHand", "Interact with blocks through walls", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
         if (mc.player.distanceTo(bhr.getBlockPos().getX(), bhr.getBlockPos().getY(), bhr.getBlockPos().getZ()) > range.get()) {
            BlockPos pos = bhr.getBlockPos();
            var state = mc.level.getBlockState(pos);
            if (state.is(Blocks.CHEST) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.BARREL) || state.is(Blocks.SHULKER_BOX)) {
               mc.player.openBlockContainer(mc.level, pos);
            }
         }
      }
   }
}
