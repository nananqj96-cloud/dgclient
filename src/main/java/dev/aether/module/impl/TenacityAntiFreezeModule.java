package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityAntiFreezeModule extends Module {
   public final BooleanSetting unfreeze = this.addSetting(new BooleanSetting("Unfreeze", "Break freeze when stuck", true));

   private int counter;

   public TenacityAntiFreezeModule() {
      super("AntiFreeze", "Prevents client freezing and lag spikes", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (unfreeze.get() && mc.player.isAlive() && mc.player.getDeltaMovement().lengthSq() < 0.001) {
         counter++;
         if (counter > 100) {
            counter = 0;
            mc.player.setPos(mc.player.getX() + 0.1, mc.player.getY(), mc.player.getZ());
         }
      }
   }
}
