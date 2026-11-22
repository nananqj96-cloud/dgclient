package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityGodModeModule extends Module {
   public final BooleanSetting fire = this.addSetting(new BooleanSetting("Fire", "Fire immunity", true));
   public final BooleanSetting lava = this.addSetting(new BooleanSetting("Lava", "Lava immunity", true));
   public final BooleanSetting fall = this.addSetting(new BooleanSetting("Fall", "Fall damage immunity", true));
   public final SliderSetting health = this.addSetting(new SliderSetting("Health", "Minimum health to maintain", 20.0, 1.0, 40.0, 1.0, "hp"));

   public TenacityGodModeModule() {
      super("GodMode", "Provides immunity to damage", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (mc.player.getHealth() < health.getFloat()) {
         mc.player.setHealth(health.getFloat());
      }
      if (fire.get() && mc.player.isOnFire()) {
         mc.player.setRemainingFireTicks(0);
      }
      if (fall.get()) {
         mc.player.fallDistance = 0.0F;
      }
   }
}
