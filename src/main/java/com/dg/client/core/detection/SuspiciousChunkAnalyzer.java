package com.dg.client.core.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multi-indicator, false-positive-resistant Suspicious Chunk & Base Detection engine.
 *
 * <p>Requires multiple independent categories of player activity or unambiguous stash
 * clusters before flagging a chunk, while explicitly discounting vanilla structures
 * (1.21 Trial Chambers, Mineshafts, Dungeons, Surface Villages, Ancient Cities,
 * Ruined Portals, End Cities, and Natural Geodes) and isolated player blocks.
 */
public final class SuspiciousChunkAnalyzer {
    public enum Sensitivity {
        LOW_STRICT(54.0, 3),
        BALANCED(42.0, 2),
        HIGH_SENSITIVE(30.0, 2),
        CUSTOM(42.0, 2);

        private final double defaultThreshold;
        private final int minIndicators;

        Sensitivity(double defaultThreshold, int minIndicators) {
            this.defaultThreshold = defaultThreshold;
            this.minIndicators = minIndicators;
        }

        public double getDefaultThreshold() {
            return defaultThreshold;
        }

        public int getMinIndicators() {
            return minIndicators;
        }
    }

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    public record Config(
            Sensitivity sensitivity,
            double customMinScore,
            boolean checkStorageAndUtilities,
            boolean checkRedstoneAutomation,
            boolean checkConcealmentAndTunnels,
            boolean checkManmadeBuilds,
            boolean checkLightAnomalies,
            boolean checkChunkDeltas,
            boolean checkNewOldChunkTells,
            boolean checkEntities,
            boolean checkPortalsAndHighways,
            boolean checkYZeroLeaks,
            double storageWeight,
            double redstoneWeight,
            double tunnelWeight,
            double manmadeWeight,
            double highwayWeight
    ) {
        public static Config defaults() {
            return new Config(
                    Sensitivity.BALANCED,
                    42.0,
                    true, true, true, true, true, true, true, true, true, true,
                    1.0, 1.0, 1.0, 1.0, 1.0
            );
        }

        public double effectiveMinScore() {
            return sensitivity == Sensitivity.CUSTOM ? customMinScore : Math.max(18.0, customMinScore);
        }
    }

