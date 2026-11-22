package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TenacitySuperKnockbackModule extends Module {
   public final SliderSetting pushX = this.addSetting(new SliderSetting("Push X", "Horizontal knockback", 1.0, 0.1, 5.0, 0.1));
   public final SliderSetting pushY = this.addSetting(new SliderSetting("Push Y", "Vertical knockback", 0.5, 0.1, 3.0, 0.1));
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Knockback range", 5.0, 1.0, 15.0, 0.5, "m"));

   public TenacitySuperKnockbackModule() {
      super("SuperKnockback", "Knocks targets back on hit", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.screen != null) return;
      if (!mc.options.keyAttack.isDown()) return;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
         if (mc.player.distanceTo(e) > range.get()) continue;
         double dx = le.getX() - mc.player.getX();
         double dz = le.getZ() - mc.player.getZ();
         double len = Math.hypot(dx, dz);
         if (len == 0) continue;
         le.setDeltaMovement(dx / len * pushX.getFloat(), pushY.getFloat(), dz / len * pushX.getFloat());
      }
   }
}
