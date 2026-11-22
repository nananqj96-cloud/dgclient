package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAutoAuthenticateModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Auth delay ms", 1000.0, 500.0, 5000.0, 500.0, "ms"));
   public final BooleanSetting auto = this.addSetting(new BooleanSetting("Auto", "Auto-send auth on join", true));

   private long lastAuth;

   public TenacityAutoAuthenticateModule() {
      super("AutoAuthenticate", "Automatically authenticates on server join", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (auto.get() && System.currentTimeMillis() - lastAuth > delay.getInt()) {
         lastAuth = System.currentTimeMillis();
         // Auth sending handled via mixin
      }
   }
}
