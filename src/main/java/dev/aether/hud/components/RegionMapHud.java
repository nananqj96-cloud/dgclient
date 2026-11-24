package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.impl.RegionMapModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.ThemeManager;

public class RegionMapHud extends HudComponent {
   private final RegionMapModule module;

   public RegionMapHud(RegionMapModule var1, ThemeManager var2) {
      super("regionMap", 0.008F, 0.05F, var1::isEnabled);
      this.module = var1;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 124.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return 144.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      // Region map HUD rendering stub - detailed region grid rendering disabled
   }
}
