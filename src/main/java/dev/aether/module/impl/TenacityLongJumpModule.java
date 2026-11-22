package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityLongJumpModule extends Module {
   public final SliderSetting power = this.addSetting(new SliderSetting("Power", "Jump power", 2.0, 1.0, 5.0, 0.1));
   public final BooleanSetting auto = this.addSetting(new BooleanSetting("Auto", "Jump automatically on jump key", true));

   public TenacityLongJumpModule() {
      super("LongJump", "Increased jump height and distance", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (mc.player.onGround() && (auto.get() ? mc.options.keyJump.isDown() : false)) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0.0, power.getFloat(), 0.0));
      }
   }
}
