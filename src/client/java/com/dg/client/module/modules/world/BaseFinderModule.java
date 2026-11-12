package com.dg.client.module.modules.world;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.core.WaypointManager;
import com.dg.client.core.detection.DetectionEntry;
import com.dg.client.core.detection.DetectionRegistry;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section 7 Suspicious Chunk Detector ("BaseFinder"):
 * Evaluates chunks asynchronously across 10 weighted heuristics, stores flagged chunks
 * per-server in DetectionRegistry (config/dgclient/basefinder/<server>.json), feeds the
 * ClickGUI Detections tab, and renders passable glowing neon-green/white 3D chunk boxes
 * with floating score + reason + distance labels.
 */
public final class BaseFinderModule extends Module {
    public enum BoxMode {
        TIGHT_BOUNDS,
        Y_RANGE,
        FULL_COLUMN
    }

    private final SettingGroup renderGroup = createGroup("Box & Alerts");
    private final SettingGroup heuristicGroup = createGroup("Heuristics");
    private final SettingGroup weightGroup = createGroup("Heuristic Weights");
    private final SettingGroup perfGroup = createGroup("Scanner Budget");

    // Visual & Alert settings
    public final EnumSetting<BoxMode> boxMode = renderGroup.add(new EnumSetting<>(
            "Box Mode", "Vertical bounds of glowing chunk highlight box", BoxMode.TIGHT_BOUNDS));
    public final IntSetting customMinY = renderGroup.add(new IntSetting(
            "Custom Min Y", "Bottom Y when Box Mode = Y_RANGE", -32, -64, 200, 4)
            .visibleWhen(() -> boxMode.getValue() == BoxMode.Y_RANGE));
    public final IntSetting customMaxY = renderGroup.add(new IntSetting(
            "Custom Max Y", "Top Y when Box Mode = Y_RANGE", 96, -32, 320, 4)
            .visibleWhen(() -> boxMode.getValue() == BoxMode.Y_RANGE));
    public final DoubleSetting minScoreThreshold = renderGroup.add(new DoubleSetting(
            "Min Score", "Minimum suspicion score (0..100) to flag & render a chunk", 28.0, 10.0, 90.0, 2.0));
    public final DoubleSetting alertThreshold = renderGroup.add(new DoubleSetting(
            "Alert Score", "Triggers sound + toast notification when chunk exceeds this score", 60.0, 25.0, 100.0, 5.0));
    public final BooleanSetting playAlertSound = renderGroup.add(new BooleanSetting(
            "Alert Sound", "Plays a subtle chime on high-score base detection", true));
    public final BooleanSetting showLabels = renderGroup.add(new BooleanSetting(
            "Floating Labels", "Draws score, top reason tags, and distance above flagged chunks", true));
    public final IntSetting maxRenderDistance = renderGroup.add(new IntSetting(
            "Render Distance (m)", "Maximum distance to render flagged chunk boxes", 320, 64, 1024, 32));

    // 10 Heuristic Toggles
    public final BooleanSetting hStorageActivity = heuristicGroup.add(new BooleanSetting("Player Activity Blocks", "Storage, furnaces, tables, beds, signs, beacons", true));
    public final BooleanSetting hRedstone = heuristicGroup.add(new BooleanSetting("Redstone Activity", "Dust, repeaters, observers, pistons, rails", true));
    public final BooleanSetting hTunnelsCavities = heuristicGroup.add(new BooleanSetting("Cavities & Tunnels", "Straight 1x2/2x2/3x3 tunnels, shafts & flat rooms", true));
    public final BooleanSetting hManmadeBlocks = heuristicGroup.add(new BooleanSetting("Unnatural Blocks", "Planks, cobble, glass, wool, concrete, bricks, torches", true));
    public final BooleanSetting hLightAnomalies = heuristicGroup.add(new BooleanSetting("Light Anomalies", "Artificial underground block-light without lava", true));
    public final BooleanSetting hDeltaTracking = heuristicGroup.add(new BooleanSetting("Chunk Delta Updates", "Live block updates ticking inside the chunk", true));
    public final BooleanSetting hNewOldChunks = heuristicGroup.add(new BooleanSetting("New/Old Chunk Tells", "Border flowing liquid & pre-1.18 generation tells", true));
    public final BooleanSetting hEntityCluster = heuristicGroup.add(new BooleanSetting("Entity Clustering", "Item frames, armor stands, carts, boats, tamed mobs", true));
    public final BooleanSetting hHighwaysPortals = heuristicGroup.add(new BooleanSetting("Portals & Highways", "Obsidian portals, ice roads, Nether roof/floor trails", true));
    public final BooleanSetting hYZeroIntegration = heuristicGroup.add(new BooleanSetting("Y-0 Leak Boost", "Incorporates Y0Bypass underground leaked data", true));

