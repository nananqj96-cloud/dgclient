package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityMCPModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "MCP overlay scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting module = this.addSetting(new BooleanSetting("Module", "Show MCP module info", true));
   public final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Show ping info", true));

   public TenacityMCPModule() {
      super("MCP", "Minecraft Chat Protocol overlay", Category.PLAYER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null || mc.player.connection == null) return;
      mc.player.connection.sendChat("/mcp");

   }
}
