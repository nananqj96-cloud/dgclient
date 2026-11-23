package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityCameraModule extends Module {
   public final SliderSetting fov = this.addSetting(new SliderSetting("FOV", "Camera FOV", 70.0, 30.0, 120.0, 1.0, "°"));
   public final SliderSetting pitch = this.addSetting(new SliderSetting("Pitch", "Camera pitch offset", 0.0, -90.0, 90.0, 1.0, "°"));
   public final BooleanSetting toggle = this.addSetting(new BooleanSetting("Toggle", "Toggle camera mode", true));

   public TenacityCameraModule() {
      super("Camera", "Modifies camera FOV and pitch", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.getAbilities().mayfly = true;

   }
}
