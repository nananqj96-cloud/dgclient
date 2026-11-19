package dev.aether.accessory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aether.module.Category;
import dev.aether.module.Module;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * Accessories — a big popup UI to equip Aether-themed avatar accessories (crowns, halos, horns,
 * masks, wings, feathers, capes, backpacks). The module holds the equipped selection per
 * accessory type, opens the popup screen and persists the selection in the config.
 */
public class AccessoriesModule extends Module {
   private final Map<AccessoryType, String> equipped = new EnumMap<>(AccessoryType.class);

   public AccessoriesModule() {
      super("Accessories", "Open the avatar accessories popup to equip crowns, halos, horns, masks, wings, feathers, capes and backpacks.", Category.CLIENT);
   }

   /** The equipped accessory id for a type, or {@code null} when nothing is equipped. */
   public String selected(AccessoryType type) {
      return this.equipped.get(type);
   }

   public Accessory equippedAccessory(AccessoryType type) {
      String id = this.equipped.get(type);
      return id == null ? null : AccessoryRegistry.find(id);
   }

   public void equip(AccessoryType type, String id) {
      if (id == null || id.isEmpty()) {
         this.equipped.remove(type);
      } else {
         this.equipped.put(type, id);
      }
   }

   public void unequip(AccessoryType type) {
      this.equipped.remove(type);
   }

   public void unequipAll() {
      this.equipped.clear();
   }

   public int equippedCount() {
      return this.equipped.size();
   }

   public void open() {
      Minecraft mc = Minecraft.getInstance();
      mc.setScreen(new dev.aether.gui.AccessoriesScreen(mc.screen, this));
   }

   public JsonObject toJson() {
      JsonObject obj = new JsonObject();
      JsonArray arr = new JsonArray();
      for (AccessoryType type : AccessoryType.values()) {
         String id = this.equipped.get(type);
         if (id != null) {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", type.name());
            entry.addProperty("id", id);
            arr.add(entry);
         }
      }
      obj.add("equipped", arr);
      return obj;
   }

   public void fromJson(JsonObject obj) {
      this.equipped.clear();
      if (obj == null || !obj.has("equipped")) {
         return;
      }
      JsonElement equippedEl = obj.get("equipped");
      if (!equippedEl.isJsonArray()) {
         return;
      }
      for (JsonElement el : equippedEl.getAsJsonArray()) {
         if (!el.isJsonObject()) {
            continue;
         }
         JsonObject entry = el.getAsJsonObject();
         if (!entry.has("type") || !entry.has("id")) {
            continue;
         }
         try {
            AccessoryType type = AccessoryType.valueOf(entry.get("type").getAsString());
            String id = entry.get("id").getAsString();
            if (AccessoryRegistry.find(id) != null) {
               this.equipped.put(type, id);
            }
         } catch (IllegalArgumentException ignored) {
         }
      }
   }
}
