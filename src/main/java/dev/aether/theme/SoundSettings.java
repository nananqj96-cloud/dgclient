package dev.aether.theme;

import com.google.gson.JsonObject;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.Setting;
import dev.aether.settings.SliderSetting;
import java.util.List;

public class SoundSettings {
   public final SliderSetting masterVolume = new SliderSetting(
      "Master Volume", "Volume for all client sounds", 60.0, 0.0, 100.0, 5.0, "%"
   );
   public final BooleanSetting guiSounds = new BooleanSetting(
      "GUI Open/Close", "Whoosh when the menu opens and closes", true
   );
   public final BooleanSetting hoverSounds = new BooleanSetting(
      "Hover", "Soft ticks when hovering elements", true
   );
   public final BooleanSetting clickSounds = new BooleanSetting(
      "Clicks & Toggles", "Pops for toggles, sliders and keybinds", true
   );
   public final BooleanSetting notificationSounds = new BooleanSetting(
      "Notifications", "Chimes with toggle toasts", true
   );
   public final BooleanSetting startupAether = new BooleanSetting(
      "Aether", "Startup track candidate", true
   );
   public final BooleanSetting startupSad = new BooleanSetting(
      "Aether Sad Song", "Startup track candidate", false
   );
   public final BooleanSetting startupSong = new BooleanSetting(
      "Aether Song", "Startup track candidate", false
   );
   public final BooleanSetting startupTiki = new BooleanSetting(
      "Aether Tiki Phonk", "Startup track candidate", false
   );
   private final List<Setting<?>> all = List.of(this.masterVolume, this.guiSounds, this.hoverSounds, this.clickSounds, this.notificationSounds);

   public List<Setting<?>> all() {
      return this.all;
   }

   public float volume() {
      return this.masterVolume.getFloat() / 100.0F;
   }

   public JsonObject toJson() {
      JsonObject var1 = new JsonObject();

      for (Setting var3 : this.all) {
         var1.add(var3.getName(), var3.toJson());
      }

      return var1;
   }

   public void fromJson(JsonObject var1) {
      for (Setting var3 : this.all) {
         if (var1.has(var3.getName())) {
            var3.fromJson(var1.get(var3.getName()));
         }
      }
   }
}
