package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * Creative-style flight for the local player. Moves the player through the air with controlled
 * horizontal and vertical speed, cancelling gravity while enabled.
 *
 * <p><b>BAN WARNING:</b> Fly is a cheating feature. Using it on any server that does not allow
 * flight (which is almost all of them) is bannable. Use at your own risk — this module does not
 * hide the fact that you are flying and anti-cheat / admins can detect it.
 */
public class FlyModule extends Module {
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Horizontal flight speed", 8.0, 1.0, 40.0, 0.5, "m/s")
   );
   public final SliderSetting verticalSpeed = this.addSetting(
      new SliderSetting("Vertical Speed", "Up / down flight speed", 8.0, 1.0, 40.0, 0.5, "m/s")
   );
   public final BooleanSetting warn = this.addSetting(
      new BooleanSetting("Show Ban Warning", "Remind that fly is bannable on most servers", true)
   );

   public FlyModule() {
      super("Fly", "Fly through the air (bannable on most servers)", Category.MISC);
   }

   @Override
   protected void onEnable() {
      if (this.warn.get() && AetherClient.notifications() != null) {
         AetherClient.notifications().push("Fly is bannable — use at your own risk", true);
      }
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) {
         return;
      }

      Input input = player.input.keyPresses;
      // Horizontal facing direction from yaw (Y zeroed).
      double yaw = player.getYRot() * (double) (Math.PI / 180.0);
      double fx = -Math.sin(yaw);
      double fz = -Math.cos(yaw);

      double forward = 0.0;
      double strafe = 0.0;
      if (input.forward()) {
         forward += 1.0;
      }
      if (input.backward()) {
         forward -= 1.0;
      }
      if (input.left()) {
         strafe -= 1.0;
      }
      if (input.right()) {
         strafe += 1.0;
      }

      // Right vector = forward rotated -90° around Y: (fz, 0, -fx)
      double rx = fz;
      double rz = -fx;
      double sx = fx * forward + rx * strafe;
      double sz = fz * forward + rz * strafe;
      double len = Math.sqrt(sx * sx + sz * sz);
      double speed = this.speed.getFloat();
      if (len > 0.001) {
         sx = (sx / len) * speed;
         sz = (sz / len) * speed;
      } else {
         sx = 0.0;
         sz = 0.0;
      }

      double vy = 0.0;
      if (input.jump()) {
         vy = this.verticalSpeed.getFloat();
      } else if (input.shift()) {
         vy = -this.verticalSpeed.getFloat();
      }

      // Cancel gravity and apply controlled motion.
      player.setDeltaMovement(new Vec3(sx, vy, sz));
      player.setOnGround(false);
      player.getAbilities().flying = true;
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.getAbilities().flying = false;
      }
   }
}
