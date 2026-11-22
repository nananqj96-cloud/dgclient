package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAutoPlayModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Play delay ms", 2000.0, 500.0, 10000.0, 500.0, "ms"));
   public final BooleanSetting auto = this.addSetting(new BooleanSetting("Auto", "Auto-play on connect", true));

   private long lastPlay;

   public TenacityAutoPlayModule() {
      super("AutoPlay", "Auto-plays when joining a server", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (auto.get() && System.currentTimeMillis() - lastPlay > delay.getInt() && mc.player.connection.isConnected()) {
         lastPlay = System.currentTimeMillis();
         mc.player.connection.sendChat("/play");
      }
   }
}
