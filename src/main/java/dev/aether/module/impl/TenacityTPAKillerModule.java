package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTPAKillerModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Detection range", 100.0, 10.0, 500.0, 10.0, "m"));
   public final BooleanSetting notify = this.addSetting(new BooleanSetting("Notify", "Notify on TPA detection", true));

   public TenacityTPAKillerModule() {
      super("TPAKiller", "Detects and blocks TPA requests", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || mc.player.connection == null) return;
      for (var e : mc.level.entries().getEntities()) {
         if (e instanceof net.minecraft.world.entity.player.Player p && p != mc.player && mc.player.distanceTo(p) < 10.0) {
            mc.player.connection.sendChat("/tpakiller");
         }
      }

   }
}
