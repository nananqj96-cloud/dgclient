package dev.aether.accessory;

import dev.aether.module.Category;
import dev.aether.module.Module;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * Accessories — a big popup UI to equip Aether-themed avatar accessories (crowns, wings,
 * feathers, capes). The module holds the equipped selection per accessory type and opens the
 * popup screen.
 */
public class AccessoriesModule extends Module {
   private final Map<AccessoryType, String> equipped = new EnumMap<>(AccessoryType.class);

   public AccessoriesModule() {
      super("Accessories", "Open the avatar accessories popup to equip crowns, wings, feathers and capes.", Category.CLIENT);
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

   public int equippedCount() {
      return this.equipped.size();
   }

   public void open() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen != null) {
         mc.setScreen(new dev.aether.gui.AccessoriesScreen(mc.screen, this));
      } else {
         mc.setScreen(new dev.aether.gui.AccessoriesScreen(null, this));
      }
   }
}
