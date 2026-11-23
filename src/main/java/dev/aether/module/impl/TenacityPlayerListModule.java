package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityPlayerListModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Player list scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Show ping", true));
   public final BooleanSetting gamemode = this.addSetting(new BooleanSetting("Gamemode", "Show gamemode", true));

   public TenacityPlayerListModule() {
      super("PlayerList", "Customizes the player list overlay", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      for (var e : mc.level.entitiesForRendering()) {
         if (e instanceof net.minecraft.world.entity.player.Player p && p != mc.player) {
            p.setGlowingTag(ping.get());
         }
      }


   }
}
