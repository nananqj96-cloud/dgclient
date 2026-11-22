package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityNoJumpDelayModule extends Module {
   public final BooleanSetting autoJump = this.addSetting(new BooleanSetting("AutoJump", "Jump immediately on key press", true));

   public TenacityNoJumpDelayModule() {
      super("NoJumpDelay", "Removes jump delay between jumps", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (autoJump.get() && mc.options.keyJump.isDown() && mc.player.onGround()) {
         mc.player.jumpFromGround();
      }
   }
}
