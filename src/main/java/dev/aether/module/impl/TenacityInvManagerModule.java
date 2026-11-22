package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;

public class TenacityInvManagerModule extends Module {
   public final BooleanSetting autoArmor = this.addSetting(new BooleanSetting("AutoArmor", "Equip best armor automatically", true));
   public final BooleanSetting autoTool = this.addSetting(new BooleanSetting("AutoTool", "Select best tool in hand", true));
   private int counter;

   public TenacityInvManagerModule() {
      super("InvManager", "Manages inventory: auto-armor and auto-tool", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (counter > 0) { counter--; return; }
      Inventory inv = mc.player.getInventory();
      if (autoTool.get()) {
         var pos = mc.hitResult;
         if (pos instanceof net.minecraft.world.phys.BlockHitResult bhr) {
            var state = mc.level.getBlockState(bhr.getBlockPos());
            if (!state.isAir()) {
               int best = inv.getSelectedSlot();
               float bestS = -1;
               for (int i = 0; i < 9; i++) {
                  var stack = inv.getItem(i);
                  float s = stack.getDestroySpeed(state);
                  if (s > bestS) { bestS = s; best = i; }
               }
               inv.setSelectedSlot(best);
            }
         }
      }
      counter = 5;
   }
}
