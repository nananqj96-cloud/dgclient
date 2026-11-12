package com.dg.client.module.modules.world;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.core.WaypointManager;
import com.dg.client.core.detection.DetectionEntry;
import com.dg.client.core.detection.DetectionRegistry;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.BlockUpdateEvent;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.setting.SettingGroup;
import com.dg.client.util.ColorUtil;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-indicator Suspicious Chunk & Base Detector ("BaseFinder").
 *
 * <p>Requires multiple independent categories of player activity (storage clusters,
 * workstation setups, redstone contraptions, rotated deepslate, sealed 1x1 dive shafts,
 * modified spawner grinders, non-natural entities) while suppressing vanilla structure
 * false positives (1.21 Trial Chambers, Mineshafts, Dungeons, Villages, Ancient Cities,
 * Ruined Portals, Geodes) and deduplicating adjacent base chunks into clean clusters.
 */
public final class BaseFinderModule extends Module {
    public enum RenderStyle {
        FLAT_MARKER,
        CORNER_BRACKETS,
        COMPACT_SLICE
    }

    public enum MarkerPlane {
        DETECTED_Y,
        FIXED_Y
    }

    public enum LabelMode {
        CLUSTER_ANCHORS,
        HIGH_CONFIDENCE_ONLY,
        ALL_CHUNKS,
        OFF
    }

    private static final int LOW_CONF_DEFAULT = ColorUtil.rgba(92, 224, 168, 175);
    private static final int HIGH_CONF_DEFAULT = ColorUtil.NEON_GREEN;

    private final SettingGroup detectionGroup = createGroup("Detection & Sensitivity");
    private final SettingGroup renderGroup = createGroup("Visuals & Alerts");
    private final SettingGroup heuristicGroup = createGroup("Signal Categories");
    private final SettingGroup weightGroup = createGroup("Category Weights");
    private final SettingGroup perfGroup = createGroup("Scanner Budget");

    // Sensitivity, Threshold & Cluster Deduplication
    public final EnumSetting<SuspiciousChunkAnalyzer.Sensitivity> sensitivity = detectionGroup.add(
            new EnumSetting<>("Sensitivity", "Preset balancing false-positive rejection vs sensitivity",
                    SuspiciousChunkAnalyzer.Sensitivity.BALANCED));
    public final DoubleSetting minScoreThreshold = detectionGroup.add(new DoubleSetting(
            "Min Score", "Custom suspicion score threshold (18..90) when Sensitivity = CUSTOM", 42.0, 18.0, 90.0, 2.0)
            .visibleWhen(() -> sensitivity.getValue() == SuspiciousChunkAnalyzer.Sensitivity.CUSTOM));
    public final IntSetting clusterMergeRadius = detectionGroup.add(new IntSetting(
            "Cluster Merge (ch)", "Merges nearby flagged chunks of the same base into one cluster", 1, 1, 3, 1)
            .onChange(r -> {
                if (DGClient.getInstance() != null) {
                    DGClient.getInstance().getDetectionRegistry().setClusterMergeRadius(r);
                }
            }));
    public final BooleanSetting pruneOutsideRender = detectionGroup.add(new BooleanSetting(
            "Clear Unloaded", "Removes unverified detections after leaving render distance", false));

