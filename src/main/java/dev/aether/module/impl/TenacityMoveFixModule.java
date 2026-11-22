package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityMoveFixModule extends Module {
   public final BooleanSetting fix = this.addSetting(new BooleanSetting("Fix", "Fix movement desync", true));

   private boolean wasSneaking;

   public TenacityMoveFixModule() {
      super("MoveFix", "Fixes movement prediction desync", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || !fix.get()) return;
      var box = mc.player.getBoundingBox();
      var pos = mc.player.getBlockPos();
      if (mc.level.getBlockState(pos.below()).isAir() && !mc.player.onGround()) {
         mc.player.setOnGround(true);
      }
   }
}
