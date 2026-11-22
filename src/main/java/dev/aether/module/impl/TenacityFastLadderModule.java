package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityFastLadderModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Climb speed", 2.0, 1.0, 5.0, 0.1));

   public TenacityFastLadderModule() {
      super("FastLadder", "Faster ladder climbing", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      var pos = mc.player.getBlockPos();
      if (mc.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LADDER) && mc.options.keyUp.isDown()) {
         mc.player.setPos(mc.player.getX(), mc.player.getY() + speed.getFloat(), mc.player.getZ());
      }
   }
}