    // Clean Visuals & Alerts
    public final EnumSetting<RenderStyle> renderStyle = renderGroup.add(new EnumSetting<>(
            "Visual Style", "Chunk boundary highlight style (Flat Marker avoids screen clutter)", RenderStyle.FLAT_MARKER));
    public final EnumSetting<MarkerPlane> markerPlane = renderGroup.add(new EnumSetting<>(
            "Marker Plane", "Vertical placement for Flat Marker style", MarkerPlane.DETECTED_Y)
            .visibleWhen(() -> renderStyle.getValue() == RenderStyle.FLAT_MARKER));
    public final IntSetting fixedMarkerY = renderGroup.add(new IntSetting(
            "Fixed Marker Y", "Y level when Marker Plane = FIXED_Y", 63, -60, 200, 1)
            .visibleWhen(() -> renderStyle.getValue() == RenderStyle.FLAT_MARKER
                    && markerPlane.getValue() == MarkerPlane.FIXED_Y));
    public final EnumSetting<LabelMode> labelMode = renderGroup.add(new EnumSetting<>(
            "Labels", "Controls floating score/reason badges to prevent screen clutter", LabelMode.CLUSTER_ANCHORS));
    public final BooleanSetting throughWalls = renderGroup.add(new BooleanSetting(
            "Through Walls", "Renders chunk boundaries through terrain", true));
    public final ColorSetting lowConfidenceColor = renderGroup.add(new ColorSetting(
            "Low Conf Color", "Subtle outline color for low/moderate confidence leads", LOW_CONF_DEFAULT));
    public final ColorSetting highConfidenceColor = renderGroup.add(new ColorSetting(
            "High Conf Color", "Vivid outline color for high-confidence bases & stashes", HIGH_CONF_DEFAULT));
    public final IntSetting maxRenderChunks = renderGroup.add(new IntSetting(
            "Max Rendered", "Cap on simultaneously rendered suspicious chunks", 48, 8, 128, 8));
    public final IntSetting maxRenderDistance = renderGroup.add(new IntSetting(
            "Render Distance (m)", "Maximum distance to render flagged chunk markers", 320, 64, 1024, 32));
    public final DoubleSetting alertThreshold = renderGroup.add(new DoubleSetting(
            "Alert Score", "Triggers notification when a base cluster reaches this score", 62.0, 30.0, 100.0, 4.0));
    public final BooleanSetting playAlertSound = renderGroup.add(new BooleanSetting(
            "Alert Sound", "Plays a subtle chime when a new high-confidence base cluster is found", true));

    // Multi-indicator signal toggles
    public final BooleanSetting hStorageActivity = heuristicGroup.add(new BooleanSetting(
            "Storage & Workstations", "Shulkers, e-chests, container clusters, hoppers, base stations, written signs", true));
    public final BooleanSetting hRedstone = heuristicGroup.add(new BooleanSetting(
            "Automation & Farms", "Redstone contraptions, bubble elevators, spawner grinders, kelp/bamboo farms", true));
    public final BooleanSetting hTunnelsCavities = heuristicGroup.add(new BooleanSetting(
            "Concealment & Shafts", "Rotated deepslate patches, sealed 1x1 dive shafts & excavated corridors", true));
    public final BooleanSetting hManmadeBlocks = heuristicGroup.add(new BooleanSetting(
            "Unnatural Builds", "Player-only build palettes (concrete, cherry/bamboo, tinted glass, sky/roof builds)", true));
    public final BooleanSetting hLightAnomalies = heuristicGroup.add(new BooleanSetting(
            "Underground Lighting", "Corroborating non-lava underground block-light", true));
    public final BooleanSetting hDeltaTracking = heuristicGroup.add(new BooleanSetting(
            "Live Chunk Deltas", "Active block updates ticking inside loaded chunks", true));
    public final BooleanSetting hNewOldChunks = heuristicGroup.add(new BooleanSetting(
            "OldChunk Signature", "Corroborating pre-1.18 generation tells in Overworld", true));
    public final BooleanSetting hEntityCluster = heuristicGroup.add(new BooleanSetting(
            "Player Entities", "Non-elytra item frames, armor stands, traded/underground villagers, pets", true));
    public final BooleanSetting hHighwaysPortals = heuristicGroup.add(new BooleanSetting(
            "Portals & Highways", "Active portals and packed/blue ice highways", true));
    public final BooleanSetting hYZeroIntegration = heuristicGroup.add(new BooleanSetting(
            "Y-0 Leak Boost", "Incorporates Y0Bypass underground leaked data", true));

