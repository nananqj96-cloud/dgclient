package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityClickGuiModule extends Module {
   public final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "GUI scale", 1.0, 0.5, 2.0, 0.1));
   public final BooleanSetting animations = this.addSetting(new BooleanSetting("Animations", "Enable GUI animations", true));
   public final BooleanSetting blur = this.addSetting(new BooleanSetting("Blur", "Background blur", true));

   public TenacityClickGuiModule() {
      super("ClickGUI", "Configures the client GUI appearance", Category.RENDER);
   }

   @Override
   public void onTick() {
      // ClickGUI rendering handled by ClickGuiScreen
   }
}
