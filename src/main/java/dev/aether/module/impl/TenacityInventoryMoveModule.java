package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityInventoryMoveModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Move delay ticks", 3.0, 1.0, 20.0, 1.0, "t"));
   public final BooleanSetting autoSwap = this.addSetting(new BooleanSetting("AutoSwap", "Auto-swap items to hand", true));

   private int counter;

   public TenacityInventoryMoveModule() {
      super("InventoryMove", "Moves items to your hand automatically", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (counter > 0) { counter--; return; }
      var inv = mc.player.getInventory();
      if (autoSwap.get()) {
         int current = inv.getSelectedSlot();
         for (int i = 0; i < 9; i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty()) {
               inv.setSelectedSlot(i);
               counter = delay.getInt();
               return;
            }
         }
         inv.setSelectedSlot(current);
      }
   }
}
