package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TenacityAutoArmorModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Equip delay in ticks", 10.0, 1.0, 60.0, 1.0, "t"));
   public final BooleanSetting autoElytra = this.addSetting(new BooleanSetting("AutoElytra", "Auto-equip elytra", true));
   private int counter;

   private final int[] SLOT_MAP = {39, 38, 37, 36}; // helmet, chest, legs, boots

   public TenacityAutoArmorModule() {
      super("AutoArmor", "Automatically equips the best armor", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (counter > 0) { counter--; return; }
      var inv = mc.player.getInventory();
      for (int i = 0; i < 4; i++) {
         ItemStack current = inv.getItem(SLOT_MAP[i]);
         if (!current.isEmpty() && !isArmor(current)) continue;
         for (int slot = 0; slot < 36; slot++) {
            ItemStack candidate = inv.getItem(slot);
            if (isArmor(candidate) && getProtection(candidate) > getProtection(current)) {
               inv.setItem(SLOT_MAP[i], candidate);
               inv.setItem(slot, current);
               counter = delay.getInt();
               return;
            }
         }
      }
   }

   private boolean isArmor(ItemStack stack) {
      if (stack.isEmpty()) return false;
      var item = stack.getItem();
      return item == Items.LEATHER_HELMET || item == Items.IRON_HELMET || item == Items.GOLDEN_HELMET
         || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET
         || item == Items.LEATHER_CHESTPLATE || item == Items.IRON_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE
         || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE
         || item == Items.LEATHER_LEGGINGS || item == Items.IRON_LEGGINGS || item == Items.GOLDEN_LEGGINGS
         || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS
         || item == Items.LEATHER_BOOTS || item == Items.IRON_BOOTS || item == Items.GOLDEN_BOOTS
         || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS;
   }

   private int getProtection(ItemStack stack) {
      if (stack.isEmpty()) return 0;
      var item = stack.getItem();
      if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE
         || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) return 40;
      if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE
         || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) return 30;
      if (item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE
         || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS) return 20;
      if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE
         || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) return 15;
      return 5;
   }
}
