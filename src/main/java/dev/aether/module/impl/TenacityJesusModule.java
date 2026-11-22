package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityJesusModule extends Module {
   public final SliderSetting distance = this.addSetting(new SliderSetting("Distance", "Fall distance before reset", 10.0, 1.0, 50.0, 1.0, "m"));
   public final BooleanSetting water = this.addSetting(new BooleanSetting("Water", "Walk on water", true));
   private float startY;

   public TenacityJesusModule() {
      super("Jesus", "Walk on water and avoid falling", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (mc.player.isInWater() && water.get()) {
         mc.player.setPos(mc.player.getX(), mc.player.getY() - 0.01, mc.player.getZ());
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.8, 0.0, 0.8));
      }
      if (mc.player.fallDistance > distance.getFloat()) {
         startY = mc.player.getY();
         mc.player.setPos(mc.player.getX(), startY, mc.player.getZ());
         mc.player.fallDistance = 0.0F;
      }
   }
}
