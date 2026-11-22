package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public class TenacityKillAuraModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Target range", 5.0, 1.0, 10.0, 0.5, "m"));
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Attack cooldown in ticks", 0.0, 0.0, 20.0, 1.0, "t"));
   public final SliderSetting fov = this.addSetting(new SliderSetting("FOV", "Field of view limit", 180.0, 10.0, 360.0, 5.0, "°"));
   public final BooleanSetting players = this.addSetting(new BooleanSetting("Players", "Target players", true));
   public final BooleanSetting hostiles = this.addSetting(new BooleanSetting("Hostiles", "Target hostile mobs", false));
   public final BooleanSetting multi = this.addSetting(new BooleanSetting("Multi", "Attack multiple targets", false));
   public final BooleanSetting raycast = this.addSetting(new BooleanSetting("Raycast", "Require line of sight", true));
   public final ModeSetting sort = this.addSetting(new ModeSetting("Sort", "Target sorting", "Distance", "Distance", "Angle", "Health"));

   private int tickCounter;

   public TenacityKillAuraModule() {
      super("KillAura", "Automatically attacks entities in range", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      if (tickCounter > 0) { tickCounter--; return; }
      if (delay.get() > 0) { tickCounter = delay.getInt(); return; }

      Vec3 eye = mc.player.getEyePosition();
      float yawRad = (float)Math.toRadians(mc.player.getYRot());
      float pitchRad = (float)Math.toRadians(mc.player.getXRot());
      Vec3 look = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));

      int maxTargets = multi.get() ? 4 : 1;
      int targets = 0;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || le.isSpectator()) continue;
         if (e instanceof Player && !players.get()) continue;
         if (!(e instanceof Player) && !hostiles.get()) continue;
         if (mc.player.distanceTo(e) > range.get()) continue;

         double angle = Math.toDegrees(Math.acos(Mth.clamp(
            e.getEyePosition().subtract(eye).normalize().dot(look), -1.0, 1.0)));
        if (angle > fov.get() / 2.0) continue;
        if (raycast.get() && !canSee(mc.player, e)) continue;

         mc.options.keyAttack.setDown(true);
         tickCounter = 1;
         targets++;
         if (targets >= maxTargets) break;
      }
      if (targets == 0) mc.options.keyAttack.setDown(false);
   }

   private boolean canSee(Player p, Entity e) {
      Vec3 from = p.getEyePosition();
      Vec3 to = e.getEyePosition();
      Vec3 dir = to.subtract(from);
      double dist = dir.length();
      dir = dir.normalize();
      for (double d = 0.5; d < dist; d += 0.5) {
         AABB box = new AABB(from.x + dir.x * d - 0.3, from.y + dir.y * d - 0.3, from.z + dir.z * d - 0.3,
            from.x + dir.x * d + 0.3, from.y + dir.y * d + 0.3, from.z + dir.z * d + 0.3);
         if (!mc_level(p).getBlockState(new net.minecraft.core.BlockPos((int)box.minX, (int)box.minY, (int)box.minZ)).isAir()) return false;
      }
      return true;
   }

   private static net.minecraft.world.level.Level mc_level(Player p) {
      return p.level();
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keyAttack.setDown(false);
   }
}
