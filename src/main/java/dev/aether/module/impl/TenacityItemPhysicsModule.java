package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityItemPhysicsModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Item scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting floating = this.addSetting(new BooleanSetting("Float", "Items float in inventory", true));
   public final BooleanSetting rotate = this.addSetting(new BooleanSetting("Rotate", "Items rotate in inventory", true));

   public TenacityItemPhysicsModule() {
      super("ItemPhysics", "Adds physics to items in the inventory", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(1.0, 0.0, 1.0));


   }
}