    // Weights
    public final DoubleSetting wStorage = weightGroup.add(new DoubleSetting("Activity Weight", "Multiplier for storage & utility blocks", 1.25, 0.2, 3.0, 0.05));
    public final DoubleSetting wRedstone = weightGroup.add(new DoubleSetting("Redstone Weight", "Multiplier for redstone components", 1.15, 0.2, 3.0, 0.05));
    public final DoubleSetting wTunnels = weightGroup.add(new DoubleSetting("Tunnel Weight", "Multiplier for geometric tunnels & rooms", 1.0, 0.2, 3.0, 0.05));
    public final DoubleSetting wManmade = weightGroup.add(new DoubleSetting("Manmade Weight", "Multiplier for unnatural building blocks", 1.0, 0.2, 3.0, 0.05));
    public final DoubleSetting wHighways = weightGroup.add(new DoubleSetting("Highway/Portal Weight", "Multiplier for portals & ice highways", 1.1, 0.2, 3.0, 0.05));

    // Scanner budget
    public final IntSetting chunksPerTick = perfGroup.add(new IntSetting(
            "Chunks / Tick", "Max queued chunks snapshotted per client tick (zero-stutter budget)", 2, 1, 8, 1));
    public final IntSetting scanRadiusChunks = perfGroup.add(new IntSetting(
            "Scan Radius (chunks)", "Radius around player queued on enable / movement", 8, 3, 16, 1));

    private final Deque<ChunkPos> pendingQueue = new ArrayDeque<>();
    private final Set<Long> queuedSet = new HashSet<>();
    private final ConcurrentHashMap<Long, Integer> chunkDeltaCounts = new ConcurrentHashMap<>();
    private final Set<Long> alertedChunks = ConcurrentHashMap.newKeySet();
    private int sweepTimer = 0;

    public BaseFinderModule() {
        super("BaseFinder", "Asynchronous multi-heuristic suspicious chunk & stash detector", ModuleCategory.DETECT);
    }

    @Override
    public String getDisplayInfo() {
        return DGClient.getInstance().getDetectionRegistry().getAll().size() + " found";
    }

