package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityNoSwingModule extends Module {
   public final BooleanSetting noSprint = this.addSetting(new BooleanSetting("No Sprint", "Prevent sprint animation", false));

   private boolean wasSwinging;

   public TenacityNoSwingModule() {
      super("NoSwing", "Suppress item swing animation", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (mc.options.keyAttack.isDown()) {
         wasSwinging = true;
      } else if (wasSwinging) {
         // Swing suppression handled via mixin
         wasSwinging = false;
      }
   }
}
