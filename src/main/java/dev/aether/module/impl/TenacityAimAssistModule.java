package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TenacityAimAssistModule extends Module {
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Maximum distance to aim at", 4.5, 2.0, 8.0, 0.25, "")
   );
   public final SliderSetting fov = this.addSetting(
      new SliderSetting("FOV", "Maximum angle from crosshair", 90.0, 10.0, 360.0, 5.0, "°")
   );
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Aim assist speed", 55.0, 5.0, 100.0, 1.0, "%")
   );
   public final SliderSetting smooth = this.addSetting(
      new SliderSetting("Smoothness", "Aim smoothing", 50.0, 0.0, 100.0, 1.0, "%")
   );
   public final BooleanSetting clickOnly = this.addSetting(
      new BooleanSetting("Only While Clicking", "Only assist while attack key is held", true)
   );
   public final BooleanSetting weaponOnly = this.addSetting(
      new BooleanSetting("Weapons Only", "Only assist while holding a weapon", false)
   );
   public final BooleanSetting sticky = this.addSetting(
      new BooleanSetting("Sticky Target", "Keep aiming at the last target", true)
   );
   public final BooleanSetting lineOfSight = this.addSetting(
      new BooleanSetting("Line Of Sight", "Require clear line of sight", true)
   );
   public final BooleanSetting teams = this.addSetting(
      new BooleanSetting("Ignore Teammates", "Don't target teammates", true)
   );
   public final BooleanSetting human = this.addSetting(
      new BooleanSetting("Humanize", "Add random noise to aim movement", true)
   );

   private int lockId = -1;
   private long last = System.nanoTime();
   private float noiseYaw, noisePitch, yawVel, pitchVel, remYaw, remPitch;

   public TenacityAimAssistModule() {
      super("Tenacity Aim Assist", "Eases your aim onto nearby players (Tenacity port)", Category.COMBAT);
   }

   @Override
   protected void onDisable() {
      lockId = -1;
      yawVel = pitchVel = remYaw = remPitch = 0.0F;
   }

   private boolean isWeapon(ItemStack stack) {
      return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)
         || stack.isIn(ItemTags.SPEARS) || stack.getItem() == Items.MACE || stack.getItem() == Items.TRIDENT;
   }

   private Vec3d aimPoint(LivingEntity entity, Vec3d eye, Vec3d look) {
      Box box = entity.getBoundingBox().contract(0.08, 0.1, 0.08);
      Vec3d projected = eye.add(look.multiply(eye.distanceTo(entity.getEyePos()) + 0.5));
      double x = MathHelper.clamp(projected.x, box.minX, box.maxX);
      double y = MathHelper.clamp(projected.y, box.minY + box.getLengthY() * 0.25, box.maxY - box.getLengthY() * 0.1);
      double z = MathHelper.clamp(projected.z, box.minZ, box.maxZ);
      return new Vec3d(x, y, z);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      long now = System.nanoTime();
      float dt = MathHelper.clamp((float)(now - last) / 1.0E9F, 5.0E-4F, 0.1F);
      last = now;

      if (mc.player == null || mc.world == null || mc.currentScreen != null || !mc.player.isAlive() || mc.player.isSpectator()) return;
      if (clickOnly.get() && !mc.options.attackKey.isPressed()) return;
      if (weaponOnly.get() && !isWeapon(mc.player.getMainHandStack())) return;

      Vec3d eye = mc.player.getEyePos();
      Vec3d look = mc.player.getRotationVec(1.0F);
      LivingEntity best = null;
      double bestAngle = fov.get() / 2.0;

      Entity locked = lockId == -1 ? null : mc.world.getEntityById(lockId);
      if (sticky.get() && locked instanceof LivingEntity le && le.isAlive()
         && mc.player.distanceTo(le) <= range.get() + 1.0) {
         best = le;
      } else {
         lockId = -1;
         for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || p.isInvisible()) continue;
            if (teams.get() && p.isTeammate(mc.player)) continue;
            if (mc.player.distanceTo(p) > range.get()) continue;
            if (lineOfSight.get() && !mc.player.canSee(p)) continue;
            Vec3d offset = p.getBoundingBox().getCenter().subtract(eye);
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp(offset.normalize().dotProduct(look), -1.0, 1.0)));
            if (angle < bestAngle) {
               bestAngle = angle;
               best = p;
            }
         }
      }

      if (best == null) {
         yawVel = pitchVel = 0.0F;
      } else if (lineOfSight.get() && !mc.player.canSee(best)) {
         lockId = -1;
      } else {
         lockId = best.getId();
         if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == best) {
            yawVel *= 0.6F;
            pitchVel *= 0.6F;
         } else {
            Vec3d vel = aimPoint(best, eye, look).add(best.getVelocity().multiply(0.6, 0.0, 0.6).multiply(dt * 4.0F));
            double dx = vel.x - eye.x, dy = vel.y - eye.y, dz = vel.z - eye.z;
            float wantYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float wantPitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
            float endY = MathHelper.wrapDegrees(wantYaw - mc.player.getYaw());
            float ep = wantPitch - mc.player.getPitch();
            if (human.get()) {
               noiseYaw += ((float)((Math.random() - 0.5) * 0.35) - noiseYaw * 1.5F * dt);
               noisePitch += ((float)((Math.random() - 0.5) * 0.2) - noisePitch * 1.5F * dt);
               endY += MathHelper.clamp(noiseYaw, -0.8F, 0.8F);
               ep += MathHelper.clamp(noisePitch, -0.5F, 0.5F);
            }
            float accel = 1.0F - (float)Math.exp(-(6.0 + smooth.get() * 0.5) * dt);
            float wantYawVel = MathHelper.clamp(endY * (float)(speed.get() * 0.12), -200.0F, 200.0F);
            float wantPitchVel = MathHelper.clamp(ep * (float)(speed.get() * 0.12), -200.0F, 200.0F);
            yawVel += (wantYawVel - yawVel) * accel;
            pitchVel += (wantPitchVel - pitchVel) * accel;
            float dyaw = yawVel * dt;
            float dpitch = pitchVel * dt;
            if (dyaw != 0.0F || dpitch != 0.0F) {
               mc.player.setYaw(mc.player.getYaw() + dyaw);
               mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + dpitch, -90.0F, 90.0F));
            }
         }
      }
   }
}
