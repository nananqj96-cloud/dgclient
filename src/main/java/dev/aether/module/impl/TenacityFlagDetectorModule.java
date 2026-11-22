package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityFlagDetectorModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Anti-cheat flag detection range", 10.0, 1.0, 50.0, 1.0, "m"));
   public final BooleanSetting notify = this.addSetting(new BooleanSetting("Notify", "Send a notification on flag", true));

   public TenacityFlagDetectorModule() {
      super("FlagDetector", "Detects when anti-cheats flag your client", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      // Detection handled via mixin on packet handling
   }
}
