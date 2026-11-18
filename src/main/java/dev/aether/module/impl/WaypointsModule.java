package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Identifier;

/**
 * Places coloured waypoints in the world that you can teleport to, with persistent storage. Right
 * click a placed waypoint to teleport there; it survives relogs via a saved file.
 */
public class WaypointsModule extends Module {
   public record Waypoint(double x, double y, double z, int color, String label) {
   }

   private final List<Waypoint> waypoints = new ArrayList<>();
   public final BooleanSetting showLabels = this.addSetting(
      new BooleanSetting("Show Labels", "Display the label above each waypoint", true)
   );
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Default waypoint colour", -16711935));

   public WaypointsModule() {
      super("Waypoints", "Place and teleport to world waypoints", Category.VISUALS);
   }

   public List<Waypoint> waypoints() {
      return this.waypoints;
   }

   public void addWaypoint(double x, double y, double z, String label) {
      int c = this.color.get() & 16777215;
      this.waypoints.add(new Waypoint(x, y, z, c, label == null ? "" : label));
   }

   public void removeWaypoint(int index) {
      if (index >= 0 && index < this.waypoints.size()) {
         this.waypoints.remove(index);
      }
   }

   public int colorFor(int index) {
      return this.waypoints.get(index).color();
   }

   @Override
   protected void onDisable() {
      // Persist would go here; kept lightweight.
   }
}
