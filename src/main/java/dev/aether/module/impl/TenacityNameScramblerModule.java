package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityNameScramblerModule extends Module {
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Character scramble speed", 5.0, 1.0, 20.0, 1.0, "")
   );
   public final BooleanSetting chatOnly = this.addSetting(
      new BooleanSetting("Chat Only", "Only scramble names in chat", true)
   );

   private long lastUpdate;
   private final StringBuilder scrambled = new StringBuilder();

   public TenacityNameScramblerModule() {
      super("Tenacity Name Scrambler", "Scrambles player names with random characters (Tenacity port)", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.world == null) return;
      long now = System.currentTimeMillis();
      int interval = (int)(1000.0 / speed.get());
      if (now - lastUpdate < interval) return;
      lastUpdate = now;

      // This is a visual module — the actual name rendering is handled by mixins.
      // Store the scramble state for the name tag renderer to use.
      NameScramblerState.active = isEnabled();
    }

   /** State holder for name scrambling. */
   public static final class NameScramblerState {
      public static boolean active;
      public static String scramble(String name) {
         if (!active || name == null || name.isEmpty()) return name;
         StringBuilder sb = new StringBuilder(name.length());
         for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
               sb.append(c);
            } else {
               sb.append((char)('a' + (int)(Math.random() * 26)));
            }
         }
         return sb.toString();
      }
   }
}
