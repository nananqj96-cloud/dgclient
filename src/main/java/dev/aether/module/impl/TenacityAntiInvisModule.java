package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class TenacityAntiInvisModule extends Module {
   public final BooleanSetting render = this.addSetting(new BooleanSetting("Render", "Render invisible entities", true));

   public TenacityAntiInvisModule() {
      super("AntiInvis", "Makes invisible entities visible", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (e.isInvisible()) {
            e.setInvisible(false);
        }
      }
   }
}
