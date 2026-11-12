package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.module.ModuleManager;
import dev.aether.module.Modules;
import dev.aether.render.anim.Animation;
import dev.aether.render.anim.Easing;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class ArrayListHud extends HudComponent {
   private static final float ENTRY_HEIGHT = 20.0F;
   private static final float FONT_SIZE = 13.0F;
   private static final float PAD_X = 9.0F;
   private static final float STRIP_W = 2.5F;
   private final ModuleManager modules;
   private final Modules.HudModule hudModule;
   private final ThemeManager themes;
   private final Map<Module, Animation> slide = new HashMap<>();
   private final List<Module> activeScratch = new ArrayList<>(32);

   public ArrayListHud(ModuleManager modules, Modules.HudModule hudModule, ThemeManager themes, BooleanSupplier visible) {
      super("Active Modules", 0.992F, 0.012F, visible);
      this.modules = modules;
      this.hudModule = hudModule;
      this.themes = themes;
   }

   private List<Module> collectVisible(NVGRenderer vg) {
      this.activeScratch.clear();
      for (Module m : this.modules.all()) {
         if (m.getCategory() == Category.CLIENT) {
            continue;
         }
         Animation anim = this.slide.computeIfAbsent(m, k -> new Animation(180.0F, k.isEnabled() ? 1.0F : 0.0F, Easing.EASE_OUT_CUBIC));
         anim.setTarget(m.isEnabled() ? 1.0F : 0.0F);
         if (anim.value() > 0.01F) {
            this.activeScratch.add(m);
         }
      }
      this.activeScratch.sort((a, b) -> Float.compare(vg.textWidth(b.getName(), FONT_SIZE), vg.textWidth(a.getName(), FONT_SIZE)));
      return this.activeScratch;
   }

   @Override
   public float measureWidth(NVGRenderer vg) {
      float maxW = 96.0F;
      for (Module m : this.collectVisible(vg)) {
         maxW = Math.max(maxW, vg.textWidth(m.getName(), FONT_SIZE) + PAD_X * 2.0F + STRIP_W + 4.0F);
      }
      return maxW;
   }

   @Override
   public float measureHeight(NVGRenderer vg) {
      float total = 0.0F;
      for (Module m : this.collectVisible(vg)) {
         Animation anim = this.slide.get(m);
         total += (ENTRY_HEIGHT + 2.0F) * (anim == null ? 1.0F : anim.value());
      }
      return Math.max(ENTRY_HEIGHT, total);
   }

   @Override
   public void render(NVGRenderer vg, float x, float y, float w, float h) {
      Theme theme = this.themes.current();
      int baseCol = this.hudModule.themeSync.get() ? theme.accent() : this.hudModule.listColor.get();
      int brightCol = Colors.lighten(baseCol, 0.24F);
      boolean rightAligned = this.getFx() >= 0.5F;

      float curY = y;
      int idx = 0;
      for (Module m : this.collectVisible(vg)) {
         Animation anim = this.slide.get(m);
         float t = anim == null ? 1.0F : Math.clamp(anim.value(), 0.0F, 1.0F);
         float tw = vg.textWidth(m.getName(), FONT_SIZE);
         float rowW = tw + PAD_X * 2.0F + STRIP_W;
         float baseX = rightAligned ? (x + w - rowW) : x;
         float slideOffset = (1.0F - t) * (rightAligned ? (rowW + 8.0F) : -(rowW + 8.0F));
         float rx = baseX + slideOffset;

         float wave = (float)(0.5 + 0.5 * Math.sin(((double)System.nanoTime() / 6.5E8) - idx * 0.45));
         int rowAccent = Colors.lerp(baseCol, brightCol, wave);

         vg.save();
         vg.alpha(t);
         vg.rect(rx, curY, rowW, ENTRY_HEIGHT, 5.0F, Colors.withAlpha(theme.background(), 0.88F));
         vg.rectOutline(rx, curY, rowW, ENTRY_HEIGHT, 5.0F, 1.0F, Colors.withAlpha(rowAccent, 0.22F));
         if (rightAligned) {
            vg.rect(rx + rowW - STRIP_W, curY + 3.0F, STRIP_W, ENTRY_HEIGHT - 6.0F, 1.25F, rowAccent);
            vg.text(m.getName(), rx + PAD_X, curY + ENTRY_HEIGHT * 0.5F, FONT_SIZE, theme.textPrimary());
         } else {
            vg.rect(rx, curY + 3.0F, STRIP_W, ENTRY_HEIGHT - 6.0F, 1.25F, rowAccent);
            vg.text(m.getName(), rx + STRIP_W + PAD_X, curY + ENTRY_HEIGHT * 0.5F, FONT_SIZE, theme.textPrimary());
         }
         vg.restore();

         curY += (ENTRY_HEIGHT + 2.0F) * t;
         idx++;
      }
   }
}
