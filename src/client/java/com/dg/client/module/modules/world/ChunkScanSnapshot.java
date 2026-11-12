package com.dg.client.module.modules.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Compact immutable snapshot of a 16x16 chunk column captured quickly on the client thread
 * so BaseFinder's 10 geometric & statistical heuristics execute 100% off-thread with zero
 * race conditions against Minecraft's chunk containers.
 */
public final class ChunkScanSnapshot {
    public static final byte CAT_OTHER = 0;
    public static final byte CAT_AIR = 1;
    public static final byte CAT_STORAGE = 2;
    public static final byte CAT_ACTIVITY_UTIL = 3;
    public static final byte CAT_REDSTONE = 4;
    public static final byte CAT_MANMADE = 5;
    public static final byte CAT_OBSIDIAN_PORTAL = 6;
    public static final byte CAT_ICE_HIGHWAY = 7;
    public static final byte CAT_TORCH_LIGHT = 8;
    public static final byte CAT_LAVA = 9;
    public static final byte CAT_FLOWING_FLUID = 10;

    public final int chunkX;
    public final int chunkZ;
    public final String dimensionId;
    public final int bottomY;
    public final int sectionCount;
    // [sectionIdx][y * 256 + z * 16 + x] -> category byte (null if section is all air)
    public final byte[][] sectionCells;

    // Pre-tallied exact counts & bounds captured during snapshot pass
    public final int blockEntityCount;
    public final int artificialUndergroundLightSamples;
    public final int borderFlowingFluidCount;
    public final boolean hasCopperOrTuffOrDeepslate;
    public final boolean hasZeroLevelBedrockSignature;
    public final int clusteredSpecialEntities;

    private ChunkScanSnapshot(
            int chunkX,
            int chunkZ,
            String dimensionId,
            int bottomY,
            int sectionCount,
            byte[][] sectionCells,
            int blockEntityCount,
            int artificialUndergroundLightSamples,
            int borderFlowingFluidCount,
            boolean hasCopperOrTuffOrDeepslate,
            boolean hasZeroLevelBedrockSignature,
            int clusteredSpecialEntities
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.dimensionId = dimensionId;
        this.bottomY = bottomY;
        this.sectionCount = sectionCount;
        this.sectionCells = sectionCells;
        this.blockEntityCount = blockEntityCount;
        this.artificialUndergroundLightSamples = artificialUndergroundLightSamples;
        this.borderFlowingFluidCount = borderFlowingFluidCount;
        this.hasCopperOrTuffOrDeepslate = hasCopperOrTuffOrDeepslate;
        this.hasZeroLevelBedrockSignature = hasZeroLevelBedrockSignature;
        this.clusteredSpecialEntities = clusteredSpecialEntities;
    }

