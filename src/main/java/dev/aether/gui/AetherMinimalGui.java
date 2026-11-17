package dev.aether.gui;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Experimental minimalistic GUI for <b>Aether Client</b> itself, in the style of the Minecraft
 * "Vape" client's ClickGUI: a flat, monochrome dark/white design with <b>no glow, no blur, and no
 * glass chrome</b>.
 *
 * <p>When the {@code Experimental Minimal GUI} setting on {@link dev.aether.module.Modules.ClickGuiModule}
 * is enabled, {@link ClickGuiScreen} renders through this class instead of the default Dashboard /
 * Columns chrome. Because it replaces the whole Aether ClickGUI surface, it applies to both the
 * panel (Dashboard) and floating (Columns) layouts at once. The 67port is left untouched and always
 * uses its original ported 67client GUI.
 *
 * <p>Module toggles still drive Aether's shared {@link ModuleManager}, so this stays in sync with the
 * rest of the client.
 */
public final class AetherMinimalGui {
   // Flat Vape-style monochrome palette (no glow, no gradients).
   private static final int C_BG = 0xFF0F0F12;
   private static final int C_SURFACE = 0xFF18181B;
   private static final int C_HEADER = 0xFF131316;
   private static final int C_BORDER = 0xFF2A2A2E;
   private static final int C_BORDER_SOFT = 0xFF222226;
   private static final int C_TEXT = 0xFFF5F5F5;
   private static final int C_MUTED = 0xFF8A8A8A;
   private static final int C_DARK = 0xFF161616;
   private static final int C_WHITE = 0xFFFFFFFF;

   private static final float SIDEBAR_W = 188.0F;
   private static final float ROW_H = 30.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float CAT_H = 30.0F;
   private static final float CLOSE_SIZE = 20.0F;

   private final ModuleManager modules;

   private final List<Hit> hits = new ArrayList<>();
   private final List<CatHit> catHits = new ArrayList<>();

   private float closeX;
   private float closeY;

   private Category selectedCat;
   private String search = "";
   private boolean searchFocused;

   private float searchX;
   private float searchY;
   private float searchW;
   private float searchH;

   private float scroll;
   private float maxScroll;

   private static final class Hit {
      final float x;
      final float y;
      final float w;
      final float h;
      final Module module;

