package com.dg.client.module.modules.world;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.module.modules.esp.StorageEspModule;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.Render3DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.setting.SettingGroup;
import com.dg.client.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Locale;

/**
 * Section 6.2 & 6.3 Modular Y-0 Underground Bypass:
 * Combines Packet-Leak reading, Anti-Decoy statistical filtering, rate-limited Safe-Mode
 * adjacent update triggers, Light/Heightmap cavity inference, and Sound/Particle activity
 * tracking into a local thread-safe UndergroundStore rendered as a confidence-weighted
 * green/white 3D overlay. Never fabricates data when the server sends none.
 */
public final class YZeroBypassModule extends Module {
    private final SettingGroup stratGroup = createGroup("Strategies");
    private final SettingGroup filterGroup = createGroup("Block Filters");
    private final SettingGroup safetyGroup = createGroup("Limits & Persistence");

    // 5 Toggleable Strategies
    public final BooleanSetting packetLeakReader = stratGroup.add(new BooleanSetting(
            "Packet-Leak Reader", "Reconstructs Y<0 blocks from updates, block entities, and world events", true));
    public final BooleanSetting antiDecoyFilter = stratGroup.add(new BooleanSetting(
            "Anti-Decoy Filter", "Filters fake Anti-Xray ore floods via density, Y-rules, and air adjacency", true));
    public final BooleanSetting updateTriggers = stratGroup.add(new BooleanSetting(
            "Update Triggers", "Rate-limited adjacent exposed-block query within player reach", false));
    public final BooleanSetting lightHeightmapInference = stratGroup.add(new BooleanSetting(
            "Light & Heightmap", "Infers lit underground cavities and open shafts below Y=0", true));
    public final BooleanSetting soundParticleInference = stratGroup.add(new BooleanSetting(
            "Sound & Particle", "Infers underground bases/redstone/mining from Y<0 sounds & particles", true));

    // Block category filters
    public final BooleanSetting showStorage = filterGroup.add(new BooleanSetting("Storage", "Render recovered chests, shulkers, barrels, e-chests", true));
    public final BooleanSetting showOres = filterGroup.add(new BooleanSetting("Ores", "Render high-confidence sub-zero ores", true));
    public final BooleanSetting showStructures = filterGroup.add(new BooleanSetting("Tunnels & Structures", "Render player blocks, redstone, and light cavities", true));
    public final BooleanSetting showActivity = filterGroup.add(new BooleanSetting("Sound/Particle Pings", "Render timed underground sound/particle markers", true));

    // Thresholds, Y-range, rate-limits, memory cap
    public final DoubleSetting confidenceThreshold = safetyGroup.add(new DoubleSetting(
            "Min Confidence", "Minimum confidence score (0..1) required to display or feed ESP", 0.55, 0.15, 0.98, 0.05));
    public final IntSetting minY = safetyGroup.add(new IntSetting(
            "Min Y", "Lowest Y coordinate rendered", -64, -64, -1, 4));
    public final IntSetting maxY = safetyGroup.add(new IntSetting(
            "Max Y", "Highest Y coordinate rendered (< 0)", -1, -64, -1, 1));
    public final BooleanSetting safeMode = safetyGroup.add(new BooleanSetting(
            "Safe Mode", "Prevents any packet rate or out-of-reach action that could kick client", true));
    public final IntSetting triggerIntervalTicks = safetyGroup.add(new IntSetting(
            "Trigger Interval (t)", "Ticks between safe adjacent update requests", 15, 4, 60, 1)
            .visibleWhen(updateTriggers::getValue));
    public final BooleanSetting persistPerServer = safetyGroup.add(new BooleanSetting(
            "Persist Known Store", "Saves confirmed Y<0 blocks per server in config/dgclient/y0_store/", true));
    public final IntSetting maxMemoryChunks = safetyGroup.add(new IntSetting(
            "Max Cached Chunks", "Memory cap on stored chunk records", 256, 32, 1024, 32));

    private final UndergroundStore store = new UndergroundStore();
    private int tickCounter = 0;
    private int probeCursor = 0;

    public YZeroBypassModule() {
        super("Y0Bypass", "Modular underground packet-leak reconstruction, anti-decoy filter & cavity inference", ModuleCategory.DETECT);
    }

    public UndergroundStore getStore() {
        return store;
    }

    @Override
    public String getDisplayInfo() {
        return store.getTotalKnownBlockCount() + " blk";
    }

