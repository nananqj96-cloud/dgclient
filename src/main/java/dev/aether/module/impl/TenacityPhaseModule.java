package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityPhaseModule extends Module {
   public final BooleanSetting onGround = this.addSetting(new BooleanSetting("OnGround", "Phase through ground", true));

   public TenacityPhaseModule() {
      super("Phase", "Phase through blocks and the ground", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      var box = mc.player.getBoundingBox();
      if (onGround.get() && mc.player.isOnGround()) {
         mc.player.setPos(mc.player.getX(), mc.player.getY() - 0.01, mc.player.getZ());
      }
   }
}
