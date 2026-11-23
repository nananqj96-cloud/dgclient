package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityMurderMysteryModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Overlay scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting highlights = this.addSetting(new BooleanSetting("Highlights", "Highlight murderer", true));
   public final BooleanSetting timer = this.addSetting(new BooleanSetting("Timer", "Show game timer", true));

   public TenacityMurderMysteryModule() {
      super("MurderMystery", "Murder Mystery game overlay", Category.MISC);
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
