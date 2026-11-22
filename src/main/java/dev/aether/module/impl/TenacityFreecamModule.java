package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityFreecamModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Fly speed", 10.0, 1.0, 50.0, 1.0));
   private boolean wasEnabled;

   public TenacityFreecamModule() {
      super("Freecam", "Fly around the world without physics", Category.RENDER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (!wasEnabled && isEnabled()) {
         mc.player.setPos(mc.player.getX(), mc.player.getY() + 0.1, mc.player.getZ());
         wasEnabled = true;
      }
      if (wasEnabled && !isEnabled()) {
         wasEnabled = false;
      }
   }
}
