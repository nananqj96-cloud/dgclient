package com.dg.client.core.detection;

import com.dg.client.core.detection.SuspiciousChunkAnalyzer.ChunkTelemetry;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.ClusterAssignment;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.Confidence;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.Config;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.EvaluationResult;
import com.dg.client.core.detection.SuspiciousChunkAnalyzer.Sensitivity;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
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
 *     <li>Suppression of all major vanilla natural structures (1.21 Trial Chambers, Mineshafts,
 *         Dungeons, Villages, Ancient Cities, Ruined Portals, End Cities, Geodes)</li>
 *     <li>Multi-indicator detection & confidence grading across realistic stashes, underground
 *         bases, camouflaged dive shafts, grinders, automated farms, and sky bases</li>
 *     <li>Sensitivity presets, custom threshold adherence, and spatial cluster deduplication</li>
 * </ul>
 */
class SuspiciousChunkAnalyzerTest {

    private static Config configFor(Sensitivity sensitivity) {
        return new Config(
                sensitivity,
                sensitivity.getDefaultThreshold(),
                true, true, true, true, true, true, true, true, true, true,
                1.0, 1.0, 1.0, 1.0, 1.0
        );
    }

    // =========================================================================
    // 1. FALSE-POSITIVE REJECTION: ISOLATED BLOCKS & ORDINARY TERRAIN EDITS
    // =========================================================================

    @Test
    void emptyChunkIsNeverFlagged() {
        ChunkTelemetry t = ChunkTelemetry.builder(0, 0, "minecraft:overworld").build();
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(t, Config.defaults());
        assertFalse(res.flagged());
        assertEquals(0.0, res.score());
    }

    @Test
    void isolatedSingleCraftingTableIsNeverFlagged() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(1, 2, "minecraft:overworld");
        b.craftingTableCount = 1;
        b.recordAnomalyY(68);
        ChunkTelemetry t = b.build();

