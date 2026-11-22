package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

public class TenacityTargetStrafeModule extends Module {
   public final SliderSetting angle = this.addSetting(new SliderSetting("Angle", "Strafing angle", 90.0, 45.0, 180.0, 5.0, "°"));
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Strafe speed", 1.0, 0.1, 3.0, 0.1));
   public final BooleanSetting clockwise = this.addSetting(new BooleanSetting("Clockwise", "Strafe direction", true));

   public TenacityTargetStrafeModule() {
      super("TargetStrafe", "Strafe around your target during combat", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      LivingEntity target = null;
      double minDist = Double.MAX_VALUE;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
         double d = mc.player.distanceTo(e);
         if (d < minDist && d < 15.0) { minDist = d; target = le; }
      }
      if (target == null) return;
      double dx = target.getX() - mc.player.getX();
      double dz = target.getZ() - mc.player.getZ();
      float yaw = (float)(Math.atan2(dz, dx) - Math.PI * 0.5);
      float strafeYaw = yaw + (clockwise.get() ? 1.0F : -1.0F) * Mth.degToRad(angle.getFloat());
      double dist = minDist;
      mc.player.setPos(mc.player.getX() + Math.cos(strafeYaw) * speed.getFloat() * 0.5,
         mc.player.getY(), mc.player.getZ() + Math.sin(strafeYaw) * speed.getFloat() * 0.5);
   }
}
