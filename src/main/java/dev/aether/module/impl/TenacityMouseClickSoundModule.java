package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.UiSounds;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class TenacityMouseClickSoundModule extends Module {
   public final ModeSetting sound = this.addSetting(
      new ModeSetting("Sound", "Click sound style", "Gaming", "Gaming", "Creamy", "Office", "Silent")
   );
   public final SliderSetting volume = this.addSetting(
      new SliderSetting("Volume", "Click sound volume", 70.0, 0.0, 100.0, 5.0, "%")
   );

   private int lastButton = -1;

   public TenacityMouseClickSoundModule() {
      super("Click Sound", "Plays a sound on mouse clicks (ported)", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getWindow() == null) return;
      for (int btn = 0; btn <= 2; btn++) {
         int state = GLFW.glfwGetMouseButton(mc.getWindow().handle(), btn);
         if (state == 1 && lastButton != btn) {
            UiSounds.select();
            lastButton = btn;
         } else if (state == 0 && lastButton == btn) {
            lastButton = -1;
         }
      }
   }
}
