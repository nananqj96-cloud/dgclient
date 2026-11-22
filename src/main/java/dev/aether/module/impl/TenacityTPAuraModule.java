package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTPAuraModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "TPA range", 50.0, 10.0, 100.0, 5.0, "m"));

   public TenacityTPAuraModule() {
      super("TPAura", "Auto-accepts TPA requests from nearby players", Category.COMBAT);
   }

   @Override
   public void onTick() {
      // TPA auto-accept handled via mixin on chat/command
   }
}
