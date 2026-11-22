package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacitySprintModule extends Module {
   public final BooleanSetting omni = this.addSetting(
      new BooleanSetting("Omni Directional", "Sprint in all directions, not just forward", false)
   );

   public TenacitySprintModule() {
      super("Tenacity Sprint", "Always sprint while moving (Tenacity port)", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      boolean moving = mc.options.keyUp.isDown()
         || (omni.get() && (mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()));
      if (moving && !mc.player.isShiftKeyDown() && mc.player.getFoodData().getFoodLevel() > 6) {
         mc.options.keySprint.setDown(true);
         mc.player.setSprinting(true);
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keySprint.setDown(false);
   }
}
