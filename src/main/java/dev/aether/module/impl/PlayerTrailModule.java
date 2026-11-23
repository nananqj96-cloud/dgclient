package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.Colors;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Renders a fading motion trail behind the local player as they move through the world. Samples the
 * player's position each tick and draws a ribbon of fading, tinted segments from the camera through
 * their recent path — a "player trail" visual effect.
 */
public class PlayerTrailModule extends Module {
   public static final int STYLE_RIBBON = 0;
   public static final int STYLE_GLOW = 1;
   public static final int STYLE_STARS = 2;
   public final ModeSetting style = this.addSetting(
      new ModeSetting(
         "Style",
         "Trail shape",
         "Ribbon",
         "Ribbon",
         "Glow",
         "Stars"
      )
   );
   public final SliderSetting length = this.addSetting(
      new SliderSetting("Length", "How many trail samples to keep", 40.0, 10.0, 120.0, 5.0)
   );
   public final SliderSetting thickness = this.addSetting(
      new SliderSetting("Thickness", "Trail width in blocks", 0.6, 0.1, 3.0, 0.1)
   );
   public final SliderSetting height = this.addSetting(
      new SliderSetting("Height", "Vertical offset of the trail", 1.0, -2.0, 6.0, 0.1)
   );
   public final SliderSetting fade = this.addSetting(
      new SliderSetting("Fade", "How quickly the trail fades", 70.0, 20.0, 100.0, 5.0, "%")
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Trail tint", -49508));
   public final BooleanSetting rainbow = this.addSetting(
      new BooleanSetting("Rainbow", "Cycle the trail through the rainbow", false)
   );
   public final BooleanSetting useTheme = this.addSetting(
      new BooleanSetting("Theme Color", "Tint the trail with the active theme accent", false)
   );

   private final Deque<Sample> trail = new ArrayDeque<>();

   public PlayerTrailModule() {
      super("PlayerTrail", "A fading motion trail behind the player", Category.VISUALS);
   }

   public Deque<Sample> trail() {
      return this.trail;
   }

   public int currentRgb() {
      if (this.rainbow.get()) {
         float hue = (float)(System.currentTimeMillis() % 3500L) / 3500.0F * 360.0F;
         return Colors.hsvToRgb(hue, 0.85F, 1.0F) & 16777215;
      } else if (this.useTheme.get()) {
         return dev.aether.AetherClient.themes().current().accent() & 16777215;
      } else {
         return this.color.get() & 16777215;
      }
   }

   public float fadeAmount() {
      return this.fade.getFloat() / 100.0F;
   }

   public float thickness() {
      return this.thickness.getFloat();
   }

   public float heightOffset() {
      return this.height.getFloat();
   }

   public int styleId() {
      if (this.style.is("Glow")) {
         return STYLE_GLOW;
      } else if (this.style.is("Stars")) {
         return STYLE_STARS;
      } else {
         return STYLE_RIBBON;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      LocalPlayer player = mc.player;
      Vec3 pos = player.position();
      this.trail.addLast(new Sample(pos.x(), pos.y() + this.heightOffset(), pos.z(), System.nanoTime()));
      int max = this.length.getInt();
      while (this.trail.size() > max) {
         this.trail.removeFirst();
      }
   }

   @Override
   protected void onDisable() {
      this.trail.clear();
   }

   public record Sample(double x, double y, double z, long time) {
   }
}
