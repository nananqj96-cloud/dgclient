package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityRegenModule extends Module {
   public final SliderSetting health = this.addSetting(new SliderSetting("Health", "Regen at this health", 50.0, 1.0, 100.0, 1.0, "hp"));
   public final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Regen speed", 1.0, 0.1, 5.0, 0.1));

   private int counter;

   public TenacityRegenModule() {
      super("Regen", "Regenerates health above a threshold", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (mc.player.getHealth() > health.getFloat() && mc.player.isAlive()) {
         mc.player.heal(speed.getFloat());
      }
   }
}
