package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityStreamerModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Overlay scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting watermark = this.addSetting(new BooleanSetting("Watermark", "Show stream watermark", true));
   public final BooleanSetting chat = this.addSetting(new BooleanSetting("Chat", "Show chat overlay", true));

   public TenacityStreamerModule() {
      super("Streamer", "Streaming overlay for streaming setups", Category.RENDER);
   }

   @Override
   public void onTick() {
      
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.player == null) return;
      mc.player.setGlowingTag(true);

   }
}
