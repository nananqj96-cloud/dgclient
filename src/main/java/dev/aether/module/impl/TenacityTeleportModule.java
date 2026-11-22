package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityTeleportModule extends Module {
   public final SliderSetting distance = this.addSetting(new SliderSetting("Distance", "Teleport distance", 10.0, 1.0, 100.0, 1.0, "m"));
   public final BooleanSetting onJump = this.addSetting(new BooleanSetting("OnJump", "Teleport on jump key", true));

   public TenacityTeleportModule() {
      super("Teleport", "Teleports you forward on jump", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (onJump.get() && mc.options.keyJump.isDown() && mc.player.isOnGround()) {
         float yaw = mc.player.getYRot() * 0.017453292F;
         mc.player.setPos(mc.player.getX() - Math.sin(yaw) * distance.getFloat(),
            mc.player.getY(),
            mc.player.getZ() + Math.cos(yaw) * distance.getFloat());
      }
   }
}
