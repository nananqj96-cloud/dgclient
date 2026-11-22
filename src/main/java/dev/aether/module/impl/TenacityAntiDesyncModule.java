package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAntiDesyncModule extends Module {
   public final SliderSetting lag = this.addSetting(new SliderSetting("Lag", "Lag backtrack ticks", 5.0, 1.0, 30.0, 1.0, "t"));
   public final BooleanSetting rotation = this.addSetting(new BooleanSetting("Rotation", "Lock rotation to last sent", true));

   private float lastYaw, lastPitch;

   public TenacityAntiDesyncModule() {
      super("AntiDesync", "Keeps your position and rotation synced to the server", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (rotation.get()) {
         mc.player.setYRot(lastYaw);
         mc.player.setXRot(lastPitch);
      }
      lastYaw = mc.player.getYRot();
      lastPitch = mc.player.getXRot();
   }
}
