package dev.aether.module.impl;

import java.util.ArrayDeque;
import java.util.Arrays;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AmethystBypassModule extends Module {
    private static final int COLOR = -3636481;
    private static final float LINE_WIDTH = 1.5F;
    private static final int RESCAN_TICKS = 20;
    private static final long BUDGET_NANOS = 2000000L;
    private static final String ANBS_SCAN = "ANBS+ Scan";
    private static final int MAX_GLOW_MARKS = 200;
    private static final int FULL_REFRESH_TICKS = 600;
    private final ModeSetting method = this.addSetting(new ModeSetting("Method", "Bypass", "Bypass", "Light", "ANBS+ Scan"));
    private final SliderSetting minCells = this.addSetting(new SliderSetting("Min Cells", 12.0, 1.0, 100.0, 1.0F));
    private final BooleanSetting tracer = this.addSetting(new BooleanSetting("Tracer", false));
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
    private final LongOpenHashSet found = new LongOpenHashSet();
    private final LongOpenHashSet bypassScanned = new LongOpenHashSet();
    private final Long2ObjectOpenHashMap<BlockPos> bypassMarkers = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<long[]> chunkHits = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet lightScanned = new LongOpenHashSet();
    private final LongOpenHashSet chunkFound = new LongOpenHashSet();
    private boolean hitsChanged;
    private int sinceRefresh;
    private final GeodeGlowScan glow = new GeodeGlowScan();
    private final LongArrayList glowCells = new LongArrayList();
    private long[] hits = new long[0];
    private int tick;

    public AmethystBypassModule() {
        super("amethyst-bypass", "Shows amethyst geodes hidden by anti-xray.", Category.EXPLOIT);
        this.minCells.visibleWhen(() -> this.method.is("ANBS+ Scan"));
    }

    @Override
    protected void onEnable() {
        this.restart();
    }

    @Override
    protected void onDisable() {
        this.restart();
    }

    private void restart() {
        this.queue.clear();
        this.found.clear();
        this.bypassScanned.clear();
        this.bypassMarkers.clear();
        this.chunkHits.clear();
        this.lightScanned.clear();
        this.hitsChanged = false;
        this.sinceRefresh = 0;
        this.hits = new long[0];
        this.tick = 0;
    }

//     @EventHandler(stage = Stage.POST)
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null && mc.player != null) {
            boolean bypass = this.method.is("Bypass");
            if (this.queue.isEmpty()) {
                if (++this.tick < 20) {
                    return;
                }

                this.tick = 0;
                ChunkPos center = mc.player.chunkPosition();
                int radius = mc.options.renderDistance().get();
                if (bypass) {
                    this.bypassScanned.removeIf(keyx -> !inRange(keyx, center, radius) || !LevelUtil.isLoaded(level, ChunkPos.getX(keyx), ChunkPos.getZ(keyx)));
                    this.bypassMarkers.keySet().removeIf(keyx -> !this.bypassScanned.contains(keyx));

                    for (int x = center.x - radius; x <= center.x + radius; x++) {
                        for (int z = center.z - radius; z <= center.z + radius; z++) {
                            long key = ChunkPos.asLong(x, z);
                            if (LevelUtil.isLoaded(level, x, z) && !this.bypassScanned.contains(key)) {
                                this.queue.add(new ChunkPos(x, z));
                            }
                        }
                    }
                } else {
                    if (++this.sinceRefresh * 20 >= 600) {
                        this.sinceRefresh = 0;
                        this.lightScanned.clear();
                    }

                    this.hitsChanged = this.hitsChanged
                        | this.chunkHits
                            .keySet()
                            .removeIf(keyx -> !inRange(keyx, center, radius) || !LevelUtil.isLoaded(level, ChunkPos.getX(keyx), ChunkPos.getZ(keyx)));
                    this.lightScanned.removeIf(keyx -> !inRange(keyx, center, radius));

                    for (int x = center.x - radius; x <= center.x + radius; x++) {
                        for (int z = center.z - radius; z <= center.z + radius; z++) {
                            if (LevelUtil.isLoaded(level, x, z) && !this.lightScanned.contains(ChunkPos.asLong(x, z))) {
                                this.queue.add(new ChunkPos(x, z));
                            }
                        }
                    }
                }
            }

            boolean optimized = PerformanceModule.optimizationsEnabled();
            long started = System.nanoTime();
            long deadline = optimized ? PerformanceBudget.acquire(2000000L) : started + 2000000L;

            while (!this.queue.isEmpty() && System.nanoTime() < deadline) {
                ChunkPos pos = this.queue.poll();
                if (LevelUtil.isLoaded(level, pos.x, pos.z)) {
                    LevelChunk chunk = level.getChunk(pos.x, pos.z);
                    if (bypass) {
                        collectPaletteLeak(chunk, this.bypassMarkers);
                        this.bypassScanned.add(pos.toLong());
                    } else {
                        this.chunkFound.clear();
                        if (this.method.is("ANBS+ Scan")) {
                            this.collectGlow(level, chunk, this.chunkFound);
                        } else {
                            AmethystLight.scanSources(level, chunk, (packed, light) -> this.chunkFound.add(packed));
                        }

                        long key = pos.toLong();
                        long[] previous = this.chunkHits.get(key);
                        long[] now = this.chunkFound.toLongArray();
                        if (previous != null && now.length < previous.length) {
                            this.lightScanned.add(key);
                        } else {
                            if (now.length == 0) {
                                if (this.chunkHits.remove(key) != null) {
                                    this.hitsChanged = true;
                                }
                            } else if (previous == null || !Arrays.equals(previous, now)) {
                                this.chunkHits.put(key, now);
                                this.hitsChanged = true;
                            }

                            this.lightScanned.add(key);
                        }
                    }
                }
            }

            if (optimized) {
                PerformanceBudget.record(started);
            }

            if (bypass) {
                if (this.queue.isEmpty()) {
                    this.hits = markersToLongArray(this.bypassMarkers);
                }
            } else if (this.hitsChanged) {
                this.hitsChanged = false;
                this.found.clear();

                for (long[] chunkMarks : this.chunkHits.values()) {
                    for (long mark : chunkMarks) {
                        this.found.add(mark);
                    }
                }

                this.hits = this.found.toLongArray();
            }
        }
    }

    private void collectGlow(ClientLevel level, LevelChunk chunk, LongOpenHashSet out) {
        this.glowCells.clear();
        if (this.glow.scan(level, chunk, this.glowCells::add) >= this.minCells.getInt()) {
            int marks = Math.min(this.glowCells.size(), 200);

            for (int i = 0; i < marks; i++) {
                out.add(this.glowCells.getLong(i));
            }
        }
    }

    private static boolean inRange(long chunkKey, ChunkPos center, int radius) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        return Math.abs(x - center.x) <= radius && Math.abs(z - center.z) <= radius;
    }

    private static long[] markersToLongArray(Long2ObjectOpenHashMap<BlockPos> markers) {
        long[] out = new long[markers.size()];
        int i = 0;

        for (BlockPos marker : markers.values()) {
            out[i++] = marker.asLong();
        }

        return out;
    }

