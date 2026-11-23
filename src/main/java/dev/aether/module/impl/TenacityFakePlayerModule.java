package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityFakePlayerModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Fake player scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", "Fake player follows rotation", true));

   public TenacityFakePlayerModule() {
      super("FakePlayer", "Spawns a fake player for testing", Category.PLAYER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(true);

   }
}
