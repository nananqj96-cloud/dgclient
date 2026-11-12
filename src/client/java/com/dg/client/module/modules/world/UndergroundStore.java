package com.dg.client.module.modules.world;

import com.dg.client.core.DGLogger;
import com.dg.client.core.WaypointManager;
import com.dg.client.module.modules.esp.StorageEspModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe local store of genuine underground (Y < 0) data reconstructed from
 * server packet leaks, anti-decoy-filtered exposures, light/heightmap discrepancies,
 * and underground sound/particle events. Never fabricates unknown blocks.
 */
public final class UndergroundStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum BlockCategory {
        STORAGE,
        ORE,
        REDSTONE,
        MANMADE,
        CAVITY_LIGHT
    }

    public enum ObfuscationMode {
        UNKNOWN("Analyzing..."),
        UNMASKED("Normal / Unmasked"),
        ALL_AIR("Zeroed / Air-Masked"),
        SOLID_DEEPSLATE("Fake Solid Deepslate/Stone"),
        RANDOM_ORE_DECOYS("Randomized Ore Decoys (Anti-Xray)"),
        MISSING_SECTIONS("No Sub-Zero Sections");

        private final String label;

        ObfuscationMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record KnownBlock(
            BlockPos pos,
            String blockId,
            BlockCategory category,
            float confidence,
            String source,
            long updatedAtMs
    ) {
    }

    public record ActivityMarker(
            Vec3d pos,
            String label,
            float confidence,
            long createdAtMs
    ) {
    }

    public static final class ChunkRecord {
        public final int chunkX;
        public final int chunkZ;
        public volatile ObfuscationMode obfuscationMode = ObfuscationMode.UNKNOWN;
        public final ConcurrentHashMap<BlockPos, KnownBlock> knownBlocks = new ConcurrentHashMap<>();
        public volatile long lastTouchedMs = System.currentTimeMillis();

        public ChunkRecord(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        public boolean hasRecoveredData() {
            return !knownBlocks.isEmpty();
        }
    }

    private final ConcurrentHashMap<Long, ChunkRecord> chunks = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ActivityMarker> activityMarkers = new CopyOnWriteArrayList<>();

    public ChunkRecord getOrCreateChunk(int chunkX, int chunkZ) {
        long key = ChunkPos.toLong(chunkX, chunkZ);
        ChunkRecord rec = chunks.computeIfAbsent(key, k -> new ChunkRecord(chunkX, chunkZ));
        rec.lastTouchedMs = System.currentTimeMillis();
        return rec;
    }

    public ChunkRecord getChunk(int chunkX, int chunkZ) {
        return chunks.get(ChunkPos.toLong(chunkX, chunkZ));
    }

    public void recordBlock(BlockPos pos, Block block, BlockCategory category, float confidence, String source, int maxChunksCap) {
        if (pos.getY() >= 0 || category == null) {
            return;
        }
        Identifier id = Registries.BLOCK.getId(block);
        recordBlockId(pos, id.toString(), category, confidence, source, maxChunksCap);
    }

    public void recordBlockId(BlockPos pos, String blockId, BlockCategory category, float confidence, String source, int maxChunksCap) {
        if (pos.getY() >= 0 || category == null) {
            return;
        }
        enforceCap(maxChunksCap);
        ChunkRecord rec = getOrCreateChunk(pos.getX() >> 4, pos.getZ() >> 4);
        BlockPos imm = pos.toImmutable();
        rec.knownBlocks.compute(imm, (k, existing) -> {
            if (existing != null && existing.confidence() > confidence) {
                return existing;
            }
            return new KnownBlock(imm, blockId, category, confidence, source, System.currentTimeMillis());
        });
    }

    public void recordActivity(Vec3d pos, String label, float confidence) {
        if (pos.y >= 0.0) {
            return;
        }
        activityMarkers.removeIf(m -> System.currentTimeMillis() - m.createdAtMs() > 25000L);
        if (activityMarkers.size() >= 96) {
            activityMarkers.remove(0);
        }
        activityMarkers.add(new ActivityMarker(pos, label, confidence, System.currentTimeMillis()));
    }

    public List<ActivityMarker> getActiveMarkers() {
        long now = System.currentTimeMillis();
        activityMarkers.removeIf(m -> now - m.createdAtMs() > 25000L);
        return new ArrayList<>(activityMarkers);
    }

    public List<ChunkRecord> getAllChunks() {
        return new ArrayList<>(chunks.values());
    }

    public int getTotalKnownBlockCount() {
        int sum = 0;
        for (ChunkRecord c : chunks.values()) {
            sum += c.knownBlocks.size();
        }
        return sum;
    }

    public List<StorageEspModule.LeakedStorage> getLeakedStorage(float minConfidence) {
        List<StorageEspModule.LeakedStorage> out = new ArrayList<>();
        for (ChunkRecord c : chunks.values()) {
            for (KnownBlock kb : c.knownBlocks.values()) {
                if (kb.category() == BlockCategory.STORAGE && kb.confidence() >= minConfidence) {
                    String shortName = kb.blockId().contains(":") ? kb.blockId().substring(kb.blockId().indexOf(':') + 1) : kb.blockId();
                    out.add(new StorageEspModule.LeakedStorage(kb.pos(), shortName, kb.confidence()));
                }
            }
        }
        return out;
    }

    /**
     * Computes an underground suspicion boost (0..35) for BaseFinder from recovered Y < 0 data.
     */
    public double computeBaseFinderBoost(int chunkX, int chunkZ, float minConfidence) {
        ChunkRecord rec = getChunk(chunkX, chunkZ);
        double score = 0.0;
        if (rec != null) {
            for (KnownBlock kb : rec.knownBlocks.values()) {
                if (kb.confidence() < minConfidence) continue;
                switch (kb.category()) {
                    case STORAGE -> score += 8.0 * kb.confidence();
                    case REDSTONE -> score += 5.0 * kb.confidence();
                    case MANMADE -> score += 2.5 * kb.confidence();
                    case CAVITY_LIGHT -> score += 3.0 * kb.confidence();
                    case ORE -> {
                    }
                }
            }
        }
        for (ActivityMarker m : getActiveMarkers()) {
            if ((((int) Math.floor(m.pos().x)) >> 4) == chunkX && (((int) Math.floor(m.pos().z)) >> 4) == chunkZ) {
                score += 6.0 * m.confidence();
            }
        }
        return Math.min(35.0, score);
    }

    public void clear() {
        chunks.clear();
        activityMarkers.clear();
    }

    private void enforceCap(int maxChunks) {
        if (chunks.size() <= maxChunks) {
            return;
        }
        List<Map.Entry<Long, ChunkRecord>> all = new ArrayList<>(chunks.entrySet());
        all.sort(Comparator.comparingLong(e -> e.getValue().lastTouchedMs));
        int toRemove = Math.max(1, all.size() - maxChunks);
        for (int i = 0; i < toRemove && i < all.size(); i++) {
            chunks.remove(all.get(i).getKey());
        }
    }

    public void saveForCurrentServer() {
        String srv = WaypointManager.getCurrentServerId();
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("dgclient").resolve("y0_store");
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(srv.replaceAll("[^a-zA-Z0-9._-]", "_") + ".json");
            JsonArray arr = new JsonArray();
            for (ChunkRecord cr : chunks.values()) {
                for (KnownBlock kb : cr.knownBlocks.values()) {
                    if (kb.confidence() < 0.55f) continue;
                    JsonObject o = new JsonObject();
                    o.addProperty("x", kb.pos().getX());
                    o.addProperty("y", kb.pos().getY());
                    o.addProperty("z", kb.pos().getZ());
                    o.addProperty("id", kb.blockId());
                    o.addProperty("cat", kb.category().name());
                    o.addProperty("conf", kb.confidence());
                    o.addProperty("src", kb.source());
                    arr.add(o);
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(arr, writer);
            }
        } catch (IOException e) {
            DGLogger.warn("Failed saving Y-0 store: {}", e.getMessage());
        }
    }

    public int loadForCurrentServer(int maxChunksCap) {
        String srv = WaypointManager.getCurrentServerId();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("dgclient").resolve("y0_store")
                .resolve(srv.replaceAll("[^a-zA-Z0-9._-]", "_") + ".json");
        if (!Files.exists(file)) {
            return 0;
        }
        int loaded = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root != null && root.isJsonArray()) {
                for (JsonElement elem : root.getAsJsonArray()) {
                    JsonObject o = elem.getAsJsonObject();
                    BlockPos pos = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
                    BlockCategory cat = BlockCategory.valueOf(o.get("cat").getAsString());
                    recordBlockId(pos, o.get("id").getAsString(), cat, o.get("conf").getAsFloat(), o.get("src").getAsString(), maxChunksCap);
                    loaded++;
                }
            }
        } catch (Throwable t) {
            DGLogger.warn("Could not load Y-0 store: {}", t.getMessage());
        }
        return loaded;
    }
}
