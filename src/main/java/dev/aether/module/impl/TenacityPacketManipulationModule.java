package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityPacketManipulationModule extends Module {
   public final SliderSetting lag = this.addSetting(new SliderSetting("Lag", "Packet lag in ms", 0.0, 0.0, 2000.0, 100.0, "ms"));
   public final BooleanSetting drop = this.addSetting(new BooleanSetting("Drop", "Drop packets randomly", false));
   public final BooleanSetting duplicate = this.addSetting(new BooleanSetting("Duplicate", "Duplicate packets", false));

   public TenacityPacketManipulationModule() {
      super("PacketManipulation", "Manipulates network packets for latency control", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || mc.player.connection == null) return;
      mc.player.connection.sendChat("/packetmanipulation");

   }
}
