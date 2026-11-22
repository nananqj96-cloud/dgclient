package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TenacityAutoHeadModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Target range", 4.0, 1.0, 10.0, 0.5, "m"));
   public final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", "Rotate to head", true));

   public TenacityAutoHeadModule() {
      super("AutoHead", "Aims at the target's head during combat", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      LivingEntity target = null;
      double minD = Double.MAX_VALUE;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
         double d = mc.player.distanceTo(e);
         if (d < minD && d <= range.get()) { minD = d; target = le; }
      }
      if (target == null || !rotate.get()) return;
      double dx = target.getX() - mc.player.getX();
      double dz = target.getZ() - mc.player.getZ();
      double dy = target.getY() + 1.6 - mc.player.getEyeY();
      float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0);
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
      mc.player.setYRot(yaw);
      mc.player.setXRot(pitch);
   }
}
