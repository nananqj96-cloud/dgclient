package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TenacityAutoHeadHitterModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Head hit range", 4.0, 1.0, 10.0, 0.5, "m"));
   public final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", "Rotate to head hit", true));

   public TenacityAutoHeadHitterModule() {
      super("AutoHeadHitter", "Aims and hits the target's head automatically", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
         if (mc.player.distanceTo(e) > range.get()) continue;
         if (rotate.get()) {
            double dx = le.getX() - mc.player.getX();
            double dz = le.getZ() - mc.player.getZ();
            double dy = le.getY() + 1.6 - mc.player.getEyeY();
            float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0);
            float pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
         }
         mc.options.keyAttack.setDown(true);
         return;
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keyAttack.setDown(false);
   }
}
