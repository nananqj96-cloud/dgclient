package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityAntiStealModule extends Module {
   public final BooleanSetting blockClicks = this.addSetting(new BooleanSetting("Block Clicks", "Block chest/inventory clicks by others", true));

   public TenacityAntiStealModule() {
      super("AntiSteal", "Prevents others from taking items from your inventory", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      // Prevent container opening by others - handled via packet mixin
   }
}
