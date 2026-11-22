package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

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
      Options opts = mc.options;
      boolean moving = opts.forwardKey.isPressed()
         || (omni.get() && (opts.backKey.isPressed() || opts.leftKey.isPressed() || opts.rightKey.isPressed()));
      if (moving && !mc.player.isSneaking() && mc.player.getHungerManager().getFoodLevel() > 6) {
         opts.sprintKey.setPressed(true);
         mc.player.setSprinting(true);
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.sprintKey.setPressed(false);
   }
}
