package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

public class TenacityAutoRespawnModule extends Module {
   public TenacityAutoRespawnModule() {
      super("Tenacity Auto Respawn", "Respawns immediately after dying (Tenacity port)", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.screen instanceof DeathScreen) {
         mc.player.requestRespawn();
      }
   }
}
