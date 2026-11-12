package dev.aether.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.settings.Setting;
import dev.aether.theme.ThemeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final Path file = FabricLoader.getInstance().getConfigDir().resolve("aetherclient.json");
   private final ModuleManager modules;
   private final ThemeManager themes;
   private final Map<String, ConfigManager.Section> sections = new LinkedHashMap<>();

   public ConfigManager(ModuleManager var1, ThemeManager var2) {
      this.modules = var1;
      this.themes = var2;
   }

   public void addSection(String var1, Supplier<JsonObject> var2, Consumer<JsonObject> var3) {
      this.sections.put(var1, new ConfigManager.Section(var2, var3));
   }

   public JsonObject captureState() {
      JsonObject var1 = new JsonObject();
      var1.add("theme", this.themes.toJson());
      JsonObject var2 = new JsonObject();

      for (Module var4 : this.modules.all()) {
         JsonObject var5 = new JsonObject();
         var5.addProperty("enabled", var4.isEnabled());
         var5.add("keybind", var4.getKeybind().toJson());
         JsonObject var6 = new JsonObject();

         for (Setting var8 : var4.getSettings()) {
            var6.add(var8.getName(), var8.toJson());
         }

         var5.add("settings", var6);
         var2.add(var4.getName() + "@" + var4.getCategory().name(), var5);
      }

      var1.add("modules", var2);

      for (Entry var10 : this.sections.entrySet()) {
         var1.add((String)var10.getKey(), (JsonElement)((ConfigManager.Section)var10.getValue()).save().get());
      }

      return var1;
   }

   public void applyState(JsonObject var1) {
      if (var1 != null) {
         if (var1.has("theme") && var1.get("theme").isJsonObject()) {
            this.themes.fromJson(var1.getAsJsonObject("theme"));
         }

         if (var1.has("modules") && var1.get("modules").isJsonObject()) {
            JsonObject var2 = var1.getAsJsonObject("modules");

            for (Module var4 : this.modules.all()) {
               JsonObject var6 = var2.getAsJsonObject(var4.getName() + "@" + var4.getCategory().name());
               if (var6 == null && var4.getName().equals("JumpCircles")) {
                  var6 = var2.getAsJsonObject("AetherJumpCircles@" + var4.getCategory().name());
               }

               if (var6 != null) {
                  if (var6.has("enabled")
                     && var6.get("enabled").getAsBoolean() != var4.isEnabled()) {
                     var4.setEnabled(var6.get("enabled").getAsBoolean());
                  }

                  if (var6.has("keybind")) {
                     var4.getKeybind().fromJson(var6.get("keybind"));
                  }

                  JsonObject var5;
                  if ((var5 = var6.getAsJsonObject("settings")) != null) {
                     for (Setting var8 : var4.getSettings()) {
                        if (var5.has(var8.getName())) {
                           var8.fromJson(var5.get(var8.getName()));
                        }
                     }
                  }
               }
            }
         }

         for (Entry var10 : this.sections.entrySet()) {
            if (var1.has((String)var10.getKey()) && var1.get((String)var10.getKey()).isJsonObject()) {
               ((ConfigManager.Section)var10.getValue()).load().accept(var1.getAsJsonObject((String)var10.getKey()));
            }
         }
      }
   }

   public synchronized void save() {
      try {
         Files.createDirectories(this.file.getParent());
         Files.writeString(this.file, GSON.toJson(this.captureState()));
      } catch (IOException var2) {
         AetherClient.LOGGER.error("Failed to save config", var2);
      }
   }

   public synchronized void load() {
      if (Files.exists(this.file)) {
         JsonObject var1;
         try {
            var1 = JsonParser.parseString(Files.readString(this.file)).getAsJsonObject();
         } catch (Exception var3) {
            AetherClient.LOGGER.error("Failed to read config, using defaults", var3);
            return;
         }

         this.applyState(var1);
      }
   }

   public static record Section(Supplier<JsonObject> save, Consumer<JsonObject> load) {
   }
}
