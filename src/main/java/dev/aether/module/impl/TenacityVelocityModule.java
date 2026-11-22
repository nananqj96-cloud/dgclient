package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityVelocityModule extends Module {
   public final SliderSetting horizontal = this.addSetting(new SliderSetting("Horizontal", "Horizontal velocity factor", 100.0, 0.0, 100.0, 5.0, "%"));
   public final SliderSetting vertical = this.addSetting(new SliderSetting("Vertical", "Vertical velocity factor", 100.0, 0.0, 100.0, 5.0, "%"));
   public final BooleanSetting noPush = this.addSetting(new BooleanSetting("No Push", "Disable push from entities", false));
   public final BooleanSetting noWater = this.addSetting(new BooleanSetting("No Water", "Disable water slowdown", false));
   public final BooleanSetting noLava = this.addSetting(new BooleanSetting("No Lava", "Disable lava slowdown", false));

   public TenacityVelocityModule() {
      super("Velocity", "Reduces knockback from explosions and attacks", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      var delta = mc.player.getDeltaMovement();
      if (delta == null) return;
      float h = (float)(horizontal.get() / 100.0);
      float v = (float)(vertical.get() / 100.0);
      mc.player.setDeltaMovement(delta.x * h, delta.y * v, delta.z * h);
   }
}