    // Category weights
    public final DoubleSetting wStorage = weightGroup.add(new DoubleSetting("Storage Weight", "Multiplier for storage & workstations", 1.0, 0.3, 2.5, 0.05));
    public final DoubleSetting wRedstone = weightGroup.add(new DoubleSetting("Automation Weight", "Multiplier for redstone & farms", 1.0, 0.3, 2.5, 0.05));
    public final DoubleSetting wTunnels = weightGroup.add(new DoubleSetting("Concealment Weight", "Multiplier for rotated deepslate & shafts", 1.0, 0.3, 2.5, 0.05));
    public final DoubleSetting wManmade = weightGroup.add(new DoubleSetting("Build Weight", "Multiplier for unnatural builds", 1.0, 0.3, 2.5, 0.05));
    public final DoubleSetting wHighways = weightGroup.add(new DoubleSetting("Portal/Highway Weight", "Multiplier for portals & ice roads", 1.0, 0.3, 2.5, 0.05));

    // Scanner budget
    public final IntSetting chunksPerTick = perfGroup.add(new IntSetting(
            "Chunks / Tick", "Max queued chunks snapshotted per client tick (zero-stutter budget)", 2, 1, 8, 1));
    public final IntSetting scanRadiusChunks = perfGroup.add(new IntSetting(
            "Scan Radius (chunks)", "Radius around player queued on enable / movement", 8, 3, 16, 1));

    private final Deque<ChunkPos> pendingQueue = new ArrayDeque<>();
    private final Set<Long> queuedSet = new HashSet<>();
    private final ConcurrentHashMap<Long, Integer> chunkDeltaCounts = new ConcurrentHashMap<>();
    private final Set<String> alertedClusters = ConcurrentHashMap.newKeySet();
    private volatile long lastAlertMs = 0L;
    private int sweepTimer = 0;

    public BaseFinderModule() {
        super("BaseFinder", "Multi-indicator suspicious chunk & base detector with natural-structure filtering", ModuleCategory.DETECT);
    }

    public double getActiveThreshold() {
        SuspiciousChunkAnalyzer.Sensitivity sens = sensitivity.getValue();
        return sens == SuspiciousChunkAnalyzer.Sensitivity.CUSTOM
                ? minScoreThreshold.getValue()
                : sens.getDefaultThreshold();
    }

    public SuspiciousChunkAnalyzer.Config buildAnalyzerConfig() {
        double threshold = getActiveThreshold();
        return new SuspiciousChunkAnalyzer.Config(
                sensitivity.getValue(),
                threshold,
                hStorageActivity.getValue(),
                hRedstone.getValue(),
                hTunnelsCavities.getValue(),
                hManmadeBlocks.getValue(),
                hLightAnomalies.getValue(),
                hDeltaTracking.getValue(),
                hNewOldChunks.getValue(),
                hEntityCluster.getValue(),
                hHighwaysPortals.getValue(),
                hYZeroIntegration.getValue(),
                wStorage.getValue(),
                wRedstone.getValue(),
                wTunnels.getValue(),
                wManmade.getValue(),
                wHighways.getValue()
        );
    }

    @Override
    public String getDisplayInfo() {
        double threshold = getActiveThreshold();
        int clusters = 0;
        for (DetectionEntry d : DGClient.getInstance().getDetectionRegistry().getDeduplicatedClusters()) {
            if (d.getScore() >= threshold || d.getStatus() == DetectionEntry.Status.VERIFIED) {
                clusters++;
            }
        }
        return clusters + (clusters == 1 ? " base" : " bases");
    }

    @Override
    protected void onEnable() {
        DetectionRegistry reg = DGClient.getInstance().getDetectionRegistry();
        reg.setClusterMergeRadius(clusterMergeRadius.getValue());
        reg.ensureServerLoaded();
        enqueueNearbyLoadedChunks();
    }

    @Override
    protected void onDisable() {
        pendingQueue.clear();
        queuedSet.clear();
        DGClient.getInstance().getDetectionRegistry().saveCurrentServer();
    }

