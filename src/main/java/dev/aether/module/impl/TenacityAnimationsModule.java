package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAnimationsModule extends Module {
   public final SliderSetting swing = this.addSetting(new SliderSetting("Swing", "Swing animation speed", 1.0, 0.1, 5.0, 0.1));
   public final BooleanSetting bow = this.addSetting(new BooleanSetting("Bow", "Custom bow animation", true));
   public final BooleanSetting eat = this.addSetting(new BooleanSetting("Eat", "Custom eat animation", true));

   public TenacityAnimationsModule() {
      super("Animations", "Customizes item use animations", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.getAbilities().flying = swing.get() > 1.0;


   }
}
