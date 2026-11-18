package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Spawns ambient particles drifting through the air around the player — snow, fireflies (with
 * trails), falling leaves, embers, dust and more. Each particle drifts with wind and gravity and
 * fades out, creating a living, atmospheric world. Purely visual.
 */
public class AirParticlesModule extends Module {
   public static final int TYPE_SNOW = 0;
   public static final int TYPE_FIREFLY = 1;
   public static final int TYPE_LEAVES = 2;
   public static final int TYPE_EMBER = 3;
   public static final int TYPE_DUST = 4;
   public static final int TYPE_MIXED = 5;

   public final ModeSetting type = this.addSetting(
      new ModeSetting(
         "Type",
         "Ambient particle style",
         "Snow",
         "Snow",
         "Fireflies",
         "Leaves",
         "Embers",
         "Dust",
         "Mixed"
      )
   );
   public final SliderSetting density = this.addSetting(
      new SliderSetting("Density", "How many particles to spawn", 60.0, 10.0, 200.0, 5.0)
   );
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Particle drift speed", 100.0, 20.0, 300.0, 10.0, "%")
   );
   public final SliderSetting wind = this.addSetting(
      new SliderSetting("Wind", "Horizontal wind strength", 30.0, 0.0, 100.0, 5.0, "%")
   );
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Spawn radius around the player", 12.0, 4.0, 30.0, 1.0)
   );
   public final SliderSetting size = this.addSetting(
      new SliderSetting("Size", "Particle size", 1.0, 0.3, 3.0, 0.1)
   );
   public final BooleanSetting trails = this.addSetting(
      new BooleanSetting("Trails", "Fireflies leave glowing trails", true)
   );
   public final BooleanSetting glow = this.addSetting(
      new BooleanSetting("Glow", "Soft glow halo on particles", true)
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Custom tint", -49508));

   private final Deque<Particle> particles = new ArrayDeque<>();

   public AirParticlesModule() {
      super("AirParticles", "Ambient particles drifting through the air", Category.VISUALS);
   }

   public Deque<Particle> particles() {
      return this.particles;
   }

   public int typeId() {
      String t = this.type.get();
      if (t.equals("Fireflies")) return TYPE_FIREFLY;
      if (t.equals("Leaves")) return TYPE_LEAVES;
      if (t.equals("Embers")) return TYPE_EMBER;
      if (t.equals("Dust")) return TYPE_DUST;
      if (t.equals("Mixed")) return TYPE_MIXED;
      return TYPE_SNOW;
   }

   public float speedUnit() {
      return this.speed.getFloat() / 100.0F;
   }

   public float windUnit() {
      return this.wind.getFloat() / 100.0F;
   }

   public float rangeValue() {
      return this.range.getFloat();
   }

   public float sizeValue() {
      return this.size.getFloat();
   }

   public int colorRgb() {
      return this.color.get() & 16777215;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }
      LocalPlayer player = mc.player;
      int type = this.typeId();
      int toSpawn = (int) (this.density.getFloat() / 4.0F);
      for (int i = 0; i < toSpawn; i++) {
         Particle p = new Particle(player);
         // Assign a particle type, honouring the selected style (mixed cycles through all).
         if (type == TYPE_MIXED) {
            p.kind = (int) (Math.random() * 5);
         } else {
            p.kind = type;
         }
         this.particles.addLast(p);
      }
      int max = this.density.getInt();
      while (this.particles.size() > max) {
         this.particles.removeFirst();
      }
   }

   @Override
   protected void onDisable() {
      this.particles.clear();
   }

   public record Particle(double x, double y, double z, double vx, double vy, double vz, int kind, long born) {
      public Particle(LocalPlayer player) {
         this(
            player.getX() + (Math.random() - 0.5) * 8.0,
            player.getY() + Math.random() * 12.0,
            player.getZ() + (Math.random() - 0.5) * 8.0,
            (Math.random() - 0.5) * 0.2,
            -0.1 - Math.random() * 0.1,
            (Math.random() - 0.5) * 0.2,
            0,
            System.nanoTime()
         );
      }
   }
}
