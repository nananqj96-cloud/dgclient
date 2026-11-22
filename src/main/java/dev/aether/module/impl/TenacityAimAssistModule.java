package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

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
      if (stack.isEmpty()) return false;
      Item item = stack.getItem();
      return item == Items.MACE || item == Items.TRIDENT
         || item == Items.WOODEN_SWORD || item == Items.STONE_SWORD
         || item == Items.GOLDEN_SWORD || item == Items.IRON_SWORD
         || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
         || item == Items.WOODEN_AXE || item == Items.STONE_AXE
         || item == Items.GOLDEN_AXE || item == Items.IRON_AXE
         || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE;
   }

   private Vec3 aimPoint(LivingEntity entity, Vec3 eye, Vec3 look) {
      AABB box = entity.getBoundingBox();
      Vec3 projected = eye.add(look.scale(eye.distanceTo(entity.getEyePosition()) + 0.5));
      double x = Mth.clamp(projected.x, box.minX, box.maxX);
      double y = Mth.clamp(projected.y, box.minY + (box.maxY - box.minY) * 0.25, box.maxY - (box.maxY - box.minY) * 0.1);
      double z = Mth.clamp(projected.z, box.minZ, box.maxZ);
      return new Vec3(x, y, z);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      long now = System.nanoTime();
      float dt = Mth.clamp((float)(now - last) / 1.0E9F, 5.0E-4F, 0.1F);
      last = now;

      if (mc.player == null || mc.level == null || mc.screen != null || !mc.player.isAlive() || mc.player.isSpectator()) return;
      if (clickOnly.get() && !mc.options.keyAttack.isDown()) return;
      if (weaponOnly.get() && !isWeapon(mc.player.getMainHandItem())) return;

      Vec3 eye = mc.player.getEyePosition();
      Vec3 look = mc.player.getRotationVector();
      LivingEntity best = null;
      double bestAngle = fov.get() / 2.0;

      Entity locked = lockId == -1 ? null : mc.level.getEntity(lockId);
      if (sticky.get() && locked instanceof LivingEntity le && le.isAlive()
         && mc.player.distanceTo(le) <= range.get() + 1.0) {
         best = le;
      } else {
         lockId = -1;
         for (AbstractClientPlayer p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || p.isInvisible()) continue;
            if (teams.get() && p.getTeam() != null && p.getTeam().equals(mc.player.getTeam())) continue;
            if (mc.player.distanceTo(p) > range.get()) continue;
            Vec3 offset = p.getBoundingBox().getCenter().subtract(eye);
            double angle = Math.toDegrees(Math.acos(Mth.clamp(offset.normalize().dot(look), -1.0, 1.0)));
            if (angle < bestAngle) {
               bestAngle = angle;
               best = p;
            }
         }
      }

      if (best == null) {
         yawVel = pitchVel = 0.0F;
      } else {
         lockId = best.getId();
         if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() == best) {
            yawVel *= 0.6F;
            pitchVel *= 0.6F;
         } else {
            Vec3 vel = aimPoint(best, eye, look).add(best.getDeltaMovement().multiply(0.6, 0.0, 0.6).scale(dt * 4.0F));
            double dx = vel.x - eye.x, dy = vel.y - eye.y, dz = vel.z - eye.z;
            float wantYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float wantPitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
            float endY = Mth.wrapDegrees(wantYaw - mc.player.getYRot());
            float ep = wantPitch - mc.player.getXRot();
            if (human.get()) {
               noiseYaw += ((float)((Math.random() - 0.5) * 0.35) - noiseYaw * 1.5F * dt);
               noisePitch += ((float)((Math.random() - 0.5) * 0.2) - noisePitch * 1.5F * dt);
               endY += Mth.clamp(noiseYaw, -0.8F, 0.8F);
               ep += Mth.clamp(noisePitch, -0.5F, 0.5F);
            }
            float accel = 1.0F - (float)Math.exp(-(6.0 + smooth.get() * 0.5) * dt);
            float wantYawVel = Mth.clamp(endY * (float)(speed.get() * 0.12), -200.0F, 200.0F);
            float wantPitchVel = Mth.clamp(ep * (float)(speed.get() * 0.12), -200.0F, 200.0F);
            yawVel += (wantYawVel - yawVel) * accel;
            pitchVel += (wantPitchVel - pitchVel) * accel;
            float dyaw = yawVel * dt;
            float dpitch = pitchVel * dt;
            if (dyaw != 0.0F || dpitch != 0.0F) {
               mc.player.setYRot(mc.player.getYRot() + dyaw);
               mc.player.setXRot(Mth.clamp(mc.player.getXRot() + dpitch, -90.0F, 90.0F));
            }
         }
      }
   }
}