    @Override
    protected void onEnable() {
        DGClient.getInstance().getDetectionRegistry().ensureServerLoaded();
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
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!hDeltaTracking.getValue()) {
            return;
        }
        if (event.getPacket() instanceof BlockUpdateS2CPacket bu) {
            long key = ChunkPos.toLong(bu.getPos().getX() >> 4, bu.getPos().getZ() >> 4);
            int c = chunkDeltaCounts.merge(key, 1, Integer::sum);
            if (c == 6 || c == 20) {
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
        if (++sweepTimer >= 80) {
            sweepTimer = 0;
            enqueueNearbyLoadedChunks();
        }

        int budget = chunksPerTick.getValue();
        String dim = WaypointManager.getCurrentDimensionId();
        while (budget > 0 && !pendingQueue.isEmpty()) {
            ChunkPos pos = pendingQueue.pollFirst();
            if (pos == null) break;
            queuedSet.remove(pos.toLong());

            WorldChunk wc = mc.world.getChunkManager().getWorldChunk(pos.x, pos.z);
            if (wc == null || wc.isEmpty()) {
                continue;
            }
            ChunkScanSnapshot snap = ChunkScanSnapshot.capture(mc.world, wc, dim);
            budget--;
            DGClient.getInstance().getScannerPool().submit(() -> evaluateSnapshot(snap));
        }
    }

    private void enqueueNearbyLoadedChunks() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = scanRadiusChunks.getValue();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                WorldChunk wc = mc.world.getChunkManager().getWorldChunk(pcx + dx, pcz + dz);
                if (wc != null && !wc.isEmpty()) {
                    enqueueChunk(wc.getPos());
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

    private void evaluateSnapshot(ChunkScanSnapshot snap) {
        int storageCount = 0;
        int utilCount = 0;
        int redstoneCount = 0;
        int manmadeCount = 0;
        int torchCount = 0;
        int portalObsidianCount = 0;
        int iceRoadCount = 0;

        int minAnomY = Integer.MAX_VALUE;
        int maxAnomY = Integer.MIN_VALUE;

        for (int s = 0; s < snap.sectionCount; s++) {
            byte[] grid = snap.sectionCells[s];
            if (grid == null) continue;
            int secBaseY = snap.bottomY + (s << 4);
            for (int idx = 0; idx < 4096; idx++) {
                byte cat = grid[idx];
                if (cat <= ChunkScanSnapshot.CAT_AIR) continue;
                int wy = secBaseY + (idx >> 8);
                boolean flaggedHere = true;
                switch (cat) {
                    case ChunkScanSnapshot.CAT_STORAGE -> storageCount++;
                    case ChunkScanSnapshot.CAT_ACTIVITY_UTIL -> utilCount++;
                    case ChunkScanSnapshot.CAT_REDSTONE -> redstoneCount++;
                    case ChunkScanSnapshot.CAT_MANMADE -> manmadeCount++;
                    case ChunkScanSnapshot.CAT_TORCH_LIGHT -> torchCount++;
                    case ChunkScanSnapshot.CAT_OBSIDIAN_PORTAL -> portalObsidianCount++;
                    case ChunkScanSnapshot.CAT_ICE_HIGHWAY -> {
                        if (snap.dimensionId.contains("the_nether") || wy >= 100 || wy <= 55) {
                            iceRoadCount++;
                        } else {
                            flaggedHere = false;
                        }
                    }
                    default -> flaggedHere = false;
                }
                if (flaggedHere) {
                    if (wy < minAnomY) minAnomY = wy;
                    if (wy > maxAnomY) maxAnomY = wy;
                }
            }
        }

        // Geometric tunnel & shaft scan (straight 1x2 / 2x2 corridors >= 7 blocks & vertical 1x1 shafts)
        int straightTunnels = 0;
        if (hTunnelsCavities.getValue()) {
            for (int wy = snap.bottomY + 4; wy < snap.bottomY + (snap.sectionCount << 4) - 4; wy += 3) {
                if (!snap.dimensionId.contains("the_nether") && wy > 56) break;
                for (int lane = 2; lane < 14; lane += 3) {
                    int runX = 0;
                    int runZ = 0;
                    for (int step = 0; step < 16; step++) {
                        // 1x2 corridor with solid floor & ceiling along X
                        if (is1x2TunnelSlice(snap, step, wy, lane)) {
                            if (++runX >= 8) {
                                straightTunnels++;
                                minAnomY = Math.min(minAnomY, wy);
                                maxAnomY = Math.max(maxAnomY, wy + 2);
                                break;
                            }
                        } else {
                            runX = 0;
                        }
                        // Along Z
                        if (is1x2TunnelSlice(snap, lane, wy, step)) {
                            if (++runZ >= 8) {
                                straightTunnels++;
                                minAnomY = Math.min(minAnomY, wy);
                                maxAnomY = Math.max(maxAnomY, wy + 2);
                                break;
                            }
                        } else {
                            runZ = 0;
                        }
                    }
                }
            }
        }

        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // 1. Player Activity Blocks (Storage + utilities)
        if (hStorageActivity.getValue()) {
            if (storageCount > 0) {
                double pts = Math.min(55.0, (storageCount * 9.0 + Math.sqrt(storageCount) * 8.0) * wStorage.getValue());
                score += pts;
                reasons.add("Storage x" + storageCount);
            }
            if (utilCount > 0) {
                double pts = Math.min(35.0, utilCount * 5.5 * wStorage.getValue());
                score += pts;
                reasons.add("Workstations x" + utilCount);
            }
        }

        // 2. Redstone Activity
        if (hRedstone.getValue() && redstoneCount > 0) {
            double pts = Math.min(40.0, redstoneCount * 4.2 * wRedstone.getValue());
            score += pts;
            reasons.add("Redstone x" + redstoneCount);
        }

        // 3. Cavities & Tunnels
        if (hTunnelsCavities.getValue() && straightTunnels > 0) {
            double pts = Math.min(28.0, straightTunnels * 8.5 * wTunnels.getValue());
            score += pts;
            reasons.add("1x2 Tunnel");
        }

        // 4. Unnatural / Manmade Blocks (filter small mineshaft/dungeon false positives by requiring >= 10 unless combined)
        if (hManmadeBlocks.getValue()) {
            if (manmadeCount >= 12) {
                double pts = Math.min(34.0, (manmadeCount * 0.45) * wManmade.getValue());
                score += pts;
                reasons.add("Builds x" + manmadeCount);
            }
            if (torchCount >= 3) {
                score += Math.min(18.0, torchCount * 2.5 * wManmade.getValue());
                reasons.add("Torches x" + torchCount);
            }
        }

        // 5. Light Anomalies
        if (hLightAnomalies.getValue() && snap.artificialUndergroundLightSamples >= 2) {
            score += Math.min(20.0, snap.artificialUndergroundLightSamples * 5.0);
            reasons.add("Underground Light");
        }

        // 6. Chunk Delta Updates
        if (hDeltaTracking.getValue()) {
            int deltas = chunkDeltaCounts.getOrDefault(ChunkPos.toLong(snap.chunkX, snap.chunkZ), 0);
            if (deltas >= 5) {
                score += Math.min(22.0, deltas * 1.4);
                reasons.add("Active Deltas x" + deltas);
            }
        }

        // 7. NewChunks / OldChunks Tells
        if (hNewOldChunks.getValue()) {
            if (snap.hasZeroLevelBedrockSignature && !snap.hasCopperOrTuffOrDeepslate && snap.dimensionId.contains("overworld")) {
                score += 18.0;
                reasons.add("Pre-1.18 OldChunk");
            } else if (snap.borderFlowingFluidCount >= 5) {
                score += 12.0;
                reasons.add("NewChunk Trail");
            }
        }

        // 8. Entity Clustering
        if (hEntityCluster.getValue() && snap.clusteredSpecialEntities >= 2) {
            score += Math.min(30.0, snap.clusteredSpecialEntities * 6.5);
            reasons.add("Entities x" + snap.clusteredSpecialEntities);
        }

        // 9. Portals & Highways
        if (hHighwaysPortals.getValue()) {
            if (portalObsidianCount >= 10) {
                score += Math.min(28.0, portalObsidianCount * 1.6 * wHighways.getValue());
                reasons.add("Portal/Obsidian");
            }
            if (iceRoadCount >= 8) {
                score += Math.min(26.0, iceRoadCount * 1.4 * wHighways.getValue());
                reasons.add("Ice Highway");
            }
        }

        // 10. Y-0 Underground Bypass Integration
        if (hYZeroIntegration.getValue()) {
            YZeroBypassModule y0 = DGClient.getInstance().getModuleManager().get(YZeroBypassModule.class);
            if (y0 != null) {
                double y0Boost = y0.getStore().computeBaseFinderBoost(snap.chunkX, snap.chunkZ, 0.55f);
                if (y0Boost >= 4.0) {
                    score += y0Boost;
                    reasons.add("Y<0 Leak");
                    minAnomY = Math.min(minAnomY, -32);
                }
            }
        }

        double clampedScore = MathHelper.clamp(score, 0.0, 100.0);
        if (clampedScore < minScoreThreshold.getValue()) {
            return;
        }

        if (minAnomY == Integer.MAX_VALUE) {
            minAnomY = 0;
            maxAnomY = 64;
        }

        DetectionEntry entry = new DetectionEntry(
                snap.chunkX,
                snap.chunkZ,
                snap.dimensionId,
                clampedScore,
                reasons,
                minAnomY,
                maxAnomY,
                System.currentTimeMillis(),
                DetectionEntry.Status.ACTIVE
        );

        DetectionRegistry reg = DGClient.getInstance().getDetectionRegistry();
        reg.putOrUpdate(entry);

        long key = ChunkPos.toLong(snap.chunkX, snap.chunkZ);
        if (clampedScore >= alertThreshold.getValue() && alertedChunks.add(key)) {
            DGClient.getInstance().getNotificationManager().push(
                    NotificationManager.Type.DETECTION,
                    String.format(Locale.ROOT, "Suspicious Chunk [%.0f%%]", clampedScore),
                    String.format(Locale.ROOT, "X:%d Z:%d (%s)", entry.getCenterX(), entry.getCenterZ(), entry.getFormattedReasons())
            );
            if (playAlertSound.getValue() && mc.player != null) {
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 0.65f, 1.35f);
                    }
                });
            }
        }
    }

    private static boolean is1x2TunnelSlice(ChunkScanSnapshot snap, int x, int wy, int z) {
        return snap.getCategoryAt(x, wy, z) == ChunkScanSnapshot.CAT_AIR
                && snap.getCategoryAt(x, wy + 1, z) == ChunkScanSnapshot.CAT_AIR
                && snap.getCategoryAt(x, wy - 1, z) != ChunkScanSnapshot.CAT_AIR
                && snap.getCategoryAt(x, wy + 2, z) != ChunkScanSnapshot.CAT_AIR;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        String currentDim = WaypointManager.getCurrentDimensionId();
        double maxDistSq = maxRenderDistance.getValue() * (double) maxRenderDistance.getValue();
        double threshold = minScoreThreshold.getValue();

        List<DetectionEntry> all = DGClient.getInstance().getDetectionRegistry().getAll();
        int rendered = 0;
        for (DetectionEntry d : all) {
            if (rendered >= 96) break;
            if (d.getStatus() == DetectionEntry.Status.IGNORED) continue;
            if (!currentDim.equals(d.getDimension())) continue;
            if (d.getScore() < threshold && d.getStatus() != DetectionEntry.Status.VERIFIED) continue;

            double cx = d.getCenterX();
            double cz = d.getCenterZ();
            double distSq = mc.player.squaredDistanceTo(cx, mc.player.getY(), cz);
            if (distSq > maxDistSq) continue;

            double y1;
            double y2;
            switch (boxMode.getValue()) {
                case FULL_COLUMN -> {
                    y1 = mc.world.getBottomY();
                    y2 = Math.min(mc.world.getTopYInclusive(), mc.player.getY() + 64.0);
                }
                case Y_RANGE -> {
                    y1 = customMinY.getValue();
                    y2 = customMaxY.getValue();
                }
                default -> {
                    y1 = d.getMinDetectedY() - 2.0;
                    y2 = Math.max(y1 + 6.0, d.getMaxDetectedY() + 3.0);
                }
            }

            double minX = d.getChunkX() << 4;
            double minZ = d.getChunkZ() << 4;
            Box chunkBox = new Box(minX + 0.05, y1, minZ + 0.05, minX + 15.95, y2, minZ + 15.95);
            if (event.getFrustum() != null && !event.getFrustum().isVisible(chunkBox.expand(2.0))) {
                continue;
            }

            // Intensity scales cleanly with score: low = subtle green, high = glowing neon green + white accents
            float norm = (float) MathHelper.clamp(d.getScore() / 100.0, 0.25, 1.0);
            int primary = d.getScore() >= 65.0 ? ColorUtil.NEON_GREEN : ColorUtil.MINT_GREEN;
            int accent = d.getScore() >= 70.0 ? ColorUtil.PURE_WHITE : ColorUtil.NEON_GREEN;

            Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), chunkBox, primary, accent, norm, true);

            if (showLabels.getValue()) {
                double labelY = MathHelper.clamp(mc.player.getEyeY() + 2.5, y1 + 2.0, y2 + 2.5);
                String tag = String.format(Locale.ROOT, "%.0f%% | %s | %.0fm",
                        d.getScore(), d.getFormattedReasons(), Math.sqrt(distSq));
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(),
                        new Vec3d(cx, labelY, cz), tag, ColorUtil.PURE_WHITE, accent, 1.0f, true);
            }
            rendered++;
        }
    }
}
