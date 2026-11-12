package dev.aether.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aether.module.Category;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClickGuiState {
   public static final String THEMES_PANEL = "__themes__";
   private final Map<String, PanelState> panels = new LinkedHashMap<>();
   private final Set<String> expandedModules = new HashSet<>();
   private final Set<String> favourites = new HashSet<>();
   private int favouritesRevision;
   private Category selectedCategory = Category.COMBAT;
   private boolean themesTabSelected;
   private boolean laidOut;
   private boolean customized;
   private float lastLayoutWidth = -1.0F;
   private float lastLayoutHeight = -1.0F;
   private static final int LAYOUT_VERSION = 3;

   public ClickGuiState() {
      float x = 16.0F;
      for (Category cat : Category.values()) {
         this.panels.put(cat.name(), new PanelState(x, 16.0F));
         x += 224.0F;
      }
      this.panels.put(THEMES_PANEL, new PanelState(x, 320.0F));
   }

   public boolean isFavourite(String key) {
      return this.favourites.contains(key);
   }

   public void toggleFavourite(String key) {
      if (!this.favourites.remove(key)) {
         this.favourites.add(key);
      }
      this.favouritesRevision++;
   }

   public int favouritesRevision() {
      return this.favouritesRevision;
   }

   public Category selectedCategory() {
      return this.selectedCategory;
   }

   public void selectCategory(Category category) {
      if (category != null) {
         this.selectedCategory = category;
         this.themesTabSelected = false;
      }
   }

   public boolean isThemesTabSelected() {
      return this.themesTabSelected;
   }

   public void selectThemesTab() {
      this.themesTabSelected = true;
   }

   public Set<String> expandedKeys() {
      return this.expandedModules;
   }

   public void markCustomized() {
      this.customized = true;
   }

   public void ensureDefaultLayout(float uiW, float uiH) {
      if (!this.customized && (!this.laidOut || uiW != this.lastLayoutWidth || uiH != this.lastLayoutHeight)) {
         this.laidOut = true;
         this.lastLayoutWidth = uiW;
         this.lastLayoutHeight = uiH;
         String[] keys = new String[]{
            Category.COMBAT.name(),
            Category.MOVEMENT.name(),
            Category.RENDER.name(),
            Category.VISUALS.name(),
            Category.PLAYER.name(),
            Category.WORLD.name(),
            Category.MISC.name(),
            Category.CLIENT.name(),
            THEMES_PANEL
         };
         float colW = 224.0F;
         int cols = Math.max(1, (int)((uiW - 24.0F) / colW));
         float totalRowW = Math.min(keys.length, cols) * colW - 10.0F;
         float startX = Math.max(12.0F, (uiW - totalRowW) * 0.5F);
         for (int i = 0; i < keys.length; i++) {
            PanelState ps = this.panel(keys[i]);
            ps.x = startX + (float)(i % cols) * colW;
            ps.y = 70.0F + (float)(i / cols) * Math.max(220.0F, uiH * 0.42F);
            ps.collapsed = i / cols > 0;
         }
      }
   }

   public PanelState panel(String key) {
      return this.panels.computeIfAbsent(key, k -> new PanelState(16.0F, 16.0F));
   }

   public boolean isExpanded(String key) {
      return this.expandedModules.contains(key);
   }

   public void setExpanded(String key, boolean expanded) {
      if (expanded) {
         this.expandedModules.add(key);
      } else {
         this.expandedModules.remove(key);
      }
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      JsonArray favArr = new JsonArray();
      this.favourites.stream().sorted().forEach(favArr::add);
      root.add("favourites", favArr);
      root.addProperty("selectedCategory", this.selectedCategory.name());
      root.addProperty("v", LAYOUT_VERSION);
      root.addProperty("custom", this.customized);

      for (Entry<String, PanelState> entry : this.panels.entrySet()) {
         JsonObject ps = new JsonObject();
         ps.addProperty("x", entry.getValue().x);
         ps.addProperty("y", entry.getValue().y);
         ps.addProperty("collapsed", entry.getValue().collapsed);
         root.add(entry.getKey(), ps);
      }
      return root;
   }

   public void fromJson(JsonObject root) {
      this.favourites.clear();
      if (root.has("favourites") && root.get("favourites").isJsonArray()) {
         for (JsonElement el : root.getAsJsonArray("favourites")) {
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
               this.favourites.add(el.getAsString().replace("AetherJumpCircles@", "JumpCircles@"));
            }
         }
      }
      this.favouritesRevision++;
      if (root.has("selectedCategory") && root.get("selectedCategory").isJsonPrimitive()) {
         try {
            this.selectedCategory = Category.valueOf(root.get("selectedCategory").getAsString());
         } catch (IllegalArgumentException ignored) {
         }
      }

      if (root.has("v")
         && root.get("v").getAsInt() >= LAYOUT_VERSION
         && root.has("custom")
         && root.get("custom").getAsBoolean()) {
         this.laidOut = true;
         this.customized = true;

         for (Entry<String, PanelState> entry : this.panels.entrySet()) {
            JsonObject ps = root.getAsJsonObject(entry.getKey());
            if (ps != null) {
               if (ps.has("x")) {
                  entry.getValue().x = ps.get("x").getAsFloat();
               }
               if (ps.has("y")) {
                  entry.getValue().y = ps.get("y").getAsFloat();
               }
               if (ps.has("collapsed")) {
                  entry.getValue().collapsed = ps.get("collapsed").getAsBoolean();
               }
            }
         }
      }
   }

   public static class PanelState {
      public float x;
      public float y;
      public boolean collapsed;

      PanelState(float x, float y) {
         this.x = x;
         this.y = y;
      }
   }
}
