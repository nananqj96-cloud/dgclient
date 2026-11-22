package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityTablistModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Tablist scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting ping = this.addSetting(new BooleanSetting("Ping", "Show ping next to players", true));
   public final BooleanSetting avatars = this.addSetting(new BooleanSetting("Avatars", "Show player avatars", true));

   public TenacityTablistModule() {
      super("Tablist", "Customizes the tablist appearance", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Tablist rendering handled via mixin
   }
}