    @EventHandler
    public void onChunkLoad(ChunkDataEvent.Load event) {
        enqueueChunk(event.getChunk().getPos());
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        enqueueChunk(new ChunkPos(event.getPos()));
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!hDeltaTracking.getValue()) {
            return;
        }
        if (event.getPacket() instanceof BlockUpdateS2CPacket bu) {
            long key = ChunkPos.toLong(bu.getPos().getX() >> 4, bu.getPos().getZ() >> 4);
            int c = chunkDeltaCounts.merge(key, 1, Integer::sum);
            if (c == 8 || c == 24) {
                enqueueChunk(new ChunkPos(bu.getPos()));
            }
        } else if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket cdu) {
            cdu.visitUpdates((pos, st) -> {
                long key = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
                chunkDeltaCounts.merge(key, 1, Integer::sum);
            });
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        String dim = WaypointManager.getCurrentDimensionId();
        if (++sweepTimer >= 80) {
            sweepTimer = 0;
            enqueueNearbyLoadedChunks();
            if (pruneOutsideRender.getValue()) {
                int keepDist = Math.max(scanRadiusChunks.getValue() + 3, mc.options.getClampedViewDistance() + 3);
                DGClient.getInstance().getDetectionRegistry().pruneOutsideRadius(
                        dim, mc.player.getChunkPos().x, mc.player.getChunkPos().z, keepDist);
            }
        }

        int budget = chunksPerTick.getValue();
        while (budget > 0 && !pendingQueue.isEmpty()) {
            ChunkPos pos = pendingQueue.pollFirst();
            if (pos == null) break;
            queuedSet.remove(pos.toLong());

            if (!mc.world.getChunkManager().isChunkLoaded(pos.x, pos.z)) {
                continue;
            }
            WorldChunk wc = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
            if (wc == null || wc.isEmpty()) {
                continue;
            }
            ChunkScanSnapshot snap = ChunkScanSnapshot.capture(mc.world, wc, dim);
            SuspiciousChunkAnalyzer.Config cfg = buildAnalyzerConfig();
            budget--;
            DGClient.getInstance().getScannerPool().submit(() -> evaluateSnapshot(snap, cfg));
        }
    }

    /**
     * Synchronously scans a single loaded chunk (used by live diagnostics and automated client tests).
     */
    public DetectionEntry scanChunkImmediate(WorldChunk chunk) {
        if (mc.world == null || chunk == null || chunk.isEmpty()) {
            return null;
        }
        String dim = WaypointManager.getCurrentDimensionId();
        ChunkScanSnapshot snap = ChunkScanSnapshot.capture(mc.world, chunk, dim);
        return evaluateSnapshot(snap, buildAnalyzerConfig());
    }

    private void enqueueNearbyLoadedChunks() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = scanRadiusChunks.getValue();
        // Spiral / near-first ordering so chunks closest to the player scan first
        for (int ring = 0; ring <= r; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    if (!mc.world.getChunkManager().isChunkLoaded(pcx + dx, pcz + dz)) continue;
                    WorldChunk wc = mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                    if (wc != null && !wc.isEmpty()) {
                        enqueueChunk(wc.getPos());
                    }
                }
            }
        }
    }

    private void enqueueChunk(ChunkPos pos) {
        if (queuedSet.size() > 512) {
            return;
        }
        if (queuedSet.add(pos.toLong())) {
            pendingQueue.addLast(pos);
        }
    }

    private DetectionEntry evaluateSnapshot(ChunkScanSnapshot snap, SuspiciousChunkAnalyzer.Config cfg) {
        int deltas = chunkDeltaCounts.getOrDefault(ChunkPos.toLong(snap.chunkX, snap.chunkZ), 0);
        double y0Boost = 0.0;
        if (cfg.checkYZeroLeaks()) {
            YZeroBypassModule y0 = DGClient.getInstance().getModuleManager().get(YZeroBypassModule.class);
            if (y0 != null) {
                y0Boost = y0.getStore().computeBaseFinderBoost(snap.chunkX, snap.chunkZ, 0.55f);
            }
        }

        SuspiciousChunkAnalyzer.ChunkTelemetry telemetry = snap.finishOffThreadAnalysis(deltas, y0Boost);
        SuspiciousChunkAnalyzer.EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(telemetry, cfg);

        DetectionRegistry reg = DGClient.getInstance().getDetectionRegistry();
        reg.setClusterMergeRadius(clusterMergeRadius.getValue());

        if (!res.flagged()) {
            reg.removeIfActive(snap.dimensionId, snap.chunkX, snap.chunkZ);
            return null;
        }

        DetectionEntry entry = new DetectionEntry(
                snap.chunkX,
                snap.chunkZ,
                snap.dimensionId,
                res.score(),
                res.indicatorCount(),
                res.reasons(),
                res.minY(),
                res.maxY(),
                res.medianY(),
                System.currentTimeMillis(),
                DetectionEntry.Status.ACTIVE
        );
        reg.putOrUpdate(entry);

        // Deduplicated, rate-limited notification: alert once per primary cluster anchor
        if (entry.isClusterPrimary() && entry.getScore() >= alertThreshold.getValue()) {
            String clusterKey = snap.dimensionId + "#cluster_" + (entry.getChunkX() >> 2) + "_" + (entry.getChunkZ() >> 2);
            long now = System.currentTimeMillis();
            if (alertedClusters.add(clusterKey) && now - lastAlertMs >= 2500L) {
                lastAlertMs = now;
                String clusterSuffix = entry.getClusterSize() > 1 ? " (" + entry.getClusterSize() + " chunks)" : "";
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.DETECTION,
                        String.format(Locale.ROOT, "Base [%s %.0f%%]%s",
                                entry.getConfidence().name(), entry.getScore(), clusterSuffix),
                        String.format(Locale.ROOT, "X:%d Z:%d (%s)",
                                entry.getCenterX(), entry.getCenterZ(), entry.getFormattedReasons())
                );
                if (playAlertSound.getValue() && mc.player != null) {
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.60f, 1.35f);
                        }
                    });
                }
            }
        }
        return entry;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        String currentDim = WaypointManager.getCurrentDimensionId();
        double maxDist = maxRenderDistance.getValue();
        double maxDistSq = maxDist * maxDist;
        double activeThreshold = getActiveThreshold();
        boolean xray = throughWalls.getValue();

        List<DetectionEntry> all = DGClient.getInstance().getDetectionRegistry().getAll();
        int rendered = 0;
        int cap = maxRenderChunks.getValue();

        for (DetectionEntry d : all) {
            if (rendered >= cap) {
                break;
            }
            if (d.getStatus() == DetectionEntry.Status.IGNORED) {
                continue;
            }
            if (!currentDim.equals(d.getDimension())) {
                continue;
            }
            // Strictly honor the configured sensitivity / score threshold
            if (d.getScore() < activeThreshold && d.getStatus() != DetectionEntry.Status.VERIFIED) {
                continue;
            }

            double cx = d.getCenterX();
            double cz = d.getCenterZ();
            double distSq = mc.player.squaredDistanceTo(cx, mc.player.getY(), cz);
            if (distSq > maxDistSq) {
                continue;
            }

            // Distance fade near outer boundary for clean horizon readability
            double dist = Math.sqrt(distSq);
            float distFade = dist > maxDist * 0.80 ? (float) MathHelper.clamp((maxDist - dist) / (maxDist * 0.20), 0.25, 1.0) : 1.0f;

            SuspiciousChunkAnalyzer.Confidence conf = d.getConfidence();
            boolean isHigh = (conf == SuspiciousChunkAnalyzer.Confidence.HIGH);
            boolean isMedium = (conf == SuspiciousChunkAnalyzer.Confidence.MEDIUM);

            int baseColor = isHigh
                    ? highConfidenceColor.getColor()
                    : (isMedium ? ColorUtil.lerp(lowConfidenceColor.getColor(), highConfidenceColor.getColor(), 0.55f)
                    : lowConfidenceColor.getColor());

            int outlineAlpha = Math.round((isHigh ? 245 : (isMedium ? 195 : 135)) * distFade);
            int fillAlpha = Math.round((isHigh ? 28 : (isMedium ? 18 : 10)) * distFade);
            int outlineCol = ColorUtil.withAlpha(baseColor, outlineAlpha);
            int accentCol = ColorUtil.withAlpha(isHigh ? ColorUtil.PURE_WHITE : baseColor, outlineAlpha);
            float lineWidth = isHigh ? 2.2f : (isMedium ? 1.65f : 1.2f);

            double minX = d.getChunkX() << 4;
            double minZ = d.getChunkZ() << 4;
            double labelAnchorY;

            switch (renderStyle.getValue()) {
                case FLAT_MARKER -> {
                    double planeY = markerPlane.getValue() == MarkerPlane.FIXED_Y
                            ? fixedMarkerY.getValue() + 0.06
                            : MathHelper.clamp(d.getMedianDetectedY() + 0.06, mc.world.getBottomY() + 1.0, mc.world.getTopYInclusive() - 1.0);
                    Box cullBox = new Box(minX, planeY - 1.0, minZ, minX + 16.0, planeY + 3.0, minZ + 16.0);
                    if (event.getFrustum() != null && !event.getFrustum().isVisible(cullBox)) {
                        continue;
                    }
                    float postH = isHigh ? 2.2f : (isMedium ? 1.0f : 0.0f);
                    Render3DUtil.drawFlatChunkMarker(
                            event.getMatrices(),
                            event.getCamera(),
                            d.getChunkX(),
                            d.getChunkZ(),
                            planeY,
                            outlineCol,
                            accentCol,
                            fillAlpha,
                            lineWidth,
                            postH,
                            isHigh,
                            xray
                    );
                    labelAnchorY = planeY + (isHigh ? 2.6 : 1.5);
                }
                case CORNER_BRACKETS -> {
                    double y1 = d.getMinDetectedY() - 0.5;
                    double y2 = Math.min(y1 + 12.0, Math.max(y1 + 3.0, d.getMaxDetectedY() + 1.5));
                    Box chunkBox = new Box(minX + 0.08, y1, minZ + 0.08, minX + 15.92, y2, minZ + 15.92);
                    if (event.getFrustum() != null && !event.getFrustum().isVisible(chunkBox)) {
                        continue;
                    }
                    Render3DUtil.drawCornerBox(event.getMatrices(), event.getCamera(), chunkBox, 0.22f, lineWidth, outlineCol, xray);
                    labelAnchorY = y2 + 1.2;
                }
                default -> {
                    // COMPACT_SLICE: crisp single-pass wireframe capped to 14m height (zero screen-filling walls)
                    double midY = d.getMedianDetectedY();
                    double y1 = Math.max(d.getMinDetectedY() - 0.5, midY - 7.0);
                    double y2 = Math.min(Math.max(y1 + 2.5, d.getMaxDetectedY() + 1.5), y1 + 14.0);
                    Box chunkBox = new Box(minX + 0.08, y1, minZ + 0.08, minX + 15.92, y2, minZ + 15.92);
                    if (event.getFrustum() != null && !event.getFrustum().isVisible(chunkBox)) {
                        continue;
                    }
                    if (fillAlpha > 0) {
                        Render3DUtil.drawFilledBox(event.getMatrices(), event.getCamera(), chunkBox, ColorUtil.withAlpha(baseColor, fillAlpha / 2), xray);
                    }
                    Render3DUtil.drawBoxOutline(event.getMatrices(), event.getCamera(), chunkBox, lineWidth, outlineCol, xray);
                    labelAnchorY = y2 + 1.2;
                }
            }

            boolean drawLabel = switch (labelMode.getValue()) {
                case CLUSTER_ANCHORS -> d.isClusterPrimary();
                case HIGH_CONFIDENCE_ONLY -> isHigh && d.isClusterPrimary();
                case ALL_CHUNKS -> true;
                case OFF -> false;
            };

            if (drawLabel) {
                String confBadge = isHigh ? "HIGH" : (isMedium ? "MED" : "LOW");
                String clusterBadge = d.getClusterSize() > 1 ? " [" + d.getClusterSize() + "ch]" : "";
                String tag = String.format(Locale.ROOT, "%s %.0f%%%s | %s | %.0fm",
                        confBadge, d.getScore(), clusterBadge, d.getFormattedReasons(), dist);
                Render3DUtil.drawBillboardLabel(
                        event.getMatrices(),
                        event.getCamera(),
                        new Vec3d(cx, labelAnchorY, cz),
                        tag,
                        isHigh ? ColorUtil.PURE_WHITE : ColorUtil.SOFT_WHITE,
                        accentCol,
                        isHigh ? 0.95f : 0.84f,
                        xray
                );
            }
            rendered++;
        }
    }
}
