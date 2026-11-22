package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DeathScreen;

public class TenacityAutoRespawnModule extends Module {
   public TenacityAutoRespawnModule() {
      super("Tenacity Auto Respawn", "Respawns immediately after dying (Tenacity port)", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
      }
   }
}
