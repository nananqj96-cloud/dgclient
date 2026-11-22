package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

public class TenacityTriggerBotModule extends Module {
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Trigger attack range", 4.0, 1.0, 8.0, 0.5, "")
   );
   public final SliderSetting delay = this.addSetting(
      new SliderSetting("Delay", "Delay between attacks in ticks", 0.0, 0.0, 10.0, 1.0, "t")
   );
   public final BooleanSetting combatOnly = this.addSetting(
      new BooleanSetting("Combat Only", "Only trigger when holding a weapon", true)
   );

   private int tickCounter;

   public TenacityTriggerBotModule() {
      super("Tenacity Trigger Bot", "Auto-attacks entities you look at (Tenacity port)", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
      if (tickCounter > 0) { tickCounter--; return; }
      if (delay.get() > 0) { tickCounter = (int) delay.get(); return; }

      if (mc.crosshairTarget instanceof EntityHitResult ehr) {
         Entity target = ehr.getEntity();
         if (target instanceof LivingEntity le && le.isAlive()
            && !le.isSpectator()
            && mc.player.distanceTo(le) <= range.get()) {
            if (!combatOnly.get() || isWeaponInHand(mc)) {
               mc.options.attackKey.setPressed(true);
               tickCounter = 1;
               return;
            }
         }
      }
      mc.options.attackKey.setPressed(false);
   }

   private boolean isWeaponInHand(Minecraft mc) {
      var stack = mc.player.getMainHandStack();
      return !stack.isEmpty();
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.attackKey.setPressed(false);
   }
}
