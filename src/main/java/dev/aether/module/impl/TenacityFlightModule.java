package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityFlightModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Flight speed", 10.0, 1.0, 50.0, 1.0));
   public final SliderSetting height = this.addSetting(new SliderSetting("Height", "Flight height", 10.0, 1.0, 50.0, 1.0));
   public final BooleanSetting creative = this.addSetting(new BooleanSetting("Creative", "Only fly in creative mode", false));
   private boolean active;

   public TenacityFlightModule() {
      super("Flight", "Enables free flight", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (creative.get() && !mc.player.getAbilities().mayfly) { active = false; return; }
      active = true;
      mc.player.getAbilities().mayfly = true;
      mc.player.getAbilities().flying = true;
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.getAbilities().mayfly = false;
         mc.player.getAbilities().flying = false;
      }
      active = false;
   }
}
