package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.OverlayRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.Colors;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Adds a fading motion trail behind the held item (first-person view). The item's on-screen
 * position is tracked each frame from the player's bob and hand-swing, and a trail of fading,
 * tinted quads is drawn behind it — a "Larp Recode" style item trail.
 */
public class ItemTrailModule extends Module {
   public static final int STYLE_STREAK = 0;
   public static final int STYLE_GLOW = 1;
   public static final int STYLE_RIBBON = 2;
   public final ModeSetting style = this.addSetting(
      new ModeSetting(
         "Style",
         "Trail shape",
         "Streak",
         "Streak",
         "Glow",
         "Ribbon"
      )
   );
   public final SliderSetting length = this.addSetting(
      new SliderSetting("Length", "How many trail samples to keep", 14.0, 4.0, 30.0, 1.0)
   );
   public final SliderSetting thickness = this.addSetting(
      new SliderSetting("Thickness", "Trail width", 14.0, 4.0, 40.0, 1.0)
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

   private final Deque<Point> trail = new ArrayDeque<>();
   private float walkPhase;
   private float swingPhase;
   private boolean wasSwinging;
   private float lastX;
   private float lastY;

   public ItemTrailModule() {
      super("ItemTrail", "A fading motion trail behind the held item", Category.VISUALS);
   }

   public Deque<Point> trail() {
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

   public int styleId() {
      if (this.style.is("Glow")) {
         return STYLE_GLOW;
      } else if (this.style.is("Ribbon")) {
         return STYLE_RIBBON;
      } else {
         return STYLE_STREAK;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      LocalPlayer player = mc.player;
      // Walk bob phase advances with horizontal movement speed.
      float speed = (float)Math.sqrt(player.getDeltaMovement().x * player.getDeltaMovement().x
         + player.getDeltaMovement().z * player.getDeltaMovement().z);
      this.walkPhase += speed * 0.18F;

      // Hand swing phase: rises during an attack, falls after.
      float swing = player.getAttackStrengthScale(0.5F);
      boolean swinging = swing < 1.0F;
      if (swinging && !wasSwinging) {
         this.swingPhase = 0.0F;
      }
      this.wasSwinging = swinging;
      if (swinging) {
         this.swingPhase = (1.0F - swing) * 26.0F;
      } else {
         this.swingPhase *= 0.85F;
      }

      // Estimate the held item's screen position (bottom-center, offset by bob + swing),
      // in the same UI space the overlay renderer draws in.
      float bob = (float)Math.sin(this.walkPhase) * 6.0F;
      float swingOffset = (float)Math.sin(this.swingPhase) * 14.0F;
      float x = OverlayRenderer.uiWidth() * 0.5F;
      float y = OverlayRenderer.uiHeight() * 0.80F + bob - swingOffset;

      this.trail.addLast(new Point(x, y, System.nanoTime()));
      int max = this.length.getInt();
      while (this.trail.size() > max) {
         this.trail.removeFirst();
      }
      this.lastX = x;
      this.lastY = y;
   }

   @Override
   protected void onDisable() {
      this.trail.clear();
   }

   public float lastX() {
      return this.lastX;
   }

   public float lastY() {
      return this.lastY;
   }

   public static final class Point {
      public final float x;
      public final float y;
      public final long nanos;

      Point(float x, float y, long nanos) {
         this.x = x;
         this.y = y;
         this.nanos = nanos;
      }

      public float ageSeconds(long now) {
         return (float)(now - this.nanos) / 1.0E9F;
      }
   }
}