    public static ChunkScanSnapshot capture(ClientWorld world, WorldChunk chunk, String dimensionId) {
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        int bottomY = chunk.getBottomY();
        ChunkSection[] sections = chunk.getSectionArray();
        int secCount = sections.length;
        byte[][] cells = new byte[secCount][];

        boolean foundModernBlocks = false;
        boolean bedrockNearY0 = false;
        int borderFlowing = 0;

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
                        byte cat = classify(st);
                        grid[yzOff | x] = cat;

                        Block b = st.getBlock();
                        if (!foundModernBlocks && (b == Blocks.TUFF || b == Blocks.DEEPSLATE
                                || b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE)) {
                            foundModernBlocks = true;
                        }
                        if (!bedrockNearY0 && wy >= 1 && wy <= 4 && b == Blocks.BEDROCK) {
                            bedrockNearY0 = true;
                        }
                        if (cat == CAT_FLOWING_FLUID && (zBorder || x == 0 || x == 15)) {
                            borderFlowing++;
                        }
                    }
                }
            }
            cells[s] = grid;
        }

        // Sample underground block-light anomalies (non-lava artificial light underground)
        int lightAnomalies = 0;
        int baseX = cx << 4;
        int baseZ = cz << 4;
        BlockPos.Mutable mpos = new BlockPos.Mutable();
        for (int wy = Math.max(bottomY + 4, -56); wy <= 46; wy += 8) {
            for (int lz = 3; lz < 16; lz += 6) {
                for (int lx = 3; lx < 16; lx += 6) {
                    mpos.set(baseX + lx, wy, baseZ + lz);
                    int bl = world.getLightLevel(LightType.BLOCK, mpos);
                    if (bl >= 9) {
                        BlockState here = chunk.getBlockState(mpos);
                        if (here.getBlock() != Blocks.LAVA && here.getBlock() != Blocks.GLOW_LICHEN) {
                            lightAnomalies++;
                        }
                    }
                }
            }
        }

        // Count special clustered entities inside chunk column
        Box chunkBox = new Box(baseX, bottomY, baseZ, baseX + 16, chunk.getTopYInclusive(), baseZ + 16);
        int specialEnts = 0;
        for (Entity e : world.getOtherEntities(null, chunkBox)) {
            if (e instanceof ItemFrameEntity || e instanceof ArmorStandEntity
                    || e instanceof AbstractMinecartEntity || e instanceof BoatEntity
                    || e instanceof VillagerEntity || e.hasCustomName()
                    || (e instanceof TameableEntity t && t.isTamed())) {
                specialEnts++;
            }
        }

        return new ChunkScanSnapshot(
                cx,
                cz,
                dimensionId,
                bottomY,
                secCount,
                cells,
                chunk.getBlockEntities().size(),
                lightAnomalies,
                borderFlowing,
                foundModernBlocks,
                bedrockNearY0,
                specialEnts
        );
    }

    public byte getCategoryAt(int localX, int worldY, int localZ) {
        int relY = worldY - bottomY;
        int secIdx = relY >> 4;
        if (secIdx < 0 || secIdx >= sectionCount) {
            return CAT_AIR;
        }
        byte[] grid = sectionCells[secIdx];
        if (grid == null) {
            return CAT_AIR;
        }
        return grid[((relY & 15) << 8) | ((localZ & 15) << 4) | (localX & 15)];
    }

    private static byte classify(BlockState st) {
        if (st.isAir()) {
            return CAT_AIR;
        }
        Block b = st.getBlock();
        if (b instanceof ChestBlock || b instanceof ShulkerBoxBlock || b == Blocks.BARREL || b == Blocks.ENDER_CHEST) {
            return CAT_STORAGE;
        }
        if (b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER
                || b == Blocks.HOPPER || b == Blocks.DISPENSER || b == Blocks.DROPPER
                || b == Blocks.CRAFTING_TABLE || b == Blocks.ANVIL || b == Blocks.CHIPPED_ANVIL
                || b == Blocks.ENCHANTING_TABLE || b == Blocks.BREWING_STAND || b == Blocks.BEACON
                || b == Blocks.LECTERN || st.isIn(BlockTags.BEDS)
                || b instanceof SignBlock || b instanceof WallSignBlock || st.isIn(BlockTags.BANNERS)) {
            return CAT_ACTIVITY_UTIL;
        }
        if (b == Blocks.REDSTONE_WIRE || b instanceof RepeaterBlock || b == Blocks.COMPARATOR
                || b instanceof ObserverBlock || b instanceof PistonBlock || b == Blocks.LEVER
                || b == Blocks.POWERED_RAIL || b == Blocks.DETECTOR_RAIL || b == Blocks.ACTIVATOR_RAIL
                || b == Blocks.DAYLIGHT_DETECTOR || b == Blocks.REDSTONE_LAMP || b == Blocks.REDSTONE_BLOCK) {
            return CAT_REDSTONE;
        }
        if (b == Blocks.OBSIDIAN || b == Blocks.CRYING_OBSIDIAN || b == Blocks.NETHER_PORTAL || b == Blocks.RESPAWN_ANCHOR) {
            return CAT_OBSIDIAN_PORTAL;
        }
        if (b == Blocks.BLUE_ICE || b == Blocks.PACKED_ICE || b == Blocks.ICE) {
            return CAT_ICE_HIGHWAY;
        }
        if (b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN
                || b == Blocks.SEA_LANTERN || b == Blocks.END_ROD || b == Blocks.SHROOMLIGHT) {
            return CAT_TORCH_LIGHT;
        }
        if (st.isIn(BlockTags.PLANKS) || b == Blocks.COBBLESTONE || b == Blocks.STONE_BRICKS
                || b == Blocks.GLASS || b == Blocks.GLASS_PANE || st.isIn(BlockTags.WOOL)
                || st.isIn(BlockTags.TERRACOTTA) || b == Blocks.LADDER || b == Blocks.SCAFFOLDING
                || b == Blocks.FARMLAND || b == Blocks.BRICKS || b == Blocks.QUARTZ_BLOCK) {
            return CAT_MANMADE;
        }
        if (b == Blocks.LAVA) {
            return CAT_LAVA;
        }
        if (b instanceof FluidBlock) {
            FluidState fs = st.getFluidState();
            if (!fs.isEmpty() && !fs.isStill()) {
                return CAT_FLOWING_FLUID;
            }
        }
        return CAT_OTHER;
    }
}
