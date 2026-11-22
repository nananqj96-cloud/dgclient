package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityPacketBreakModule extends Module {
   public final SliderSetting packets = this.addSetting(new SliderSetting("Packets", "Packets per tick", 5.0, 1.0, 20.0, 1.0));
   public final BooleanSetting instant = this.addSetting(new BooleanSetting("Instant", "Instant break packets", true));

   public TenacityPacketBreakModule() {
      super("PacketBreak", "Breaks blocks using packet spam", Category.PLAYER);
   }

   @Override
   public void onTick() {
      // Packet break handled via mixin
   }
}