    /**
     * Per-chunk structural & entity telemetry captured from a 16x16 chunk column.
     */
    public record ChunkTelemetry(
            int chunkX,
            int chunkZ,
            String dimensionId,
            // High-certainty & standard storage
            int shulkerCount,
            int enderChestCount,
            int playerContainerCount,
            int structureContainerCount,
            int hopperCount,
            int shelfAndBookStorageCount,
            // Workstations & living quarters
            int furnaceCount,
            int enchantingTableCount,
            int bookshelfCount,
            int anvilCount,
            int brewingStandCount,
            int beaconCount,
            int craftingTableCount,
            int undergroundBedCount,
            int surfaceBedCount,
            int writtenSignCount,
            // Redstone & farm automation
            int observerCount,
            int pistonCount,
            int repeaterComparatorCount,
            int dropperDispenserCount,
            int redstoneComponentCount,
            int poweredRailCount,
            int soulSandBubbleCount,
            int farmColumnScore,
            // Concealment, geometry & construction
            int rotatedDeepslateCount,
            int enclosedFillPillarCount,
            int straightTunnelCount,
            int exoticBuildBlockCount,
            int genericBuildBlockCount,
            int torchLanternCount,
            int activePortalBlockCount,
            int obsidianCount,
            int cryingObsidianCount,
            int iceHighwayCount,
            int skyOrRoofBuildCount,
            // Natural structure context counters
            int tuffBricksCount,
            int trialCopperAndVaultCount,
            int railCount,
            int cobwebCount,
            int chainCount,
            int spawnerCount,
            int mossyCobbleCount,
            int sculkCount,
            int deepslateTilesBricksCount,
            int dirtPathCount,
            int hayBellCount,
            int noviceVillagerCount,
            int amethystClusterCount,
            int geodeShellCount,
            int purpurEndCityBlockCount,
            int netherrackMagmaCount,
            // Entities, light & live deltas
            int playerItemFrameCount,
            int armorStandCount,
            int tradedOrUndergroundVillagerCount,
            int tamedOrNamedEntityCount,
            int vehicleCount,
            int undergroundLightSamples,
            int liveChunkDeltas,
            int borderFlowingFluids,
            boolean pre118OldChunkSignature,
            double yZeroBoost,
            int minAnomalyY,
            int maxAnomalyY,
            int medianAnomalyY
    ) {
        public static Builder builder(int chunkX, int chunkZ, String dimensionId) {
            return new Builder(chunkX, chunkZ, dimensionId);
        }

        public static final class Builder {
            private final int chunkX;
            private final int chunkZ;
            private final String dimensionId;
            public int shulkerCount;
            public int enderChestCount;
            public int playerContainerCount;
            public int structureContainerCount;
            public int hopperCount;
            public int shelfAndBookStorageCount;
            public int furnaceCount;
            public int enchantingTableCount;
            public int bookshelfCount;
            public int anvilCount;
            public int brewingStandCount;
            public int beaconCount;
            public int craftingTableCount;
            public int undergroundBedCount;
            public int surfaceBedCount;
            public int writtenSignCount;
            public int observerCount;
            public int pistonCount;
            public int repeaterComparatorCount;
            public int dropperDispenserCount;
            public int redstoneComponentCount;
            public int poweredRailCount;
            public int soulSandBubbleCount;
            public int farmColumnScore;
            public int rotatedDeepslateCount;
            public int enclosedFillPillarCount;
            public int straightTunnelCount;
            public int exoticBuildBlockCount;
            public int genericBuildBlockCount;
            public int torchLanternCount;
            public int activePortalBlockCount;
            public int obsidianCount;
            public int cryingObsidianCount;
            public int iceHighwayCount;
            public int skyOrRoofBuildCount;
            public int tuffBricksCount;
            public int trialCopperAndVaultCount;
            public int railCount;
            public int cobwebCount;
            public int chainCount;
            public int spawnerCount;
            public int mossyCobbleCount;
            public int sculkCount;
            public int deepslateTilesBricksCount;
            public int dirtPathCount;
            public int hayBellCount;
            public int noviceVillagerCount;
            public int amethystClusterCount;
            public int geodeShellCount;
            public int purpurEndCityBlockCount;
            public int netherrackMagmaCount;
            public int playerItemFrameCount;
            public int armorStandCount;
            public int tradedOrUndergroundVillagerCount;
            public int tamedOrNamedEntityCount;
            public int vehicleCount;
            public int undergroundLightSamples;
            public int liveChunkDeltas;
            public int borderFlowingFluids;
            public boolean pre118OldChunkSignature;
            public double yZeroBoost;
            public int minAnomalyY = Integer.MAX_VALUE;
            public int maxAnomalyY = Integer.MIN_VALUE;
            public int anomalyYSum = 0;
            public int anomalyYCount = 0;

            public Builder(int chunkX, int chunkZ, String dimensionId) {
                this.chunkX = chunkX;
                this.chunkZ = chunkZ;
                this.dimensionId = dimensionId == null ? "minecraft:overworld" : dimensionId;
            }

            public void recordAnomalyY(int wy) {
                if (wy < minAnomalyY) minAnomalyY = wy;
                if (wy > maxAnomalyY) maxAnomalyY = wy;
                anomalyYSum += wy;
                anomalyYCount++;
            }

            public ChunkTelemetry build() {
                int minY = minAnomalyY == Integer.MAX_VALUE ? 32 : minAnomalyY;
                int maxY = maxAnomalyY == Integer.MIN_VALUE ? 48 : maxAnomalyY;
                int medY = anomalyYCount > 0 ? Math.round((float) anomalyYSum / anomalyYCount) : (minY + maxY) / 2;
                return new ChunkTelemetry(
                        chunkX, chunkZ, dimensionId,
                        shulkerCount, enderChestCount, playerContainerCount, structureContainerCount,
                        hopperCount, shelfAndBookStorageCount,
                        furnaceCount, enchantingTableCount, bookshelfCount, anvilCount,
                        brewingStandCount, beaconCount, craftingTableCount, undergroundBedCount,
                        surfaceBedCount, writtenSignCount,
                        observerCount, pistonCount, repeaterComparatorCount, dropperDispenserCount,
                        redstoneComponentCount, poweredRailCount, soulSandBubbleCount, farmColumnScore,
                        rotatedDeepslateCount, enclosedFillPillarCount, straightTunnelCount,
                        exoticBuildBlockCount, genericBuildBlockCount, torchLanternCount,
                        activePortalBlockCount, obsidianCount, cryingObsidianCount, iceHighwayCount,
                        skyOrRoofBuildCount,
                        tuffBricksCount, trialCopperAndVaultCount, railCount, cobwebCount, chainCount,
                        spawnerCount, mossyCobbleCount, sculkCount, deepslateTilesBricksCount,
                        dirtPathCount, hayBellCount, noviceVillagerCount, amethystClusterCount,
                        geodeShellCount, purpurEndCityBlockCount, netherrackMagmaCount,
                        playerItemFrameCount, armorStandCount, tradedOrUndergroundVillagerCount,
                        tamedOrNamedEntityCount, vehicleCount,
                        undergroundLightSamples, liveChunkDeltas, borderFlowingFluids,
                        pre118OldChunkSignature, yZeroBoost,
                        minY, maxY, medY
                );
            }
        }
    }

