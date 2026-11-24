package dev.aether.hud.components;

import dev.aether.hud.HudComponent;
import dev.aether.module.impl.StaffListModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.ThemeManager;

public class StaffListHud extends HudComponent {
   private final StaffListModule module;

   public StaffListHud(StaffListModule var1, ThemeManager var2) {
      super("staffList", 0.008F, 0.05F, var1::isEnabled);
      this.module = var1;
   }

   @Override
   public float measureWidth(NVGRenderer var1) {
      return 92.0F;
   }

   @Override
   public float measureHeight(NVGRenderer var1) {
      return 16.0F;
   }

   @Override
   public void render(NVGRenderer var1, float var2, float var3, float var4, float var5) {
      // Staff list HUD rendering stub - detailed staff listing rendering disabled
   }
}
