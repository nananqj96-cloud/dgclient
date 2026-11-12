package dev.aether.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aether.AetherClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigStore {
   public static final int SLOT_COUNT = 5;
   public static final int CONFIG_VERSION = 1;
   public static final String FORMAT = "aetherclient-config";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static volatile boolean applying;
   private final ConfigManager config;
   private final Path dir;
   private final ConfigStore.Slot[] slots = new ConfigStore.Slot[5];
   private int active = -1;

   public ConfigStore(ConfigManager var1) {
      this.config = var1;
      this.dir = FabricLoader.getInstance().getConfigDir().resolve("aetherclient-configs");

      for (int var2 = 0; var2 < 5; var2++) {
         this.slots[var2] = new ConfigStore.Slot(var2);
      }
   }

   public static String defaultName(int var0) {
      return "Config " + (var0 + 1);
   }

   public ConfigStore.Slot slot(int var1) {
      return this.slots[var1];
   }

   public ConfigStore.Slot[] slots() {
      return this.slots;
   }

   public int activeIndex() {
      return this.active;
   }

   public Path directory() {
      return this.dir;
   }

   public void loadAll() {
      for (int var2 = 0; var2 < 5; var2++) {
         this.refreshSlot(var2);
      }

      JsonObject var3 = this.readJson(this.dir.resolve("index.json"));
      int var1;
      if (var3 != null
         && var3.has("active")
         && (var1 = var3.get("active").getAsInt()) >= 0
         && var1 < 5
         && this.slots[var1].filled) {
         this.active = var1;
      }
   }

   private void refreshSlot(int var1) {
      ConfigStore.Slot var2 = this.slots[var1];
      JsonObject var3 = this.readJson(this.slotPath(var1));
      JsonObject var4 = this.extractState(var3);
      if (var4 == null) {
         var2.filled = false;
         var2.savedAt = 0L;
         var2.name = defaultName(var1);
      } else {
         var2.filled = true;
         var2.name = var3.has("name") && !var3.get("name").getAsString().isBlank()
            ? var3.get("name").getAsString()
            : defaultName(var1);
         var2.savedAt = var3.has("savedAt") ? var3.get("savedAt").getAsLong() : 0L;
      }
   }

   public boolean save(int var1) {
      ConfigStore.Slot var2 = this.slots[var1];
      JsonObject var3 = new JsonObject();
      var3.addProperty("format", "aetherclient-config");
      var3.addProperty("version", 1);
      var3.addProperty("name", var2.name);
      var3.addProperty("savedAt", System.currentTimeMillis());
      var3.addProperty("client", "1.6.2");
      var3.add("state", this.config.captureState());
      if (!this.write(this.slotPath(var1), var3)) {
         return false;
      } else {
         this.refreshSlot(var1);
         return true;
      }
   }

   public boolean activate(int var1) {
      JsonObject var2 = this.extractState(this.readJson(this.slotPath(var1)));
      if (var2 == null) {
         return false;
      } else {
         applying = true;

         try {
            this.config.applyState(var2);
         } finally {
            applying = false;
         }

         this.active = var1;
         this.writeIndex();
         return true;
      }
   }

   public boolean delete(int var1) {
      try {
         Files.deleteIfExists(this.slotPath(var1));
      } catch (IOException var3) {
         AetherClient.LOGGER.error("Failed to delete config file {}", this.slotPath(var1).getFileName(), var3);
         return false;
      }

      if (this.active == var1) {
         this.active = -1;
         this.writeIndex();
      }

      this.refreshSlot(var1);
      return true;
   }

   public boolean rename(int var1, String var2) {
      ConfigStore.Slot var3 = this.slots[var1];
      String var4 = this.sanitizeName(var2);
      if (var4.isEmpty()) {
         var4 = defaultName(var1);
      }

      var3.name = var4;
      if (!var3.filled) {
         return true;
      } else {
         JsonObject var5 = this.readJson(this.slotPath(var1));
         if (var5 == null) {
            return false;
         } else {
            var5.addProperty("name", var4);
            return this.write(this.slotPath(var1), var5);
         }
      }
   }

   public String export(int var1) {
      JsonObject var2 = this.readJson(this.slotPath(var1));
      return this.extractState(var2) == null ? null : GSON.toJson(var2);
   }

   public ConfigStore.ImportResult importInto(int var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         JsonObject var3;
         try {
            var3 = JsonParser.parseString(var2).getAsJsonObject();
         } catch (Exception var8) {
            return new ConfigStore.ImportResult(false, "Not valid config JSON");
         }

         if (var3.has("version")
            && var3.get("version").isJsonPrimitive()
            && var3.get("version").getAsInt() > 1) {
            return new ConfigStore.ImportResult(false, "Config is from a newer client");
         } else {
            JsonObject var4 = this.extractState(var3);
            if (var4 == null) {
               return new ConfigStore.ImportResult(false, "No config data found");
            } else {
               ConfigStore.Slot var5 = this.slots[var1];
               String var6 = var3.has("name") && !var3.get("name").getAsString().isBlank()
                  ? this.sanitizeName(var3.get("name").getAsString())
                  : var5.name;
               JsonObject var7 = new JsonObject();
               var7.addProperty("format", "aetherclient-config");
               var7.addProperty("version", 1);
               var7.addProperty("name", var6);
               var7.addProperty(
                  "savedAt",
                  var3.has("savedAt")
                     ? var3.get("savedAt").getAsLong()
                     : System.currentTimeMillis()
               );
               if (var3.has("client")) {
                  var7.addProperty("client", var3.get("client").getAsString());
               }

               var7.add("state", var4);
               if (!this.write(this.slotPath(var1), var7)) {
                  return new ConfigStore.ImportResult(false, "Couldn't write the slot file");
               } else {
                  this.refreshSlot(var1);
                  return new ConfigStore.ImportResult(true, "Imported \"" + var5.name + "\"");
               }
            }
         }
      } else {
         return new ConfigStore.ImportResult(false, "Clipboard is empty");
      }
   }

   private JsonObject extractState(JsonObject var1) {
      if (var1 == null) {
         return null;
      } else if (var1.has("state") && var1.get("state").isJsonObject()) {
         return var1.getAsJsonObject("state");
      } else {
         return var1.has("modules") && var1.get("modules").isJsonObject() ? var1 : null;
      }
   }

   private Path slotPath(int var1) {
      return this.dir.resolve("slot" + (var1 + 1) + ".json");
   }

   private void writeIndex() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("active", this.active);
      this.write(this.dir.resolve("index.json"), var1);
   }

   private JsonObject readJson(Path var1) {
      if (!Files.exists(var1)) {
         return null;
      } else {
         try {
            return JsonParser.parseString(Files.readString(var1)).getAsJsonObject();
         } catch (Exception var3) {
            AetherClient.LOGGER.warn("Ignoring unreadable config file {}", var1.getFileName(), var3);
            return null;
         }
      }
   }

   private boolean write(Path var1, JsonObject var2) {
      try {
         Files.createDirectories(this.dir);
         Files.writeString(var1, GSON.toJson(var2));
         return true;
      } catch (IOException var4) {
         AetherClient.LOGGER.error("Failed to write config file {}", var1.getFileName(), var4);
         return false;
      }
   }

   private String sanitizeName(String var1) {
      if (var1 == null) {
         return "";
      } else {
         String var2 = var1.replaceAll("[\\r\\n\\t]", " ").trim();
         return var2.length() > 24 ? var2.substring(0, 24) : var2;
      }
   }

   public static record ImportResult(boolean ok, String message) {
   }

   public static final class Slot {
      private final int index;
      private String name;
      private boolean filled;
      private long savedAt;

      Slot(int var1) {
         this.index = var1;
         this.name = ConfigStore.defaultName(var1);
      }

      public int index() {
         return this.index;
      }

      public String name() {
         return this.name;
      }

      public boolean filled() {
         return this.filled;
      }

      public long savedAt() {
         return this.savedAt;
      }
   }
}