    public record EvaluationResult(
            boolean flagged,
            double score,
            Confidence confidence,
            int indicatorCount,
            List<String> reasons,
            String naturalStructureFiltered,
            int minY,
            int maxY,
            int medianY
    ) {
    }

    public record ClusterAssignment(
            int clusterId,
            boolean primary,
            int clusterSize,
            double corroboratedScore,
            Confidence confidence
    ) {
    }

    private SuspiciousChunkAnalyzer() {
    }

    /**
     * Evaluates a single chunk's telemetry against natural-structure filters and multi-indicator rules.
     */
    public static EvaluationResult evaluate(ChunkTelemetry t, Config cfg) {
        boolean isOverworld = t.dimensionId.contains("overworld");
        boolean isNether = t.dimensionId.contains("the_nether");
        boolean isEnd = t.dimensionId.contains("the_end");

        // 1. Identify vanilla natural structure contexts so natural loot/decor never triggers false positives
        boolean isTrialChamber = isOverworld && t.tuffBricksCount >= 8 && t.trialCopperAndVaultCount >= 4;
        boolean isMineshaft = isOverworld && (
                (t.railCount >= 4 && t.cobwebCount >= 2)
                        || (t.cobwebCount >= 5 && t.genericBuildBlockCount >= 6)
                        || (t.railCount >= 6 && t.chainCount >= 2)
        );
        boolean isNaturalDungeon = isOverworld && t.spawnerCount >= 1 && t.mossyCobbleCount >= 6;
        boolean isAncientCity = isOverworld && t.sculkCount >= 10 && t.deepslateTilesBricksCount >= 12;
        boolean isSurfaceVillage = isOverworld && (
                (t.dirtPathCount >= 8 && (t.surfaceBedCount >= 1 || t.noviceVillagerCount >= 1 || t.hayBellCount >= 1))
                        || (t.noviceVillagerCount >= 2 && t.surfaceBedCount >= 1)
                        || (t.hayBellCount >= 2 && t.dirtPathCount >= 4)
        );
        boolean isRuinedPortal = t.activePortalBlockCount == 0
                && (t.cryingObsidianCount >= 1 || t.obsidianCount >= 4)
                && t.netherrackMagmaCount >= 6;
        boolean isEndCity = isEnd && t.purpurEndCityBlockCount >= 12;

        String naturalTag = isTrialChamber ? "TrialChamber"
                : isAncientCity ? "AncientCity"
                : isMineshaft ? "Mineshaft"
                : isNaturalDungeon ? "Dungeon"
                : isSurfaceVillage ? "Village"
                : isRuinedPortal ? "RuinedPortal"
                : isEndCity ? "EndCity"
                : null;

        double score = 0.0;
        int distinctIndicators = 0;
        boolean hasUnmistakableStashCluster = false;
        List<String> reasons = new ArrayList<>();

        // --- CATEGORY 1: High-Certainty & Clustered Player Storage ---
        if (cfg.checkStorageAndUtilities()) {
            int effectiveShulkers = isEndCity ? Math.max(0, t.shulkerCount - 2) : t.shulkerCount;
            int effectiveEnderChests = isEndCity ? Math.max(0, t.enderChestCount - 1) : t.enderChestCount;

            int naturalContainerAllowance = 0;
            if (isTrialChamber) naturalContainerAllowance += 10;
            if (isAncientCity) naturalContainerAllowance += 6;
            if (isSurfaceVillage) naturalContainerAllowance += 4;
            if (isMineshaft || isNaturalDungeon || isRuinedPortal) naturalContainerAllowance += 2;

            int effectiveContainers = Math.max(0, t.playerContainerCount - naturalContainerAllowance);
            int effectiveHoppers = isAncientCity ? Math.max(0, t.hopperCount - 2) : t.hopperCount;

            if (effectiveShulkers > 0 || effectiveEnderChests > 0) {
                double pts = (effectiveShulkers * 18.0 + effectiveEnderChests * 14.0) * cfg.storageWeight();
                score += Math.min(46.0, pts);
                distinctIndicators++;
                if (effectiveShulkers >= 2 || (effectiveShulkers >= 1 && (effectiveEnderChests >= 1 || effectiveContainers >= 2))) {
                    hasUnmistakableStashCluster = true;
                }
                if (effectiveShulkers > 0 && effectiveEnderChests > 0) {
                    reasons.add("Shulker x" + effectiveShulkers + " + EChest");
                } else if (effectiveShulkers > 0) {
                    reasons.add("Shulker x" + effectiveShulkers);
                } else {
                    reasons.add("EnderChest x" + effectiveEnderChests);
                }
            }

            // Require >= 2 non-structure containers to count as a storage indicator;
            // >= 6 non-structure containers is a dedicated storage room / stash.
            if (effectiveContainers >= 2) {
                double pts = (effectiveContainers * 4.8 + Math.sqrt(effectiveContainers) * 5.0) * cfg.storageWeight();
                score += Math.min(42.0, pts);
                distinctIndicators++;
                if (effectiveContainers >= 6) {
                    hasUnmistakableStashCluster = true;
                }
                reasons.add("Storage x" + effectiveContainers);
            } else if (effectiveContainers == 1 && effectiveHoppers >= 1) {
                score += 10.0 * cfg.storageWeight();
                distinctIndicators++;
                reasons.add("Hopper Chest");
            }

            if (effectiveHoppers >= 2) {
                double pts = Math.min(24.0, (effectiveHoppers * 4.5) * cfg.storageWeight());
                score += pts;
                distinctIndicators++;
                reasons.add("Hoppers x" + effectiveHoppers);
            }

            if (t.shelfAndBookStorageCount >= 2 && !isAncientCity && !isSurfaceVillage) {
                score += Math.min(14.0, t.shelfAndBookStorageCount * 3.5 * cfg.storageWeight());
                distinctIndicators++;
                reasons.add("Book/Shelves x" + t.shelfAndBookStorageCount);
            }
        }

        // --- CATEGORY 2: Player Base Workstations & Living Quarters ---
        if (cfg.checkStorageAndUtilities()) {
            int stationVariety = 0;
            double stationPts = 0.0;
            List<String> stationLabels = new ArrayList<>();

            if (t.beaconCount > 0) {
                stationVariety += 2;
                stationPts += 26.0;
                stationLabels.add("Beacon");
            }
            if (t.enchantingTableCount > 0) {
                stationVariety += (t.bookshelfCount >= 6 ? 2 : 1);
                stationPts += t.bookshelfCount >= 6 ? 24.0 : 15.0;
                stationLabels.add(t.bookshelfCount >= 6 ? "Enchant Setup" : "Enchant Table");
            }
            if (t.anvilCount > 0 && !isSurfaceVillage) {
                stationVariety++;
                stationPts += Math.min(14.0, t.anvilCount * 8.0);
                stationLabels.add("Anvil");
            }
            if (t.undergroundBedCount > 0) {
                stationVariety++;
                stationPts += Math.min(18.0, 11.0 + (t.undergroundBedCount - 1) * 4.0);
                stationLabels.add("Base Bed");
            }
            int effFurnaces = isSurfaceVillage ? Math.max(0, t.furnaceCount - 2) : t.furnaceCount;
            if (effFurnaces >= 2) {
                stationVariety++;
                stationPts += Math.min(18.0, effFurnaces * 3.8);
                stationLabels.add("Smeltery x" + effFurnaces);
            }
            int effBrewing = isSurfaceVillage ? Math.max(0, t.brewingStandCount - 1) : t.brewingStandCount;
            if (effBrewing > 0) {
                stationVariety++;
                stationPts += Math.min(12.0, effBrewing * 6.5);
                stationLabels.add("Brewing");
            }
            if (t.writtenSignCount > 0) {
                stationVariety++;
                stationPts += Math.min(16.0, 9.0 + (t.writtenSignCount - 1) * 3.5);
                stationLabels.add("Written Sign");
            }
            if (t.craftingTableCount > 0 && !isSurfaceVillage && stationVariety >= 1) {
                // A lone crafting table is ignored; alongside other stations it reinforces a workshop
                stationPts += 4.0;
            }

            // Require real workstation commitment (variety >= 2 or high-tier setup like Enchant/Beacon/Smeltery array)
            // so a lone furnace or lone crafting table left in a cave never counts as a base indicator.
            if (stationVariety >= 2 || t.beaconCount > 0 || (t.enchantingTableCount > 0 && t.bookshelfCount >= 6) || effFurnaces >= 4) {
                score += Math.min(38.0, stationPts * cfg.storageWeight());
                distinctIndicators++;
                reasons.add(String.join("/", stationLabels.subList(0, Math.min(2, stationLabels.size()))));
            } else if (stationVariety == 1 && stationPts >= 9.0) {
                // Secondary corroborating points only if another major indicator is also present
                score += Math.min(11.0, stationPts * 0.75 * cfg.storageWeight());
                if (!stationLabels.isEmpty()) {
                    reasons.add(stationLabels.get(0));
                }
            }
        }

        // --- CATEGORY 3: Redstone Contraptions, Spawner Grinders & Farm Automation ---
        if (cfg.checkRedstoneAutomation()) {
            int effObservers = isAncientCity ? Math.max(0, t.observerCount - 1) : t.observerCount;
            int effPistons = (!isTrialChamber && !isAncientCity) ? t.pistonCount : 0;
            int effLogic = isAncientCity ? Math.max(0, t.repeaterComparatorCount - 4) : t.repeaterComparatorCount;
            int effDroppers = isTrialChamber ? 0 : t.dropperDispenserCount;

            boolean hasAutomation = (effObservers >= 1 && (effPistons >= 1 || effDroppers >= 1 || t.hopperCount >= 1))
                    || (effLogic >= 2 && (t.hopperCount >= 2 || effPistons >= 2 || effDroppers >= 1))
                    || (effPistons >= 4)
                    || (t.redstoneComponentCount >= 6 && !isAncientCity && !isTrialChamber);

            if (hasAutomation) {
                double pts = (effObservers * 5.0 + effPistons * 4.0 + effLogic * 3.5
                        + effDroppers * 3.5 + t.redstoneComponentCount * 1.5 + t.poweredRailCount * 1.5) * cfg.redstoneWeight();
                score += Math.min(36.0, Math.max(14.0, pts));
                distinctIndicators++;
                reasons.add("Redstone Contraption");
            }

            if (t.soulSandBubbleCount >= 3) {
                score += Math.min(20.0, (10.0 + t.soulSandBubbleCount * 1.4) * cfg.redstoneWeight());
                distinctIndicators++;
                reasons.add("Bubble Elevator");
            }

            if (t.farmColumnScore >= 2 && !isSurfaceVillage) {
                score += Math.min(22.0, t.farmColumnScore * 5.5 * cfg.redstoneWeight());
                distinctIndicators++;
                reasons.add("Player Farm");
            }

            // Modified Spawner Grinder (Spawner without natural dungeon moss, combined with collection/water/glass)
            if (t.spawnerCount >= 1 && !isNaturalDungeon && !isMineshaft && !isTrialChamber) {
                if (t.hopperCount >= 1 || t.soulSandBubbleCount >= 1 || t.exoticBuildBlockCount >= 4 || t.playerContainerCount >= 2) {
                    score += 26.0 * cfg.storageWeight();
                    distinctIndicators++;
                    reasons.add("Spawner Grinder");
                }
            }
        }

        // --- CATEGORY 4: Concealment, Dive Shafts & Unnatural Architecture ---
        if (cfg.checkConcealmentAndTunnels()) {
            if (t.rotatedDeepslateCount >= 3 && !isAncientCity) {
                double pts = Math.min(22.0, (8.0 + t.rotatedDeepslateCount * 2.2) * cfg.tunnelWeight());
                score += pts;
                distinctIndicators++;
                reasons.add("Rotated Deepslate x" + t.rotatedDeepslateCount);
            }
            if (t.enclosedFillPillarCount >= 1) {
                double pts = Math.min(20.0, (12.0 + (t.enclosedFillPillarCount - 1) * 5.0) * cfg.tunnelWeight());
                score += pts;
                distinctIndicators++;
                reasons.add("Sealed 1x1 Shaft");
            }
            if (t.straightTunnelCount >= 1 && !isMineshaft && !isAncientCity && !isTrialChamber) {
                // A bare tunnel alone is only a mining tunnel, so give moderate points that require another indicator
                double pts = Math.min(16.0, t.straightTunnelCount * 7.5 * cfg.tunnelWeight());
                score += pts;
                if (t.torchLanternCount >= 4 || t.rotatedDeepslateCount >= 2 || t.enclosedFillPillarCount >= 1) {
                    distinctIndicators++;
                }
                reasons.add("Excavated Corridor");
            }
        }

        if (cfg.checkManmadeBuilds()) {
            // Exotic player-only palettes (concrete, glazed terracotta, cherry/bamboo/crimson/warped in overworld,
            // stained/tinted glass, scaffolding, mineral blocks) never spawn in dungeons/mineshafts/villages.
            int effExotic = (isTrialChamber || isEndCity) ? 0 : t.exoticBuildBlockCount;
            if (effExotic >= 10) {
                double pts = Math.min(28.0, (8.0 + effExotic * 0.45) * cfg.manmadeWeight());
                score += pts;
                distinctIndicators++;
                reasons.add("Custom Build x" + effExotic);
            } else if (t.genericBuildBlockCount >= 36
                    && !isMineshaft && !isSurfaceVillage && !isNaturalDungeon && !isAncientCity && !isTrialChamber) {
                double pts = Math.min(20.0, (t.genericBuildBlockCount * 0.25) * cfg.manmadeWeight());
                score += pts;
                distinctIndicators++;
                reasons.add("Structure x" + t.genericBuildBlockCount);
            }

            if (t.skyOrRoofBuildCount >= 6) {
                score += Math.min(26.0, 14.0 + t.skyOrRoofBuildCount * 0.6);
                distinctIndicators++;
                reasons.add(isNether ? "Nether Roof Build" : "Sky Build");
            }
        }

        // --- CATEGORY 5: Entities, Portals, Light & Live Deltas ---
        if (cfg.checkEntities()) {
            int entSignals = t.playerItemFrameCount + t.armorStandCount
                    + t.tradedOrUndergroundVillagerCount * 2 + t.tamedOrNamedEntityCount;
            if (entSignals >= 2) {
                score += Math.min(26.0, 8.0 + entSignals * 4.5);
                distinctIndicators++;
                if (t.tradedOrUndergroundVillagerCount > 0) {
                    reasons.add("Villager Hall/Traded");
                } else if (t.playerItemFrameCount > 0) {
                    reasons.add("Item Frames x" + t.playerItemFrameCount);
                } else {
                    reasons.add("Base Entities x" + entSignals);
                }
            }
        }

        if (cfg.checkPortalsAndHighways()) {
            if (t.activePortalBlockCount >= 4) {
                score += Math.min(22.0, (12.0 + t.activePortalBlockCount * 0.8) * cfg.highwayWeight());
                distinctIndicators++;
                reasons.add("Active Portal");
            }
            if (t.iceHighwayCount >= 8 && (isNether || t.medianAnomalyY < 50 || t.medianAnomalyY > 110)) {
                score += Math.min(24.0, t.iceHighwayCount * 1.4 * cfg.highwayWeight());
                distinctIndicators++;
                reasons.add("Ice Highway");
            }
        }

        // Corroborating signals (never flag a chunk by themselves; boost confidence when paired with real indicators)
        if (distinctIndicators >= 1) {
            if (cfg.checkLightAnomalies() && t.undergroundLightSamples >= 3
                    && !isMineshaft && !isTrialChamber && !isAncientCity) {
                score += Math.min(12.0, t.undergroundLightSamples * 2.5);
                if (t.undergroundLightSamples >= 4) {
                    distinctIndicators++;
                    reasons.add("Underground Lighting");
                }
            }
            if (cfg.checkChunkDeltas() && t.liveChunkDeltas >= 8) {
                score += Math.min(14.0, t.liveChunkDeltas * 1.1);
                distinctIndicators++;
                reasons.add("Active Ticking");
            }
            if (cfg.checkNewOldChunkTells() && t.pre118OldChunkSignature && isOverworld) {
                score += 10.0;
                reasons.add("Pre-1.18 Chunk");
            }
            if (cfg.checkYZeroLeaks() && t.yZeroBoost >= 6.0) {
                score += Math.min(18.0, t.yZeroBoost);
                distinctIndicators++;
                reasons.add("Y<0 Underground Leak");
            }
        }

        // Synergy bonus when 3+ distinct player-activity categories converge in one chunk
        if (distinctIndicators >= 3) {
            score += (distinctIndicators - 2) * 6.5;
        }

        double clampedScore = Math.max(0.0, Math.min(100.0, score));
        int requiredIndicators = cfg.sensitivity().getMinIndicators();
        double minScore = cfg.effectiveMinScore();

        boolean passesMultiIndicatorGate = (distinctIndicators >= requiredIndicators)
                || (hasUnmistakableStashCluster && distinctIndicators >= 1);

        boolean flagged = passesMultiIndicatorGate && clampedScore >= minScore;
        Confidence conf = classifyConfidence(clampedScore);

        return new EvaluationResult(
                flagged,
                clampedScore,
                conf,
                distinctIndicators,
                Collections.unmodifiableList(reasons),
                naturalTag,
                t.minAnomalyY,
                t.maxAnomalyY,
                t.medianAnomalyY
        );
    }