    @Override
    protected void onEnable() {
        if (persistPerServer.getValue()) {
            store.loadForCurrentServer(maxMemoryChunks.getValue());
        }
        StorageEspModule storageEsp = DGClient.getInstance().getModuleManager().get(StorageEspModule.class);
        if (storageEsp != null) {
            storageEsp.setLeakedStorageSupplier(() -> isEnabled()
                    ? store.getLeakedStorage(confidenceThreshold.getValue().floatValue())
                    : java.util.List.of());
        }
    }

    @Override
    protected void onDisable() {
        if (persistPerServer.getValue()) {
            store.saveForCurrentServer();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        Packet<?> pkt = event.getPacket();

        if (packetLeakReader.getValue()) {
            if (pkt instanceof BlockUpdateS2CPacket bu && bu.getPos().getY() < 0) {
                UndergroundStore.BlockCategory cat = classifyBlock(bu.getState());
                if (cat != null) {
                    store.recordBlock(bu.getPos(), bu.getState().getBlock(), cat, 0.98f, "BlockUpdate", maxMemoryChunks.getValue());
                }
            } else if (pkt instanceof ChunkDeltaUpdateS2CPacket cdu) {
                cdu.visitUpdates((pos, state) -> {
                    if (pos.getY() < 0) {
                        UndergroundStore.BlockCategory cat = classifyBlock(state);
                        if (cat != null) {
                            store.recordBlock(pos, state.getBlock(), cat, 0.96f, "SectionDelta", maxMemoryChunks.getValue());
                        }
                    }
                });
            } else if (pkt instanceof BlockEntityUpdateS2CPacket beu && beu.getPos().getY() < 0) {
                String id = String.valueOf(Registries.BLOCK_ENTITY_TYPE.getId(beu.getBlockEntityType()));
                UndergroundStore.BlockCategory cat = isStorageId(id) ? UndergroundStore.BlockCategory.STORAGE : UndergroundStore.BlockCategory.MANMADE;
                store.recordBlockId(beu.getPos(), id, cat, 0.99f, "BlockEntityPacket", maxMemoryChunks.getValue());
            } else if (pkt instanceof ChunkDataS2CPacket cd) {
                cd.getChunkData().getBlockEntities(cd.getChunkX(), cd.getChunkZ()).accept((pos, type, nbt) -> {
                    if (pos.getY() < 0) {
                        String id = String.valueOf(Registries.BLOCK_ENTITY_TYPE.getId(type));
                        UndergroundStore.BlockCategory cat = isStorageId(id) ? UndergroundStore.BlockCategory.STORAGE : UndergroundStore.BlockCategory.MANMADE;
                        store.recordBlockId(pos, id, cat, 0.99f, "ChunkBlockEntity", maxMemoryChunks.getValue());
                    }
                });
            } else if (pkt instanceof WorldEventS2CPacket we && we.getPos().getY() < 0) {
                store.recordActivity(Vec3d.ofCenter(we.getPos()), "WorldEvent #" + we.getEventId(), 0.85f);
            }
        }

        if (soundParticleInference.getValue()) {
            if (pkt instanceof PlaySoundS2CPacket snd && snd.getY() < 0.0) {
                String soundName = snd.getSound().getKey().map(k -> k.getValue().getPath()).orElse("sound");
                if (isInterestingUndergroundSound(soundName)) {
                    store.recordActivity(new Vec3d(snd.getX(), snd.getY(), snd.getZ()), "Sound: " + soundName, 0.78f);
                }
            } else if (pkt instanceof ParticleS2CPacket part && part.getY() < 0.0) {
                String pName = String.valueOf(Registries.PARTICLE_TYPE.getId(part.getParameters().getType()));
                store.recordActivity(new Vec3d(part.getX(), part.getY(), part.getZ()), "Particle: " + pName, 0.68f);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkDataEvent.Load event) {
        WorldChunk chunk = event.getChunk();
        if (chunk == null || chunk.getBottomY() >= 0) {
            return;
        }
        analyzeChunkSubZeroAsync(chunk);
    }

    private void analyzeChunkSubZeroAsync(WorldChunk chunk) {
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        int bottomY = chunk.getBottomY();
        ChunkSection[] secRef = chunk.getSectionArray();
        int subZeroCount = Math.min(secRef.length, (0 - bottomY) >> 4);
        if (subZeroCount <= 0) {
            return;
        }

        // Snapshot compact 16x16x16 state references on main thread before handing off to worker pool
        BlockState[][][][] snapshot = new BlockState[subZeroCount][][][];
        for (int s = 0; s < subZeroCount; s++) {
            ChunkSection sec = secRef[s];
            if (sec == null || sec.isEmpty()) {
                continue;
            }
            BlockState[][][] cube = new BlockState[16][16][16];
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        cube[y][z][x] = sec.getBlockState(x, y, z);
                    }
                }
            }
            snapshot[s] = cube;
        }

        // Also check heightmap discrepancies & block-light leaks on main thread safely
        if (lightHeightmapInference.getValue() && mc.world != null) {
            int startX = cx << 4;
            int startZ = cz << 4;
            for (int lx = 2; lx < 16; lx += 4) {
                for (int lz = 2; lz < 16; lz += 4) {
                    int motionY = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, lx, lz);
                    if (motionY < 0) {
                        BlockPos shaft = new BlockPos(startX + lx, motionY, startZ + lz);
                        store.recordBlockId(shaft, "heightmap_shaft", UndergroundStore.BlockCategory.CAVITY_LIGHT, 0.82f, "Heightmap", maxMemoryChunks.getValue());
                    }
                    for (int y = bottomY + 4; y < 0; y += 6) {
                        BlockPos bp = new BlockPos(startX + lx, y, startZ + lz);
                        int bl = mc.world.getLightLevel(LightType.BLOCK, bp);
                        if (bl >= 7) {
                            store.recordBlockId(bp, "lit_cavity_L" + bl, UndergroundStore.BlockCategory.CAVITY_LIGHT, 0.85f, "BlockLight", maxMemoryChunks.getValue());
                        }
                    }
                }
            }
        }

