package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityCrasherModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Send crash packets every N ticks", 20.0, 5.0, 100.0, 5.0, "t"));
   public final BooleanSetting crash = this.addSetting(new BooleanSetting("Crash", "Send crash packets", true));
   private int counter;

   public TenacityCrasherModule() {
      super("Crasher", "Sends exploit packets to crash players", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      if (!crash.get()) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (counter > 0) { counter--; return; }
      counter = delay.getInt();
      // Packet sending handled via mixin
   }
}
