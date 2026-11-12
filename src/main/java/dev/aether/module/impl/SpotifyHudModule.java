package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Modules;

public final class SpotifyHudModule extends Modules.SpotifyModule {
   public SpotifyHudModule() {
      this.setEnabled(false);
      this.volume.set(Boolean.valueOf(false));
      this.volume.visibleWhen(() -> false);
   }

   @Override
   public String getDescription() {
      return "Shows the Windows Spotify desktop song locally. No account or scripts required.";
   }

   @Override
   protected void onEnable() {
      this.sync(true);
   }

   @Override
   protected void onDisable() {
      this.sync(false);
   }

   @Override
   public void onTick() {
      this.sync(!this.source.is("Demo"));
   }

   private void sync(boolean var1) {
      if (AetherClient.spotify() != null) {
         AetherClient.spotify().setEnabled(var1);
      }
   }
}
