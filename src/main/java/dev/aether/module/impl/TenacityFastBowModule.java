package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;

public class TenacityFastBowModule extends Module {
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Arrow speed multiplier", 5.0, 1.0, 20.0, 0.5));
   private boolean charging;

   public TenacityFastBowModule() {
      super("FastBow", "Fires arrows at increased speed", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      if (mc.player.getMainHandItem().getItem() != Items.BOW) return;
      if (mc.options.keyUse.isDown()) {
         charging = true;
      } else if (charging) {
         for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof AbstractArrow arrow && arrow.owner() == mc.player) {
               arrow.setDeltaMovement(arrow.getDeltaMovement().multiply(speed.getFloat(), speed.getFloat(), speed.getFloat()));
            }
         }
         charging = false;
      }
   }
}
