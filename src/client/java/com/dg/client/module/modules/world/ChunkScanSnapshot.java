package com.dg.client.module.modules.world;

import com.dg.client.core.detection.SuspiciousChunkAnalyzer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compact immutable snapshot of a 16x16 chunk column captured on the client thread
 * so BaseFinder's multi-indicator structural, concealment, and natural-structure filters
 * execute 100% off-thread with zero race conditions against Minecraft's chunk containers.
 */
public final class ChunkScanSnapshot {
    // Compact cell types for off-thread geometric passes (tunnels, sealed 1x1 shafts, farm columns)
    public static final byte CELL_AIR = 0;
    public static final byte CELL_NATURAL_STONE = 1;
    public static final byte CELL_FILL_COBBLE = 2;
    public static final byte CELL_FILL_DIRT = 3;
    public static final byte CELL_FILL_DIORITE = 4;
    public static final byte CELL_FILL_OBSIDIAN = 5;
    public static final byte CELL_ROTATED_DEEPSLATE = 6;
    public static final byte CELL_KELP = 7;
    public static final byte CELL_BAMBOO_CANE = 8;
    public static final byte CELL_SOLID_OTHER = 9;

    private enum IdCategory {
        NONE,
        TUFF_TRIAL,
        TRIAL_COPPER_VAULT,
        EXOTIC_BUILD,
        GENERIC_BUILD,
        PURPUR_END_CITY
    }

    private static final ConcurrentHashMap<Block, IdCategory> ID_CACHE = new ConcurrentHashMap<>();

    public final int chunkX;
    public final int chunkZ;
    public final String dimensionId;
    public final int bottomY;
    public final int sectionCount;
    public final byte[][] sectionCells;
    public final SuspiciousChunkAnalyzer.ChunkTelemetry.Builder telemetry;

    private ChunkScanSnapshot(
            int chunkX,
            int chunkZ,
            String dimensionId,
            int bottomY,
            int sectionCount,
            byte[][] sectionCells,
            SuspiciousChunkAnalyzer.ChunkTelemetry.Builder telemetry
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimensionId = dimensionId;
        this.bottomY = bottomY;
        this.sectionCount = sectionCount;
        this.sectionCells = sectionCells;
        this.telemetry = telemetry;
    }

    public static ChunkScanSnapshot capture(ClientWorld world, WorldChunk chunk, String dimensionId) {
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        int bottomY = chunk.getBottomY();
        ChunkSection[] sections = chunk.getSectionArray();
        int secCount = sections.length;
        byte[][] cells = new byte[secCount][];

        SuspiciousChunkAnalyzer.ChunkTelemetry.Builder tb =
                SuspiciousChunkAnalyzer.ChunkTelemetry.builder(cx, cz, dimensionId);

        boolean foundModernBlocks = false;
        boolean bedrockNearY0 = false;
        boolean isNether = dimensionId != null && dimensionId.contains("the_nether");

        for (int s = 0; s < secCount; s++) {
            ChunkSection sec = sections[s];
            if (sec == null || sec.isEmpty()) {
                continue;
            }
            byte[] grid = new byte[4096];
            int secBaseY = bottomY + (s << 4);
            for (int y = 0; y < 16; y++) {
                int wy = secBaseY + y;
                int yOff = y << 8;
                for (int z = 0; z < 16; z++) {
                    int yzOff = yOff | (z << 4);
                    boolean zBorder = (z == 0 || z == 15);
                    for (int x = 0; x < 16; x++) {
                        BlockState st = sec.getBlockState(x, y, z);
                        if (st.isAir()) {
                            grid[yzOff | x] = CELL_AIR;
                            continue;
                        }
                        Block b = st.getBlock();

                        // Natural stone vs fill material classification for off-thread geometric passes
                        if (b == Blocks.STONE || b == Blocks.GRANITE || b == Blocks.ANDESITE || b == Blocks.TUFF) {
                            grid[yzOff | x] = CELL_NATURAL_STONE;
                            if (b == Blocks.TUFF) foundModernBlocks = true;
                            continue;
                        }
                        if (b == Blocks.DEEPSLATE) {
                            foundModernBlocks = true;
                            if (st.contains(Properties.AXIS) && st.get(Properties.AXIS) != Direction.Axis.Y) {
                                grid[yzOff | x] = CELL_ROTATED_DEEPSLATE;
                                tb.rotatedDeepslateCount++;
                                tb.recordAnomalyY(wy);
                            } else {
                                grid[yzOff | x] = CELL_NATURAL_STONE;
                            }
                            continue;
                        }
                        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) {
                            foundModernBlocks = true;
                            grid[yzOff | x] = CELL_NATURAL_STONE;
                            continue;
                        }
                        if (b == Blocks.BEDROCK) {
                            if (!bedrockNearY0 && wy >= 1 && wy <= 4) {
                                bedrockNearY0 = true;
                            }
                            grid[yzOff | x] = CELL_NATURAL_STONE;
                            continue;
                        }

                        // Sky / Nether roof build check
                        if ((isNether && wy >= 128 && b != Blocks.BROWN_MUSHROOM && b != Blocks.RED_MUSHROOM)
                                || (!isNether && wy >= 256)) {
                            tb.skyOrRoofBuildCount++;
                            tb.recordAnomalyY(wy);
                        }

                        // Fast block checks
                        grid[yzOff | x] = classifyBlockAndAccumulate(sec, x, y, z, wy, isNether, zBorder || x == 0 || x == 15, st, b, tb);
                    }
                }
            }
            cells[s] = grid;
        }

