package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
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
   private boolean warned;

   public FlyModule() {
      super("Fly", "Fly through the air (bannable on most servers)", Category.MISC);
   }

   @Override
   protected void onEnable() {
      if (this.warn.get() && AetherClient.notifications() != null) {
         AetherClient.notifications().push("Fly is bannable — use at your own risk", true);
         this.warned = true;
      }
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.level == null) {
         return;
      }

      Vec3 input = player.getDeltaMovement();
      // Input direction from the player's movement input (normalized).
      Vec3 dir = player.getDirection();
      float forward = 0.0F;
      float strafe = 0.0F;
      if (player.input.leftPressed) {
         strafe -= 1.0F;
      }
      if (player.input.rightPressed) {
         strafe += 1.0F;
      }
      if (player.input.forwardPressed) {
         forward += 1.0F;
      }
      if (player.input.backPressed) {
         forward -= 1.0F;
      }

      double sx = dir.x() * forward - dir.z() * strafe;
      double sz = dir.z() * forward + dir.x() * strafe;
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
      if (player.input.jumping) {
         vy = this.verticalSpeed.getFloat();
      } else if (player.input.shiftPressed) {
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
