package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityBlockOverlayModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Overlay range", 5.0, 1.0, 20.0, 0.5, "m"));
   public final SliderSetting colorR = this.addSetting(new SliderSetting("Red", "Overlay red", 255.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorG = this.addSetting(new SliderSetting("Green", "Overlay green", 0.0, 0.0, 255.0, 1.0));
   public final SliderSetting colorB = this.addSetting(new SliderSetting("Blue", "Overlay blue", 0.0, 0.0, 255.0, 1.0));
   public final SliderSetting alpha = this.addSetting(new SliderSetting("Alpha", "Overlay opacity", 100.0, 0.0, 255.0, 1.0));

   public TenacityBlockOverlayModule() {
      super("BlockOverlay", "Draws a custom overlay on blocks", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || mc.level == null) return;
      var pos = mc.player.blockPosition();
      if (mc.level.getBlockState(pos).isAir()) return;
      mc.player.setGlowingTag(true);


   }
}
