package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityMusicPlayerModule extends Module {
   public final SliderSetting volume = this.addSetting(new SliderSetting("Volume", "Music volume", 50.0, 0.0, 100.0, 5.0, "%"));
   public final BooleanSetting ambient = this.addSetting(new BooleanSetting("Ambient", "Ambient background music", true));
   public final BooleanSetting loop = this.addSetting(new BooleanSetting("Loop", "Loop music", true));

   public TenacityMusicPlayerModule() {
      super("MusicPlayer", "In-game music player with ambient tracks", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Music player handled via mixin
   }
}
