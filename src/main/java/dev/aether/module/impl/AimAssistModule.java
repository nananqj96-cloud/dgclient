package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;

public class AimAssistModule extends Module {
   public final SliderSetting range = this.addSetting(
      new SliderSetting(
         "Range", "Assist any target within this distance", 4.0, 1.0, 8.0, 0.5, "m"
      )
   );
   public final SliderSetting speed = this.addSetting(
      new SliderSetting(
         "Speed", "How fast the aim is pulled (higher = snappier)", 4.0, 1.0, 10.0, 1.0
      )
   );
   public final SliderSetting smoothness = this.addSetting(
      new SliderSetting(
         "Smoothness",
         "How eased / human the pull is (higher = smoother)",
         6.0,
         1.0,
         10.0,
         1.0
      )
   );
   public final ModeSetting targetPart = this.addSetting(
      new ModeSetting(
         "Target",
         "Which part of the target to aim at",
         "Body",
         "Head",
         "Body",
         "Feet",
         "Nearest"
      )
   );
   public final SliderSetting fov = this.addSetting(
      new SliderSetting(
         "FOV",
         "Only assist within this facing cone (180 = all around)",
         180.0,
         10.0,
         180.0,
         5.0,
         "°"
      )
   );
   public final BooleanSetting vertical = this.addSetting(
      new BooleanSetting("Vertical", "Also correct pitch (up/down), not just yaw", true)
   );
   public final BooleanSetting players = this.addSetting(
      new BooleanSetting("Players", "Target other players", true)
   );
   public final BooleanSetting hostiles = this.addSetting(
      new BooleanSetting("Hostiles", "Target hostile mobs", false)
   );
   public final BooleanSetting passive = this.addSetting(
      new BooleanSetting("Passive", "Target passive mobs", false)
   );
   public final BooleanSetting invisibles = this.addSetting(
      new BooleanSetting("Invisibles", "Also target invisible entities", false)
   );
   public final BooleanSetting wallCheck = this.addSetting(
      new BooleanSetting("Wall Check", "Only assist targets you can actually see", false)
   );
   public final BooleanSetting alwaysActive = this.addSetting(
      new BooleanSetting(
         "Always Active", "Assist all the time — off = only while attacking (holding left-click)", true
      )
   );
   public final BooleanSetting sticky = this.addSetting(
      new BooleanSetting("Sticky Target", "Keep one target until it leaves range", true)
   );

   public AimAssistModule() {
      super(
         "AimAssist", "Legit aim assist — smoothly pulls toward targets in range", Category.COMBAT
      );
   }

   @Override
   public void onTick() {
   }

   public double[] computePixels(double dt, double userDX, double userDY) {
      return null;
   }
}
