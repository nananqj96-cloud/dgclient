package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TenacityCombatBotModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Target range", 5.0, 1.0, 20.0, 0.5, "m"));
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Attack delay", 0.0, 0.0, 20.0, 1.0, "t"));
   public final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", "Rotate to target", true));
   private int counter;

   public TenacityCombatBotModule() {
      super("CombatBot", "Auto-attacks nearby entities with rotation", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      if (counter > 0) { counter--; return; }
      LivingEntity target = null;
      double minD = Double.MAX_VALUE;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
         double d = mc.player.distanceTo(e);
         if (d < minD && d <= range.get()) { minD = d; target = le; }
      }
      if (target == null) return;
      if (rotate.get()) {
         double dx = target.getX() - mc.player.getX();
         double dz = target.getZ() - mc.player.getZ();
         float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0);
         float pitch = (float)(-Math.toDegrees(Math.atan2(target.getY() - mc.player.getEyeY(), Math.hypot(dx, dz))));
         mc.player.setYRot(yaw);
         mc.player.setXRot(pitch);
      }
      mc.options.keyAttack.setDown(true);
      counter = delay.getInt() + 1;
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keyAttack.setDown(false);
   }
}
