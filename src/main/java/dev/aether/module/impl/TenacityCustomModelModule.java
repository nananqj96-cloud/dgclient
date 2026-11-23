package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityCustomModelModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Model scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting firstPerson = this.addSetting(new BooleanSetting("FirstPerson", "Show custom model in first person", true));
   public final BooleanSetting thirdPerson = this.addSetting(new BooleanSetting("ThirdPerson", "Show custom model in third person", true));

   public TenacityCustomModelModule() {
      super("CustomModel", "Applies a custom model to your player", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(true);


   }
}
