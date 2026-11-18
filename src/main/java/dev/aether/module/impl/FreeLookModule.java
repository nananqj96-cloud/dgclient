package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class FreeLookModule extends Module {
   private static FreeLookModule instance;
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Which entity the mouse rotates.",
         "Player",
         "Player",
         "Camera"
      )
   );
   public final BooleanSetting togglePerspective = this.addSetting(
      new BooleanSetting("Toggle Perspective", "Switch to third person on toggle.", true)
   );
   public final BooleanSetting throughWalls = this.addSetting(
      new BooleanSetting(
         "Through Walls",
         "See through walls — the third-person camera ignores wall collision.",
         false
      )
   );
   public final SliderSetting sensitivity = this.addSetting(
      new SliderSetting(
         "Camera Sensitivity", "How fast the camera moves in Camera mode.", 8.0, 0.0, 10.0, 0.1
      )
   );
   public final BooleanSetting arrows = this.addSetting(
      new BooleanSetting(
         "Arrows Control Opposite",
         "Control the other entity's rotation with the arrow keys.",
         true
      )
   );
   public final SliderSetting arrowSpeed = this.addSetting(
      new SliderSetting("Arrow Speed", "Rotation speed with the arrow keys.", 4.0, 0.0, 10.0, 0.5)
   );
   private float cameraYaw;
   private float cameraPitch;
   private CameraType prePers;

   public FreeLookModule() {
      super("FreeLook", "Allows more rotation options in third person.", Category.MISC);
      instance = this;
   }

   public static FreeLookModule get() {
      return instance;
   }

   @Override
   protected void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         this.cameraYaw = mc.player.getYRot();
         this.cameraPitch = mc.player.getXRot();
         this.prePers = mc.options.getCameraType();
         if (this.prePers != CameraType.THIRD_PERSON_BACK && this.togglePerspective.get()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         }
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (this.prePers != null && mc.options.getCameraType() != this.prePers && this.togglePerspective.get()) {
         mc.options.setCameraType(this.prePers);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (this.arrows.get()) {
            long win = mc.getWindow().handle();
            boolean left = GLFW.glfwGetKey(win, 263) == 1;
            boolean right = GLFW.glfwGetKey(win, 262) == 1;
            boolean up = GLFW.glfwGetKey(win, 265) == 1;
            boolean down = GLFW.glfwGetKey(win, 264) == 1;
            int iterations = (int)(this.arrowSpeed.get() * 2.0);

            for (int i = 0; i < iterations; i++) {
               if (this.mode.is("Player")) {
                  if (left) {
                     this.cameraYaw -= 0.5F;
                  }

                  if (right) {
                     this.cameraYaw += 0.5F;
                  }

                  if (up) {
                     this.cameraPitch -= 0.5F;
                  }

                  if (down) {
                     this.cameraPitch += 0.5F;
                  }
               } else {
                  float yaw = mc.player.getYRot();
                  float pitch = mc.player.getXRot();
                  if (left) {
                     yaw -= 0.5F;
                  }

                  if (right) {
                     yaw += 0.5F;
                  }

                  if (up) {
                     pitch -= 0.5F;
                  }

                  if (down) {
                     pitch += 0.5F;
                  }

                  mc.player.setYRot(yaw);
                  mc.player.setXRot(pitch);
               }
            }
         }

         mc.player.setXRot(Mth.clamp(mc.player.getXRot(), -90.0F, 90.0F));
         this.cameraPitch = Mth.clamp(this.cameraPitch, -90.0F, 90.0F);
      }
   }

   public boolean isActive() {
      return this.isEnabled() && Minecraft.getInstance().player != null;
   }

   public boolean seeThroughWalls() {
      return this.isActive() && this.throughWalls.get();
   }

   public boolean cameraMode() {
      return this.isActive() && this.mode.is("Camera");
   }

   public boolean playerMode() {
      return this.isActive()
         && Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK
         && this.mode.is("Player");
   }

   public void addCameraLook(double deltaX, double deltaY) {
      float sens = this.sensitivity.getFloat();
      if (sens <= 0.0F) {
         sens = 1.0F;
      }

      this.cameraYaw += (float)(deltaX / (double)sens);
      this.cameraPitch += (float)(deltaY / (double)sens);
      if (Math.abs(this.cameraPitch) > 90.0F) {
         this.cameraPitch = this.cameraPitch > 0.0F ? 90.0F : -90.0F;
      }
   }

   public float getCameraYaw() {
      return this.cameraYaw;
   }

   public float getCameraPitch() {
      return this.cameraPitch;
   }
}
