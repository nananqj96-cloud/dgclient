package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTeamsModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Team overlay scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting highlight = this.addSetting(new BooleanSetting("Highlight", "Highlight teammates", true));
   public final BooleanSetting names = this.addSetting(new BooleanSetting("Names", "Show teammate names", true));

   public TenacityTeamsModule() {
      super("Teams", "Displays team information overlay", Category.MISC);
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
