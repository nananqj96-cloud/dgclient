package dev.aether.module.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HoleEspModule extends Module {
    private static final String MODE_ALL = "All";
    private static final String MODE_HOLES_TUNNELS = "Holes + Tunnels";
    private static final String MODE_HOLES_STAIRS = "Holes + Stairs";
    private static final String MODE_TUNNELS_STAIRS = "Tunnels + Stairs";
    private static final String MODE_HOLES = "Holes";
    private static final String MODE_TUNNELS = "Tunnels";
    private static final String MODE_STAIRS = "Stairs";
    private static final int TYPE_HOLE = 0;
    private static final int TYPE_HOLE_3X1 = 1;
    private static final int TYPE_TUNNEL = 2;
    private static final int TYPE_STAIR = 3;
    private static final int MAX_HITS_PER_CHUNK = 1024;
    private static final int MAX_RENDER_BOXES = 8192;
    private static final int HOLE_GRID_W = 20;
    private static final int ELYTRA_REFRESH_TICKS = 5;
    private static final int NORMAL_REFRESH_TICKS = 20;
    private static final float ELYTRA_SCAN_RADIUS = 12.0F;
    private static final float ELYTRA_SCAN_BUDGET_MS = 8.0F;
    private static final int[][] HORIZONTAL_DIRS = new int[][]{{1, 0}, {-1, 0}, {0, -1}, {0, 1}};
    private static final ThreadLocal<MutableBlockPos> PASSABLE_POS = ThreadLocal.withInitial(MutableBlockPos::new);
    private final ConcurrentHashMap<Long, List<HoleEspModule.Hit>> chunkCache = new ConcurrentHashMap<>();
    private final Queue<Long> scanQueue = new ConcurrentLinkedQueue<>();
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    private long lastPlayerChunk = Long.MIN_VALUE;
    private int refreshTicks = 0;
    private ClientLevel sessionLevel;
    private boolean lastOptimized;
    private HoleEspModule.FastHoleScan activeScan;
    private boolean[] passableBuffer = new boolean[0];
    private final ModeSetting sMode = this.addSetting(
        "detection", new ModeSetting("Detection", "Holes", "Holes", "All", "Holes + Tunnels", "Holes + Stairs", "Tunnels + Stairs", "Tunnels", "Stairs")
    );
    private final SliderSetting sScanRadius = this.addSetting(new SliderSetting("Scan Radius", 8.0, 1.0, 24.0, 1.0F));
    private final SliderSetting sChunksPerTick = this.addSetting(new SliderSetting("Chunks/Tick", 64.0, 1.0, 100.0, 1.0F));
    private final SliderSetting sScanBudgetMs = this.addSetting(new SliderSetting("Scan Budget ms", 3.0, 1.0, 10.0, 1.0F));
    private final BooleanSetting sAirOnly = this.addSetting(new BooleanSetting("Only Air Blocks", true));
    private final SliderSetting sMinYOffset = this.addSetting(new SliderSetting("Min Y Offset", 0.0, 0.0, 319.0, 1.0F));
    private final SliderSetting sMaxYOffset = this.addSetting(new SliderSetting("Max Y Offset", 0.0, 0.0, 319.0, 1.0F));
    private final SliderSetting sMinHoleDepth = this.addSetting(new SliderSetting("Min Hole Depth", 4.0, 1.0, 20.0, 1.0F));
    private final SliderSetting sMinTunnelLength = this.addSetting(new SliderSetting("Min Tunnel Length", 3.0, 1.0, 20.0, 1.0F));
    private final SliderSetting sMinTunnelHeight = this.addSetting(new SliderSetting("Min Tunnel Height", 2.0, 1.0, 10.0, 1.0F));
    private final SliderSetting sMaxTunnelHeight = this.addSetting(new SliderSetting("Max Tunnel Height", 3.0, 2.0, 10.0, 1.0F));
    private final SliderSetting sMinStairLength = this.addSetting(new SliderSetting("Min Stair Length", 3.0, 1.0, 20.0, 1.0F));
    private final SliderSetting sMinStairHeight = this.addSetting(new SliderSetting("Min Stair Height", 3.0, 2.0, 10.0, 1.0F));
    private final SliderSetting sMaxStairHeight = this.addSetting(new SliderSetting("Max Stair Height", 5.0, 2.0, 10.0, 1.0F));
    private final BooleanSetting sFill = this.addSetting(new BooleanSetting("Fill", true));
    private final BooleanSetting sOutline = this.addSetting(new BooleanSetting("Outline", true));
    private final SliderSetting sLineWidth = this.addSetting(new SliderSetting("Line Width", 1.5, 0.5, 5.0, 0.5F));
    private final ColorSetting sHoleLine = this.addSetting(new ColorSetting("1x1 Line", 1291845631));
    private final ColorSetting sHoleFill = this.addSetting(new ColorSetting("1x1 Fill", 1342177279));
    private final ColorSetting sHole3Line = this.addSetting(new ColorSetting("3x1 Line", 644245094));
    private final ColorSetting sHole3Fill = this.addSetting(new ColorSetting("3x1 Fill", 644245094));
    private final ColorSetting sTunnelLine = this.addSetting(new ColorSetting("Tunnel Line", 1593835775));
    private final ColorSetting sTunnelFill = this.addSetting(new ColorSetting("Tunnel Fill", 503316735));
    private final ColorSetting sStairLine = this.addSetting(new ColorSetting("Stair Line", 1610547455));
    private final ColorSetting sStairFill = this.addSetting(new ColorSetting("Stair Fill", 520028415));

    public HoleEspModule() {
        super("hole-esp", "Highlights holes, tunnels, and staircases.", Category.RENDER);
        this.sAirOnly.visibleWhen(() -> this.currentMode().tunnels() || this.currentMode().stairs());
        List.of(
                this.sMode,
                this.sScanRadius,
                this.sAirOnly,
                this.sMinYOffset,
                this.sMaxYOffset,
                this.sMinHoleDepth,
                this.sMinTunnelLength,
                this.sMinTunnelHeight,
                this.sMaxTunnelHeight,
                this.sMinStairLength,
                this.sMinStairHeight,
                this.sMaxStairHeight
            )
            .forEach(setting -> setting.onChange(this::resetScan));
    }

    @Override
    protected void onEnable() {
        this.resetScan();
    }

    private void resetScan() {
        this.lastPlayerChunk = Long.MIN_VALUE;
        this.refreshTicks = 0;
        this.chunkCache.clear();
        this.scanQueue.clear();
        this.queued.clear();
        this.activeScan = null;
    }

    @Override
    protected void onDisable() {
        this.resetScan();
        this.sessionLevel = null;
        this.passableBuffer = new boolean[0];
    }

//     @EventHandler
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        boolean optimized = PerformanceModule.optimizationsEnabled();
        if (this.lastOptimized != optimized || this.sessionLevel != mc.level) {
            this.resetScan();
        }

        this.lastOptimized = optimized;
        this.sessionLevel = mc.level;
        if (mc.level != null && mc.player != null) {
            int cx = mc.player.getBlockX() >> 4;
            int cz = mc.player.getBlockZ() >> 4;
            long curChunk = ChunkPos.asLong(cx, cz);
            int radius = this.scanRadius(mc);
            boolean fastTravel = isFastTravel(mc);
            boolean periodicRefresh = ++this.refreshTicks >= (fastTravel ? 5 : 20);
            if (curChunk != this.lastPlayerChunk || periodicRefresh) {
                this.refreshTicks = 0;
                this.lastPlayerChunk = curChunk;
                this.chunkCache.keySet().removeIf(keyx -> {
                    int kx = chunkX(keyx);
                    int kz = chunkZ(keyx);
                    return Math.abs(kx - cx) > radius || Math.abs(kz - cz) > radius;
                });
                List<long[]> toQueue = new ArrayList<>();
                Vec3 velocity = mc.player.getDeltaMovement();
                double vx = velocity.x;
                double vz = velocity.z;
                double horizontalSpeed = Math.sqrt(vx * vx + vz * vz);
                if (horizontalSpeed > 1.0E-4) {
                    vx /= horizontalSpeed;
                    vz /= horizontalSpeed;
                }

                for (int x = cx - radius; x <= cx + radius; x++) {
                    for (int z = cz - radius; z <= cz + radius; z++) {
                        long key = ChunkPos.asLong(x, z);
                        if (!this.chunkCache.containsKey(key)
                            && !this.queued.contains(key)
                            && (this.activeScan == null || this.activeScan.key != key)
                            && mc.level.getChunkSource().hasChunk(x, z)) {
                            int dx = x - cx;
                            int dz = z - cz;
                            long distScore = (dx * dx + dz * dz) * 1024L;
                            long aheadScore = fastTravel ? Math.round((dx * vx + dz * vz) * 1536.0) : 0L;
                            toQueue.add(new long[]{key, distScore - aheadScore});
                        }
                    }
                }

                toQueue.sort(Comparator.comparingLong(a -> a[1]));

                for (long[] entry : toQueue) {
                    this.scanQueue.add(entry[0]);
                    this.queued.add(entry[0]);
                }
            }

            int chunksPerTick = Math.max(1, Math.round(value(this.sChunksPerTick, 4.0F)));
            float budgetMs = fastTravel && !optimized ? Math.max(value(this.sScanBudgetMs, 3.0F), 8.0F) : value(this.sScanBudgetMs, 3.0F);
            long requested = (long)(Math.max(1.0F, budgetMs) * 1000000.0F);
            long deadline = optimized ? PerformanceBudget.acquire(requested) : System.nanoTime() + requested;
            long started = System.nanoTime();
            if (started < deadline) {
                try {
                    int i = 0;

                    while (i < chunksPerTick && System.nanoTime() < deadline) {
                        if (this.activeScan != null) {
                            if (!this.activeScan.validFor(mc.level, cx, cz, radius)) {
                                int sx = this.activeScan.chunk.getPos().x;
                                int sz = this.activeScan.chunk.getPos().z;
                                long key = this.activeScan.key;
                                this.activeScan = null;
                                if (Math.abs((long)sx - cx) <= radius
                                    && Math.abs((long)sz - cz) <= radius
                                    && mc.level.getChunkSource().hasChunk(sx, sz)
                                    && this.queued.add(key)) {
                                    this.scanQueue.add(key);
                                }
                            } else if (this.activeScan) {
                                this.storeHits(this.activeScan.key, this.activeScan.hits);
                                this.activeScan = null;
                                i++;
                            }
                        } else {
                            Long key = this.scanQueue.poll();
                            if (key == null) {
                                break;
                            }

                            this.queued.remove(key);
                            int sx = chunkX(key);
                            int sz = chunkZ(key);
                            if (mc.level.getChunkSource().hasChunk(sx, sz)) {
                                this.scanChunk(mc.level, sx, sz, key);
                            }

                            if (this.activeScan == null) {
                                i++;
                            }
                        }
                    }
                } finally {
                    if (optimized) {
                        PerformanceBudget.record(started);
                    }
                }
            }
        }
    }

