package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAntiAuraModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Knockback range", 5.0, 1.0, 20.0, 0.5, "m"));
   public final BooleanSetting returnKill = this.addSetting(new BooleanSetting("ReturnKill", "Kill aura returning attackers", true));

   public TenacityAntiAuraModule() {
      super("AntiAura", "Pushes players attacking you out of range", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      for (var entity : mc.level.entitiesForRendering()) {
         if (entity == mc.player || !entity.isAlive()) continue;
         if (!(entity instanceof net.minecraft.world.entity.player.Player)) continue;
         double dist = entity.distanceTo(mc.player);
         if (dist <= range.get() && entity.getX() != mc.player.getX()) {
            double dx = mc.player.getX() - entity.getX();
            double dz = mc.player.getZ() - entity.getZ();
            double len = Math.hypot(dx, dz);
            if (len == 0) continue;
            entity.setDeltaMovement(dx / len * 0.5, 0.0, dz / len * 0.5);
         }
      }
   }
}
