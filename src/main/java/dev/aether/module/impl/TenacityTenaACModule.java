package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTenaACModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "TenaAC detection range", 20.0, 5.0, 100.0, 5.0, "m"));
   public final BooleanSetting autoReport = this.addSetting(new BooleanSetting("AutoReport", "Auto-report detected cheaters", true));

   public TenacityTenaACModule() {
      super("TenaAC", "Detects other Tenacity clients on the server", Category.MISC);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || mc.player.connection == null) return;
      mc.player.connection.sendChat("/tenaac");

   }
}
