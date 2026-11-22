package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityNoRotateModule extends Module {
   public final BooleanSetting reset = this.addSetting(new BooleanSetting("Reset", "Reset rotation to last valid", true));

   private float lastYaw, lastPitch;

   public TenacityNoRotateModule() {
      super("NoRotate", "Prevent server rotation packets from moving your view", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      lastYaw = mc.player.getYRot();
      lastPitch = mc.player.getXRot();
      if (reset.get()) {
         mc.player.setYRot(lastYaw);
         mc.player.setXRot(lastPitch);
      }
   }
}
