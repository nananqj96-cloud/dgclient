package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TenacityScaffoldModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Place delay in ticks", 0.0, 0.0, 10.0, 1.0, "t"));
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Search range", 10.0, 3.0, 30.0, 1.0, "m"));
   public final ModeSetting block = this.addSetting(new ModeSetting("Block", "Block", "Block to place", "Obsidian", "Obsidian", "Cobblestone", "Stone"));
   public final BooleanSetting down = this.addSetting(new BooleanSetting("Down", "Also place below", true));

   private int counter;

   public TenacityScaffoldModule() {
      super("Scaffold", "Automatically places blocks beneath you", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      if (counter > 0) { counter--; return; }
      if (delay.get() > 0) { counter = delay.getInt(); return; }

      double range = this.range.get();
      for (int x = -(int)range; x <= range; x++) {
         for (int z = -(int)range; z <= range; z++) {
            BlockPos below = new BlockPos((int)mc.player.getX() + x, (int)mc.player.getY() - 3, (int)mc.player.getZ() + z);
            if (!isAir(mc, below)) continue;
            BlockPos under = new BlockPos(below.getX(), below.getY() - 1, below.getZ());
            if (isAir(mc, under)) continue;
            placeBlock(mc, below);
            return;
         }
      }
      if (down.get()) {
         for (int i = 1; i <= 5; i++) {
            BlockPos below = new BlockPos((int)mc.player.getX(), (int)mc.player.getY() - i, (int)mc.player.getZ());
            if (!isAir(mc, below)) continue;
            BlockPos under = new BlockPos(below.getX(), below.getY() - 1, below.getZ());
            if (isAir(mc, under)) continue;
            placeBlock(mc, below);
            return;
         }
      }
   }

   private boolean isAir(Minecraft mc, BlockPos pos) {
      return mc.level.getBlockState(pos).isAir();
   }

   private void placeBlock(Minecraft mc, BlockPos pos) {
      if (mc.player == null) return;
      mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
         new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
            net.minecraft.core.Direction.UP, pos, false));
      counter = delay.getInt() + 1;
   }
}
