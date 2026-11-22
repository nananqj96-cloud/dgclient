package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.EntityAttributeInstance;
import net.minecraft.world.entity.ai.attributes.EntityAttributes;

public class TenacityStepModule extends Module {
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Step height for walking up blocks", 1.0, 0.6, 2.0, 0.1, "")
   );

   public TenacityStepModule() {
      super("Tenacity Step", "Walk up blocks without jumping (Tenacity port)", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      EntityAttributeInstance attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
      if (attr != null && Math.abs(attr.getBaseValue() - height.get()) > 1.0E-6) {
         attr.setBaseValue(height.get());
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         EntityAttributeInstance attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
         if (attr != null) attr.setBaseValue(0.6);
      }
   }
}
