package com.dg.client.core.detection;

import com.dg.client.core.detection.SuspiciousChunkAnalyzer.ChunkInput;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.ChunkTelemetry;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.ClusterAssignment;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.Confidence;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.Config;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.EvaluationResult;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.Sensitivity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Multi-scenario verification suite for {@link SuspiciousChunkAnalyzer}, validating:
 * <ul>
 *     <li>Strict rejection of isolated player blocks & ordinary mining/terrain edits</li>
 *     <li>Suppression of all major vanilla natural structures (Trial Chambers, Mineshafts,
 *         Dungeons, Villages, Ancient Cities, Ruined Portals, End Cities, Geodes)</li>
 *     <li>Multi-indicator detection & confidence grading across realistic stashes, underground
 *         bases, camouflaged dive shafts, grinders, automated farms, and sky bases</li>
 *     <li>Sensitivity presets, custom threshold adherence, and spatial cluster deduplication</li>
 * </ul>
 */
class SuspiciousChunkAnalyzerTest {

    private static ChunkTelemetry emptyOverworld(int cx, int cz) {
        return new ChunkTelemetry(
                cx, cz, "minecraft:overworld",
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                64, 64, 64
        );
    }

    // =========================================================================
    // 1. FALSE-POSITIVE REJECTION: ISOLATED BLOCKS & ORDINARY TERRAIN EDITS
    // =========================================================================

    @Test
    void emptyChunkIsNeverFlagged() {
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(emptyOverworld(0, 0), Config.defaults());
        assertFalse(res.flagged());
        assertEquals(0.0, res.score());
        assertEquals(Confidence.NONE, res.confidence());
    }

    @Test
    void isolatedSingleCraftingTableIsNeverFlagged() {
        ChunkTelemetry t = new ChunkTelemetry(
                1, 2, "minecraft:overworld",
                0, 0, 0, 0, 0,
                1, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                68, 68, 68
        );
        for (Sensitivity s : List.of(Sensitivity.LOW_STRICT, Sensitivity.BALANCED, Sensitivity.HIGH_SENSITIVE)) {
            EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(t, Config.withSensitivity(s));
            assertFalse(res.flagged(), "Isolated crafting table must not flag under " + s);
        }
    }