//     @EventHandler
//     public void onPacketApplied(PacketAppliedEvent event) {
        if (!this.method.is("Bypass")) {
            if (event.packet instanceof ClientboundLevelChunkWithLightPacket chunk) {
                this.dirtyAround(chunk.getX(), chunk.getZ());
            } else if (event.packet instanceof ClientboundLightUpdatePacket light) {
                this.dirtyAround(light.getX(), light.getZ());
            } else if (event.packet instanceof ClientboundBlockUpdatePacket update) {
                this.dirtyAround(update.getPos().getX() >> 4, update.getPos().getZ() >> 4);
            } else if (event.packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
                sectionUpdate.runUpdates((pos, state) -> this.dirtyAround(pos.getX() >> 4, pos.getZ() >> 4));
            }
        } else {
            if (event.packet instanceof ClientboundLevelChunkWithLightPacket chunk) {
                this.dirtyBypassChunk(chunk.getX(), chunk.getZ());
            } else if (event.packet instanceof ClientboundBlockUpdatePacket update) {
                this.dirtyBypassChunk(update.getPos().getX() >> 4, update.getPos().getZ() >> 4);
            } else if (event.packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
                sectionUpdate.runUpdates((pos, state) -> this.dirtyBypassChunk(pos.getX() >> 4, pos.getZ() >> 4));
            }
        }
    }

    private void dirtyAround(int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                this.lightScanned.remove(ChunkPos.asLong(chunkX + dx, chunkZ + dz));
            }
        }
    }

    private void dirtyBypassChunk(int chunkX, int chunkZ) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (this.bypassScanned.remove(key)) {
            this.bypassMarkers.remove(key);
        }
    }

    private static void collectPaletteLeak(LevelChunk chunk, Long2ObjectOpenHashMap<BlockPos> out) {
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        ChunkPos pos = chunk.getPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section != null) {
                PalettedContainer<BlockState> states = section.getStates();
                if (!states.maybeHas(state -> false) && states.maybeHas(AmethystBypassModule::isBud)) {
                    int baseY = (minSectionY + i) * 16;
                    BlockPos marker = pickMarker(chunk, section, baseY, pos);
                    out.put(pos.toLong(), marker != null ? marker : new BlockPos(pos.getMinBlockX() + 8, baseY + 8, pos.getMinBlockZ() + 8));
                    return;
                }
            }
        }
    }

    private static BlockPos pickMarker(LevelChunk chunk, LevelChunkSection section, int baseY, ChunkPos pos) {
        MutableBlockPos neighbour = new MutableBlockPos();
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int y = 0; y < 16; y++) {
            int worldY = baseY + y;

            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (section.getBlockState(x, y, z).isAir()) {
                        int worldX = pos.getMinBlockX() + x;
                        int worldZ = pos.getMinBlockZ() + z;
                        boolean touchesGeode = false;

                        for (Direction direction : Direction.values()) {
                            int nx = worldX + direction.getStepX();
                            int nz = worldZ + direction.getStepZ();
                            if (nx >> 4 == pos.x && nz >> 4 == pos.z) {
                                neighbour.set(nx, worldY + direction.getStepY(), nz);
                                Block block = chunk.getBlockState(neighbour).getBlock();
                                if (block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST) {
                                    touchesGeode = true;
                                    break;
                                }
                            }
                        }

                        if (touchesGeode) {
                            int score = scatter(worldX, worldY, worldZ);
                            if (score < bestScore) {
                                bestScore = score;
                                best = new BlockPos(worldX, worldY, worldZ);
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    static int scatter(int x, int y, int z) {
        long h = x * -7046029254386353131L ^ y * -4417276706812531889L ^ z * 1609587929392839161L;
        h ^= h >>> 29;
        h *= -4658895280553007687L;
        h ^= h >>> 32;
        return Math.floorMod(h, 100);
    }

    private static boolean isBud(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SMALL_AMETHYST_BUD || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.LARGE_AMETHYST_BUD;
    }

    private static boolean isGeode(BlockState state) {
        return state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST);
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        long[] current = this.hits;
        if (level != null && current.length != 0) {
            GizmoStyle style = GizmoStyle.strokeAndFill(-3636481, 1.5F, -3636481);
            boolean drawTracers = this.tracer.get();

            for (long packed : current) {
                BlockPos pos = BlockPos.of(packed);
                if (LevelUtil.isLoaded(level, pos.getX() >> 4, pos.getZ() >> 4)) {
                    AABB box = new AABB(pos);
                    if (event.frustum == null || event.frustum.isVisible(box)) {
                        Gizmos.cuboid(box, style).setAlwaysOnTop();
                    }

                    if (drawTracers) {
                        TracerEngine.draw(Vec3.atCenterOf(pos), -3636481);
                    }
                }
            }
        }
    }
}
