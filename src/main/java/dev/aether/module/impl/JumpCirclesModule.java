package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;

public class JumpCirclesModule extends Module {
   public final SliderSetting size = this.addSetting(
      new SliderSetting("Size", "Decal scale (1.0 = one block wide)", 1.0, 0.5, 3.0, 0.1, "x")
   );
   public final ColorSetting color = this.addSetting(
      new ColorSetting("Color", "Decal color (alpha is animated)", -38476)
   );
   public final SliderSetting lifetime = this.addSetting(new SliderSetting("Lifetime", "How long a jump circle stays visible", 1.5, 0.5, 4.0, 0.1, "s"));
   public final BooleanSetting rainbow = this.addSetting(
      new BooleanSetting("Rainbow", "Cycle the color through the rainbow", false)
   );
   public final BooleanSetting shockwave = this.addSetting(
      new BooleanSetting("Shockwave", "Expanding ring on spawn", true)
   );
   public final BooleanSetting particles = this.addSetting(
      new BooleanSetting("Particles", "Drifting dust motes on jump", true)
   );
   public final SliderSetting maxCircles = this.addSetting(
      new SliderSetting("Max", "Max simultaneous decals (oldest drops first)", 10.0, 1.0, 30.0, 1.0)
   );
   private final Deque<JumpCirclesModule.JumpCircle> circles = new ArrayDeque<>();
   private int spawnCounter;

   public JumpCirclesModule() {
      super("JumpCircles", "Draws animated circles on the ground when you jump.", Category.CLIENT);
   }

   public Deque<JumpCirclesModule.JumpCircle> circles() {
      return this.circles;
   }

   @Override
   protected void onDisable() {
      this.clear();
   }

   public void clear() {
      this.circles.clear();
   }

   public int baseRgb() {
      return this.color.get() & 16777215;
   }

   public void onPlayerJump(LocalPlayer var1) {
      if (this.isEnabled()) {
         double var2 = var1.getX();
         double var4 = var1.getY();
         double var6 = var1.getZ();
         float var8 = 0.01F + (float)(this.spawnCounter % 8) * 0.001F;
         this.spawnCounter++;
         this.circles.addLast(new JumpCirclesModule.JumpCircle(var2, var4, var6, var1.getYRot(), var8, System.nanoTime()));
         int var9 = Math.max(1, this.maxCircles.getInt());

         while (this.circles.size() > var9) {
            this.circles.removeFirst();
         }

         if (this.particles.get()) {
            this.spawnParticles(var1);
         }
      }
   }

   private void spawnParticles(LocalPlayer var1) {
      RandomSource var2 = var1.getRandom();
      int var3 = this.baseRgb();

      for (int var4 = 0; var4 < 10; var4++) {
         double var5 = var2.nextDouble() * Math.PI * 2.0;
         double var7 = 0.15 + var2.nextDouble() * 0.45;
         var1.level()
            .addParticle(
               new DustParticleOptions(var3, 0.9F),
               var1.getX() + Math.cos(var5) * var7,
               var1.getY() + 0.05,
               var1.getZ() + Math.sin(var5) * var7,
               0.0,
               0.6 + var2.nextDouble() * 0.4,
               0.0
            );
      }
   }

   public static final class JumpCircle {
      public final double x;
      public final double y;
      public final double z;
      public final float yawDegrees;
      public final float yLift;
      public final long spawnNanos;

      JumpCircle(double var1, double var3, double var5, float var7, float var8, long var9) {
         this.x = var1;
         this.y = var3;
         this.z = var5;
         this.yawDegrees = var7;
         this.yLift = var8;
         this.spawnNanos = var9;
      }

      public float ageSeconds(long var1) {
         return (float)(var1 - this.spawnNanos) / 1.0E9F;
      }
   }
}
