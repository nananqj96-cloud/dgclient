package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityHypixelInfoModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Hypixel info scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Show ping", true));
   public final BooleanSetting game = this.addSetting(new BooleanSetting("Game", "Show current game", true));
   public final BooleanSetting guild = this.addSetting(new BooleanSetting("Guild", "Show guild info", false));

   public TenacityHypixelInfoModule() {
      super("HypixelInfo", "Displays Hypixel-specific information", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || mc.player.connection == null) return;
      // Hypixel info displayed via HUD overlay

   }
}
