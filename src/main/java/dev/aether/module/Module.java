package dev.aether.module;

import dev.aether.settings.KeybindSetting;
import dev.aether.settings.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public abstract class Module {
   private final String name;
   private final String lowerName;
   private final String description;
   private final String lowerDescription;
   private final Category category;
   private final KeybindSetting keybind;
   private final List<Setting<?>> settings = new ArrayList<>();
   private boolean enabled;
   private BiConsumer<Module, Boolean> toggleCallback;

   protected Module(String name, String description, Category category) {
      this.name = name;
      this.lowerName = name.toLowerCase(Locale.ROOT);
      this.description = description == null ? "" : description;
      this.lowerDescription = this.description.toLowerCase(Locale.ROOT);
      this.category = category;
      this.keybind = new KeybindSetting("Keybind", "Toggles " + name, -1);
   }

   protected <T extends Setting<?>> T addSetting(T setting) {
      this.settings.add(setting);
      return setting;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public boolean matchesFilter(String lowerFilter) {
      if (lowerFilter == null || lowerFilter.isEmpty()) {
         return true;
      }
      return this.lowerName.contains(lowerFilter) || this.lowerDescription.contains(lowerFilter);
   }

   public Category getCategory() {
      return this.category;
   }

   public KeybindSetting getKeybind() {
      return this.keybind;
   }

   public List<Setting<?>> getSettings() {
      return Collections.unmodifiableList(this.settings);
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }
         if (this.toggleCallback != null) {
            this.toggleCallback.accept(this, enabled);
         }
      }
   }

   void setToggleCallback(BiConsumer<Module, Boolean> callback) {
      this.toggleCallback = callback;
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   public void onTick() {
   }

   public boolean onKeyPress(int keyCode) {
      return false;
   }
}
