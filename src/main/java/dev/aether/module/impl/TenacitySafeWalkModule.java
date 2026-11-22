package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacitySafeWalkModule extends Module {
   public final BooleanSetting edge = this.addSetting(new BooleanSetting("Edge", "Prevent walking off edges", true));
   public final BooleanSetting liquid = this.addSetting(new BooleanSetting("Liquid", "Prevent walking into liquids", true));

   public TenacitySafeWalkModule() {
      super("SafeWalk", "Prevents walking into dangerous blocks", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (!mc.player.isOnGround()) return;
      var box = mc.player.getBoundingBox();
      var pos = mc.player.getBlockPos();
      var ahead = mc.level.getBlockState(pos.offset(mc.player.getX() < mc.player.getX() + mc.player.getDeltaMovement().getX() ? 1 : -1, -1, 0));
      if (edge.get() && ahead.isAir()) {
         mc.options.keyForward.setDown(false);
      }
   }
}
