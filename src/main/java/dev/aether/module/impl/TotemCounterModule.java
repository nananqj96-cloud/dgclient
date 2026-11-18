package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;

/**
 * HUD counter that tracks how many Totems of Undying you have in your inventory and displays the
 * count on screen.
 */
public class TotemCounterModule extends Module {
   public final ModeSetting style = this.addSetting(
      new ModeSetting("Style", "HUD display style", "Auto", "Auto", "Native", "Classic")
   );
   public final BooleanSetting showLabel = this.addSetting(
      new BooleanSetting("Show Label", "Show the 'TOTEMS' label above the count", true)
   );

   private int lastCount;

   public TotemCounterModule() {
      super("Totem Counter", "Track and display your totem count on the HUD", Category.VISUALS);
   }

   public int countTotems() {
      net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
      if (mc.player == null) {
         return 0;
      }
      int count = 0;
      net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         if (inv.getItem(i).getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
            count++;
         }
      }
      return count;
   }

   public int getCount() {
      int c = this.countTotems();
      if (c != this.lastCount) {
         this.lastCount = c;
      }
      return this.lastCount;
   }

   public boolean isClassic() {
      String s = this.style.get();
      return "Classic".equals(s) || ("Auto".equals(s) && false);
   }

   public boolean isNative() {
      String s = this.style.get();
      return "Native".equals(s) || ("Auto".equals(s) && true);
   }

   public boolean showLabel() {
      return this.showLabel.get();
   }
}
