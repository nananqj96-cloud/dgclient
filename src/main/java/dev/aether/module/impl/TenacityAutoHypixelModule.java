package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAutoHypixelModule extends Module {
   public final SliderSetting retryDelay = this.addSetting(new SliderSetting("Retry Delay", "Reconnection delay ms", 3000.0, 1000.0, 10000.0, 500.0, "ms"));
   public final BooleanSetting autoJoin = this.addSetting(new BooleanSetting("AutoJoin", "Auto-join Hypixel on connect", true));
   public final BooleanSetting retry = this.addSetting(new BooleanSetting("Retry", "Retry on disconnect", true));

   private long lastRetry;

   public TenacityAutoHypixelModule() {
      super("AutoHypixel", "Auto-joins Hypixel on server connect", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (retry.get() && mc.player.connection != null && System.currentTimeMillis() - lastRetry > retryDelay.getInt()) {
         lastRetry = System.currentTimeMillis();
         mc.setScreen(null);
      }
   }
}
