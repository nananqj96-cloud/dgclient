package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityHudModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "HUD scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting armor = this.addSetting(new BooleanSetting("Armor", "Show armor bar", true));
   public final BooleanSetting health = this.addSetting(new BooleanSetting("Health", "Show health bar", true));
   public final BooleanSetting hunger = this.addSetting(new BooleanSetting("Hunger", "Show hunger bar", true));
   public final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Show ping", true));
   public final BooleanSetting fps = this.addSetting(new BooleanSetting("FPS", "Show FPS", true));

   public TenacityHudModule() {
      super("HUD", "Customizable heads-up display", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(health.get());


   }
}
