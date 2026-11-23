package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class TenacityVisualsModule extends Module {
   public final BooleanSetting players = this.addSetting(
      new BooleanSetting("Players", "Render visual overlays on players", true)
   );
   public final BooleanSetting tracers = this.addSetting(
      new BooleanSetting("Tracers", "Draw lines from you to entities", true)
   );
   public final BooleanSetting hitboxes = this.addSetting(
      new BooleanSetting("Hitboxes", "Render enhanced entity hitboxes", false)
   );
   public final BooleanSetting names = this.addSetting(
      new BooleanSetting("Names", "Display entity names above them", true)
   );
   public final BooleanSetting boxes = this.addSetting(
      new BooleanSetting("Boxes", "Draw bounding boxes around entities", true)
   );
   public final ModeSetting boxMode = this.addSetting(
      new ModeSetting("Box Mode", "Box Mode", "Box rendering style", "Outline", "Outline", "Filled", "Glow")
   );
   public final ColorSetting playerColor = this.addSetting(
      new ColorSetting("Player Color", "Color for player visuals", 0xFF8B5CF6)
   );
   public final ColorSetting tracerColor = this.addSetting(
      new ColorSetting("Tracer Color", "Color for tracer lines", 0xFF8B5CF6)
   );
   public final SliderSetting boxAlpha = this.addSetting(
      new SliderSetting("Box Alpha", "Fill opacity for box fills", 25.0, 0.0, 100.0, 5.0, "%")
   );
   public final SliderSetting tracerWidth = this.addSetting(
      new SliderSetting("Tracer Width", "Tracer line thickness", 1.5, 0.5, 4.0, 0.5, "px")
   );
   public final SliderSetting renderRange = this.addSetting(
      new SliderSetting("Range", "Maximum render distance", 60.0, 10.0, 150.0, 5.0, "m")
   );

   private final List<Entity> trackedEntities = new ArrayList<>();

   public TenacityVisualsModule() {
      super("Visuals", "Entity visual overlays: tracers, boxes, names", Category.VISUALS);
   }

   public List<Entity> getTrackedEntities() {
      return this.trackedEntities;
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      this.trackedEntities.clear();
      if (mc.player == null || mc.level == null || mc.screen != null) return;

      double range = renderRange.get();

      if (players.get()) {
         for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            if (mc.player.distanceTo(p) > range) continue;
            this.trackedEntities.add(p);
         }
      }

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e == mc.player || !e.isAlive()) continue;
         if (e instanceof Player) continue;
         if (mc.player.distanceTo(e) > range) continue;
         this.trackedEntities.add(e);
      }
   }

   @Override
   protected void onDisable() {
      this.trackedEntities.clear();
   }
}
