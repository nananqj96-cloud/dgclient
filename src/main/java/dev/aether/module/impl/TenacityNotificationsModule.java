package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityNotificationsModule extends Module {
   public final SliderSetting duration = this.addSetting(new SliderSetting("Duration", "Notification duration ms", 2000.0, 500.0, 5000.0, 100.0, "ms"));
   public final BooleanSetting connect = this.addSetting(new BooleanSetting("Connect", "Notify on server join", true));
   public final BooleanSetting disconnect = this.addSetting(new BooleanSetting("Disconnect", "Notify on server disconnect", true));
   public final BooleanSetting death = this.addSetting(new BooleanSetting("Death", "Notify on death", true));

   public TenacityNotificationsModule() {
      super("Notifications", "Shows in-game notifications for events", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Notification rendering handled by HUD overlay
   }
}
