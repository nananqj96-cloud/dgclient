package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityNoSlowModule extends Module {
   public final BooleanSetting water = this.addSetting(new BooleanSetting("Water", "Disable water slowdown", true));
   public final BooleanSetting soul = this.addSetting(new BooleanSetting("Soul", "Disable soul sand slowdown", true));
   public final SliderSetting strength = this.addSetting(new SliderSetting("Strength", "Slowdown reduction", 100.0, 0.0, 100.0, 5.0, "%"));

   private boolean wasInWater;
   private boolean wasInSoul;

   public TenacityNoSlowModule() {
      super("NoSlow", "Prevents movement slowdown from water and soul sand", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      boolean inWater = mc.player.isInWater();
      boolean inSoul = false;
      var block = mc.level.getBlockState(mc.player.getBlockPos().below());
      if (block.is(net.minecraft.world.level.block.Blocks.SOUL_SAND) || block.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
         inSoul = true;
      }
      if (water.get() && inWater && !wasInWater) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(strength.getFloat() / 100.0, strength.getFloat() / 100.0, strength.getFloat() / 100.0));
      }
      if (soul.get() && inSoul && !wasInSoul) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(strength.getFloat() / 100.0, 1.0, strength.getFloat() / 100.0));
      }
      wasInWater = inWater;
      wasInSoul = inSoul;
   }
}
