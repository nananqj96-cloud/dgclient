package dev.aether.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
   public static final int AETHER_CYAN = 0xFF38BDF8;
   public static final int PINK = -49508;
   private final List<Theme> themes = new ArrayList<>();
   private Theme current;

   public ThemeManager() {
      this.themes.add(new Theme("Aether", AETHER_CYAN, false));
      this.themes.add(new Theme("Aurora", 0xFF2DD4BF, false));
      this.themes.add(new Theme("Indigo", 0xFF818CF8, false));
      this.themes.add(new Theme("Blue", -11689985, false));
      this.themes.add(new Theme("Purple", -5743361, false));
      this.themes.add(new Theme("Pink", PINK, false));
      this.themes.add(new Theme("Emerald", -12654960, false));
      this.themes.add(new Theme("Red", -45715, false));
      this.themes.add(new Theme("Slate", -9255492, false));
      this.themes.add(new RainbowTheme());
      this.current = this.themes.getFirst();
   }

   public Theme current() {
      return this.current;
   }

   public List<Theme> getThemes() {
      return this.themes;
   }

   public void select(Theme theme) {
      if (this.themes.contains(theme)) {
         this.current = theme;
      }
   }

   public Theme addCustom(int accent) {
      int idx = 1;
      for (Theme t : this.themes) {
         if (t.isCustom()) {
            idx++;
         }
      }
      Theme custom = new Theme("Custom " + idx, accent, true);
      this.themes.add(custom);
      return custom;
   }

   public void removeCustom(Theme theme) {
      if (theme.isCustom() && this.themes.remove(theme) && this.current == theme) {
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
         if (root.has("custom")) {
            for (JsonElement el : root.getAsJsonArray("custom")) {
               JsonObject obj = el.getAsJsonObject();
               this.themes.add(new Theme(obj.get("name").getAsString(), obj.get("accent").getAsInt(), true));
            }
         }
         if (root.has("current")) {
            String name = root.get("current").getAsString();
            for (Theme t : this.themes) {
               if (t.getName().equals(name)) {
                  this.current = t;
                  break;
               }
            }
         }
      }
   }
}
