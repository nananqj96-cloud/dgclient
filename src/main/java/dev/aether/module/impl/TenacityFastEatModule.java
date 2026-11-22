package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public class TenacityFastEatModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Eat speed multiplier", 3.0, 1.0, 10.0, 0.5));
   public final BooleanSetting autoEat = this.addSetting(new BooleanSetting("AutoEat", "Auto-eat food items", true));

   public TenacityFastEatModule() {
      super("FastEat", "Eats food faster than normal", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      // Fast eat handled via mixin on use item
   }
}
