package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityHotbarModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Hotbar scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting shadows = this.addSetting(new BooleanSetting("Shadows", "Show slot shadows", true));
   public final BooleanSetting cooldown = this.addSetting(new BooleanSetting("Cooldown", "Show cooldown overlay", true));

   public TenacityHotbarModule() {
      super("Hotbar", "Customizes the hotbar appearance", Category.RENDER);
   }

   @Override
   public void onTick() {
      // Hotbar rendering handled via mixin
   }
}
