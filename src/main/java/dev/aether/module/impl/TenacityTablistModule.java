package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTablistModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Tablist scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Show ping next to players", true));
   public final BooleanSetting avatars = this.addSetting(new BooleanSetting("Avatars", "Show player avatars", true));

   public TenacityTablistModule() {
      super("Tablist", "Customizes the tablist appearance", Category.RENDER);
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
