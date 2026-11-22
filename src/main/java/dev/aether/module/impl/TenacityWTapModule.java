package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityWTapModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "W-tap range", 5.0, 1.0, 20.0, 0.5, "m"));
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Tap delay ticks", 5.0, 1.0, 30.0, 1.0, "t"));
   public final BooleanSetting sprint = this.addSetting(new BooleanSetting("Sprint", "Sprint on tap", true));

   private int counter;

   public TenacityWTapModule() {
      super("WTap", "Taps W to move towards targets", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      if (counter > 0) { counter--; return; }
      for (var e : mc.level.entitiesForRendering()) {
         if (e == mc.player || !e.isAlive()) continue;
         if (mc.player.distanceTo(e) <= range.get()) {
            mc.options.keyForward.setDown(true);
            counter = delay.getInt();
            return;
         }
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keyForward.setDown(false);
   }
}