//     @EventHandler
//     public void onPacketApplied(PacketAppliedEvent event) {
        if (event.packet instanceof ClientboundLevelChunkWithLightPacket chunk) {
            this.invalidateAround(chunk.getX(), chunk.getZ());
        }
    }

    private void invalidateAround(int cx, int cz) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int radius = this.scanRadius(mc) + 1;
            if (Math.abs((long)cx - (mc.player.getBlockX() >> 4)) <= radius && Math.abs((long)cz - (mc.player.getBlockZ() >> 4)) <= radius) {
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = cz - 1; z <= cz + 1; z++) {
                        long key = ChunkPos.asLong(x, z);
                        boolean known = this.chunkCache.containsKey(key);
                        if (this.activeScan != null && this.activeScan.key == key) {
                            this.activeScan = null;
                            known = true;
                        }

                        if (known && this.queued.add(key)) {
                            this.scanQueue.add(key);
                        }
                    }
                }

                this.lastPlayerChunk = Long.MIN_VALUE;
            }
        }
    }

//     @EventHandler
//     public void onBlockUpdate(BlockUpdateEvent event) {
        if (event.pos != null) {
            this.invalidateAround(event.pos.getX() >> 4, event.pos.getZ() >> 4);
        }
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean fill = this.sFill == null || this.sFill.value().get();
            boolean outline = this.sOutline == null || this.sOutline.value().get();
            if (fill || outline) {
                int px = mc.player.getBlockX() >> 4;
                int pz = mc.player.getBlockZ() >> 4;
                int radius = this.scanRadius(mc);
                float lineWidth = value(this.sLineWidth, 1.5F);
                int rendered = 0;

                for (Entry<Long, List<HoleEspModule.Hit>> entry : this.chunkCache.entrySet()) {
                    long key = entry.getKey();
                    if (Math.abs(chunkX(key) - px) <= radius && Math.abs(chunkZ(key) - pz) <= radius) {
                        for (HoleEspModule.Hit hit : entry.getValue()) {
                            if (++rendered > 8192) {
                                return;
                            }

                            GizmoStyle style = !fill
                                ? GizmoStyle.stroke(this.lineColor(hit.type()), lineWidth)
                                : (
                                    !outline
                                        ? GizmoStyle.fill(this.fillColor(hit.type()))
                                        : GizmoStyle.strokeAndFill(this.lineColor(hit.type()), lineWidth, this.fillColor(hit.type()))
                                );
                            Gizmos.cuboid(hit.box(), style).setAlwaysOnTop();
                        }
                    }
                }
            }
        }
    }

    private void scanChunk(ClientLevel level, int chunkX, int chunkZ, long chunkKey) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk != null) {
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;
            int minBuildY = level.dimensionType().minY();
            int maxBuildY = minBuildY + chunk.getSections().length * 16;
            int minY = Math.min(maxBuildY - 1, minBuildY + Math.round(value(this.sMinYOffset, 0.0F)));
            int maxY = Math.max(minY + 1, maxBuildY - Math.round(value(this.sMaxYOffset, 0.0F)));
            HoleEspModule.Mode mode = this.currentMode();
            boolean airOnly = this.sAirOnly != null && this.sAirOnly.value().get();
            if (mode.holes() && !mode.tunnels() && !mode.stairs()) {
                if (PerformanceModule.optimizationsEnabled()) {
                    this.activeScan = new HoleEspModule.FastHoleScan(chunk, chunkKey, baseX, baseZ, minY, maxY);
                } else {
                    this.storeHits(chunkKey, this.holeHits(level, baseX, baseZ, minY, maxY));
                }
            } else {
                List<HoleEspModule.Hit> hits = mode.holes() ? this.holeHits(level, baseX, baseZ, minY, maxY) : new ArrayList<>();
                Set<String> seen = new HashSet<>();
                MutableBlockPos pos = new MutableBlockPos();
                LevelChunkSection[] sections = chunk.getSections();

                for (int si = 0; si < sections.length; si++) {
                    LevelChunkSection section = sections[si];
                    if (section != null && !section.hasOnlyAir()) {
                        int sectionBaseY = minBuildY + si * 16;

                        for (int ly = 0; ly < 16; ly++) {
                            int y = sectionBaseY + ly;
                            if (y > minY && y < maxY) {
                                for (int z = 0; z < 16; z++) {
                                    for (int x = 0; x < 16; x++) {
                                        int wx = baseX + x;
                                        int wz = baseZ + z;
                                        pos.set(wx, y, wz);
                                        if (isPassable(level, pos, airOnly)) {
                                            if (mode.tunnels()) {
                                                this.checkTunnelStarts(level, pos, airOnly, hits, seen);
                                            }

                                            if (mode.stairs()) {
                                                this.checkStaircaseStarts(level, pos, airOnly, hits, seen);
                                            }

                                            if (hits.size() >= 1024) {
                                                this.chunkCache.put(chunkKey, List.copyOf(hits));
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                this.storeHits(chunkKey, hits);
            }
        }
    }

    private void storeHits(long key, List<HoleEspModule.Hit> hits) {
        this.chunkCache.put(key, List.copyOf(hits));
    }

    private List<HoleEspModule.Hit> holeHits(ClientLevel level, int baseX, int baseZ, int minY, int maxY) {
        List<HoleEspModule.Hit> hits = new ArrayList<>();
        int gridMinY = minY - 1;
        int yCount = maxY - gridMinY + 1;
        if (yCount <= 1) {
            return hits;
        }

        boolean[] open = new boolean[yCount * 20 * 20];
        MutableBlockPos pos = new MutableBlockPos();

        for (int yi = 0; yi < yCount; yi++) {
            int y = gridMinY + yi;
            int yBase = yi * 20 * 20;

            for (int lz = -1; lz <= 18; lz++) {
                int zBase = yBase + (lz + 1) * 20;

                for (int lx = -1; lx <= 18; lx++) {
                    pos.set(baseX + lx, y, baseZ + lz);
                    open[zBase + lx + 1] = isOpen(level, pos);
                }
            }
        }

        int minDepth = Math.round(value(this.sMinHoleDepth, 4.0F));
        Set<String> seen = new HashSet<>();

        for (int column = 0; column < 256 && hits.size() < 1024; column++) {
            shaftsInColumn(open, column & 15, column >> 4, gridMinY, minY, maxY, minDepth, baseX, baseZ, hits, seen);
        }

        return hits;
    }

    private static void shaftsInColumn(
        boolean[] grid, int lx, int lz, int gridMinY, int minY, int maxY, int minDepth, int baseX, int baseZ, List<HoleEspModule.Hit> hits, Set<String> seen
    ) {
        int x = baseX + lx;
        int z = baseZ + lz;
        int single = 0;
        int alongX = 0;
        int alongZ = 0;

        for (int y = minY; y <= maxY; y++) {
            boolean inside = y < maxY;
            if (inside && walled1x1(grid, y, lx, lz, gridMinY)) {
                single++;
            } else {
                single = endShaft(single, y, minDepth, hits, seen, x, z, 1, 1, 0);
            }

            if (inside && walled3x1X(grid, y, lx, lz, gridMinY)) {
                alongX++;
            } else {
                alongX = endShaft(alongX, y, minDepth, hits, seen, x, z, 3, 1, 1);
            }

            if (inside && walled3x1Z(grid, y, lx, lz, gridMinY)) {
                alongZ++;
            } else {
                alongZ = endShaft(alongZ, y, minDepth, hits, seen, x, z, 1, 3, 1);
            }
        }
    }

    private static int endShaft(int run, int top, int minDepth, List<HoleEspModule.Hit> hits, Set<String> seen, int x, int z, int width, int length, int type) {
        if (run >= minDepth) {
            addHit(hits, seen, new AABB(x, top - run, z, x + width, top, z + length), type);
        }

        return 0;
    }

    private static boolean walled1x1(boolean[] grid, int y, int lx, int lz, int gridMinY) {
        return grid(grid, y, lx, lz, gridMinY)
            && !grid(grid, y, lx - 1, lz, gridMinY)
            && !grid(grid, y, lx + 1, lz, gridMinY)
            && !grid(grid, y, lx, lz - 1, gridMinY)
            && !grid(grid, y, lx, lz + 1, gridMinY);
    }

    private static boolean walled3x1X(boolean[] grid, int y, int lx, int lz, int gridMinY) {
        return grid(grid, y, lx, lz, gridMinY)
            && grid(grid, y, lx + 1, lz, gridMinY)
            && grid(grid, y, lx + 2, lz, gridMinY)
            && !grid(grid, y, lx - 1, lz, gridMinY)
            && !grid(grid, y, lx + 3, lz, gridMinY)
            && !grid(grid, y, lx, lz - 1, gridMinY)
            && !grid(grid, y, lx, lz + 1, gridMinY)
            && !grid(grid, y, lx + 1, lz - 1, gridMinY)
            && !grid(grid, y, lx + 1, lz + 1, gridMinY)
            && !grid(grid, y, lx + 2, lz - 1, gridMinY)
            && !grid(grid, y, lx + 2, lz + 1, gridMinY);
    }

    private static boolean walled3x1Z(boolean[] grid, int y, int lx, int lz, int gridMinY) {
        return grid(grid, y, lx, lz, gridMinY)
            && grid(grid, y, lx, lz + 1, gridMinY)
            && grid(grid, y, lx, lz + 2, gridMinY)
            && !grid(grid, y, lx, lz - 1, gridMinY)
            && !grid(grid, y, lx, lz + 3, gridMinY)
            && !grid(grid, y, lx - 1, lz, gridMinY)
            && !grid(grid, y, lx + 1, lz, gridMinY)
            && !grid(grid, y, lx - 1, lz + 1, gridMinY)
            && !grid(grid, y, lx + 1, lz + 1, gridMinY)
            && !grid(grid, y, lx - 1, lz + 2, gridMinY)
            && !grid(grid, y, lx + 1, lz + 2, gridMinY);
    }

    private static boolean grid(boolean[] grid, int y, int lx, int lz, int gridMinY) {
        int yi = y - gridMinY;
        return grid[yi * 20 * 20 + (lz + 1) * 20 + lx + 1];
    }

    private void checkTunnelStarts(ClientLevel level, BlockPos pos, boolean airOnly, List<HoleEspModule.Hit> hits, Set<String> seen) {
        int minLength = Math.round(value(this.sMinTunnelLength, 3.0F));

        for (int[] dir : HORIZONTAL_DIRS) {
            MutableBlockPos previous = new MutableBlockPos(pos.getX() - dir[0], pos.getY(), pos.getZ() - dir[1]);
            if (!this.isTunnelSection(level, previous, dir, airOnly)) {
                MutableBlockPos current = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
                BlockPos start = null;
                BlockPos end = null;
                int steps = 0;
                int maxHeight = 0;
                if (this.isTunnelSection(level, current, dir, airOnly)) {
                    start = current.immutable();
                }

                while (this.isTunnelSection(level, current, dir, airOnly)) {
                    maxHeight = Math.max(maxHeight, this.getTunnelHeight(level, current, airOnly));
                    end = current.immutable();
                    current.move(dir[0], 0, dir[1]);
                    steps++;
                }

                if (start != null && end != null && steps >= minLength) {
                    addHit(
                        hits,
                        seen,
                        new AABB(
                            Math.min(start.getX(), end.getX()),
                            start.getY(),
                            Math.min(start.getZ(), end.getZ()),
                            Math.max(start.getX(), end.getX()) + 1,
                            start.getY() + maxHeight,
                            Math.max(start.getZ(), end.getZ()) + 1
                        ),
                        2
                    );
                }
            }
        }
    }

    private void checkStaircaseStarts(ClientLevel level, BlockPos pos, boolean airOnly, List<HoleEspModule.Hit> hits, Set<String> seen) {
        int minLength = Math.round(value(this.sMinStairLength, 3.0F));

        for (int[] dir : HORIZONTAL_DIRS) {
            MutableBlockPos previous = new MutableBlockPos(pos.getX() - dir[0], pos.getY() - 1, pos.getZ() - dir[1]);
            if (!this.isStaircaseSection(level, previous, dir, airOnly)) {
                MutableBlockPos current = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
                int steps = 0;
                List<AABB> boxes = new ArrayList<>();

                while (this.isStaircaseSection(level, current, dir, airOnly)) {
                    int height = this.getStaircaseHeight(level, current, airOnly);
                    boxes.add(new AABB(current.getX(), current.getY(), current.getZ(), current.getX() + 1, current.getY() + height, current.getZ() + 1));
                    current.move(dir[0], 1, dir[1]);
                    steps++;
                }

                if (steps >= minLength) {
                    for (AABB box : boxes) {
                        addHit(hits, seen, box, 3);
                    }
                }
            }
        }
    }

    private boolean isTunnelSection(ClientLevel level, BlockPos pos, int[] dir, boolean airOnly) {
        int height = this.getTunnelHeight(level, pos, airOnly);
        if (height >= Math.round(value(this.sMinTunnelHeight, 2.0F)) && height <= Math.round(value(this.sMaxTunnelHeight, 3.0F))) {
            if (!isPassable(level, pos.getX(), pos.getY() - 1, pos.getZ(), airOnly) && !isPassable(level, pos.getX(), pos.getY() + height, pos.getZ(), airOnly)
                )
             {
                boolean alongX = dir[0] != 0;

                for (int i = 0; i < height; i++) {
                    int y = pos.getY() + i;
                    if (alongX) {
                        if (isPassable(level, pos.getX(), y, pos.getZ() - 1, airOnly) || isPassable(level, pos.getX(), y, pos.getZ() + 1, airOnly)) {
                            return false;
                        }
                    } else if (isPassable(level, pos.getX() - 1, y, pos.getZ(), airOnly) || isPassable(level, pos.getX() + 1, y, pos.getZ(), airOnly)) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isStaircaseSection(ClientLevel level, BlockPos pos, int[] dir, boolean airOnly) {
        int height = this.getStaircaseHeight(level, pos, airOnly);
        if (height >= Math.round(value(this.sMinStairHeight, 3.0F)) && height <= Math.round(value(this.sMaxStairHeight, 5.0F))) {
            if (!isPassable(level, pos.getX(), pos.getY() - 1, pos.getZ(), airOnly) && !isPassable(level, pos.getX(), pos.getY() + height, pos.getZ(), airOnly)
                )
             {
                boolean alongX = dir[0] != 0;

                for (int i = 0; i < height; i++) {
                    int y = pos.getY() + i;
                    if (alongX) {
                        if (isPassable(level, pos.getX(), y, pos.getZ() - 1, airOnly) || isPassable(level, pos.getX(), y, pos.getZ() + 1, airOnly)) {
                            return false;
                        }
                    } else if (isPassable(level, pos.getX() - 1, y, pos.getZ(), airOnly) || isPassable(level, pos.getX() + 1, y, pos.getZ(), airOnly)) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private int getTunnelHeight(ClientLevel level, BlockPos pos, boolean airOnly) {
        int height = 0;
        int max = Math.round(value(this.sMaxTunnelHeight, 3.0F));

        while (height < max && isPassable(level, pos.getX(), pos.getY() + height, pos.getZ(), airOnly)) {
            height++;
        }

        return height;
    }

    private int getStaircaseHeight(ClientLevel level, BlockPos pos, boolean airOnly) {
        int height = 0;
        int max = Math.round(value(this.sMaxStairHeight, 5.0F));

        while (height < max && isPassable(level, pos.getX(), pos.getY() + height, pos.getZ(), airOnly)) {
            height++;
        }

        return height;
    }

    private static boolean isOpen(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isPassable(ClientLevel level, int x, int y, int z, boolean airOnly) {
        return isPassable(level, PASSABLE_POS.get().set(x, y, z), airOnly);
    }

    private static boolean isPassable(ClientLevel level, BlockPos pos, boolean airOnly) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }

        if (airOnly) {
            return false;
        }

        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || !Shapes.block().equals(shape);
    }

    private static void addHit(List<HoleEspModule.Hit> hits, Set<String> seen, AABB box, int type) {
        String key = type + ":" + box.minX + "," + box.minY + "," + box.minZ + "," + box.maxX + "," + box.maxY + "," + box.maxZ;
        if (seen.add(key)) {
            for (HoleEspModule.Hit hit : hits) {
                if (hit.type() == type && hit.box().intersects(box)) {
                    return;
                }
            }

            hits.add(new HoleEspModule.Hit(box, type));
        }
    }

    private HoleEspModule.Mode currentMode() {
        String mode = this.sMode == null ? "All" : this.sMode.value();

        return switch (mode) {
            case "Holes + Tunnels" -> new HoleEspModule.Mode(true, true, false);
            case "Holes + Stairs" -> new HoleEspModule.Mode(true, false, true);
            case "Tunnels + Stairs" -> new HoleEspModule.Mode(false, true, true);
            case "Holes" -> new HoleEspModule.Mode(true, false, false);
            case "Tunnels" -> new HoleEspModule.Mode(false, true, false);
            case "Stairs" -> new HoleEspModule.Mode(false, false, true);
            default -> new HoleEspModule.Mode(true, true, true);
        };
    }

    private int lineColor(int type) {
        return switch (type) {
            case 1 -> color(this.sHole3Line, 1610589440);
            case 2 -> color(this.sTunnelLine, 1593835775);
            case 3 -> color(this.sStairLine, 1610547455);
            default -> color(this.sHoleLine, 1610547200);
        };
    }

    private int fillColor(int type) {
        return switch (type) {
            case 1 -> color(this.sHole3Fill, 520070400);
            case 2 -> color(this.sTunnelFill, 503316735);
            case 3 -> color(this.sStairFill, 520028415);
            default -> color(this.sHoleFill, 520028160);
        };
    }

    private static int chunkX(long key) {
        return (int)key;
    }

    private static int chunkZ(long key) {
        return (int)(key >> 32);
    }

    private int scanRadius(Minecraft mc) {
        int renderDistance = mc.options.renderDistance().get();
        float configuredValue = value(this.sScanRadius, 4.0F);
        if (isFastTravel(mc)) {
            configuredValue = Math.max(configuredValue, 12.0F);
        }

        int configured = Math.max(1, Math.round(configuredValue));
        return Math.min(renderDistance, configured);
    }

    private static boolean isFastTravel(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }

        Vec3 velocity = mc.player.getDeltaMovement();
        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        return mc.player.isFallFlying() || horizontalSpeedSq > 0.18;
    }

    private static float value(NumberSetting setting, float fallback) {
        return setting == null ? fallback : setting.value()[0];
    }

    private static int color(ColorSetting setting, int fallback) {
        return setting == null ? fallback : setting.argb();
    }

        private final class FastHoleScan {
        final LevelChunk chunk;
        final ClientLevel level;
        final LevelChunk[] neighbors = new LevelChunk[9];
        final long key;
        final int baseX;
        final int baseZ;
        final int minY;
        final int maxY;
        final int gridMinY;
        final int yCount;
        final int minDepth;
        final boolean[] passable;
        final List<HoleEspModule.Hit> hits = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        final MutableBlockPos pos = new MutableBlockPos();
        int row;
        int column;

        FastHoleScan(LevelChunk chunk, long key, int baseX, int baseZ, int minY, int maxY) {
            this.chunk = chunk;
            this.level = HoleEspModule.this.sessionLevel;
            this.key = key;
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.minY = minY;
            this.maxY = maxY;
            this.gridMinY = minY - 1;
            this.yCount = maxY - this.gridMinY + 1;
            this.minDepth = Math.round(HoleEspModule.value(HoleEspModule.this.sMinHoleDepth, 4.0F));
            int size = Math.max(0, this.yCount) * 20 * 20;
            if (HoleEspModule.this.passableBuffer.length < size) {
                HoleEspModule.this.passableBuffer = new boolean[size];
            }

            this.passable = HoleEspModule.this.passableBuffer;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    this.neighbors[(dx + 1) * 3 + dz + 1] = this.level.getChunkSource().getChunkNow((baseX >> 4) + dx, (baseZ >> 4) + dz);
                }
            }
        }

        boolean validFor(ClientLevel currentLevel, int centerX, int centerZ, int radius) {
            if (currentLevel == this.level
                && Math.abs((long)this.chunk.getPos().x - centerX) <= radius
                && Math.abs((long)this.chunk.getPos().z - centerZ) <= radius) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (this.neighbors[(dx + 1) * 3 + dz + 1] != this.level.getChunkSource().getChunkNow((this.baseX >> 4) + dx, (this.baseZ >> 4) + dz)) {
                            return false;
                        }
                    }
                }

                return true;
            } else {
                return false;
            }
        }

        boolean step() {
            if (this.yCount <= 1) {
                return true;
            }

            if (this.row < this.yCount) {
                int y = this.gridMinY + this.row;
                int yBase = this.row++ * 20 * 20;

                for (int lz = -1; lz <= 18; lz++) {
                    int zBase = yBase + (lz + 1) * 20;

                    for (int lx = -1; lx <= 18; lx++) {
                        this.pos.set(this.baseX + lx, y, this.baseZ + lz);
                        this.passable[zBase + lx + 1] = HoleEspModule.isOpen(this.level, this.pos);
                    }
                }

                return false;
            } else {
                if (this.column >= 256) {
                    return true;
                }

                int lx = this.column & 15;
                int lz = this.column++ >> 4;
                HoleEspModule.shaftsInColumn(
                    this.passable, lx, lz, this.gridMinY, this.minY, this.maxY, this.minDepth, this.baseX, this.baseZ, this.hits, this.seen
                );
                return this.column >= 256 || this.hits.size() >= 1024;
            }
        }
    }

        private record Hit(AABB box, int type) {
    }

        private record Mode(boolean holes, boolean tunnels, boolean stairs) {
    }
}
