package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class TenacityAntiBotModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Detection range", 50.0, 10.0, 150.0, 5.0, "m"));
   public final SliderSetting maxPing = this.addSetting(new SliderSetting("Max Ping", "Suspicious ping threshold", 200.0, 50.0, 1000.0, 10.0, "ms"));
   public final BooleanSetting mark = this.addSetting(new BooleanSetting("Mark", "Highlight detected bots", true));
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Bot highlight color", 0xFFFF0000));

   public TenacityAntiBotModule() {
      super("AntiBot", "Detects and marks bot players", Category.COMBAT);
   }

   public boolean isBot(Player player) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() == null) return false;
      var info = mc.getConnection().getPlayerInfo(player.getUUID());
      if (info == null) return false;
      if (info.latency() > maxPing.getInt()) return true;
      return info.latency() == 0;
   }
}
