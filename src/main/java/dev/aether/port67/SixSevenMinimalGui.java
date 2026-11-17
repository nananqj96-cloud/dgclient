package dev.aether.port67;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Experimental minimalistic GUI for the 67port, in the style of the Minecraft "Vape" client's
 * ClickGUI: a flat, monochrome dark/white design with <b>no glow, no blur, no glass chrome</b>.
 *
 * <p>Enabled module rows render as paper-white cards with near-black text; disabled rows render dark
 * with grey text. When the {@code Experimental Minimal GUI} setting is on, {@link SixSevenPortWindow}
 * renders through this class instead of the default ported 67client dropdown chrome — so it
 * <em>overrides both</em> the panel (inside the Aether ClickGui) and the floating-window versions.
 *
 * <p>Module toggles still drive Aether's shared {@link ModuleManager}, so it stays in sync with the
 * main Aether GUI.
 */
public final class SixSevenMinimalGui {
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
   private static final float HEADER_H = 36.0F;
   private static final float ROW_H = 28.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float CAT_PAD = 10.0F;
   private static final float CAT_H = 22.0F;

   private final ModuleManager modules;
   private final ThemeManager themes;

   private final List<Hit> hits = new ArrayList<>();
   private float closeX;
   private float closeY;
   private float closeSize = 20.0F;
   private float headerX;
   private float headerY;
   private float headerW;
   private float headerH;

   private float scroll;
   private float maxScroll;
   private String search = "";

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

   public SixSevenMinimalGui(ModuleManager modules, ThemeManager themes) {
      this.modules = modules;
      this.themes = themes;
   }

   public void setSearch(String search) {
      this.search = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();
   }

   public float scroll() {
      return this.scroll;
   }

   /** Draws the full minimal window (flat shell + scrollable module list). */
   public void render(NVGRenderer vg, float mx, float my, float x, float y, float w, float h) {
      this.hits.clear();
      this.headerX = x;
      this.headerY = y;
      this.headerW = w;
      this.headerH = HEADER_H;

      try {
         // Flat dark shell — no glow.
         vg.rect(x, y, w, h, 10.0F, C_BG);
         vg.rectOutline(x, y, w, h, 10.0F, 1.0F, C_BORDER);

         // Header band (flat, no glow) — rounded top corners only.
         vg.rectVarying(x, y, w, HEADER_H, 10.0F, 10.0F, 0.0F, 0.0F, C_HEADER);
         vg.rect(x, y + HEADER_H - 1.0F, w, 1.0F, 0.0F, C_BORDER_SOFT);
         vg.text("67 PORT", x + 16.0F, y + HEADER_H * 0.5F, 13.0F, C_TEXT);
         vg.text("minimal", x + 16.0F + vg.textWidth("67 PORT", 13.0F) + 8.0F, y + HEADER_H * 0.5F, 10.0F, C_MUTED);

         // Close button (flat X).
         this.closeX = x + w - 28.0F;
         this.closeY = y + (HEADER_H - this.closeSize) * 0.5F;
         boolean cHov = this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize);
         vg.rect(this.closeX, this.closeY, this.closeSize, this.closeSize, 5.0F, Colors.withAlpha(C_WHITE, cHov ? 0.12F : 0.05F));
         vg.cross(this.closeX + 5.0F, this.closeY + 5.0F, 10.0F, 1.4F, cHov ? C_TEXT : C_MUTED);

         // Body (scissor-clipped, scrollable).
         float bodyTop = y + HEADER_H;
         float bodyH = h - HEADER_H;
         vg.save();
         vg.scissor(x + 1.0F, bodyTop, w - 2.0F, bodyH);

         float contentY = bodyTop + 10.0F - this.scroll;
         float contentH = this.computeContentHeight(vg);
         this.maxScroll = Math.max(0.0F, contentH - bodyH);
         this.scroll = Math.clamp(this.scroll, 0.0F, this.maxScroll);

         float innerX = x + 12.0F;
         float innerW = w - 24.0F;
         for (Category cat : Category.values()) {
            List<Module> inCat = this.modules.inCategory(cat);
            if (inCat.isEmpty()) {
               continue;
            }
            // Category section header.
            vg.text(cat.getDisplayName().toUpperCase(Locale.ROOT), innerX, contentY + CAT_H * 0.5F, 10.5F, C_MUTED);
            contentY += CAT_H + 4.0F;

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
               float dotX = innerX + 16.0F;
               float dotY = contentY + ROW_H * 0.5F;
               vg.circle(dotX, dotY, 3.4F, enabled ? C_WHITE : C_MUTED);

               // Module name.
               vg.text(module.getName(), innerX + 28.0F, dotY, 12.0F, enabled ? C_DARK : C_MUTED);

               contentY += ROW_H + ROW_GAP;
            }
            contentY += 6.0F;
         }

         if (this.maxScroll > 1.0F) {
            float trackH = bodyH - 12.0F;
            float thumbH = Math.max(24.0F, trackH * (bodyH / Math.max(1.0F, contentH)));
            float thumbY = bodyTop + 6.0F + (trackH - thumbH) * (this.scroll / this.maxScroll);
            vg.rect(x + w - 7.0F, bodyTop + 6.0F, 2.5F, trackH, 1.2F, C_BORDER_SOFT);
            vg.rect(x + w - 7.0F, thumbY, 2.5F, thumbH, 1.2F, Colors.withAlpha(C_WHITE, 0.55F));
         }
         vg.restore();
      } catch (Throwable t) {
         AetherClient.LOGGER.error("67Port minimal GUI render failed", t);
      }
   }

   private float computeContentHeight(NVGRenderer vg) {
      float h = 10.0F;
      for (Category cat : Category.values()) {
         List<Module> inCat = this.modules.inCategory(cat);
         if (inCat.isEmpty()) {
            continue;
         }
         h += CAT_H + 4.0F;
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

   /** Returns true if a module row or the close button consumed the click. */
   public boolean mouseClicked(float mx, float my, int btn) {
      if (btn != 0) {
         return false;
      }
      if (this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize)) {
         return true;
      }
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         Hit hit = this.hits.get(i);
         if (this.hit(mx, my, hit.x, hit.y, hit.w, hit.h)) {
            hit.module.toggle();
            return true;
         }
      }
      return false;
   }

   /** True if the click is on the header (used to start window dragging). */
   public boolean hitHeader(float mx, float my) {
      return this.hit(mx, my, this.headerX, this.headerY, this.headerW, this.headerH);
   }

   /** True if the click is on the close button. */
   public boolean hitClose(float mx, float my) {
      return this.hit(mx, my, this.closeX, this.closeY, this.closeSize, this.closeSize);
   }

   public boolean mouseScrolled(float mx, float my, double scrollY, float x, float y, float w, float h) {
      float bodyTop = y + HEADER_H;
      float bodyH = h - HEADER_H;
      if (this.hit(mx, my, x, bodyTop, w, bodyH) && this.maxScroll > 0.0F) {
         this.scroll = Math.clamp(this.scroll - (float) scrollY * 34.0F, 0.0F, this.maxScroll);
         return true;
      }
      return false;
   }

   private boolean hit(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}
