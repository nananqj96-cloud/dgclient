package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

public class TenacityAutoPotModule extends Module {
   public final SliderSetting health = this.addSetting(new SliderSetting("Health", "Pot when health drops below", 15.0, 1.0, 36.0, 1.0, "hp"));
   public final BooleanSetting duringFight = this.addSetting(new BooleanSetting("During Fight", "Only pot during combat", false));
   private int previous = -1;
   private boolean drinking;

   public TenacityAutoPotModule() {
      super("AutoPot", "Automatically drinks potions when health is low", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) { stop(); return; }
      Player p = mc.player;
      int hp = p.getHealth();
      if (hp <= health.getInt() && (!drinking || p.getHealth() < 20)) {
         if (!drinking) {
            int slot = findPotion(mc);
            if (slot < 0) return;
            previous = p.getInventory().getSelectedSlot();
            p.getInventory().setSelectedSlot(slot);
            drinking = true;
         }
         mc.options.keyUse.setDown(true);
      } else {
         stop();
      }
   }

   private int findPotion(Minecraft mc) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.has(DataComponents.POTION)) {
            return i;
         }
      }
      return -1;
   }

   private void stop() {
      Minecraft mc = Minecraft.getInstance();
      if (drinking) {
         drinking = false;
         if (mc.options != null) mc.options.keyUse.setDown(false);
         if (previous >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(previous);
         previous = -1;
      }
   }

   @Override
   protected void onDisable() { stop(); }
}
