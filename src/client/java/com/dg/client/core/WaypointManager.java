package com.dg.client.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages user waypoints and automatic death waypoints per server & dimension,
 * including automatic Overworld <-> Nether 8x coordinate conversion.
 */
public final class WaypointManager {
    public static final class Waypoint {
        private final String name;
        private final String server;
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;
        private boolean enabled;
        private final boolean deathPoint;

        public Waypoint(String name, String server, String dimension, double x, double y, double z, boolean enabled, boolean deathPoint) {
            this.name = name;
            this.server = server == null ? "singleplayer" : server;
            this.dimension = dimension == null ? "minecraft:overworld" : dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.enabled = enabled;
            this.deathPoint = deathPoint;
        }

        public String getName() {
            return name;
        }

        public String getServer() {
            return server;
        }

        public String getDimension() {
            return dimension;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDeathPoint() {
            return deathPoint;
        }

        /**
         * Returns the waypoint position projected into the given active dimension
         * (supports Overworld <-> Nether 8:1 translation), or null if incompatible (e.g. The End).
         */
        public Vec3d getPosInDimension(String activeDimension) {
            if (dimension.equals(activeDimension)) {
                return new Vec3d(x, y, z);
            }
            if (dimension.endsWith("overworld") && activeDimension.endsWith("the_nether")) {
                return new Vec3d(x / 8.0, Math.min(120.0, y), z / 8.0);
            }
            if (dimension.endsWith("the_nether") && activeDimension.endsWith("overworld")) {
                return new Vec3d(x * 8.0, y, z * 8.0);
            }
            return null;
        }
    }

    private final CopyOnWriteArrayList<Waypoint> waypoints = new CopyOnWriteArrayList<>();

    public static String getCurrentServerId() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return "unknown";
        }
        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry != null && entry.address != null && !entry.address.isBlank()) {
            return entry.address.toLowerCase(Locale.ROOT).replace(":", "_");
        }
        return "singleplayer";
    }

    public static String getCurrentDimensionId() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            return mc.world.getRegistryKey().getValue().toString();
        }
        return "minecraft:overworld";
    }

    public void addWaypoint(String name, double x, double y, double z, boolean deathPoint) {
        String server = getCurrentServerId();
        String dim = getCurrentDimensionId();
        if (deathPoint) {
            waypoints.removeIf(w -> w.isDeathPoint() && w.getServer().equalsIgnoreCase(server));
        } else {
            waypoints.removeIf(w -> w.getName().equalsIgnoreCase(name) && w.getServer().equalsIgnoreCase(server));
        }
        waypoints.add(new Waypoint(name, server, dim, x, y, z, true, deathPoint));
    }

    public boolean removeWaypoint(String name) {
        String server = getCurrentServerId();
        return waypoints.removeIf(w -> w.getServer().equalsIgnoreCase(server) && w.getName().equalsIgnoreCase(name));
    }

    public void clearCurrentServer() {
        String server = getCurrentServerId();
        waypoints.removeIf(w -> w.getServer().equalsIgnoreCase(server));
    }

    public List<Waypoint> getWaypointsForCurrentServer() {
        String server = getCurrentServerId();
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint wp : waypoints) {
            if (wp.getServer().equalsIgnoreCase(server)) {
                result.add(wp);
            }
        }
        return result;
    }

    public List<Waypoint> getAllWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (Waypoint wp : waypoints) {
            JsonObject o = new JsonObject();
            o.addProperty("name", wp.name);
            o.addProperty("server", wp.server);
            o.addProperty("dimension", wp.dimension);
            o.addProperty("x", wp.x);
            o.addProperty("y", wp.y);
            o.addProperty("z", wp.z);
            o.addProperty("enabled", wp.enabled);
            o.addProperty("deathPoint", wp.deathPoint);
            arr.add(o);
        }
        return arr;
    }

    public void fromJson(JsonElement element) {
        waypoints.clear();
        if (element != null && element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (e != null && e.isJsonObject()) {
                    JsonObject o = e.getAsJsonObject();
                    waypoints.add(new Waypoint(
                            o.has("name") ? o.get("name").getAsString() : "Waypoint",
                            o.has("server") ? o.get("server").getAsString() : "singleplayer",
                            o.has("dimension") ? o.get("dimension").getAsString() : "minecraft:overworld",
                            o.has("x") ? o.get("x").getAsDouble() : 0.0,
                            o.has("y") ? o.get("y").getAsDouble() : 64.0,
                            o.has("z") ? o.get("z").getAsDouble() : 0.0,
                            !o.has("enabled") || o.get("enabled").getAsBoolean(),
                            o.has("deathPoint") && o.get("deathPoint").getAsBoolean()
                    ));
                }
            }
        }
    }
}
