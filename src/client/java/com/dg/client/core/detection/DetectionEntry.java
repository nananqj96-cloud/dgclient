package com.dg.client.core.detection;

import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of a suspicious chunk flagged by BaseFinder.
 */
public final class DetectionEntry {
    public enum Status {
        ACTIVE,
        VERIFIED,
        IGNORED
    }

    private final int chunkX;
    private final int chunkZ;
    private final String dimension;
    private final double score; // 0..100
    private final List<String> reasonTags;
    private final int minDetectedY;
    private final int maxDetectedY;
    private final long timestampMs;
    private volatile Status status;

    public DetectionEntry(
            int chunkX,
            int chunkZ,
            String dimension,
            double score,
            List<String> reasonTags,
            int minDetectedY,
            int maxDetectedY,
            long timestampMs,
            Status status
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimension = dimension == null ? "minecraft:overworld" : dimension;
        this.score = score;
        this.reasonTags = Collections.unmodifiableList(new ArrayList<>(reasonTags));
        this.minDetectedY = minDetectedY;
        this.maxDetectedY = maxDetectedY;
        this.timestampMs = timestampMs;
        this.status = status == null ? Status.ACTIVE : status;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getCenterX() {
        return (chunkX << 4) + 8;
    }

    public int getCenterZ() {
        return (chunkZ << 4) + 8;
    }

    public int getCenterY() {
        return (minDetectedY + maxDetectedY) / 2;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }

    public String getDimension() {
        return dimension;
    }

    public double getScore() {
        return score;
    }

    public List<String> getReasonTags() {
        return reasonTags;
    }

    public String getFormattedReasons() {
        if (reasonTags.isEmpty()) {
            return "Anomaly";
        }
        return String.join(" + ", reasonTags);
    }

    public int getMinDetectedY() {
        return minDetectedY;
    }

    public int getMaxDetectedY() {
        return maxDetectedY;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getKey() {
        return ChunkPos.toLong(chunkX, chunkZ);
    }
}
