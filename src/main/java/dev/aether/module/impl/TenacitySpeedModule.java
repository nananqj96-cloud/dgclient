package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacitySpeedModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Movement speed multiplier", 5.0, 1.0, 10.0, 0.5, "x"));
   public final BooleanSetting onGround = this.addSetting(new BooleanSetting("On Ground", "Apply on ground", true));
   public final BooleanSetting inAir = this.addSetting(new BooleanSetting("In Air", "Apply in air", true));
   private boolean active;

   public TenacitySpeedModule() {
      super("Speed", "Increases movement speed", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      boolean ground = mc.player.isOnGround();
      boolean should = (ground && onGround.get()) || (!ground && inAir.get());
      if (should) {
         active = true;
         mc.player.setShiftKeyDown(false);
      } else {
         active = false;
      }
   }

   public boolean isActive() { return active; }
   public float getSpeed() { return speed.getFloat(); }
}