    @Test
    void ordinaryCaveMiningTorchesAndSingleChestDoNotFlag() {
        // Explorer placed 14 torches underground and left 1 single chest — not a base
        ChunkTelemetry t = new ChunkTelemetry(
                3, -4, "minecraft:overworld",
                0, 0, 1, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 14, 28, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                12, 18, 15
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(t, Config.defaults());
        assertFalse(res.flagged(), "Cave mining torches + 1 chest should not trigger base alert");
    }

    @Test
    void isolatedPortalWithoutBaseIndicatorsDoesNotFlagAlone() {
        ChunkTelemetry t = new ChunkTelemetry(
                -10, 5, "minecraft:overworld",
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 6, 0,
                0, 0, 0.0,
                false, false,
                64, 68, 66
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(t, Config.defaults());
        assertFalse(res.flagged(), "Single standalone portal without base activity must not flag as a base");
    }

    // =========================================================================
    // 2. FALSE-POSITIVE REJECTION: VANILLA NATURAL STRUCTURES
    // =========================================================================

    @Test
    void vanillaTrialChamberIn121IsSuppressed() {
        // 1.21 Trial Chamber: tuff bricks, waxed copper, copper bulbs, trial spawners,
        // dispensers/pots/barrels (filtered at capture when resting on tuff bricks, plus structure signature)
        ChunkTelemetry trialChamber = new ChunkTelemetry(
                12, 8, "minecraft:overworld",
                0, 0, 1, 2, 0,
                0, 0, 2, 0, 0,
                0, 2, 0, 0, 0,
                0, 0, 0, 18, 40, 0,
                180, 3, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -28, -10, -20
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(trialChamber, Config.defaults());
        assertFalse(res.flagged(), "1.21 Trial Chamber must not produce false positive (score=" + res.score() + ")");
    }

    @Test
    void vanillaAbandonedMineshaftIsNotFlagged() {
        ChunkTelemetry mineshaft = new ChunkTelemetry(
                -5, 19, "minecraft:overworld",
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 1, 0,
                0, 0, 1, 8, 16, 0,
                0, 0, 14, 18, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -15, -8, -12
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(mineshaft, Config.defaults());
        assertFalse(res.flagged(), "Vanilla Mineshaft must not be flagged");
    }

    @Test
    void vanillaDungeonSpawnerWithMossyCobbleIsNotFlagged() {
        ChunkTelemetry dungeon = new ChunkTelemetry(
                7, -3, "minecraft:overworld",
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 1, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 32,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                10, 15, 12
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(dungeon, Config.defaults());
        assertFalse(res.flagged(), "Natural mossy-cobble dungeon spawner must not flag");
    }

    @Test
    void vanillaSurfaceVillageIsSuppressed() {
        ChunkTelemetry village = new ChunkTelemetry(
                20, 20, "minecraft:overworld",
                0, 0, 2, 1, 0,
                4, 3, 2, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                2, 24, 16, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                64, 74, 67
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(village, Config.defaults());
        assertFalse(res.flagged(), "Natural surface village must be suppressed (score=" + res.score() + ")");
    }

    @Test
    void vanillaAncientCityRedstoneRoomIsSuppressed() {
        ChunkTelemetry ancientCity = new ChunkTelemetry(
                -40, 15, "minecraft:overworld",
                0, 0, 0, 0, 0,
                0, 0, 2, 0, 0,
                3, 4, 0, 0, 0,
                0, 0, 0, 20, 35, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 95, 110, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -51, -38, -46
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(ancientCity, Config.defaults());
        assertFalse(res.flagged(), "Ancient City redstone/sculk room must be suppressed");
    }

    @Test
    void vanillaRuinedPortalAndEndCityShipAreSuppressed() {
        ChunkTelemetry ruinedPortal = new ChunkTelemetry(
                8, 9, "minecraft:overworld",
                0, 0, 1, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 12,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                63, 70, 65
        );
        assertFalse(SuspiciousChunkAnalyzer.evaluate(ruinedPortal, Config.defaults()).flagged());

        // End City ship: 1 ender chest + purpur + elytra frame (already excluded from customItemFrames)
        ChunkTelemetry endCity = new ChunkTelemetry(
                80, -60, "minecraft:the_end",
                0, 1, 1, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                160, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                95, 115, 102
        );
        assertFalse(SuspiciousChunkAnalyzer.evaluate(endCity, Config.defaults()).flagged());
    }

    // =========================================================================
    // 3. TRUE-POSITIVE DETECTION: REAL BASES, STASHES, GRINDERS & FARMS
    // =========================================================================

    @Test
    void undergroundShulkerStashFlagsHighConfidence() {
        ChunkTelemetry stash = new ChunkTelemetry(
                -120, 340, "minecraft:overworld",
                3, 1, 6, 0, 0,
                1, 0, 1, 0, 0,
                0, 0, 0, 0, 0,
                6, 0, 0, 4, 18, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -48, -42, -45
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(stash, Config.defaults());
        assertTrue(res.flagged());
        assertEquals(Confidence.HIGH, res.confidence());
        assertTrue(res.score() >= 72.0, "Shulker stash should reach HIGH confidence: " + res.score());
        assertTrue(res.indicatorCount() >= 2);
    }

    @Test
    void establishedUndergroundPlayerBaseFlagsHighConfidence() {
        ChunkTelemetry base = new ChunkTelemetry(
                45, -88, "minecraft:overworld",
                1, 1, 8, 2, 0,
                3, 1, 2, 1, 1,
                4, 2, 0, 0, 0,
                12, 1, 1, 16, 42, 24,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 2, 0, 0, 0,
                0, 4, 0.0,
                false, false,
                -36, -24, -30
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(base, Config.defaults());
        assertTrue(res.flagged());
        assertEquals(Confidence.HIGH, res.confidence());
        assertTrue(res.score() >= 85.0);
        assertTrue(res.indicatorCount() >= 4);
    }

    @Test
    void camouflagedDiveShaftWithRotatedDeepslateAndWorkstationsFlags() {
        ChunkTelemetry hiddenHole = new ChunkTelemetry(
                99, 101, "minecraft:overworld",
                0, 0, 2, 3, 0,
                2, 1, 1, 0, 0,
                0, 0, 0, 0, 0,
                9, 2, 0, 6, 16, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -52, -18, -46
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(hiddenHole, Config.defaults());
        assertTrue(res.flagged(), "Hidden dive-shaft base with rotated deepslate + storage/workstations must flag");
        assertTrue(res.score() >= 54.0);
    }

    @Test
    void modifiedMobSpawnerGrinderFlags() {
        // Spawner with 0 mossy cobble + 4 collection hoppers + chests + bubble column
        ChunkTelemetry grinder = new ChunkTelemetry(
                -15, -22, "minecraft:overworld",
                0, 0, 4, 4, 0,
                1, 0, 0, 0, 0,
                0, 0, 4, 1, 0,
                0, 0, 0, 4, 10, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -22, 12, -18
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(grinder, Config.defaults());
        assertTrue(res.flagged(), "Player-modified spawner grinder with hoppers + bubble elevator must flag");
        assertTrue(res.reasons().stream().anyMatch(r -> r.contains("Grinder") || r.contains("Hoppers")));
    }

    @Test
    void automatedObserverPistonCropFarmFlags() {
        ChunkTelemetry farm = new ChunkTelemetry(
                33, 34, "minecraft:overworld",
                0, 0, 3, 6, 0,
                0, 0, 0, 0, 0,
                8, 4, 0, 0, 12,
                0, 0, 0, 12, 30, 18,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 6, 0.0,
                false, false,
                -10, 4, -4
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(farm, Config.defaults());
        assertTrue(res.flagged(), "Automated observer/piston farm with hoppers & chests must flag");
        assertEquals(Confidence.HIGH, res.confidence());
    }

    @Test
    void undergroundVillagerTradingHallFlags() {
        ChunkTelemetry tradingHall = new ChunkTelemetry(
                -60, 12, "minecraft:overworld",
                0, 0, 2, 4, 0,
                2, 1, 6, 0, 0,
                2, 1, 0, 0, 0,
                4, 0, 0, 14, 36, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 4, 0, 0,
                0, 0, 0.0,
                false, false,
                -30, -24, -27
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(tradingHall, Config.defaults());
        assertTrue(res.flagged(), "Underground villager trading hall must flag");
        assertEquals(Confidence.HIGH, res.confidence());
    }

    @Test
    void skyBuildWithStorageAndWorkstationsFlags() {
        ChunkTelemetry skyBase = new ChunkTelemetry(
                200, -200, "minecraft:overworld",
                0, 0, 3, 0, 0,
                2, 1, 1, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 32,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                48, 0, 0.0,
                false, false,
                210, 224, 215
        );
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(skyBase, Config.defaults());
        assertTrue(res.flagged(), "Sky base with unnatural build palette + storage/workstations must flag");
    }

    // =========================================================================
    // 4. SENSITIVITY PRESETS, CUSTOM THRESHOLDS & CLUSTER DEDUPLICATION
    // =========================================================================

    @Test
    void sensitivityPresetsControlBorderlineChunksPredictably() {
        // Moderate 2-indicator camp (~36 pts): 2 chests + 1 crafting/furnace/bed + 5 rotated deepslate
        ChunkTelemetry smallCamp = new ChunkTelemetry(
                14, 14, "minecraft:overworld",
                0, 0, 2, 0, 0,
                1, 1, 1, 0, 0,
                0, 0, 0, 0, 0,
                5, 0, 0, 4, 12, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -25, -20, -22
        );

        EvaluationResult strictRes = SuspiciousChunkAnalyzer.evaluate(smallCamp, Config.withSensitivity(Sensitivity.LOW_STRICT));
        EvaluationResult sensitiveRes = SuspiciousChunkAnalyzer.evaluate(smallCamp, Config.withSensitivity(Sensitivity.HIGH_SENSITIVE));

        assertFalse(strictRes.flagged(), "LOW_STRICT (>=54% & >=3 indicators) must reject small 2-signal camp");
        assertTrue(sensitiveRes.flagged(), "HIGH_SENSITIVE (>=30% & >=2 indicators) should catch small 2-signal camp");
    }

    @Test
    void customThresholdIsStrictlyHonored() {
        ChunkTelemetry stash = new ChunkTelemetry(
                5, 5, "minecraft:overworld",
                1, 0, 3, 0, 0,
                1, 0, 1, 0, 0,
                0, 0, 0, 0, 0,
                6, 0, 0, 4, 12, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0.0,
                false, false,
                -40, -35, -38
        );
        Config lowCustom = new Config(
                Sensitivity.CUSTOM, 40.0,
                true, true, true, true, true, true, true, true, true, true,
                1.0, 1.0, 1.0, 1.0, 1.0
        );
        Config veryHighCustom = new Config(
                Sensitivity.CUSTOM, 88.0,
                true, true, true, true, true, true, true, true, true, true,
                1.0, 1.0, 1.0, 1.0, 1.0
        );
        EvaluationResult r1 = SuspiciousChunkAnalyzer.evaluate(stash, lowCustom);
        EvaluationResult r2 = SuspiciousChunkAnalyzer.evaluate(stash, veryHighCustom);
        assertTrue(r1.score() >= 40.0 && r1.score() < 88.0, "Expected mid-high score, got " + r1.score());
        assertTrue(r1.flagged());
        assertFalse(r2.flagged(), "Must strictly respect custom threshold of 88.0");
    }

    @Test
    void spatialClusterDeduplicationMergesMultiChunkBaseAndSeparatesDistantOutpost() {
        // 4-chunk L-shaped base around (10, 10) + 1 separate outpost at (20, 20)
        List<ChunkInput> inputs = List.of(
                new ChunkInput(10, 10, "minecraft:overworld", 62.0),
                new ChunkInput(11, 10, "minecraft:overworld", 91.0), // highest score -> primary anchor
                new ChunkInput(11, 11, "minecraft:overworld", 55.0),
                new ChunkInput(12, 11, "minecraft:overworld", 48.0),
                new ChunkInput(20, 20, "minecraft:overworld", 78.0),
                new ChunkInput(11, 10, "minecraft:the_nether", 80.0) // different dimension -> separate cluster
        );

        Map<String, ClusterAssignment> map = SuspiciousChunkAnalyzer.deduplicateClusters(inputs, 1);
        assertEquals(6, map.size());

        ClusterAssignment mainPrimary = map.get("minecraft:overworld:11:10");
        assertNotNull(mainPrimary);
        assertTrue(mainPrimary.primary(), "(11,10) has highest score (91%) and must be cluster primary");
        assertEquals(4, mainPrimary.clusterSize(), "All 4 connected base chunks must merge into 1 cluster");

        assertFalse(map.get("minecraft:overworld:10:10").primary());
        assertEquals(mainPrimary.clusterId(), map.get("minecraft:overworld:12:11").clusterId());

        ClusterAssignment outpost = map.get("minecraft:overworld:20:20");
        assertTrue(outpost.primary());
        assertEquals(1, outpost.clusterSize());
        assertTrue(outpost.clusterId() != mainPrimary.clusterId());

        ClusterAssignment nether = map.get("minecraft:the_nether:11:10");
        assertTrue(nether.primary());
        assertEquals(1, nether.clusterSize());
    }
}
