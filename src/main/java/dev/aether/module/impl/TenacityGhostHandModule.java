package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

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
         BlockPos pos = bhr.getBlockPos();
         double dx = pos.getX() - mc.player.getX();
         double dy = pos.getY() - mc.player.getY();
         double dz = pos.getZ() - mc.player.getZ();
         if (Math.sqrt(dx*dx + dy*dy + dz*dz) > range.get()) {
            var state = mc.level.getBlockState(pos);
            if (state.is(Blocks.CHEST) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.BARREL) || state.is(Blocks.SHULKER_BOX)) {
               mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                  new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
            }
         }
      }
   }
}
