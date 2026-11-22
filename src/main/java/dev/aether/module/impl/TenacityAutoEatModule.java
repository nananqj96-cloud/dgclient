package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class TenacityAutoEatModule extends Module {
   public final SliderSetting hunger = this.addSetting(
      new SliderSetting("Eat Below", "Eat when hunger drops to this level", 14.0, 1.0, 19.0, 1.0, "")
   );

   private int previous = -1;
   private boolean eating;

   public TenacityAutoEatModule() {
      super("Tenacity Auto Eat", "Eats food from your hotbar when hungry (Tenacity port)", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) {
         stop();
         return;
      }
      int food = mc.player.getFoodData().getFoodLevel();
      if (food <= hunger.getInt() && (!eating || food < 20)) {
         if (!eating) {
            int slot = findFood(mc);
            if (slot < 0) return;
            previous = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(slot);
            eating = true;
         }
         mc.options.keyUse.setDown(true);
      } else {
         stop();
      }
   }

   private int findFood(Minecraft mc) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
            return i;
         }
      }
      return -1;
   }

   private void stop() {
      Minecraft mc = Minecraft.getInstance();
      if (eating) {
         eating = false;
         if (mc.options != null) mc.options.keyUse.setDown(false);
         if (previous >= 0 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previous);
         }
         previous = -1;
      }
   }

   @Override
   protected void onDisable() {
      stop();
   }
}
