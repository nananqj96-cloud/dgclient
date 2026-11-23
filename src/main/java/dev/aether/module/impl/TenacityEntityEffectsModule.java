package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityEntityEffectsModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Effect scale", 1.0, 0.5, 3.0, 0.1));
   public final BooleanSetting glow = this.addSetting(new BooleanSetting("Glow", "Glow effect on entities", true));
   public final BooleanSetting trails = this.addSetting(new BooleanSetting("Trails", "Motion trails on entities", true));

   public TenacityEntityEffectsModule() {
      super("EntityEffects", "Adds visual effects to entities", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(true);

   }
}