      Hit(float x, float y, float w, float h, Module module) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.module = module;
      }
   }

   private static final class CatHit {
      final float x;
      final float y;
      final float w;
      final float h;
      final Category cat; // null == ALL

      CatHit(float x, float y, float w, float h, Category cat) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.cat = cat;
      }
   }

   public AetherMinimalGui(ModuleManager modules) {
      this.modules = modules;
      this.selectedCat = null; // start on ALL
   }

   public void setSearch(String search) {
      this.search = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
   }

   public float scroll() {
      return this.scroll;
   }

   /** Draws the full-screen flat minimal GUI (sidebar + scrollable module list). */
   public void render(NVGRenderer vg, float mx, float my, float uiW, float uiH) {
      this.hits.clear();
      this.catHits.clear();

      try {
         // Full-screen flat dark background — no glow.
         vg.rect(0.0F, 0.0F, uiW, uiH, 0.0F, C_BG);

         // Left sidebar (flat surface).
         vg.rect(0.0F, 0.0F, SIDEBAR_W, uiH, 0.0F, C_SURFACE);
         vg.rectOutline(0.0F, 0.0F, SIDEBAR_W, uiH, 0.0F, 1.0F, C_BORDER);

         // Sidebar title + close button.
         vg.text("AETHER", 22.0F, 46.0F, 15.0F, C_TEXT);
         float subX = 22.0F + vg.textWidth("AETHER", 15.0F) + 8.0F;
         vg.text("minimal", subX, 46.0F, 10.0F, C_MUTED);

         this.closeX = uiW - 28.0F;
         this.closeY = 14.0F;
         boolean cHov = this.hit(mx, my, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
         vg.rect(this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE, 5.0F, Colors.withAlpha(C_WHITE, cHov ? 0.14F : 0.05F));
         vg.cross(this.closeX + 5.0F, this.closeY + 5.0F, 10.0F, 1.4F, cHov ? C_TEXT : C_MUTED);

         // Category list (ALL + each category).
         float cy = 78.0F;
         this.addCategoryHit(vg, mx, my, cy, null, "ALL");
         cy += CAT_H + 3.0F;
         for (Category cat : Category.values()) {
            this.addCategoryHit(vg, mx, my, cy, cat, cat.getDisplayName());
            cy += CAT_H + 3.0F;
         }

         // Right area: search box + scrollable module cards.
         float pad = 18.0F;
         float bodyX = SIDEBAR_W + pad;
         float bodyW = uiW - SIDEBAR_W - pad * 2.0F;

         this.searchH = 32.0F;
         this.searchX = bodyX;
         this.searchY = 20.0F;
         this.searchW = bodyW;
         this.drawSearchBox(vg, mx, my);

         float bodyTop = this.searchY + this.searchH + 18.0F;
         float bodyH = uiH - bodyTop - 20.0F;

         vg.save();
         vg.scissor(bodyX - 1.0F, bodyTop, bodyW + 2.0F, bodyH);

         float contentY = bodyTop + 10.0F - this.scroll;
         float contentH = this.computeContentHeight(vg);
         this.maxScroll = Math.max(0.0F, contentH - bodyH);
         this.scroll = Math.clamp(this.scroll, 0.0F, this.maxScroll);

         float innerX = bodyX;
         float innerW = bodyW;
         for (Category cat : Category.values()) {
            if (this.selectedCat != null && this.selectedCat != cat) {
               continue;
            }
            List<Module> inCat = this.modules.inCategory(cat);
            if (inCat.isEmpty()) {
               continue;
            }
            vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT), innerX, contentY + 12.0F, 10.5F, C_MUTED);
            contentY += 20.0F;

            for (Module module : inCat) {
               if (!this.search.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(this.search)) {
                  continue;
               }
               boolean enabled = module.isEnabled();
               boolean hov = this.hit(mx, my, innerX, contentY, innerW, ROW_H);
               this.hits.add(new Hit(innerX, contentY, innerW, ROW_H, module));

               // Row card: enabled = white, disabled = dark.
               int card = enabled
                  ? Colors.withAlpha(C_WHITE, hov ? 0.16F : 0.10F)
                  : Colors.withAlpha(C_WHITE, hov ? 0.07F : 0.035F);
               vg.rect(innerX, contentY, innerW, ROW_H, 6.0F, card);
               vg.rectOutline(innerX, contentY, innerW, ROW_H, 6.0F, 0.8F, enabled ? Colors.withAlpha(C_WHITE, 0.22F) : C_BORDER_SOFT);

               // State dot.
               float dotX = innerX + 18.0F;
               float dotY = contentY + ROW_H * 0.5F;
               vg.circle(dotX, dotY, 3.4F, enabled ? C_WHITE : C_MUTED);

               // Module name.
               vg.text(module.getName(), innerX + 30.0F, dotY, 12.0F, enabled ? C_DARK : C_MUTED);

               contentY += ROW_H + ROW_GAP;
            }
            contentY += 6.0F;
         }

         if (this.maxScroll > 1.0F) {
            float trackH = bodyH - 12.0F;
            float thumbH = Math.max(24.0F, trackH * (bodyH / Math.max(1.0F, contentH)));
            float thumbY = bodyTop + 6.0F + (trackH - thumbH) * (this.scroll / this.maxScroll);
            vg.rect(bodyX + bodyW - 7.0F, bodyTop + 6.0F, 2.5F, trackH, 1.2F, C_BORDER_SOFT);
            vg.rect(bodyX + bodyW - 7.0F, thumbY, 2.5F, thumbH, 1.2F, Colors.withAlpha(C_WHITE, 0.55F));
         }
         vg.restore();
      } catch (Throwable t) {
         AetherClient.LOGGER.error("Aether minimal GUI render failed", t);
      }
   }

   private void addCategoryHit(NVGRenderer vg, float mx, float my, float y, Category cat, String label) {
      float x = 14.0F;
      float w = SIDEBAR_W - 28.0F;
      boolean selected = (cat == null && this.selectedCat == null) || (cat != null && cat == this.selectedCat);
      boolean hov = this.hit(mx, my, x, y, w, CAT_H);
      this.catHits.add(new CatHit(x, y, w, CAT_H, cat));

      if (selected) {
         vg.rect(x, y, w, CAT_H, 6.0F, Colors.withAlpha(C_WHITE, 0.12F));
         vg.rectOutline(x, y, w, CAT_H, 6.0F, 1.0F, Colors.withAlpha(C_WHITE, 0.30F));
         vg.rect(x + 3.0F, y + 7.0F, 2.6F, CAT_H - 14.0F, 1.3F, C_WHITE);
      } else if (hov) {
         vg.rect(x, y, w, CAT_H, 6.0F, Colors.withAlpha(C_WHITE, 0.05F));
      }
      vg.text(label, x + 22.0F, y + CAT_H * 0.5F, 12.0F, selected ? C_TEXT : (hov ? C_TEXT : C_MUTED));
   }

   private void drawSearchBox(NVGRenderer vg, float mx, float my) {
      boolean hov = this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH);
      vg.rect(this.searchX, this.searchY, this.searchW, this.searchH, 8.0F, Colors.withAlpha(C_HEADER, 0.9F));
      vg.rectOutline(this.searchX, this.searchY, this.searchW, this.searchH, 8.0F, 1.0F,
         this.searchFocused ? Colors.withAlpha(C_WHITE, 0.55F) : C_BORDER);
      float cy = this.searchY + this.searchH * 0.5F;
      if (this.search.isEmpty() && !this.searchFocused) {
         vg.text("Search modules...", this.searchX + 16.0F, cy, 11.5F, C_MUTED);
      } else {
         vg.textTruncated(this.search, this.searchX + 16.0F, cy, 12.0F, C_TEXT, this.searchW - 48.0F);
      }
      // Clear (x) button on the right.
      if (!this.search.isEmpty()) {
         vg.cross(this.searchX + this.searchW - 20.0F, cy - 5.0F, 10.0F, 1.5F, C_MUTED);
      }
   }

   private float computeContentHeight(NVGRenderer vg) {
      float h = 10.0F;
      for (Category cat : Category.values()) {
         if (this.selectedCat != null && this.selectedCat != cat) {
            continue;
         }
         List<Module> inCat = this.modules.inCategory(cat);
         if (inCat.isEmpty()) {
            continue;
         }
         h += 20.0F;
         for (Module module : inCat) {
            if (!this.search.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(this.search)) {
               continue;
            }
            h += ROW_H + ROW_GAP;
         }
         h += 6.0F;
      }
      return h;
   }

   /** Handles a click; returns true if consumed. Toggles modules / selects categories / closes. */
   public boolean mouseClicked(float mx, float my, int btn) {
      if (btn != 0) {
         return false;
      }
      if (this.hit(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
         if (!this.search.isEmpty() && mx >= this.searchX + this.searchW - 26.0F) {
            this.search = "";
         } else {
            this.searchFocused = true;
         }
         return true;
      }
      this.searchFocused = false;

      // Close button.
      if (this.hit(mx, my, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE)) {
         return true;
      }
      // Category selection.
      for (int i = this.catHits.size() - 1; i >= 0; i--) {
         CatHit ch = this.catHits.get(i);
         if (this.hit(mx, my, ch.x, ch.y, ch.w, ch.h)) {
            this.selectedCat = ch.cat;
            this.scroll = 0.0F;
            return true;
         }
      }
      // Module toggle.
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         Hit hit = this.hits.get(i);
         if (this.hit(mx, my, hit.x, hit.y, hit.w, hit.h)) {
            hit.module.toggle();
            return true;
         }
      }
      return true;
   }

   /** True if the click is on the close button (the caller closes the whole GUI). */
   public boolean hitClose(float mx, float my) {
      return this.hit(mx, my, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
   }

   public boolean mouseScrolled(float mx, float my, double scrollY, float uiW, float uiH) {
      float bodyTop = this.searchY + this.searchH + 18.0F;
      float bodyH = uiH - bodyTop - 20.0F;
      if (this.hit(mx, my, SIDEBAR_W + 18.0F, bodyTop, uiW - SIDEBAR_W - 36.0F, bodyH) && this.maxScroll > 0.0F) {
         this.scroll = Math.clamp(this.scroll - (float) scrollY * 34.0F, 0.0F, this.maxScroll);
         return true;
      }
      return false;
   }

   /** Handles a key press in minimal mode. Escape signals close (returns true). */
   public boolean keyPressed(int keyCode) {
      if (keyCode == 256) {
         return true; // Escape — caller closes the GUI.
      }
      if (keyCode == 259 && !this.search.isEmpty()) {
         this.search = this.search.substring(0, this.search.length() - 1);
         return true;
      }
      if (keyCode == 257 || keyCode == 335) {
         this.searchFocused = false;
         return true;
      }
      return false;
   }

   /** Appends a typed character to the search (when focused). */
   public boolean charTyped(int codepoint) {
      if (!this.searchFocused) {
         return false;
      }
      if (this.search.length() < 32) {
         this.search += Character.toLowerCase(codepoint);
      }
      return true;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
