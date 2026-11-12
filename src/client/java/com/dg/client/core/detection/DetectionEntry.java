package com.dg.client.core.detection;

import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of a suspicious chunk flagged by BaseFinder, including multi-indicator
 * confidence tier and spatial cluster deduplication metadata.
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
    private final double rawScore; // 0..100
    private final int indicatorCount;
    private final List<String> reasonTags;
    private final int minDetectedY;
    private final int maxDetectedY;
    private final int medianDetectedY;
    private final long timestampMs;
    private volatile Status status;

    // Updated dynamically by DetectionRegistry cluster deduplication
    private volatile double effectiveScore;
    private volatile SuspiciousChunkAnalyzer.Confidence confidence;
    private volatile int clusterId = 0;
    private volatile boolean clusterPrimary = true;
    private volatile int clusterSize = 1;

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
        this(
                chunkX,
                chunkZ,
                dimension,
                score,
                Math.max(2, reasonTags != null ? reasonTags.size() : 2),
                reasonTags,
                minDetectedY,
                maxDetectedY,
                (minDetectedY + maxDetectedY) / 2,
                timestampMs,
                status
        );
    }

    public DetectionEntry(
            int chunkX,
            int chunkZ,
            String dimension,
            double score,
            int indicatorCount,
            List<String> reasonTags,
            int minDetectedY,
            int maxDetectedY,
            int medianDetectedY,
            long timestampMs,
            Status status
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimension = dimension == null ? "minecraft:overworld" : dimension;
        this.rawScore = score;
        this.effectiveScore = score;
        this.indicatorCount = Math.max(1, indicatorCount);
        this.reasonTags = Collections.unmodifiableList(reasonTags != null ? new ArrayList<>(reasonTags) : List.of());
        this.minDetectedY = minDetectedY;
        this.maxDetectedY = maxDetectedY;
        this.medianDetectedY = medianDetectedY;
        this.timestampMs = timestampMs;
        this.status = status == null ? Status.ACTIVE : status;
        this.confidence = SuspiciousChunkAnalyzer.classifyConfidence(score);
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
        return medianDetectedY;
    }

    public int getMedianDetectedY() {
        return medianDetectedY;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }

    public String getDimension() {
        return dimension;
    }

    public double getRawScore() {
        return rawScore;
    }

    public double getScore() {
        return effectiveScore;
    }

    public int getIndicatorCount() {
        return indicatorCount;
    }

    public SuspiciousChunkAnalyzer.Confidence getConfidence() {
        return status == Status.VERIFIED ? SuspiciousChunkAnalyzer.Confidence.HIGH : confidence;
    }

    public int getClusterId() {
        return clusterId;
    }

    public boolean isClusterPrimary() {
        return clusterPrimary;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void applyClusterAssignment(SuspiciousChunkAnalyzer.ClusterAssignment assignment) {
        if (assignment == null) {
            this.clusterId = 0;
            this.clusterPrimary = true;
            this.clusterSize = 1;
            this.effectiveScore = this.rawScore;
            this.confidence = SuspiciousChunkAnalyzer.classifyConfidence(this.rawScore);
            return;
        }
        this.clusterId = assignment.clusterId();
        this.clusterPrimary = assignment.primary();
        this.clusterSize = assignment.clusterSize();
        this.effectiveScore = assignment.corroboratedScore();
        this.confidence = assignment.confidence();
    }

    public List<String> getReasonTags() {
        return reasonTags;
    }

    public String getFormattedReasons() {
        if (reasonTags.isEmpty()) {
            return "Multi-Signal";
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
