package dev.aether.module.impl;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.aether.mixin.KeyMappingAccessor;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class AutoWalkModule extends Module {
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Walking mode.",
         "Simple",
         "Simple",
         "Smart"
      )
   );
   public final ModeSetting direction = this.addSetting(
      new ModeSetting(
         "Direction",
         "The direction to walk in Simple mode.",
         "Forwards",
         "Forwards",
         "Backwards",
         "Left",
         "Right"
      )
   );
   public final BooleanSetting disableOnInput = this.addSetting(
      new BooleanSetting(
         "Disable On Input", "Disable the module on manual movement input.", false
      )
   );
   public final BooleanSetting disableOnY = this.addSetting(
      new BooleanSetting(
         "Disable On Y Change", "Disable the module if you move vertically.", false
      )
   );
   public final BooleanSetting waitForChunks = this.addSetting(
      new BooleanSetting("No Unloaded Chunks", "Do not walk into unloaded chunks.", true)
   );

   public AutoWalkModule() {
      super("AutoWalk", "Automatically walks forward.", Category.MISC);
      this.direction.visibleWhen(() -> this.mode.is("Simple"));
      this.disableOnY.visibleWhen(() -> this.mode.is("Simple"));
      this.waitForChunks.visibleWhen(() -> this.mode.is("Simple"));
   }

   @Override
   protected void onDisable() {
      this.release(Minecraft.getInstance());
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         this.release(mc);
         if (this.mode.is("Smart")) {
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
            if (mc.player.horizontalCollision && mc.player.onGround()) {
               mc.options.keyJump.setDown(true);
            }
         } else if (this.disableOnY.get() && mc.player.yo != mc.player.getY()) {
            this.toggle();
         } else if (!this.waitForChunks.get() || this.chunkAheadLoaded(mc)) {
            String var2 = this.direction.get();
            switch (var2) {
               case "Forwards":
                  mc.options.keyUp.setDown(true);
                  break;
               case "Backwards":
                  mc.options.keyDown.setDown(true);
                  break;
               case "Left":
                  mc.options.keyLeft.setDown(true);
                  break;
               case "Right":
                  mc.options.keyRight.setDown(true);
            }
         }
      } else {
         this.release(mc);
      }
   }

   @Override
   public boolean onKeyPress(int keyCode) {
      Minecraft mc = Minecraft.getInstance();
      if (this.disableOnInput.get() && mc.screen == null && this.isMovementKey(mc, keyCode)) {
         this.toggle();
      }

      return false;
   }

   private boolean chunkAheadLoaded(Minecraft mc) {
      double yawRad = Math.toRadians((double)mc.player.getYRot());
      double fx = -Math.sin(yawRad);
      double fz = Math.cos(yawRad);
      String direction = this.direction.get();
      double dx;
      double dz;
      switch (direction) {
         case "Backwards":
            dx = -fx;
            dz = -fz;
            break;
         case "Left":
            dx = -fz;
            dz = fx;
            break;
         case "Right":
            dx = fz;
            dz = -fx;
            break;
         default:
            dx = fx;
            dz = fz;
      }

      int bx = (int)Math.floor(mc.player.getX() + dx * 2.0);
      int bz = (int)Math.floor(mc.player.getZ() + dz * 2.0);
      return mc.level.getChunkSource().hasChunk(bx >> 4, bz >> 4);
   }

   private void release(Minecraft mc) {
      if (mc.options != null) {
         mc.options.keyUp.setDown(false);
         mc.options.keyDown.setDown(false);
         mc.options.keyLeft.setDown(false);
         mc.options.keyRight.setDown(false);
         mc.options.keyJump.setDown(false);
         mc.options.keySprint.setDown(false);
      }
   }

   private boolean isMovementKey(Minecraft mc, int keyCode) {
      Options o = mc.options;
      return matches(o.keyUp, keyCode)
         || matches(o.keyDown, keyCode)
         || matches(o.keyLeft, keyCode)
         || matches(o.keyRight, keyCode)
         || matches(o.keyJump, keyCode)
         || matches(o.keyShift, keyCode);
   }

   private static boolean matches(KeyMapping km, int keyCode) {
      Key key = ((KeyMappingAccessor)km).aetherclient$getKey();
      return key.getType() == Type.KEYSYM && key.getValue() == keyCode;
   }
}