    public static Confidence classifyConfidence(double score) {
        if (score >= 72.0) {
            return Confidence.HIGH;
        }
        if (score >= 54.0) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    /**
     * Groups contiguous or nearby flagged chunks (within {@code mergeRadiusChunks} Chebyshev distance)
     * into deduplicated structure/base clusters so a multi-chunk base emits ONE primary label/alert
     * and receives a modest multi-chunk corroboration boost, WITHOUT marking unflagged empty chunks.
     */
    public static Map<Long, ClusterAssignment> deduplicateClusters(
            Map<Long, Double> chunkScores,
            int mergeRadiusChunks
    ) {
        Map<Long, ClusterAssignment> out = new HashMap<>();
        if (chunkScores.isEmpty()) {
            return out;
        }
        int r = Math.max(1, Math.min(4, mergeRadiusChunks));
        Set<Long> visited = new HashSet<>();
        List<Long> keys = new ArrayList<>(chunkScores.keySet());
        keys.sort(Long::compare);

        int nextClusterId = 1;
        for (long seed : keys) {
            if (!visited.add(seed)) {
                continue;
            }
            List<Long> component = new ArrayList<>();
            component.add(seed);
            int head = 0;
            while (head < component.size()) {
                long cur = component.get(head++);
                int cx = unpackX(cur);
                int cz = unpackZ(cur);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        long nb = packChunk(cx + dx, cz + dz);
                        if (chunkScores.containsKey(nb) && visited.add(nb)) {
                            component.add(nb);
                        }
                    }
                }
            }

            // Pick highest-scoring chunk (tie-break deterministic by packed key) as cluster anchor
            long anchor = component.get(0);
            double bestScore = -1.0;
            for (long member : component) {
                double s = chunkScores.getOrDefault(member, 0.0);
                if (s > bestScore || (s == bestScore && member < anchor)) {
                    bestScore = s;
                    anchor = member;
                }
            }

            int clusterId = nextClusterId++;
            int size = component.size();
            // Multi-chunk corroboration bonus (+4 per extra corroborated chunk, capped at +12)
            double clusterBonus = size > 1 ? Math.min(12.0, (size - 1) * 4.0) : 0.0;
            for (long member : component) {
                double boosted = Math.min(100.0, chunkScores.getOrDefault(member, 0.0) + clusterBonus);
                out.put(member, new ClusterAssignment(
                        clusterId,
                        member == anchor,
                        size,
                        boosted,
                        classifyConfidence(boosted)
                ));
            }
        }
        return out;
    }

    public static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static int unpackX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    public static int unpackZ(long packed) {
        return (int) (packed >>> 32);
    }
}