        if (bedrockNearY0 && !foundModernBlocks && dimensionId != null && dimensionId.contains("overworld")) {
            tb.pre118OldChunkSignature = true;
        }

        // Inspect BlockEntities on client thread (safe, typically 0..20 per chunk)
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be.isRemoved()) continue;
            BlockPos pos = be.getPos();
            int wy = pos.getY();
            if (be instanceof ShulkerBoxBlockEntity) {
                tb.shulkerCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof EnderChestBlockEntity) {
                tb.enderChestCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
                BlockState below = chunk.getBlockState(pos.down());
                if (isNaturalStructureSupport(below)) {
                    tb.structureContainerCount++;
                } else {
                    tb.playerContainerCount++;
                    tb.recordAnomalyY(wy);
                }
            } else if (be instanceof HopperBlockEntity) {
                tb.hopperCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof ShelfBlockEntity || be instanceof ChiseledBookshelfBlockEntity) {
                tb.shelfAndBookStorageCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof EnchantingTableBlockEntity) {
                tb.enchantingTableCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof BeaconBlockEntity) {
                tb.beaconCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof BrewingStandBlockEntity) {
                tb.brewingStandCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof MobSpawnerBlockEntity) {
                tb.spawnerCount++;
                tb.recordAnomalyY(wy);
            } else if (be instanceof SignBlockEntity sign) {
                if (hasPlayerWrittenText(sign)) {
                    tb.writtenSignCount++;
                    tb.recordAnomalyY(wy);
                }
            }
        }

        // Sample underground non-lava artificial block-light anomalies
        int baseX = cx << 4;
        int baseZ = cz << 4;
        BlockPos.Mutable mpos = new BlockPos.Mutable();
        for (int wy = Math.max(bottomY + 4, -56); wy <= 46; wy += 8) {
            for (int lz = 3; lz < 16; lz += 6) {
                for (int lx = 3; lx < 16; lx += 6) {
                    mpos.set(baseX + lx, wy, baseZ + lz);
                    int bl = world.getLightLevel(LightType.BLOCK, mpos);
                    if (bl >= 10) {
                        BlockState here = chunk.getBlockState(mpos);
                        Block hb = here.getBlock();
                        if (hb != Blocks.LAVA && hb != Blocks.GLOW_LICHEN && hb != Blocks.MAGMA_BLOCK
                                && hb != Blocks.SCULK_CATALYST && hb != Blocks.AMETHYST_CLUSTER) {
                            tb.undergroundLightSamples++;
                        }
                    }
                }
            }
        }

        // Entity signals inside chunk column
        Box chunkBox = new Box(baseX, bottomY, baseZ, baseX + 16, chunk.getTopYInclusive(), baseZ + 16);
        for (Entity e : world.getOtherEntities(null, chunkBox)) {
            if (e instanceof ItemFrameEntity frame) {
                if (!frame.getHeldItemStack().isOf(Items.ELYTRA)) {
                    tb.playerItemFrameCount++;
                    tb.recordAnomalyY(e.getBlockY());
                }
            } else if (e instanceof ArmorStandEntity) {
                tb.armorStandCount++;
                tb.recordAnomalyY(e.getBlockY());
            } else if (e instanceof VillagerEntity villager) {
                int lvl = villager.getVillagerData().level();
                if (lvl > 1 || villager.getBlockY() < 50) {
                    tb.tradedOrUndergroundVillagerCount++;
                    tb.recordAnomalyY(villager.getBlockY());
                } else {
                    tb.noviceVillagerCount++;
                }
            } else if ((e instanceof TameableEntity tame && tame.isTamed()) || e.hasCustomName()) {
                tb.tamedOrNamedEntityCount++;
                tb.recordAnomalyY(e.getBlockY());
            } else if (e instanceof AbstractMinecartEntity || e instanceof BoatEntity) {
                tb.vehicleCount++;
            }
        }

        return new ChunkScanSnapshot(cx, cz, dimensionId, bottomY, secCount, cells, tb);
    }

    /**
     * Completes off-thread geometric passes (excavated 1x2 corridors, enclosed 1x1 dive pillars,
     * and sea-level/vertical farm columns) and builds the final immutable {@link SuspiciousChunkAnalyzer.ChunkTelemetry}.
     */
    public SuspiciousChunkAnalyzer.ChunkTelemetry finishOffThreadAnalysis(int liveChunkDeltas, double yZeroBoost) {
        telemetry.straightTunnelCount = scanStraightTunnels();
        telemetry.enclosedFillPillarCount = scanEnclosedFillPillars();
        telemetry.farmColumnScore += scanVerticalFarmColumns();
        telemetry.liveChunkDeltas = liveChunkDeltas;
        telemetry.yZeroBoost = yZeroBoost;
        return telemetry.build();
    }

    private static byte classifyBlockAndAccumulate(
            ChunkSection sec,
            int lx,
            int ly,
            int lz,
            int wy,
            boolean isNether,
            boolean isBorder,
            BlockState st,
            Block b,
            SuspiciousChunkAnalyzer.ChunkTelemetry.Builder tb
    ) {
        if (b == Blocks.COBBLESTONE || b == Blocks.COBBLED_DEEPSLATE) {
            tb.genericBuildBlockCount++;
            return CELL_FILL_COBBLE;
        }
        if (b == Blocks.DIRT || b == Blocks.COARSE_DIRT) {
            return CELL_FILL_DIRT;
        }
        if (b == Blocks.DIORITE) {
            return CELL_FILL_DIORITE;
        }
        if (b == Blocks.OBSIDIAN) {
            tb.obsidianCount++;
            return CELL_FILL_OBSIDIAN;
        }
        if (b == Blocks.CRYING_OBSIDIAN) {
            tb.cryingObsidianCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.NETHER_PORTAL || b == Blocks.END_PORTAL) {
            tb.activePortalBlockCount++;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.NETHERRACK || b == Blocks.MAGMA_BLOCK) {
            if (!isNether) {
                tb.netherrackMagmaCount++;
            }
            return CELL_SOLID_OTHER;
        }

        // Workstations not backed by unique BlockEntities
        if (b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) {
            tb.furnaceCount++;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.ANVIL || b == Blocks.CHIPPED_ANVIL || b == Blocks.DAMAGED_ANVIL) {
            tb.anvilCount++;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.CRAFTING_TABLE) {
            tb.craftingTableCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.BOOKSHELF) {
            tb.bookshelfCount++;
            return CELL_SOLID_OTHER;
        }
        if (st.isIn(BlockTags.BEDS)) {
            // Beds occupy 2 blocks (HEAD + FOOT); count half by checking BED_PART if present, or divide by 2
            if (!st.contains(Properties.BED_PART) || st.get(Properties.BED_PART) == net.minecraft.block.enums.BedPart.HEAD) {
                if (wy < 50) {
                    tb.undergroundBedCount++;
                    tb.recordAnomalyY(wy);
                } else {
                    tb.surfaceBedCount++;
                }
            }
            return CELL_SOLID_OTHER;
        }

        // Redstone & farm mechanics
        if (b instanceof ObserverBlock) {
            tb.observerCount++;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b instanceof PistonBlock) {
            tb.pistonCount++;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b instanceof RepeaterBlock || b == Blocks.COMPARATOR) {
            tb.repeaterComparatorCount++;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.DROPPER || b == Blocks.DISPENSER) {
            tb.dropperDispenserCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.REDSTONE_WIRE || b == Blocks.REDSTONE_LAMP || b == Blocks.REDSTONE_BLOCK
                || b == Blocks.DAYLIGHT_DETECTOR || b == Blocks.LEVER || b == Blocks.REDSTONE_TORCH) {
            tb.redstoneComponentCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.POWERED_RAIL || b == Blocks.DETECTOR_RAIL || b == Blocks.ACTIVATOR_RAIL) {
            tb.poweredRailCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.RAIL) {
            tb.railCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.BUBBLE_COLUMN) {
            if (ly > 0) {
                Block below = sec.getBlockState(lx, ly - 1, lz).getBlock();
                if (below == Blocks.SOUL_SAND || below == Blocks.MAGMA_BLOCK) {
                    tb.soulSandBubbleCount += 3;
                    tb.recordAnomalyY(wy);
                    return CELL_SOLID_OTHER;
                }
            }
            tb.soulSandBubbleCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.BEEHIVE && st.contains(Properties.HONEY_LEVEL) && st.get(Properties.HONEY_LEVEL) == 5) {
            tb.farmColumnScore += 2;
            tb.recordAnomalyY(wy);
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.KELP || b == Blocks.KELP_PLANT) {
            return CELL_KELP;
        }
        if (b == Blocks.BAMBOO || b == Blocks.SUGAR_CANE) {
            return CELL_BAMBOO_CANE;
        }
        if (b == Blocks.BLUE_ICE || b == Blocks.PACKED_ICE) {
            tb.iceHighwayCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN
                || b == Blocks.SEA_LANTERN || b == Blocks.END_ROD || b == Blocks.SHROOMLIGHT) {
            tb.torchLanternCount++;
            return CELL_SOLID_OTHER;
        }

        // Natural structure signature blocks
        if (b == Blocks.COBWEB) {
            tb.cobwebCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.IRON_CHAIN) {
            tb.chainCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.MOSSY_COBBLESTONE) {
            tb.mossyCobbleCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.SCULK || b == Blocks.SCULK_CATALYST || b == Blocks.SCULK_SHRIEKER || b == Blocks.SCULK_VEIN) {
            tb.sculkCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.DEEPSLATE_TILES || b == Blocks.DEEPSLATE_BRICKS
                || b == Blocks.CRACKED_DEEPSLATE_TILES || b == Blocks.CRACKED_DEEPSLATE_BRICKS) {
            tb.deepslateTilesBricksCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.DIRT_PATH) {
            tb.dirtPathCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.HAY_BLOCK || b == Blocks.BELL) {
            tb.hayBellCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.AMETHYST_CLUSTER) {
            tb.amethystClusterCount++;
            return CELL_SOLID_OTHER;
        }
        if (b == Blocks.BUDDING_AMETHYST || b == Blocks.CALCITE || b == Blocks.SMOOTH_BASALT) {
            tb.geodeShellCount++;
            return CELL_SOLID_OTHER;
        }

        if (b instanceof FluidBlock) {
            FluidState fs = st.getFluidState();
            if (isBorder && !fs.isEmpty() && !fs.isStill()) {
                tb.borderFlowingFluids++;
            }
            return CELL_SOLID_OTHER;
        }

        // Cached registry-ID family lookup for Trial Chamber copper/tuff & exotic build palettes
        IdCategory idCat = ID_CACHE.computeIfAbsent(b, ChunkScanSnapshot::classifyByRegistryId);
        switch (idCat) {
            case TUFF_TRIAL -> tb.tuffBricksCount++;
            case TRIAL_COPPER_VAULT -> tb.trialCopperAndVaultCount++;
            case EXOTIC_BUILD -> {
                tb.exoticBuildBlockCount++;
                tb.recordAnomalyY(wy);
            }
            case GENERIC_BUILD -> tb.genericBuildBlockCount++;
            case PURPUR_END_CITY -> tb.purpurEndCityBlockCount++;
            default -> {
            }
        }
        return CELL_SOLID_OTHER;
    }

    private static IdCategory classifyByRegistryId(Block b) {
        if (b instanceof ShulkerBoxBlock) {
            return IdCategory.NONE;
        }
        String path = Registries.BLOCK.getId(b).getPath();
        if (path.contains("tuff_brick") || path.startsWith("chiseled_tuff") || path.startsWith("polished_tuff")) {
            return IdCategory.TUFF_TRIAL;
        }
        if (path.startsWith("waxed_") || path.contains("copper_bulb") || path.contains("copper_grate")
                || path.equals("trial_spawner") || path.equals("vault") || path.equals("crafter")) {
            return IdCategory.TRIAL_COPPER_VAULT;
        }
        if (path.startsWith("purpur_") || path.equals("end_stone_bricks")) {
            return IdCategory.PURPUR_END_CITY;
        }
        if (path.endsWith("_concrete") || path.endsWith("_concrete_powder")
                || path.endsWith("_glazed_terracotta") || path.endsWith("_stained_glass")
                || path.endsWith("_stained_glass_pane") || path.equals("tinted_glass")
                || path.startsWith("cherry_") || path.startsWith("bamboo_") || path.startsWith("mangrove_")
                || path.equals("scaffolding") || path.equals("sponge") || path.equals("wet_sponge")
                || path.equals("quartz_bricks") || path.equals("quartz_pillar") || path.equals("chiseled_quartz_block")
                || path.equals("iron_block") || path.equals("diamond_block")
                || path.equals("emerald_block") || path.equals("netherite_block")) {
            return IdCategory.EXOTIC_BUILD;
        }
        if (path.endsWith("_planks") || path.endsWith("_stairs") || path.endsWith("_slab")
                || path.endsWith("_door") || path.endsWith("_trapdoor") || path.equals("glass")
                || path.equals("glass_pane") || path.equals("stone_bricks") || path.equals("bricks")
                || path.equals("ladder") || path.endsWith("_wool")) {
            return IdCategory.GENERIC_BUILD;
        }
        return IdCategory.NONE;
    }

    private static boolean isNaturalStructureSupport(BlockState below) {
        Block b = below.getBlock();
        if (b == Blocks.MOSSY_COBBLESTONE || b == Blocks.STONE_BRICKS || b == Blocks.MOSSY_STONE_BRICKS
                || b == Blocks.CRACKED_STONE_BRICKS || b == Blocks.SCULK || b == Blocks.DEEPSLATE_TILES
                || b == Blocks.DEEPSLATE_BRICKS || b == Blocks.TUFF_BRICKS) {
            return true;
        }
        IdCategory cat = ID_CACHE.computeIfAbsent(b, ChunkScanSnapshot::classifyByRegistryId);
        return cat == IdCategory.TUFF_TRIAL || cat == IdCategory.TRIAL_COPPER_VAULT || cat == IdCategory.PURPUR_END_CITY;
    }

    private static boolean hasPlayerWrittenText(SignBlockEntity sign) {
        return checkSignSide(sign.getFrontText(), true) || checkSignSide(sign.getBackText(), false);
    }

    private static boolean checkSignSide(SignText text, boolean checkIglooArrow) {
        if (text == null) return false;
        for (int i = 0; i < 4; i++) {
            String line = text.getMessage(i, false).getString().trim();
            if (line.isEmpty()) continue;
            // Exclude vanilla Igloo basement sign arrows
            if (checkIglooArrow && ((i == 1 && Objects.equals(line, "<----")) || (i == 2 && Objects.equals(line, "---->")))) {
                continue;
            }
            return true;
        }
        return false;
    }

    public byte getCellAt(int localX, int worldY, int localZ) {
        int relY = worldY - bottomY;
        int secIdx = relY >> 4;
        if (secIdx < 0 || secIdx >= sectionCount) {
            return CELL_AIR;
        }
        byte[] grid = sectionCells[secIdx];
        if (grid == null) {
            return CELL_AIR;
        }
        return grid[((relY & 15) << 8) | ((localZ & 15) << 4) | (localX & 15)];
    }

    private int scanStraightTunnels() {
        int tunnels = 0;
        int maxScanY = (dimensionId != null && dimensionId.contains("the_nether"))
                ? Math.min(122, bottomY + (sectionCount << 4) - 4)
                : Math.min(52, bottomY + (sectionCount << 4) - 4);
        for (int wy = bottomY + 5; wy < maxScanY; wy += 3) {
            for (int lane = 2; lane < 14; lane += 3) {
                int runX = 0;
                int runZ = 0;
                for (int step = 0; step < 16; step++) {
                    if (is1x2CorridorSlice(step, wy, lane)) {
                        if (++runX >= 9) {
                            tunnels++;
                            telemetry.recordAnomalyY(wy);
                            break;
                        }
                    } else {
                        runX = 0;
                    }
                    if (is1x2CorridorSlice(lane, wy, step)) {
                        if (++runZ >= 9) {
                            tunnels++;
                            telemetry.recordAnomalyY(wy);
                            break;
                        }
                    } else {
                        runZ = 0;
                    }
                }
            }
        }
        return tunnels;
    }

    private boolean is1x2CorridorSlice(int x, int wy, int z) {
        return getCellAt(x, wy, z) == CELL_AIR
                && getCellAt(x, wy + 1, z) == CELL_AIR
                && getCellAt(x, wy - 1, z) != CELL_AIR
                && getCellAt(x, wy + 2, z) != CELL_AIR;
    }

    /**
     * Detects sealed 1x1 vertical dive holes (6+ vertical filler blocks completely enclosed horizontally
     * by natural stone/deepslate).
     */
    private int scanEnclosedFillPillars() {
        int pillars = 0;
        int topY = Math.min(62, bottomY + (sectionCount << 4) - 2);
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                int run = 0;
                byte activeFill = 0;
                for (int wy = bottomY + 5; wy <= topY; wy++) {
                    byte c = getCellAt(x, wy, z);
                    boolean isFill = (c == CELL_FILL_COBBLE || c == CELL_FILL_DIRT
                            || c == CELL_FILL_DIORITE || c == CELL_FILL_OBSIDIAN || c == CELL_ROTATED_DEEPSLATE);
                    if (isFill && (run == 0 || c == activeFill) && isSurroundedByNaturalStone(x, wy, z)) {
                        activeFill = c;
                        run++;
                        if (run == 6) {
                            pillars++;
                            telemetry.recordAnomalyY(wy - 3);
                        }
                    } else {
                        run = 0;
                        activeFill = 0;
                    }
                }
            }
        }
        return pillars;
    }

    private boolean isSurroundedByNaturalStone(int x, int wy, int z) {
        return getCellAt(x - 1, wy, z) == CELL_NATURAL_STONE
                && getCellAt(x + 1, wy, z) == CELL_NATURAL_STONE
                && getCellAt(x, wy, z - 1) == CELL_NATURAL_STONE
                && getCellAt(x, wy, z + 1) == CELL_NATURAL_STONE;
    }

    private int scanVerticalFarmColumns() {
        int kelpCols = 0;
        int kelpSeaTops = 0;
        int caneOrBambooCols = 0;
        int topY = Math.min(120, bottomY + (sectionCount << 4) - 1);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int kelpRun = 0;
                int maxKelpY = Integer.MIN_VALUE;
                int bambooRun = 0;
                for (int wy = Math.max(bottomY, 30); wy <= topY; wy++) {
                    byte c = getCellAt(x, wy, z);
                    if (c == CELL_KELP) {
                        kelpRun++;
                        maxKelpY = wy;
                    }
                    if (c == CELL_BAMBOO_CANE) {
                        bambooRun++;
                    } else if (bambooRun > 0) {
                        if (bambooRun >= 6) caneOrBambooCols++;
                        bambooRun = 0;
                    }
                }
                if (bambooRun >= 6) caneOrBambooCols++;
                if (kelpRun >= 8) {
                    kelpCols++;
                    if (maxKelpY == 62) kelpSeaTops++;
                }
            }
        }
        int score = 0;
        if (kelpCols >= 12 && kelpSeaTops >= 8) {
            score += 3;
            telemetry.recordAnomalyY(58);
        }
        if (caneOrBambooCols >= 12) {
            score += 3;
            telemetry.recordAnomalyY(66);
        }
        return score;
    }
}
