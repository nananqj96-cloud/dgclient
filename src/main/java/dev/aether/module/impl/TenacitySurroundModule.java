package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class TenacitySurroundModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Place delay ticks", 4.0, 1.0, 20.0, 1.0, "t"));
   public final BooleanSetting up = this.addSetting(new BooleanSetting("Up", "Place blocks above head", true));
   private int counter;

   public TenacitySurroundModule() {
      super("Surround", "Places obsidian blocks around you for protection", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (counter > 0) { counter--; return; }
      BlockPos pos = mc.player.getBlockPos();
      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            BlockPos p = pos.offset(x, 0, z);
            if (mc.level.getBlockState(p).isAir()) {
               mc.player.placeBlock(p, mc.level.getBlockState(p), null, null);
               counter = delay.getInt();
               return;
            }
         }
      }
   }
}
