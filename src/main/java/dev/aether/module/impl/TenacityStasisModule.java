package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityStasisModule extends Module {
   public final SliderSetting y = this.addSetting(new SliderSetting("Y", "Stasis height", 1.0, 0.1, 10.0, 0.1, "y"));
   public final BooleanSetting freeze = this.addSetting(new BooleanSetting("Freeze", "Freeze vertical movement", true));

   public TenacityStasisModule() {
      super("Stasis", "Holds you at a fixed height in the air", Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (freeze.get()) {
         mc.player.setPos(mc.player.getX(), y.getFloat(), mc.player.getZ());
      }
   }
}
