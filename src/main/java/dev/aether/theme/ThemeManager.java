package dev.aether.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThemeManager {
   private final List<Theme> themes = new ArrayList<>();
   private Theme current;
   private int customCounter = 1;

   public ThemeManager() {
      this.themes.add(new Theme("PinkBloom", 0xFFFF66C4, false, true));
      this.themes.add(new Theme("Sakura", 0xFFFB7185, false, true));
      this.themes.add(new Theme("Aether", 0xFF38BDF8));
      this.themes.add(new Theme("Aurora", 0xFF2DD4BF));
      this.themes.add(new Theme("Indigo", 0xFF818CF8));
      this.themes.add(new Theme("Blue", 0xFF3B82F6));
      this.themes.add(new Theme("Purple", 0xFFA855F7));
      this.themes.add(new Theme("Pink", 0xFFF472B6, false, true));
      this.themes.add(new Theme("Emerald", 0xFF10B981));
      this.themes.add(new Theme("Red", 0xFFEF4444));
      this.themes.add(new Theme("Slate", 0xFF94A3B8));
      this.themes.add(new RainbowTheme());
      this.current = this.themes.getFirst();
   }

   public List<Theme> getThemes() {
      return Collections.unmodifiableList(this.themes);
   }

   public Theme current() {
      return this.current;
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
      } while (this.findByName(name) != null);

      Theme custom = new Theme(name, accent, true, true);
      this.themes.add(custom);
      return custom;
   }

   public Theme addCustom(String name, int accent) {
      Theme existing = this.findByName(name);
      if (existing != null && existing.isCustom()) {
         existing.setAccent(accent);
         return existing;
      } else {
         Theme custom = new Theme(name, accent, true, true);
         this.themes.add(custom);
         return custom;
      }
   }

   public void removeCustom(Theme theme) {
      if (theme != null && theme.isCustom()) {
         this.themes.remove(theme);
         if (this.current == theme) {
            this.current = this.themes.getFirst();
         }
      }
   }

   public void clearCustoms() {
      this.themes.removeIf(Theme::isCustom);
      if (!this.themes.contains(this.current)) {
         this.current = this.themes.getFirst();
      }
   }

   private Theme findByName(String name) {
      for (Theme t : this.themes) {
         if (t.getName().equalsIgnoreCase(name)) {
            return t;
         }
      }
      return null;
   }
}
