package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityGuiAnimationsModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Animation speed", 1.0, 0.1, 5.0, 0.1));
   public final BooleanSetting slide = this.addSetting(new BooleanSetting("Slide", "Slide animations", true));
   public final BooleanSetting fade = this.addSetting(new BooleanSetting("Fade", "Fade animations", true));
   public final BooleanSetting scale = this.addSetting(new BooleanSetting("Scale", "Scale animations", true));

   public TenacityGuiAnimationsModule() {
      super("GUIAnimations", "Animates GUI element transitions", Category.RENDER);
   }

   @Override
   public void onTick() {
      // GUI animations handled by ClickGui rendering
   }
}
