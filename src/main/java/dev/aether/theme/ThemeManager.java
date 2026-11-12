package dev.aether.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThemeManager {
   public static final int PINKBLOOM = 0xFFFF66C4;
   public static final int AETHER_CYAN = 0xFF38BDF8;
   public static final int PINK = -49508;
   private final List<Theme> themes = new ArrayList<>();
   private Theme current;
   private int customCounter = 1;

   public ThemeManager() {
      this.themes.add(new Theme("PinkBloom", PINKBLOOM, false, true));
      this.themes.add(new Theme("Sakura", 0xFFFB7185, false, true));
      this.themes.add(new Theme("Aether", AETHER_CYAN, false, false));
      this.themes.add(new Theme("Aurora", 0xFF2DD4BF, false, false));
      this.themes.add(new Theme("Indigo", 0xFF818CF8, false, false));
      this.themes.add(new Theme("Blue", -11689985, false, false));
      this.themes.add(new Theme("Purple", -5743361, false, false));
      this.themes.add(new Theme("Pink", PINK, false, true));
      this.themes.add(new Theme("Emerald", -12654960, false, false));
      this.themes.add(new Theme("Red", -45715, false, false));
      this.themes.add(new Theme("Slate", -9255492, false, false));
      this.themes.add(new RainbowTheme());
      this.current = this.themes.getFirst();
   }

   public Theme current() {
      return this.current;
   }

   public List<Theme> getThemes() {
      return Collections.unmodifiableList(this.themes);
   }

   public Theme byName(String name) {
      if (name == null) {
         return null;
      }
      for (Theme t : this.themes) {
         if (t.getName().equalsIgnoreCase(name)) {
            return t;
         }
      }
      return null;
   }

   public void select(Theme theme) {
      if (this.themes.contains(theme)) {
         this.current = theme;
      }
   }

   public Theme addCustom(int accent) {
      String name;
      do {
         name = "Custom " + this.customCounter++;
      } while (this.byName(name) != null);

      Theme custom = new Theme(name, accent, true, true);
      this.themes.add(custom);
      return custom;
   }

   public Theme addCustom(String name, int accent) {
      Theme existing = this.byName(name);
      if (existing != null && existing.isCustom()) {
         existing.setAccent(accent);
         return existing;
      }
      Theme custom = new Theme(name, accent, true, true);
      this.themes.add(custom);
      return custom;
   }

   public void removeCustom(Theme theme) {
      if (theme != null && theme.isCustom() && this.themes.remove(theme) && this.current == theme) {
         this.current = this.themes.getFirst();
      }
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("current", this.current.getName());
      JsonArray arr = new JsonArray();
      for (Theme t : this.themes) {
         if (t.isCustom()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", t.getName());
            obj.addProperty("accent", t.accent());
            arr.add(obj);
         }
      }
      root.add("custom", arr);
      return root;
   }

   public void fromJson(JsonObject root) {
      if (root != null) {
         this.themes.removeIf(Theme::isCustom);
         if (root.has("custom") && root.get("custom").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("custom")) {
               JsonObject obj = el.getAsJsonObject();
               this.themes.add(new Theme(obj.get("name").getAsString(), obj.get("accent").getAsInt(), true, true));
            }
         }
         if (root.has("current")) {
            Theme match = this.byName(root.get("current").getAsString());
            if (match != null) {
               this.current = match;
            }
         }
      }
   }
}
