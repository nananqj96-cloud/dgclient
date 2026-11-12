package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;

public class PlayerEspModule extends Module {
   public final ModeSetting style = this.addSetting(
      new ModeSetting(
         "Style",
         "Highlight style",
         "Outline",
         "Box",
         "Outline",
         "Glow"
      )
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Highlight color", -49508));
   public final BooleanSetting tracers = this.addSetting(
      new BooleanSetting("Tracers", "Draw lines from the crosshair to each player", false)
   );

   public PlayerEspModule() {
      super("PlayerESP", "Highlights players through walls", Category.RENDER);
   }
}
