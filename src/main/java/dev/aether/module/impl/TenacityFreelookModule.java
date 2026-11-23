package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityFreelookModule extends Module {
   public final SliderSetting sensitivity = this.addSetting(new SliderSetting("Sensitivity", "Camera sensitivity", 1.0, 0.1, 3.0, 0.1));
   public final BooleanSetting toggle = this.addSetting(new BooleanSetting("Toggle", "Toggle freelook on key", true));

   private boolean wasToggled;

   public TenacityFreelookModule() {
      super("Freelook", "Free camera rotation for spectating", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.getAbilities().mayfly = true;

   }
}
