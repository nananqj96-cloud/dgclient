package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityBrightPlayersModule extends Module {
   public final SliderSetting brightness = this.addSetting(new SliderSetting("Brightness", "Player brightness", 100.0, 0.0, 200.0, 10.0, "%"));
   public final BooleanSetting glow = this.addSetting(new BooleanSetting("Glow", "Player glow", true));

   public TenacityBrightPlayersModule() {
      super("BrightPlayers", "Makes players appear brighter on screen", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.level == null) return;
      for (var e : mc.level.entitiesForRendering()) {
         if (e instanceof net.minecraft.world.entity.player.Player p && p != mc.player) {
            p.setGlowingTag(true);
         }
      }

   }
}