        for (Sensitivity s : List.of(Sensitivity.LOW_STRICT, Sensitivity.BALANCED, Sensitivity.HIGH_SENSITIVE)) {
            EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(t, configFor(s));
            assertFalse(res.flagged(), "Isolated crafting table must not flag under " + s);
        }
    }

    @Test
    void ordinaryCaveMiningTorchesAndSingleChestDoNotFlag() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(3, -4, "minecraft:overworld");
        b.playerContainerCount = 1;
        b.torchLanternCount = 14;
        b.genericBuildBlockCount = 18;
        b.undergroundLightSamples = 12;
        b.recordAnomalyY(14);
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "Cave mining torches + 1 chest should not trigger base alert");
    }

    @Test
    void isolatedPortalWithoutBaseIndicatorsDoesNotFlagAlone() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(-10, 5, "minecraft:overworld");
        b.activePortalBlockCount = 6;
        b.obsidianCount = 10;
        b.recordAnomalyY(65);
        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "Single standalone portal without base activity must not flag as a base");
    }

    // =========================================================================
    // 2. FALSE-POSITIVE REJECTION: VANILLA NATURAL STRUCTURES
    // =========================================================================

    @Test
    void vanillaTrialChamberIn121IsSuppressed() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(12, 8, "minecraft:overworld");
        b.tuffBricksCount = 180;
        b.trialCopperAndVaultCount = 48;
        b.structureContainerCount = 6;
        b.playerContainerCount = 2;
        b.dropperDispenserCount = 4;
        b.hopperCount = 1;
        b.torchLanternCount = 16;
        b.undergroundLightSamples = 32;
        b.recordAnomalyY(-22);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "1.21 Trial Chamber must not produce false positive (score=" + res.score() + ")");
        assertEquals("TrialChamber", res.naturalStructureFiltered());
    }

    @Test
    void vanillaAbandonedMineshaftIsNotFlagged() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(-5, 19, "minecraft:overworld");
        b.railCount = 14;
        b.cobwebCount = 18;
        b.chainCount = 4;
        b.genericBuildBlockCount = 24;
        b.torchLanternCount = 8;
        b.vehicleCount = 1;
        b.straightTunnelCount = 1;
        b.recordAnomalyY(-12);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "Vanilla Mineshaft must not be flagged");
        assertEquals("Mineshaft", res.naturalStructureFiltered());
    }

    @Test
    void vanillaDungeonSpawnerWithMossyCobbleIsNotFlagged() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(7, -3, "minecraft:overworld");
        b.spawnerCount = 1;
        b.mossyCobbleCount = 32;
        b.structureContainerCount = 2;
        b.playerContainerCount = 1;
        b.recordAnomalyY(12);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "Natural mossy-cobble dungeon spawner must not flag");
        assertEquals("Dungeon", res.naturalStructureFiltered());
    }

    @Test
    void vanillaSurfaceVillageIsSuppressed() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(20, 20, "minecraft:overworld");
        b.dirtPathCount = 28;
        b.hayBellCount = 6;
        b.noviceVillagerCount = 3;
        b.surfaceBedCount = 4;
        b.craftingTableCount = 2;
        b.furnaceCount = 2;
        b.playerContainerCount = 3;
        b.genericBuildBlockCount = 80;
        b.recordAnomalyY(68);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "Natural surface village must be suppressed (score=" + res.score() + ")");
        assertEquals("Village", res.naturalStructureFiltered());
    }

    @Test
    void vanillaAncientCityRedstoneRoomIsSuppressed() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(-40, 15, "minecraft:overworld");
        b.sculkCount = 95;
        b.deepslateTilesBricksCount = 120;
        b.structureContainerCount = 3;
        b.redstoneComponentCount = 14;
        b.repeaterComparatorCount = 3;
        b.pistonCount = 2;
        b.torchLanternCount = 18;
        b.recordAnomalyY(-46);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertFalse(res.flagged(), "Ancient City redstone/sculk room must be suppressed");
        assertEquals("AncientCity", res.naturalStructureFiltered());
    }

    @Test
    void vanillaRuinedPortalAndEndCityShipAreSuppressed() {
        ChunkTelemetry.Builder rp = ChunkTelemetry.builder(8, 9, "minecraft:overworld");
        rp.obsidianCount = 8;
        rp.cryingObsidianCount = 3;
        rp.netherrackMagmaCount = 24;
        rp.playerContainerCount = 1;
        rp.recordAnomalyY(65);
        EvaluationResult rpRes = SuspiciousChunkAnalyzer.evaluate(rp.build(), Config.defaults());
        assertFalse(rpRes.flagged());
        assertEquals("RuinedPortal", rpRes.naturalStructureFiltered());

        ChunkTelemetry.Builder ec = ChunkTelemetry.builder(80, -60, "minecraft:the_end");
        ec.purpurEndCityBlockCount = 160;
        ec.enderChestCount = 1;
        ec.playerContainerCount = 1;
        ec.shulkerCount = 2;
        ec.recordAnomalyY(102);
        EvaluationResult ecRes = SuspiciousChunkAnalyzer.evaluate(ec.build(), Config.defaults());
        assertFalse(ecRes.flagged());
        assertEquals("EndCity", ecRes.naturalStructureFiltered());
    }

    // =========================================================================
    // 3. TRUE-POSITIVE DETECTION: REAL BASES, STASHES, GRINDERS & FARMS
    // =========================================================================

    @Test
    void undergroundShulkerStashFlagsHighConfidence() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(-120, 340, "minecraft:overworld");
        b.shulkerCount = 3;
        b.enderChestCount = 1;
        b.playerContainerCount = 6;
        b.craftingTableCount = 1;
        b.undergroundBedCount = 1;
        b.rotatedDeepslateCount = 6;
        b.recordAnomalyY(-45);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged());
        assertEquals(Confidence.HIGH, res.confidence());
        assertTrue(res.score() >= 72.0, "Shulker stash should reach HIGH confidence: " + res.score());
        assertTrue(res.indicatorCount() >= 2);
    }

    @Test
    void establishedUndergroundPlayerBaseFlagsHighConfidence() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(45, -88, "minecraft:overworld");
        b.shulkerCount = 1;
        b.enderChestCount = 1;
        b.playerContainerCount = 8;
        b.hopperCount = 2;
        b.craftingTableCount = 1;
        b.furnaceCount = 4;
        b.enchantingTableCount = 1;
        b.bookshelfCount = 15;
        b.anvilCount = 1;
        b.undergroundBedCount = 2;
        b.observerCount = 3;
        b.pistonCount = 3;
        b.rotatedDeepslateCount = 12;
        b.playerItemFrameCount = 4;
        b.exoticBuildBlockCount = 32;
        b.recordAnomalyY(-30);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged());
        assertEquals(Confidence.HIGH, res.confidence());
        assertTrue(res.score() >= 85.0);
        assertTrue(res.indicatorCount() >= 4);
    }

    @Test
    void camouflagedDiveShaftWithRotatedDeepslateAndWorkstationsFlags() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(99, 101, "minecraft:overworld");
        b.playerContainerCount = 3;
        b.hopperCount = 2;
        b.craftingTableCount = 1;
        b.furnaceCount = 2;
        b.undergroundBedCount = 1;
        b.rotatedDeepslateCount = 9;
        b.enclosedFillPillarCount = 2;
        b.recordAnomalyY(-46);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged(), "Hidden dive-shaft base with rotated deepslate + storage/workstations must flag");
        assertTrue(res.score() >= 54.0);
    }

    @Test
    void modifiedMobSpawnerGrinderFlags() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(-15, -22, "minecraft:overworld");
        b.spawnerCount = 1;
        b.mossyCobbleCount = 0;
        b.hopperCount = 4;
        b.playerContainerCount = 4;
        b.soulSandBubbleCount = 4;
        b.craftingTableCount = 1;
        b.recordAnomalyY(-18);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged(), "Player-modified spawner grinder with hoppers + bubble elevator must flag");
    }

    @Test
    void automatedObserverPistonCropFarmFlags() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(33, 34, "minecraft:overworld");
        b.playerContainerCount = 3;
        b.hopperCount = 6;
        b.observerCount = 8;
        b.pistonCount = 8;
        b.farmColumnScore = 12;
        b.exoticBuildBlockCount = 24;
        b.liveChunkDeltas = 18;
        b.recordAnomalyY(-4);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged(), "Automated observer/piston farm with hoppers & chests must flag");
        assertEquals(Confidence.HIGH, res.confidence());
    }

    @Test
    void undergroundVillagerTradingHallFlags() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(-60, 12, "minecraft:overworld");
        b.tradedOrUndergroundVillagerCount = 4;
        b.playerContainerCount = 4;
        b.hopperCount = 2;
        b.craftingTableCount = 1;
        b.anvilCount = 1;
        b.rotatedDeepslateCount = 6;
        b.recordAnomalyY(-27);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged(), "Underground villager trading hall must flag");
        assertTrue(res.score() >= 54.0);
    }

    @Test
    void skyBuildWithStorageAndWorkstationsFlags() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(200, -200, "minecraft:overworld");
        b.skyOrRoofBuildCount = 48;
        b.exoticBuildBlockCount = 36;
        b.playerContainerCount = 4;
        b.craftingTableCount = 1;
        b.furnaceCount = 2;
        b.anvilCount = 1;
        b.recordAnomalyY(215);

        EvaluationResult res = SuspiciousChunkAnalyzer.evaluate(b.build(), Config.defaults());
        assertTrue(res.flagged(), "Sky base with unnatural build palette + storage/workstations must flag");
    }

    // =========================================================================
    // 4. SENSITIVITY PRESETS, CUSTOM THRESHOLDS & CLUSTER DEDUPLICATION
    // =========================================================================

    @Test
    void sensitivityPresetsControlBorderlineChunksPredictably() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(14, 14, "minecraft:overworld");
        b.playerContainerCount = 2;
        b.rotatedDeepslateCount = 6;
        b.recordAnomalyY(-22);
        ChunkTelemetry smallCamp = b.build();

        EvaluationResult strictRes = SuspiciousChunkAnalyzer.evaluate(smallCamp, configFor(Sensitivity.LOW_STRICT));
        EvaluationResult sensitiveRes = SuspiciousChunkAnalyzer.evaluate(smallCamp, configFor(Sensitivity.HIGH_SENSITIVE));

        assertFalse(strictRes.flagged(), "LOW_STRICT (>=54% & >=3 indicators) must reject small 2-signal camp");
        assertTrue(sensitiveRes.flagged(), "HIGH_SENSITIVE (>=30% & >=2 indicators) should catch small 2-signal camp");
    }

    @Test
    void customThresholdIsStrictlyHonored() {
        ChunkTelemetry.Builder b = ChunkTelemetry.builder(5, 5, "minecraft:overworld");
        b.shulkerCount = 1;
        b.playerContainerCount = 3;
        b.rotatedDeepslateCount = 6;
        b.recordAnomalyY(-38);
        ChunkTelemetry stash = b.build();

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
        Map<Long, Double> scores = new LinkedHashMap<>();
        long k10_10 = SuspiciousChunkAnalyzer.packChunk(10, 10);
        long k11_10 = SuspiciousChunkAnalyzer.packChunk(11, 10);
        long k11_11 = SuspiciousChunkAnalyzer.packChunk(11, 11);
        long k12_11 = SuspiciousChunkAnalyzer.packChunk(12, 11);
        long k20_20 = SuspiciousChunkAnalyzer.packChunk(20, 20);

        scores.put(k10_10, 62.0);
        scores.put(k11_10, 91.0); // highest score -> primary anchor
        scores.put(k11_11, 55.0);
        scores.put(k12_11, 48.0);
        scores.put(k20_20, 78.0);

        Map<Long, ClusterAssignment> map = SuspiciousChunkAnalyzer.deduplicateClusters(scores, 1);
        assertEquals(5, map.size());

        ClusterAssignment mainPrimary = map.get(k11_10);
        assertNotNull(mainPrimary);
        assertTrue(mainPrimary.primary(), "(11,10) has highest score (91%) and must be cluster primary");
        assertEquals(4, mainPrimary.clusterSize(), "All 4 connected base chunks must merge into 1 cluster");

        assertFalse(map.get(k10_10).primary());
        assertEquals(mainPrimary.clusterId(), map.get(k12_11).clusterId());

        ClusterAssignment outpost = map.get(k20_20);
        assertTrue(outpost.primary());
        assertEquals(1, outpost.clusterSize());
        assertTrue(outpost.clusterId() != mainPrimary.clusterId());
    }
}
