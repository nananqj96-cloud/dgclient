package dev.aether.gui.panel;

import dev.aether.gui.ClickGuiState;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.render.nanovg.NVGIcons;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.ThemeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryPanel extends Panel {
   private static final float ENTRY_GAP = 4.0F;
   private static final float ENTRY_INSET = 6.0F;
   private final Category category;
   private final ClickGuiState state;
   private final List<ModuleEntry> entries = new ArrayList<>();
   private final List<ModuleEntry> cachedVisible = new ArrayList<>();
   private boolean favouritesOnly;
   private String filter = "";
   private String cachedFilter = null;
   private boolean cachedFavOnly;
   private int cachedFavRev = -1;

   public CategoryPanel(Category category, ModuleManager modules, ThemeManager themes, ClickGuiState state) {
      super(themes, state.panel(category.name()));
      this.category = category;
      this.state = state;
      for (Module mod : modules.inCategory(category)) {
         this.entries.add(new ModuleEntry(mod, themes, state));
      }
   }

   public Category getCategory() {
      return this.category;
   }

   public List<ModuleEntry> allEntries() {
      return this.entries;
   }

   public void setFavouritesOnly(boolean favOnly) {
      this.favouritesOnly = favOnly;
   }

   public void setFilter(String rawFilter) {
      this.filter = rawFilter == null ? "" : rawFilter.toLowerCase(Locale.ROOT).trim();
   }

   public List<ModuleEntry> visibleEntries() {
      int favRev = this.state.favouritesRevision();
      if (!this.filter.equals(this.cachedFilter) || this.favouritesOnly != this.cachedFavOnly || favRev != this.cachedFavRev) {
         this.cachedFilter = this.filter;
         this.cachedFavOnly = this.favouritesOnly;
         this.cachedFavRev = favRev;
         this.cachedVisible.clear();
         // Favourites first without allocating streams/comparators
         for (ModuleEntry e : this.entries) {
            if (e.isFavourite() && e.getModule().matchesFilter(this.filter)) {
               this.cachedVisible.add(e);
            }
         }
         if (!this.favouritesOnly) {
            for (ModuleEntry e : this.entries) {
               if (!e.isFavourite() && e.getModule().matchesFilter(this.filter)) {
                  this.cachedVisible.add(e);
               }
            }
         }
      }
      return this.cachedVisible;
   }

   @Override
   protected String title() {
      return this.category.getDisplayName();
   }

   @Override
   protected int icon() {
      return NVGIcons.get(this.category);
   }

   @Override
   protected float contentHeight(NVGRenderer vg) {
      List<ModuleEntry> vis = this.visibleEntries();
      if (vis.isEmpty()) {
         return 36.0F;
      }
      float total = 12.0F;
      for (ModuleEntry e : vis) {
         e.setShowSubtitle(false);
         total += e.height(vg) + ENTRY_GAP;
      }
      return total - ENTRY_GAP;
   }

   @Override
   protected void renderContent(NVGRenderer vg, float startY, float mx, float my, float viewTop, float viewBottom) {
      List<ModuleEntry> vis = this.visibleEntries();
      if (vis.isEmpty()) {
         vg.text(this.favouritesOnly ? "No favourites here" : "No matches", this.ps.x + 12.0F, startY + 12.0F, 11.0F, this.theme().textDisabled());
         return;
      }
      float y = startY;
      float entryW = Panel.WIDTH - ENTRY_INSET * 2.0F;
      for (ModuleEntry e : vis) {
         e.setShowSubtitle(false);
         e.setBounds(this.ps.x + ENTRY_INSET, y, entryW);
         float eh = e.height(vg);
         if (y + eh >= viewTop - 20.0F && y <= viewBottom + 20.0F) {
            float fade = this.edgeFade(y, y + eh, viewTop, viewBottom);
            e.render(vg, mx, my, fade);
         }
         y += eh + ENTRY_GAP;
      }
   }

   @Override
   public boolean mouseClicked(float mx, float my, int button) {
      for (ModuleEntry e : this.visibleEntries()) {
         if (e.mouseClicked(mx, my, button)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void mouseDragged(float mx, float my) {
      for (ModuleEntry e : this.entries) {
         e.mouseDragged(mx, my);
      }
   }

   @Override
   public void mouseReleased() {
      for (ModuleEntry e : this.entries) {
         e.mouseReleased();
      }
   }

   @Override
   public boolean keyPressed(int keyCode) {
      for (ModuleEntry e : this.entries) {
         if (e.keyPressed(keyCode)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean charTyped(int codepoint) {
      for (ModuleEntry e : this.entries) {
         if (e.charTyped(codepoint)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean isListening() {
      for (ModuleEntry e : this.entries) {
         if (e.isListening()) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean hasPendingKeybind() {
      for (ModuleEntry e : this.entries) {
         if (e.hasPendingKeybind()) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean handlePendingKeybindMouse(int button) {
      for (ModuleEntry e : this.entries) {
         if (e.hasPendingKeybind() && e.handlePendingKeybindMouse(button)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void unfocusInputs() {
      for (ModuleEntry e : this.entries) {
         e.unfocusInputs();
      }
   }
}