        boolean useAntiDecoy = antiDecoyFilter.getValue();
        int cap = maxMemoryChunks.getValue();

        DGClient.getInstance().getScannerPool().submit(() -> {
            int totalNonAir = 0;
            int totalOres = 0;
            for (int s = 0; s < subZeroCount; s++) {
                BlockState[][][] cube = snapshot[s];
                if (cube == null) continue;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState st = cube[y][z][x];
                            if (!st.isAir()) {
                                totalNonAir++;
                                if (classifyBlock(st) == UndergroundStore.BlockCategory.ORE) {
                                    totalOres++;
                                }
                            }
                        }
                    }
                }
            }

            UndergroundStore.ChunkRecord rec = store.getOrCreateChunk(cx, cz);
            if (totalNonAir == 0) {
                rec.obfuscationMode = UndergroundStore.ObfuscationMode.ALL_AIR;
                return;
            }
            boolean decoyFlood = totalOres > totalNonAir * 0.085;
            if (decoyFlood) {
                rec.obfuscationMode = UndergroundStore.ObfuscationMode.RANDOM_ORE_DECOYS;
            } else if (rec.obfuscationMode == UndergroundStore.ObfuscationMode.UNKNOWN) {
                rec.obfuscationMode = UndergroundStore.ObfuscationMode.UNMASKED;
            }

            int baseBlockX = cx << 4;
            int baseBlockZ = cz << 4;
            for (int s = 0; s < subZeroCount; s++) {
                BlockState[][][] cube = snapshot[s];
                if (cube == null) continue;
                int secBaseY = bottomY + (s << 4);
                for (int y = 0; y < 16; y++) {
                    int wy = secBaseY + y;
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState st = cube[y][z][x];
                            UndergroundStore.BlockCategory cat = classifyBlock(st);
                            if (cat == null) continue;

                            boolean exposedToAir = hasAdjacentAir(cube, x, y, z);
                            float conf = evaluateConfidence(st.getBlock(), cat, wy, exposedToAir, decoyFlood, useAntiDecoy);
                            if (conf >= 0.40f) {
                                store.recordBlock(new BlockPos(baseBlockX + x, wy, baseBlockZ + z),
                                        st.getBlock(), cat, conf, exposedToAir ? "ExposedSection" : "ChunkSection", cap);
                            }
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (!updateTriggers.getValue() || mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (mc.player.getY() > 6.0) {
            return; // Only useful when near or below Y=0
        }
        int interval = safeMode.getValue() ? Math.max(12, triggerIntervalTicks.getValue()) : triggerIntervalTicks.getValue();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        // Safe adjacent exposed block query within normal 4.0-block reach
        BlockPos eye = BlockPos.ofFloored(mc.player.getEyePos());
        int[][] offsets = {{2, -1, 0}, {-2, -1, 0}, {0, -1, 2}, {0, -1, -2}, {0, -2, 0}, {1, -2, 1}};
        int[] off = offsets[probeCursor++ % offsets.length];
        BlockPos target = eye.add(off[0], off[1], off[2]);
        if (target.getY() < 0 && target.getSquaredDistance(mc.player.getEyePos()) <= 16.0) {
            // Only query blocks with at least one air neighbor in safe mode
            boolean airNeighbor = false;
            for (Direction d : Direction.values()) {
                if (mc.world.getBlockState(target.offset(d)).isAir()) {
                    airNeighbor = true;
                    break;
                }
            }
            if (airNeighbor || !safeMode.getValue()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, target, Direction.UP));
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        float minConf = confidenceThreshold.getValue().floatValue();
        int yLow = minY.getValue();
        int yHigh = maxY.getValue();
        int rendered = 0;

        for (UndergroundStore.ChunkRecord cr : store.getAllChunks()) {
            double chunkDistSq = mc.player.squaredDistanceTo((cr.chunkX << 4) + 8, mc.player.getY(), (cr.chunkZ << 4) + 8);
            if (chunkDistSq > 160.0 * 160.0) {
                continue;
            }
            for (UndergroundStore.KnownBlock kb : cr.knownBlocks.values()) {
                if (rendered >= 260) break;
                if (kb.confidence() < minConf) continue;
                int y = kb.pos().getY();
                if (y < yLow || y > yHigh) continue;
                if (!passesFilter(kb.category())) continue;

                Box box = new Box(kb.pos()).contract(0.06);
                if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.3))) {
                    continue;
                }
                int baseCol = switch (kb.category()) {
                    case STORAGE -> ColorUtil.PURE_WHITE;
                    case ORE -> ColorUtil.NEON_GREEN;
                    case REDSTONE, MANMADE -> ColorUtil.MINT_GREEN;
                    case CAVITY_LIGHT -> ColorUtil.SOFT_WHITE;
                };
                int fillAlpha = Math.round(52.0f * kb.confidence());
                int edgeCol = ColorUtil.withAlpha(baseCol, Math.round(225.0f * kb.confidence()));
                Render3DUtil.drawFilledBox(event.getMatrices(), event.getCamera(), box, ColorUtil.withAlpha(baseCol, fillAlpha), true);
                Render3DUtil.drawBoxOutline(event.getMatrices(), event.getCamera(), box, 1.6f, edgeCol, true);
                rendered++;
            }
        }

        if (showActivity.getValue()) {
            for (UndergroundStore.ActivityMarker m : store.getActiveMarkers()) {
                Box markerBox = new Box(m.pos().x - 0.35, m.pos().y - 0.35, m.pos().z - 0.35,
                        m.pos().x + 0.35, m.pos().y + 0.35, m.pos().z + 0.35);
                Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), markerBox,
                        ColorUtil.NEON_GREEN, ColorUtil.PURE_WHITE, m.confidence(), true);
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(), m.pos().add(0, 0.8, 0),
                        m.label(), ColorUtil.PURE_WHITE, ColorUtil.NEON_GREEN, 0.85f, true);
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        int cx = mc.player.getChunkPos().x;
        int cz = mc.player.getChunkPos().z;
        UndergroundStore.ChunkRecord rec = store.getChunk(cx, cz);
        String status;
        if (rec == null) {
            status = "Y<0 Status: No data yet for chunk [" + cx + ", " + cz + "]";
        } else if (!rec.hasRecoveredData() && (rec.obfuscationMode == UndergroundStore.ObfuscationMode.ALL_AIR
                || rec.obfuscationMode == UndergroundStore.ObfuscationMode.SOLID_DEEPSLATE
                || rec.obfuscationMode == UndergroundStore.ObfuscationMode.MISSING_SECTIONS)) {
            status = "Y<0 Status: No data (" + rec.obfuscationMode.getLabel() + ")";
        } else {
            status = String.format(Locale.ROOT, "Y<0 Chunk [%d,%d]: %d verified blocks (%s)",
                    cx, cz, rec.knownBlocks.size(), rec.obfuscationMode.getLabel());
        }

        float w = DGFontRenderer.getWidth(status, false, 0.8f) + 14.0f;
        float x = (event.getScreenWidth() - w) * 0.5f;
        float y = event.getScreenHeight() - 48.0f;
        Render2DUtil.drawRoundedRect(event.getContext().getMatrices(), x, y, w, 14.0f, 4.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(event.getContext().getMatrices(), x, y, w, 14.0f, 4.0f, 0.9f, ColorUtil.BORDER_SUBTLE);
        DGFontRenderer.drawScaledText(event.getContext(), status, x + 7.0f, y + 3.5f,
                status.contains("No data") ? ColorUtil.SOFT_WHITE : ColorUtil.MINT_GREEN, false, 0.8f, false);
    }

    private boolean passesFilter(UndergroundStore.BlockCategory cat) {
        return switch (cat) {
            case STORAGE -> showStorage.getValue();
            case ORE -> showOres.getValue();
            case REDSTONE, MANMADE, CAVITY_LIGHT -> showStructures.getValue();
        };
    }

    private static boolean hasAdjacentAir(BlockState[][][] cube, int x, int y, int z) {
        return (x > 0 && cube[y][z][x - 1].isAir())
                || (x < 15 && cube[y][z][x + 1].isAir())
                || (y > 0 && cube[y - 1][z][x].isAir())
                || (y < 15 && cube[y + 1][z][x].isAir())
                || (z > 0 && cube[y][z - 1][x].isAir())
                || (z < 15 && cube[y][z + 1][x].isAir());
    }

    private static float evaluateConfidence(
            Block b,
            UndergroundStore.BlockCategory cat,
            int worldY,
            boolean exposedToAir,
            boolean decoyFlood,
            boolean useAntiDecoy
    ) {
        if (!useAntiDecoy) {
            return decoyFlood ? 0.35f : 0.75f;
        }
        // Impossible sub-zero vanilla generation checks for decoy ores
        if ((b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE || b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)
                && worldY < -16) {
            return 0.10f; // Vanilla coal/emerald does not generate deep at Y < -16
        }
        if (decoyFlood && cat == UndergroundStore.BlockCategory.ORE) {
            return exposedToAir ? 0.62f : 0.12f;
        }
        if (cat == UndergroundStore.BlockCategory.STORAGE || cat == UndergroundStore.BlockCategory.MANMADE) {
            return decoyFlood && !exposedToAir ? 0.30f : 0.88f;
        }
        return exposedToAir ? 0.86f : 0.68f;
    }

    private static UndergroundStore.BlockCategory classifyBlock(BlockState st) {
        if (st == null || st.isAir()) {
            return null;
        }
        Block b = st.getBlock();
        if (b instanceof ChestBlock || b instanceof ShulkerBoxBlock || b == Blocks.BARREL
                || b == Blocks.ENDER_CHEST || b == Blocks.HOPPER) {
            return UndergroundStore.BlockCategory.STORAGE;
        }
        if (st.isIn(BlockTags.DIAMOND_ORES) || st.isIn(BlockTags.GOLD_ORES) || st.isIn(BlockTags.IRON_ORES)
                || st.isIn(BlockTags.REDSTONE_ORES) || st.isIn(BlockTags.LAPIS_ORES) || st.isIn(BlockTags.EMERALD_ORES)
                || b == Blocks.ANCIENT_DEBRIS) {
            return UndergroundStore.BlockCategory.ORE;
        }
        if (b == Blocks.REDSTONE_WIRE || b == Blocks.REPEATER || b == Blocks.COMPARATOR
                || b == Blocks.PISTON || b == Blocks.STICKY_PISTON || b == Blocks.OBSERVER) {
            return UndergroundStore.BlockCategory.REDSTONE;
        }
        if (st.isIn(BlockTags.PLANKS) || b == Blocks.COBBLESTONE || b == Blocks.STONE_BRICKS
                || b == Blocks.OBSIDIAN || b == Blocks.GLASS || b == Blocks.TORCH || b == Blocks.LANTERN) {
            return UndergroundStore.BlockCategory.MANMADE;
        }
        return null;
    }

    private static boolean isStorageId(String id) {
        return id.contains("chest") || id.contains("shulker") || id.contains("barrel") || id.contains("hopper");
    }

    private static boolean isInterestingUndergroundSound(String path) {
        return path.contains("piston") || path.contains("chest") || path.contains("barrel")
                || path.contains("shulker") || path.contains("door") || path.contains("minecart")
                || path.contains("explode") || path.contains("lever") || path.contains("button");
    }
}
