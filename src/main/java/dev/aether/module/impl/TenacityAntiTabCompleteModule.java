package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityAntiTabCompleteModule extends Module {
   public final BooleanSetting block = this.addSetting(new BooleanSetting("Block", "Block tab completion suggestions", true));

   public TenacityAntiTabCompleteModule() {
      super("AntiTabComplete", "Blocks tab-complete suggestions in chat", Category.MISC);
   }

   @Override
   public void onTick() {
      // Handled via mixin on chat input
   }
}
