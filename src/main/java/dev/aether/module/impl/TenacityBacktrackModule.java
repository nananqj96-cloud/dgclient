package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class TenacityBacktrackModule extends Module {
   public final SliderSetting ticks = this.addSetting(new SliderSetting("Ticks", "Lag backtrack ticks", 10.0, 1.0, 40.0, 1.0, "t"));
   private int counter;

   public TenacityBacktrackModule() {
      super("Backtrack", "Delays packet sending for anti-aim", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      counter = ticks.getInt();
   }

   @Override
   protected void onDisable() {
      counter = 0;
   }
}
