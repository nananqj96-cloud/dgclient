package dev.aether.gui;

import dev.aether.module.Category;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ClickGuiState {
   public static final String THEMES_PANEL = "__themes__";
   private final Map<String, ClickGuiState.PanelState> panels = new LinkedHashMap<>();
   private final Set<String> expanded = new LinkedHashSet<>();
   private final Set<String> favourites = new LinkedHashSet<>();
   private long favouritesRevision = 1L;
   private boolean laidOut;
   private boolean customized;
   private float lastLayoutWidth;
   private float lastLayoutHeight;
   private Category selectedCategory = Category.COMBAT;
   private boolean themesTabSelected;
   private boolean corzPortOpen;
   private String corzSelectedCategory = "All";

   public ClickGuiState.PanelState panel(String key) {
      return this.panels.computeIfAbsent(key, k -> new ClickGuiState.PanelState());
   }

   public Map<String, ClickGuiState.PanelState> panels() {
      return Collections.unmodifiableMap(this.panels);
   }

   public boolean isExpanded(String key) {
      return this.expanded.contains(key);
   }

   public void setExpanded(String key, boolean value) {
      if (value) {
         this.expanded.add(key);
      } else {
         this.expanded.remove(key);
      }
   }

   public Set<String> expandedKeys() {
      return Collections.unmodifiableSet(this.expanded);
   }

   public void replaceExpanded(Iterable<String> keys) {
      this.expanded.clear();
      for (String k : keys) {
         this.expanded.add(k);
      }
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

   public Set<String> favouriteKeys() {
      return Collections.unmodifiableSet(this.favourites);
   }

   public long favouritesRevision() {
      return this.favouritesRevision;
   }

   public void replaceFavourites(Iterable<String> keys) {
      this.favourites.clear();
      for (String k : keys) {
         this.favourites.add(k);
      }
      this.favouritesRevision++;
   }

   public Category selectedCategory() {
      return this.selectedCategory;
   }

   public void selectCategory(Category cat) {
      if (cat != null) {
         this.selectedCategory = cat;
         this.themesTabSelected = false;
      }
   }

   public boolean isThemesTabSelected() {
      return this.themesTabSelected;
   }

   public void selectThemesTab() {
      this.themesTabSelected = true;
   }

   public boolean isCorzPortOpen() {
      return this.corzPortOpen;
   }

   public void setCorzPortOpen(boolean open) {
      this.corzPortOpen = open;
   }

   public void toggleCorzPort() {
      this.corzPortOpen = !this.corzPortOpen;
   }

   public String corzSelectedCategory() {
      return this.corzSelectedCategory;
   }

   public void setCorzSelectedCategory(String category) {
      if (category != null && !category.isBlank()) {
         this.corzSelectedCategory = category;
      }
   }

   public boolean isCustomized() {
      return this.customized;
   }

   public void setCustomized(boolean customized) {
      this.customized = customized;
      if (customized) {
         this.laidOut = true;
      }
   }

   public void markCustomized() {
      this.customized = true;
   }

   public void ensureDefaultLayout(float uiW, float uiH) {
      if (!this.customized && (!this.laidOut || uiW != this.lastLayoutWidth || uiH != this.lastLayoutHeight)) {
         this.laidOut = true;
         this.lastLayoutWidth = uiW;
         this.lastLayoutHeight = uiH;
         Category[] cats = Category.values();
         String[] keys = new String[cats.length + 1];
         for (int i = 0; i < cats.length; i++) {
            keys[i] = cats[i].name();
         }
         keys[cats.length] = THEMES_PANEL;
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

   public static final class PanelState {
      public float x;
      public float y;
      public boolean collapsed;
      public float scrollTarget;
   }
}
