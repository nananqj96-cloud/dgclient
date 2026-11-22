package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class TenacityChestStealerModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Steal delay", 1.0, 1.0, 10.0, 1.0, "t"));
   public final BooleanSetting autoClose = this.addSetting(new BooleanSetting("AutoClose", "Close chest after stealing", true));
   private int counter;

   public TenacityChestStealerModule() {
      super("ChestStealer", "Steals items from nearby chests automatically", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
         if (counter > 0) { counter--; return; }
         Player player = mc.player;
         Inventory inv = player.getInventory();
         var slots = screen.getMenu().getSlots();
         boolean stole = false;
         for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.hasItem() && inv.canPlaceItem(slot.getItem())) {
               screen.getMenu().removeItem(i, 1);
               stole = true;
               counter = delay.getInt();
            }
         }
         if (stole && autoClose.get()) { mc.player.closeContainer(); }
      }
   }
}
