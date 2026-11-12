package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.notification.NotificationManager;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public class WeatherNotifierModule extends Module {
   public final BooleanSetting rain = this.addSetting(
      new BooleanSetting("Rain", "Announce when rain starts/stops", true)
   );
   public final BooleanSetting thunder = this.addSetting(
      new BooleanSetting("Thunder", "Announce when a thunderstorm starts/stops", true)
   );
   public final BooleanSetting sound = this.addSetting(
      new BooleanSetting("Sound", "Play a ping on each change", true)
   );
   public final ModeSetting output = this.addSetting(
      new ModeSetting(
         "Output",
         "How the change is shown",
         "Notification",
         "Notification",
         "Chat",
         "Action Bar"
      )
   );
   private Boolean lastRaining;
   private Boolean lastThundering;

   public WeatherNotifierModule() {
      super("WeatherNotifier", "Themed toast when the weather changes", Category.MISC);
   }

   @Override
   protected void onEnable() {
      this.lastRaining = null;
      this.lastThundering = null;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level != null && mc.player != null) {
         boolean raining = level.isRaining();
         boolean thundering = level.isThundering();
         if (this.lastRaining != null && this.lastThundering != null) {
            if (thundering != this.lastThundering) {
               this.lastThundering = thundering;
               if (this.thunder.get()) {
                  if (thundering) {
                     this.notify(
                        mc,
                        "Thunderstorm",
                        "A storm rolls in",
                        NotificationManager.Weather.THUNDER,
                        true
                     );
                  } else {
                     this.notify(
                        mc,
                        "Storm cleared",
                        "The thunder has passed",
                        NotificationManager.Weather.CLEAR,
                        false
                     );
                  }
               }
            }

            if (raining != this.lastRaining) {
               this.lastRaining = raining;
               if (this.rain.get() && !thundering) {
                  if (raining) {
                     this.notify(mc, "Rain", "Rain starts to fall", NotificationManager.Weather.RAIN, true);
                  } else {
                     this.notify(
                        mc,
                        "Skies cleared",
                        "The rain has stopped",
                        NotificationManager.Weather.CLEAR,
                        false
                     );
                  }
               }
            }
         } else {
            this.lastRaining = raining;
            this.lastThundering = thundering;
         }
      }
   }

   private void notify(Minecraft mc, String title, String subtitle, NotificationManager.Weather weather, boolean started) {
      if (mc.player != null) {
         if (this.output.is("Notification")) {
            if (AetherClient.notifications() != null) {
               AetherClient.notifications().pushWeather(title, subtitle, weather, started);
            }
         } else {
            boolean actionBar = this.output.is("Action Bar");
            mc.player.displayClientMessage(Component.literal("§b[Aether] §f" + title + " — " + subtitle), actionBar);
         }

         if (this.sound.get()) {
            float pitch = started ? 1.2F : 0.8F;
            mc.getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value(), pitch, 0.6F));
         }
      }
   }
}
