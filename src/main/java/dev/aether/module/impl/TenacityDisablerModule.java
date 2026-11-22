package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityDisablerModule extends Module {
   public final BooleanSetting rotation = this.addSetting(new BooleanSetting("Rotation", "Disable server rotation", true));
   public final BooleanSetting movement = this.addSetting(new BooleanSetting("Movement", "Disable server movement updates", true));
   public final SliderSetting lag = this.addSetting(new SliderSetting("Lag", "Lag amount", 0.0, 0.0, 100.0, 1.0, "t"));

   public TenacityDisablerModule() {
      super("Disabler", "Disables server-side movement and rotation", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      // Disabler is handled via packet mixin; this module is a settings holder
   }
}
