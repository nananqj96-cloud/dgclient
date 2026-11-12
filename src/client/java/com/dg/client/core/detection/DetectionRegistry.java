package com.dg.client.core.detection;

import com.dg.client.core.DGLogger;
import com.dg.client.core.WaypointManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.ChunkPos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe per-server store of BaseFinder chunk detections with spatial cluster
 * deduplication and JSON & CSV export/import.
 */
public final class DetectionRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ConcurrentHashMap<String, DetectionEntry> entries = new ConcurrentHashMap<>();
    private volatile String loadedServer = "";
    private volatile int clusterMergeRadius = 1;

    private static String makeKey(String dimension, int chunkX, int chunkZ) {
        return dimension + "@" + ChunkPos.toLong(chunkX, chunkZ);
    }

    public int getClusterMergeRadius() {
        return clusterMergeRadius;
    }

    public void setClusterMergeRadius(int radius) {
        int clamped = Math.max(1, Math.min(4, radius));
        if (this.clusterMergeRadius != clamped) {
            this.clusterMergeRadius = clamped;
            recomputeAllClusters();
        }
    }

    public void ensureServerLoaded() {
        String current = WaypointManager.getCurrentServerId();
        if (!current.equals(loadedServer)) {
            loadedServer = current;
            loadForServer(current);
        }
    }

    public synchronized DetectionEntry putOrUpdate(DetectionEntry incoming) {
        ensureServerLoaded();
        String key = makeKey(incoming.getDimension(), incoming.getChunkX(), incoming.getChunkZ());
        DetectionEntry prev = entries.get(key);
        if (prev != null && prev.getStatus() == DetectionEntry.Status.IGNORED) {
            return prev;
        }
        if (prev != null && prev.getStatus() == DetectionEntry.Status.VERIFIED) {
            incoming.setStatus(DetectionEntry.Status.VERIFIED);
        }
        entries.put(key, incoming);
        recomputeClustersForDimension(incoming.getDimension());
        return prev;
    }

    /**
     * Removes an unverified active detection when re-scanning shows the chunk no longer meets
     * multi-indicator suspicion requirements (respects user-pinned VERIFIED and IGNORED states).
     */
    public synchronized boolean removeIfActive(String dimension, int chunkX, int chunkZ) {
        String key = makeKey(dimension, chunkX, chunkZ);
        DetectionEntry existing = entries.get(key);
        if (existing != null && existing.getStatus() == DetectionEntry.Status.ACTIVE) {
            entries.remove(key);
            recomputeClustersForDimension(dimension);
            return true;
        }
        return false;
    }

    public synchronized void pruneOutsideRadius(String dimension, int centerChunkX, int centerChunkZ, int maxChunkDist) {
        boolean removedAny = entries.entrySet().removeIf(e -> {
            DetectionEntry d = e.getValue();
            if (d.getStatus() != DetectionEntry.Status.ACTIVE || !d.getDimension().equals(dimension)) {
                return false;
            }
            int dx = Math.abs(d.getChunkX() - centerChunkX);
            int dz = Math.abs(d.getChunkZ() - centerChunkZ);
            return dx > maxChunkDist || dz > maxChunkDist;
        });
        if (removedAny) {
            recomputeClustersForDimension(dimension);
        }
    }

    private void recomputeAllClusters() {
        List<String> dims = entries.values().stream().map(DetectionEntry::getDimension).distinct().toList();
        for (String dim : dims) {
            recomputeClustersForDimension(dim);
        }
    }

    private void recomputeClustersForDimension(String dimension) {
        Map<Long, Double> rawScores = new HashMap<>();
        List<DetectionEntry> dimEntries = new ArrayList<>();
        for (DetectionEntry d : entries.values()) {
            if (!d.getDimension().equals(dimension) || d.getStatus() == DetectionEntry.Status.IGNORED) {
                continue;
            }
            dimEntries.add(d);
            rawScores.put(SuspiciousChunkAnalyzer.packChunk(d.getChunkX(), d.getChunkZ()), d.getRawScore());
        }
        Map<Long, SuspiciousChunkAnalyzer.ClusterAssignment> assignments =
                SuspiciousChunkAnalyzer.deduplicateClusters(rawScores, clusterMergeRadius);
        for (DetectionEntry d : dimEntries) {
            long packed = SuspiciousChunkAnalyzer.packChunk(d.getChunkX(), d.getChunkZ());
            d.applyClusterAssignment(assignments.get(packed));
        }
    }

    public DetectionEntry get(String dimension, int chunkX, int chunkZ) {
        return entries.get(makeKey(dimension, chunkX, chunkZ));
    }

    public List<DetectionEntry> getAll() {
        ensureServerLoaded();
        List<DetectionEntry> list = new ArrayList<>(entries.values());
        list.sort(Comparator.comparingDouble(DetectionEntry::getScore).reversed());
        return list;
    }

    /**
     * Returns only the primary representative chunk of each deduplicated base cluster
     * (used for clean, spam-free UI summaries and counts).
     */
    public List<DetectionEntry> getDeduplicatedClusters() {
        ensureServerLoaded();
        List<DetectionEntry> list = new ArrayList<>();
        for (DetectionEntry d : entries.values()) {
            if (d.getStatus() != DetectionEntry.Status.IGNORED && d.isClusterPrimary()) {
                list.add(d);
            }
        }
        list.sort(Comparator.comparingDouble(DetectionEntry::getScore).reversed());
        return list;
    }

    public synchronized void clearAll() {
        entries.clear();
        saveCurrentServer();
    }

    public Path getBaseFinderDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("dgclient").resolve("basefinder");
    }

    public Path saveCurrentServer() {
        String server = loadedServer.isBlank() ? WaypointManager.getCurrentServerId() : loadedServer;
        return exportJson(server);
    }

    public Path exportJson(String serverId) {
        try {
            Path dir = getBaseFinderDir();
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitizeFilename(serverId) + ".json");
            JsonArray arr = new JsonArray();
            for (DetectionEntry d : getAll()) {
                JsonObject o = new JsonObject();
                o.addProperty("chunkX", d.getChunkX());
                o.addProperty("chunkZ", d.getChunkZ());
                o.addProperty("blockX", d.getCenterX());
                o.addProperty("blockZ", d.getCenterZ());
                o.addProperty("dimension", d.getDimension());
                o.addProperty("score", d.getRawScore());
                o.addProperty("effectiveScore", d.getScore());
                o.addProperty("confidence", d.getConfidence().name());
                o.addProperty("indicatorCount", d.getIndicatorCount());
                o.addProperty("clusterId", d.getClusterId());
                o.addProperty("clusterSize", d.getClusterSize());
                o.addProperty("minY", d.getMinDetectedY());
                o.addProperty("maxY", d.getMaxDetectedY());
                o.addProperty("medianY", d.getMedianDetectedY());
                o.addProperty("timestampMs", d.getTimestampMs());
                o.addProperty("status", d.getStatus().name());
                JsonArray reasons = new JsonArray();
                for (String r : d.getReasonTags()) {
                    reasons.add(r);
                }
                o.add("reasons", reasons);
                arr.add(o);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(arr, writer);
            }
            return file;
        } catch (IOException e) {
            DGLogger.error("Failed exporting BaseFinder JSON", e);
            return null;
        }
    }

    public Path exportCsv(String serverId) {
        try {
            Path dir = getBaseFinderDir();
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitizeFilename(serverId) + ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("chunkX,chunkZ,centerX,centerY,centerZ,dimension,score,confidence,clusterSize,status,reasons\n");
                for (DetectionEntry d : getAll()) {
                    writer.write(String.format(
                            java.util.Locale.ROOT,
                            "%d,%d,%d,%d,%d,%s,%.1f,%s,%d,%s,\"%s\"%n",
                            d.getChunkX(),
                            d.getChunkZ(),
                            d.getCenterX(),
                            d.getCenterY(),
                            d.getCenterZ(),
                            d.getDimension(),
                            d.getScore(),
                            d.getConfidence().name(),
                            d.getClusterSize(),
                            d.getStatus().name(),
                            d.getFormattedReasons().replace("\"", "'")
                    ));
                }
            }
            return file;
        } catch (IOException e) {
            DGLogger.error("Failed exporting BaseFinder CSV", e);
            return null;
        }
    }

    public synchronized int loadForServer(String serverId) {
        entries.clear();
        Path file = getBaseFinderDir().resolve(sanitizeFilename(serverId) + ".json");
        if (!Files.exists(file)) {
            return 0;
        }
        int loaded = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null && root.isJsonArray()) {
                for (JsonElement elem : root.getAsJsonArray()) {
                    if (!elem.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = elem.getAsJsonObject();
                    int cx = o.get("chunkX").getAsInt();
                    int cz = o.get("chunkZ").getAsInt();
                    String dim = o.has("dimension") ? o.get("dimension").getAsString() : "minecraft:overworld";
                    double score = o.has("score") ? o.get("score").getAsDouble() : 50.0;
                    int indicators = o.has("indicatorCount") ? o.get("indicatorCount").getAsInt() : 2;
                    int minY = o.has("minY") ? o.get("minY").getAsInt() : -64;
                    int maxY = o.has("maxY") ? o.get("maxY").getAsInt() : 120;
                    int medY = o.has("medianY") ? o.get("medianY").getAsInt() : (minY + maxY) / 2;
                    long ts = o.has("timestampMs") ? o.get("timestampMs").getAsLong() : System.currentTimeMillis();
                    DetectionEntry.Status st = DetectionEntry.Status.ACTIVE;
                    if (o.has("status")) {
                        try {
                            st = DetectionEntry.Status.valueOf(o.get("status").getAsString());
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    List<String> reasons = new ArrayList<>();
                    if (o.has("reasons") && o.get("reasons").isJsonArray()) {
                        for (JsonElement r : o.getAsJsonArray("reasons")) {
                            reasons.add(r.getAsString());
                        }
                    }
                    entries.put(makeKey(dim, cx, cz), new DetectionEntry(cx, cz, dim, score, indicators, reasons, minY, maxY, medY, ts, st));
                    loaded++;
                }
            }
            recomputeAllClusters();
        } catch (Throwable t) {
            DGLogger.warn("Could not read BaseFinder file {}: {}", file, t.getMessage());
        }
        return loaded;
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "singleplayer";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
