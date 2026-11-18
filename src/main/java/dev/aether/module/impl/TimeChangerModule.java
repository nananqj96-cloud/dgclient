package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;

/**
 * Client-side time of day. Lets you force day, noon, sunset, night or midnight regardless of the
 * server's actual time, for a better-looking world at any moment.
 */
public class TimeChangerModule extends Module {
   public final ModeSetting time = this.addSetting(
      new ModeSetting("Time", "Client-side time of day", "Day", "Day", "Noon", "Sunset", "Night", "Midnight")
   );

   public TimeChangerModule() {
      super("Time Changer", "Force a client-side time of day", Category.MISC);
   }

   public long timeOfDay() {
      String t = this.time.get();
      return switch (t) {
         case "Noon" -> 6000L;
         case "Sunset" -> 12000L;
         case "Night" -> 14000L;
         case "Midnight" -> 18000L;
         default -> 1000L;
      };
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         mc.level.setDayTime(this.timeOfDay());
      }
   }
}
