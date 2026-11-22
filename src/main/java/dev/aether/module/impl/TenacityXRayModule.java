package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;

public class TenacityXRayModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "X-Ray range", 16.0, 5.0, 64.0, 1.0, "m"));
   public final BooleanSetting chests = this.addSetting(new BooleanSetting("Chests", "Reveal chests", true));
   public final BooleanSetting ores = this.addSetting(new BooleanSetting("Ores", "Reveal ores", true));
   public final BooleanSetting diamonds = this.addSetting(new BooleanSetting("Diamonds", "Reveal diamond ore", true));

   public TenacityXRayModule() {
      super("XRay", "Sees through blocks to reveal valuables", Category.RENDER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return;
      // X-Ray rendering handled via mixin; block list provided here
   }

   public boolean isRevealed(net.minecraft.world.level.block.Block block) {
      if (chests.get() && block == Blocks.CHEST) return true;
      if (ores.get()) {
         if (diamonds.get() && block == Blocks.DIAMOND_ORE) return true;
         if (block == Blocks.EMERALD_ORE || block == Blocks.GOLD_ORE
            || block == Blocks.IRON_ORE || block == Blocks.REDSTONE_ORE) return true;
      }
      return false;
   }
}
