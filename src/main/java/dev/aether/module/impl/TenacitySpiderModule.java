package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacitySpiderModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Climb speed", 0.5, 0.1, 3.0, 0.1));

   public TenacitySpiderModule() {
      super("Spider", "Climb up walls like a spider", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      var pos = mc.player.blockPosition();
      var state = mc.level.getBlockState(pos);
      if (!state.isAir() && mc.player.onGround() && (mc.options.keyJump.isDown())) {
         mc.player.setPos(mc.player.getX(), mc.player.getY() + speed.getFloat(), mc.player.getZ());
      }
   }
}
