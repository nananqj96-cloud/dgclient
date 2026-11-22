package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityAntiVoidModule extends Module {
   public final SliderSetting y = this.addSetting(new SliderSetting("Y", "Fallback Y position", -64.0, -256.0, -1.0, 1.0, "y"));

   public TenacityAntiVoidModule() {
      super("AntiVoid", "Prevents falling into the void", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (mc.player.getY() < y.getFloat()) {
         mc.player.setPos(mc.player.getX(), y.getFloat() + 1.0, mc.player.getZ());
      }
   }
}
