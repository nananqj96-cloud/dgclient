package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTickBaseModule extends Module {
   public final SliderSetting ticks = this.addSetting(new SliderSetting("Ticks", "Tick base delay", 0.0, 0.0, 100.0, 1.0, "t"));

   public TenacityTickBaseModule() {
      super("TickBase", "Delays packet processing for anti-cheats", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(1.02, 1.0, 1.02));

   }
}
