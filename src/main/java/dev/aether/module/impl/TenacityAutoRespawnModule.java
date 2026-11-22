package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

public class TenacityAutoRespawnModule extends Module {
   public TenacityAutoRespawnModule() {
      super("Auto Respawn", "Respawns immediately after dying (ported)", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.screen instanceof DeathScreen) {
         mc.player.respawn();
      }
   }
}
