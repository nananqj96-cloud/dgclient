package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class TenacityAutoJumpModule extends Module {
   private boolean pressing;

   public TenacityAutoJumpModule() {
      super("Tenacity Auto Jump", "Jumps for you while you walk (Tenacity port)", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.currentScreen != null) {
         if (this.pressing) {
            mc.options.jumpKey.setPressed(false);
            this.pressing = false;
         }
         return;
      }
      Options opts = mc.options;
      if (mc.player.isOnGround() && opts.forwardKey.isPressed()) {
         opts.jumpKey.setPressed(true);
         this.pressing = true;
      } else if (this.pressing) {
         opts.jumpKey.setPressed(false);
         this.pressing = false;
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.jumpKey.setPressed(false);
      this.pressing = false;
   }
}
