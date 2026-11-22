package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TenacityIdleFighterModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Range", 5.0, 1.0, 20.0, 0.5, "m"));
   private int counter;

   public TenacityIdleFighterModule() {
      super("IdleFighter", "Attacks nearby targets when idle", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      if (mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) return;
      if (counter > 0) { counter--; return; }
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
         if (mc.player.distanceTo(e) <= range.get()) {
            mc.options.keyAttack.setDown(true);
            counter = 1;
            return;
         }
      }
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) mc.options.keyAttack.setDown(false);
   }
}
