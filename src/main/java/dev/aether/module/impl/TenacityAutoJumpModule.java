package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.client.Minecraft;

public class TenacityAutoJumpModule extends Module {
   private boolean pressing;

   public TenacityAutoJumpModule() {
      super("Tenacity Auto Jump", "Jumps for you while you walk (Tenacity port)", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) {
         if (this.pressing) {
            mc.options.keyJump.setDown(false);
            this.pressing = false;
         }
         return;
      }
      if (mc.player.onGround() && mc.options.keyUp.isDown()) {
         mc.options.keyJump.setDown(true);
         this.pressing = true;
      } else if (this.pressing) {
         mc.options.keyJump.setDown(false);
         this.pressing = false;
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keyJump.setDown(false);
      this.pressing = false;
   }
}
