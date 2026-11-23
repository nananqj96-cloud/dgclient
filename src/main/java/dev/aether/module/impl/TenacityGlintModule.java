package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityGlintModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Glint speed", 1.0, 0.1, 5.0, 0.1));
   public final BooleanSetting custom = this.addSetting(new BooleanSetting("Custom", "Use custom glint texture", true));

   public TenacityGlintModule() {
      super("Glint", "Customizes item glint effects", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.getMainHandItem().setDamageValue(0);

   }
}
