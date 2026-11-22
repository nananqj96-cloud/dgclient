package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityQuickStopModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Stop speed", 0.5, 0.1, 2.0, 0.1));

   public TenacityQuickStopModule() {
      super("QuickStop", "Quickly stops horizontal momentum", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (mc.options.keySprint.isDown() && mc.options.keyUp.isDown() && mc.player.onGround()) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(1.0 - speed.getFloat(), 1.0, 1.0 - speed.getFloat()));
      }
   }
}
