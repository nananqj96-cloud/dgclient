package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityBlinkModule extends Module {
   public final SliderSetting distance = this.addSetting(new SliderSetting("Distance", "Blink distance", 10.0, 1.0, 50.0, 1.0, "m"));
   private double startX, startY, startZ;
   private boolean active;

   public TenacityBlinkModule() {
      super("Blink", "Teleports you a set distance on jump", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (mc.player.onGround() && mc.options.keyJump.isDown() && !active) {
         startX = mc.player.getX();
         startY = mc.player.getY();
         startZ = mc.player.getZ();
         active = true;
         mc.player.setPos(mc.player.getX(), mc.player.getY() + distance.getFloat(), mc.player.getZ());
      } else if (active && mc.player.onGround()) {
         active = false;
      }
   }
}
