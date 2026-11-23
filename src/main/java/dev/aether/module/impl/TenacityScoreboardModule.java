package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityScoreboardModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Scoreboard scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting custom = this.addSetting(new BooleanSetting("Custom", "Use custom scoreboard", true));
   public final BooleanSetting showTeam = this.addSetting(new BooleanSetting("ShowTeam", "Show team colors", true));

   public TenacityScoreboardModule() {
      super("Scoreboard", "Customizes the scoreboard display", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) return;
      mc.player.setGlowingTag(custom.get());


   }
}
