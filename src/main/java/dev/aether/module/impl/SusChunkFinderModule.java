package dev.aether.module.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class SusChunkFinderModule extends Module {
    private static final long SCAN_BUDGET_NANOS = 2000000L;
    private static final int MIN_SCANNED_NEIGHBOURS = 3;
    private static final int NO_PACKET_SENSITIVITY = 1;
    private static final int VISIBLE_REFRESH_TICKS = 20;
    private static final int LIGHT_RESCAN_TICKS = 20;
    private static final int FULL_RESCAN_TICKS = 1200;
    private static final int VISIBLE_MIN_TICKS = 5;
    private static final int LIGHT_WAIT_LIMIT_TICKS = 200;
    private static final int PRUNE_TICKS = 200;
    private static final int PRUNE_MARGIN = 4;
    private static final int PACKET_MIN_Y = -16;
    private static final int PACKET_MAX_Y = 30;
    private static final String METHOD_CLASSIC = "Classic";
    private static final String METHOD_ANBS = "ANBS+ Scan";
    private static final int GLOW_CELLS_PER_POINT = 12;
    private static final int GEODE_TOP_Y = 47;
    private static final int MIN_EXPOSED_BUDDING = 10;
    private static final int MIN_EXPOSED_LINING = 160;
    private static final int GEODE_SETTLE_TICKS = 40;
    private static final int GEODE_CELL_SHIFT = 2;
    private static final int TORCH_LIGHT = 14;
    private static final Direction[] SIDES = Direction.values();
    private static final int AMETHYST = 0;
    private static final int KELP = 1;
    private static final int CAVE_VINES = 2;
    private static final int VINES = 3;
    private static final int BAMBOO = 4;
    private static final int COCOA = 5;
    private static final int BEE_NEST = 6;
    private static final int GROWTH_KINDS = 7;
    private static final int GROWN = 1;
    private static final int UNGROWN = -1;
    private static final int NO_SIGN = 0;
    private static final int BAMBOO_GROWTH_LIGHT = 9;
    private static final String DONUT_BRAND = "DonutFolia";
    private static final int STYLE_VOTES = 3;
    private final ModeSetting method = this.addSetting(new ModeSetting("Method", "Classic", "Classic", "ANBS+ Scan"));
    private final SliderSetting spreadRadius = this.addSetting(new SliderSetting("Simulation Distance", 5.0, 2.0, 16.0, 1.0F));
    private final SliderSetting sensitivity = this.addSetting(new SliderSetting("Sensitivity", 6.0, 1.0, 20.0, 1.0F));
    private final BooleanSetting kelp = this.growthSign("growth-kelp", "Kelp");
    private final BooleanSetting caveVines = this.growthSign("growth-cave-vines", "Cave Vines");
    private final BooleanSetting vines = this.growthSign("growth-vines", "Vines");
    private final BooleanSetting bamboo = this.growthSign("growth-bamboo", "Bamboo");
    private final BooleanSetting cocoa = this.growthSign("growth-cocoa", "Cocoa");
    private final BooleanSetting beeNests = this.growthSign("growth-bee-nests", "Bee Nests");
    private final BooleanSetting[] growthSigns = new BooleanSetting[]{null, this.kelp, this.caveVines, this.vines, this.bamboo, this.cocoa, this.beeNests};
    private final ColorSetting chunkColor = this.addSetting(new ColorSetting("Chunk Color", 1627325444));
    private final BooleanSetting tracers = this.addSetting(new BooleanSetting("Tracers", true));
    private final BooleanSetting detectMined = this.addSetting(new BooleanSetting("Detect Mined Clusters", false));
    private final ColorSetting minedColor = this.addSetting(new ColorSetting("Mined Color", 1621511679));
    private final ChunkAreas chunkAreas = new ChunkAreas();
    private final ChunkOverlay chunkOverlay = new ChunkOverlay();
    private final ChunkAreas minedAreas = new ChunkAreas();
    private final ChunkOverlay minedOverlay = new ChunkOverlay();
    private final GeodeGlowScan glow = new GeodeGlowScan();
    private final ServerBlockLight serverLight = new ServerBlockLight();
    private int darkVotes;
    private int glowVotes;
    private final Long2IntOpenHashMap[] heat = perKind();
    private final Long2IntOpenHashMap[] vetoes = perKind();
    private final Long2ObjectOpenHashMap<int[]> chunkGrown = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap chunkUngrown = new Long2IntOpenHashMap();
    private final Long2BooleanOpenHashMap chunkHasPacket = new Long2BooleanOpenHashMap();
    private final LongOpenHashSet scannedChunks = new LongOpenHashSet();
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private final LongOpenHashSet queued = new LongOpenHashSet();
    private final LongOpenHashSet lightDirty = new LongOpenHashSet();
    private final Long2IntOpenHashMap waitingSince = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap bestLightClusters = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap bestLightBuds = new Long2IntOpenHashMap();
    private final LongOpenHashSet geodeChunks = new LongOpenHashSet();
    private Long2IntOpenHashMap visible = new Long2IntOpenHashMap();
    private boolean visibleDirty = true;
    private int visibleAge;
    private ClientLevel lastLevel;
    private int appliedRadius;
    private final Long2ObjectOpenHashMap<SusChunkFinderModule.GeodeSlice> geodeSlices = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap geodeWaiting = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap geodeScanTicks = new Long2IntOpenHashMap();
    private List<SusChunkFinderModule.GeodeCluster> geodeClusters = List.of();
    private boolean geodeClustersDirty;
    private LongOpenHashSet minedChunks = new LongOpenHashSet();
    private int minedAge;
    private int ticks;

    public SusChunkFinderModule() {
        super("sus-chunk-finder", "Finds chunks with signs of player activity from grown amethyst, kelp, vines, bamboo, cocoa and bee nests.", Category.EXPLOIT);
        for (BooleanSetting sign : this.growthSigns) {
            if (sign != null) {
                sign.visibleWhen(() -> this.method.is("Classic"));
                sign.onChange(this::rescan);
            }
        }
        this.minedColor.visibleWhen(this.detectMined::get);
    }

    private BooleanSetting growthSign(String id, String name) {
        return this.addSetting(id, new BooleanSetting(name, true));
    }

    private static Long2IntOpenHashMap[] perKind() {
        Long2IntOpenHashMap[] maps = new Long2IntOpenHashMap[7];

        for (int kind = 0; kind < 7; kind++) {
            maps[kind] = new Long2IntOpenHashMap();
        }

        return maps;
    }

    private int countedKinds() {
        int kinds = 1;

        for (int kind = 0; kind < 7; kind++) {
            if (this.growthSigns[kind] != null && this.growthSigns[kind].get()) {
                kinds |= 1 << kind;
            }
        }

        return kinds;
    }

    private void rescan() {
        this.clearAll();
        this.queueLoadedChunks();
    }

    private void restartGeodes() {
        this.clearGeodes();
        if (this.detectMined.get()) {
            this.queueLoadedChunks();
        }
    }

    @Override
    protected void onEnable() {
        this.clearAll();
        this.resetLight();
        this.lastLevel = Minecraft.getInstance().level;
        this.queueLoadedChunks();
    }

    @Override
    protected void onDisable() {
        this.clearAll();
        this.resetLight();
        this.lastLevel = null;
    }

    private void resetLight() {
        this.serverLight.clear();
        this.darkVotes = 0;
        this.glowVotes = 0;
    }

    private void clearAll() {
        for (int kind = 0; kind < 7; kind++) {
            this.heat[kind].clear();
            this.vetoes[kind].clear();
        }

        this.chunkGrown.clear();
        this.chunkUngrown.clear();
        this.chunkHasPacket.clear();
        this.scannedChunks.clear();
        this.queue.clear();
        this.queued.clear();
        this.lightDirty.clear();
        this.waitingSince.clear();
        this.bestLightClusters.clear();
        this.bestLightBuds.clear();
        this.geodeChunks.clear();
        this.visible = new Long2IntOpenHashMap();
        this.visibleDirty = true;
        this.appliedRadius = this.spreadRadius.getInt();
        this.clearGeodes();
    }

    private void clearGeodes() {
        this.geodeSlices.clear();
        this.geodeWaiting.clear();
        this.geodeScanTicks.clear();
        this.geodeClusters = List.of();
        this.geodeClustersDirty = false;
        this.minedChunks = new LongOpenHashSet();
        this.minedAge = 0;
    }

    private void queueLoadedChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            int radius = mc.options.renderDistance().get() + 1;
            ChunkPos center = mc.player.chunkPosition();

            for (int x = center.x - radius; x <= center.x + radius; x++) {
                for (int z = center.z - radius; z <= center.z + radius; z++) {
                    if (LevelUtil.isLoaded(mc.level, x, z)) {
                        this.enqueue(ChunkPos.asLong(x, z));
                    }
                }
            }
        }
    }

    private void enqueue(long key) {
        if (this.queued.add(key)) {
            this.queue.enqueue(key);
        }
    }

//     @EventHandler
//     public void onPacketApplied(PacketAppliedEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (event.packet instanceof ClientboundLevelChunkWithLightPacket chunk) {
            if (level != null) {
                this.serverLight.onChunk(level, chunk.getX(), chunk.getZ(), chunk.getLightData());
            }

            this.enqueue(ChunkPos.asLong(chunk.getX(), chunk.getZ()));
            this.markLightDirty(chunk.getX(), chunk.getZ());
        } else if (event.packet instanceof ClientboundLightUpdatePacket light) {
            if (level != null) {
                this.serverLight.onLight(level, light.getX(), light.getZ(), light.getLightData());
            }

            this.markLightDirty(light.getX(), light.getZ());
        }
    }

//     @EventHandler
//     public void onBlockUpdate(BlockUpdateEvent event) {
        this.serverLight.onBlockChanged(event.pos);
    }

    private void markLightDirty(int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = ChunkPos.asLong(chunkX + dx, chunkZ + dz);
                if (this.geodeChunks.contains(key) || !this.scannedChunks.contains(key)) {
                    this.lightDirty.add(key);
                }
            }
        }
    }

//     @EventHandler
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != this.lastLevel) {
            this.clearAll();
            this.resetLight();
            this.lastLevel = level;
            this.queueLoadedChunks();
        }

        if (level != null) {
            this.ticks++;
            if (this.spreadRadius.getInt() != this.appliedRadius) {
                this.clearAll();
                this.queueLoadedChunks();
            }

            if (this.ticks % 1200 == 0) {
                this.lightDirty.clear();
                this.queueLoadedChunks();
            } else if (this.ticks % 20 == 0 && !this.lightDirty.isEmpty()) {
                for (long key : this.lightDirty) {
                    if (LevelUtil.isLoaded(level, ChunkPos.getX(key), ChunkPos.getZ(key))) {
                        this.enqueue(key);
                    }
                }

                this.lightDirty.clear();
            }

            boolean optimized = PerformanceModule.optimizationsEnabled();
            long started = System.nanoTime();
            long deadline = optimized ? PerformanceBudget.acquire(2000000L) : started + 2000000L;

            while (!this.queue.isEmpty() && System.nanoTime() < deadline) {
                long key = this.queue.dequeueLong();
                this.queued.remove(key);
                int cx = ChunkPos.getX(key);
                int cz = ChunkPos.getZ(key);
                if (!LevelUtil.isLoaded(level, cx, cz)) {
                    this.waitingSince.remove(key);
                } else if (this.method.is("ANBS+ Scan") && !this.lightSettled(level, key)) {
                    this.lightDirty.add(key);
                } else {
                    this.waitingSince.remove(key);
                    this.updateChunk(key, level.getChunk(cx, cz), level);
                    this.scannedChunks.add(key);
                    this.visibleDirty = true;
                }
            }

            if (optimized) {
                PerformanceBudget.record(started);
            }

            this.visibleAge++;
            if (this.visibleDirty && this.visibleAge >= 5 || this.visibleAge >= 20) {
                this.visible = this.computeVisible(level);
                this.visibleDirty = false;
                this.visibleAge = 0;
            }

            if (this.ticks % 200 == 0) {
                this.pruneFar(Minecraft.getInstance());
            }

            if (this.detectMined.get() && ++this.minedAge >= 20) {
                this.minedChunks = this.computeMined(level);
                this.minedAge = 0;
            }
        }
    }

    private boolean lightSettled(ClientLevel level, long key) {
        if (!this.waitingSince.containsKey(key)) {
            this.waitingSince.put(key, this.ticks);
        }

        if (this.ticks - this.waitingSince.get(key) >= 200) {
            return true;
        }

        LevelLightEngine engine = level.getLightEngine();
        if (engine.hasLightWork()) {
            return false;
        }

        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!LevelUtil.isLoaded(level, cx + dx, cz + dz)) {
                    return false;
                }

                if (!engine.lightOnInColumn(SectionPos.getZeroNode(cx + dx, cz + dz))) {
                    return false;
                }
            }
        }

        return true;
    }

    private void pruneFar(Minecraft mc) {
        if (mc.player != null) {
            int reach = mc.options.getEffectiveRenderDistance() + 2 * this.spreadRadius.getInt() + 4;
            int px = mc.player.getBlockX() >> 4;
            int pz = mc.player.getBlockZ() >> 4;
            LongArrayList far = new LongArrayList();

            for (long key : this.scannedChunks) {
                if (Math.max(Math.abs(ChunkPos.getX(key) - px), Math.abs(ChunkPos.getZ(key) - pz)) > reach) {
                    far.add(key);
                }
            }

            for (int i = 0; i < far.size(); i++) {
                this.forget(far.getLong(i));
            }

            if (!far.isEmpty()) {
                this.visibleDirty = true;
            }

            if (mc.level != null) {
                this.serverLight.forgetUnloaded(mc.level);
            }
        }
    }

    private void forget(long key) {
        int r = this.spreadRadius.getInt();
        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);
        this.withdraw(cx, cz, r, this.chunkGrown.remove(key), this.chunkUngrown.remove(key));
        this.chunkHasPacket.remove(key);
        this.scannedChunks.remove(key);
        this.bestLightClusters.remove(key);
        this.bestLightBuds.remove(key);
        this.geodeChunks.remove(key);
        this.waitingSince.remove(key);
        this.lightDirty.remove(key);
    }

    private void updateChunk(long key, LevelChunk chunk, ClientLevel level) {
        int r = this.spreadRadius.getInt();
        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);
        this.withdraw(cx, cz, r, this.chunkGrown.remove(key), this.chunkUngrown.remove(key));
        this.chunkHasPacket.put(key, hasBlocksInRange(chunk, -16, 30));
        if (hasLining(chunk)) {
            this.geodeChunks.add(key);
        } else {
            this.geodeChunks.remove(key);
        }

        SusChunkFinderModule.ChunkScore raw = this.scoreChunk(chunk, level);
        int bestClusters = this.bestLightClusters.get(key);
        int bestBuds = this.bestLightBuds.get(key);
        if (raw.lightClusters() > bestClusters || raw.lightClusters() == bestClusters && raw.lightBuds() > bestBuds) {
            bestClusters = raw.lightClusters();
            bestBuds = raw.lightBuds();
            this.bestLightClusters.put(key, bestClusters);
            this.bestLightBuds.put(key, bestBuds);
        }

        int[] grown = raw.grown();
        grown[0] += bestClusters;
        int ungrown = bestBuds > 0 ? raw.ungrown() | 1 : raw.ungrown();
        this.chunkGrown.put(key, grown);
        this.chunkUngrown.put(key, ungrown);
        this.spread(cx, cz, r, grown, ungrown);
        if (this.detectMined.get()) {
            this.collectGeode(key, chunk, level);
        }
    }

    private void spread(int cx, int cz, int r, int[] grown, int ungrown) {
        for (int kind = 0; kind < 7; kind++) {
            Long2IntOpenHashMap kindHeat = this.heat[kind];
            Long2IntOpenHashMap kindVetoes = this.vetoes[kind];
            int amount = grown[kind];
            if (amount > 0) {
                forArea(cx, cz, r, area -> kindHeat.addTo(area, amount));
            }

            if ((ungrown & 1 << kind) != 0) {
                forArea(cx, cz, r, area -> kindVetoes.addTo(area, 1));
            }
        }
    }

    private void withdraw(int cx, int cz, int r, int[] grown, int ungrown) {
        for (int kind = 0; kind < 7; kind++) {
            Long2IntOpenHashMap kindHeat = this.heat[kind];
            Long2IntOpenHashMap kindVetoes = this.vetoes[kind];
            int amount = grown == null ? 0 : grown[kind];
            if (amount > 0) {
                forArea(cx, cz, r, area -> subtract(kindHeat, area, amount));
            }

            if ((ungrown & 1 << kind) != 0) {
                forArea(cx, cz, r, area -> subtract(kindVetoes, area, 1));
            }
        }
    }

    private static void forArea(int cx, int cz, int r, LongConsumer action) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                action.accept(ChunkPos.asLong(cx + dx, cz + dz));
            }
        }
    }

    private static void subtract(Long2IntOpenHashMap map, long key, int amount) {
        int left = map.get(key) - amount;
        if (left > 0) {
            map.put(key, left);
        } else {
            map.remove(key);
        }
    }

    private static boolean hasLining(LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (section != null && !section.hasOnlyAir() && section.maybeHas(SusChunkFinderModule::isLining)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasBlocksInRange(LevelChunk chunk, int minY, int maxY) {
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section != null && !section.hasOnlyAir()) {
                int baseY = minSectionY + i << 4;
                if (baseY + 15 >= minY && baseY <= maxY) {
                    for (int y = 0; y < 16; y++) {
                        int worldY = baseY + y;
                        if (worldY >= minY && worldY <= maxY) {
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    if (!section.getBlockState(x, y, z).isAir()) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private SusChunkFinderModule.ChunkScore scoreChunk(LevelChunk chunk, ClientLevel level) {
        int[] grown = new int[7];
        if (this.method.is("ANBS+ Scan")) {
            grown[0] = this.glow.scan(level, chunk, null) / 12;
            return new SusChunkFinderModule.ChunkScore(grown, 0, 0, 0);
        }

        LevelChunkSection[] sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        ChunkPos pos = chunk.getPos();
        int counted = this.countedKinds();
        int ungrown = 0;
        MutableBlockPos cursor = new MutableBlockPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section != null && !section.hasOnlyAir() && section.maybeHas(statex -> isCounted(statex, counted))) {
                int baseY = minSectionY + i << 4;

                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState state = section.getBlockState(x, y, z);
                            int kind = growthKind(state);
                            if (kind >= 0 && (counted & 1 << kind) != 0) {
                                cursor.set(pos.getMinBlockX() + x, baseY + y, pos.getMinBlockZ() + z);
                                int sign = readSign(level, cursor, state, kind);
                                if (sign == 1) {
                                    grown[kind]++;
                                } else if (sign == -1) {
                                    ungrown |= 1 << kind;
                                }
                            }
                        }
                    }
                }
            }
        }

        HiddenAmethyst.Reading hidden = HiddenAmethyst.read(level, chunk, this.serverLight, this.hiddenSolids());
        this.darkVotes = this.darkVotes + hidden.darkVotes();
        this.glowVotes = this.glowVotes + hidden.glowVotes();
        return new SusChunkFinderModule.ChunkScore(grown, ungrown, hidden.clusters(), hidden.buds());
    }

    private Boolean hiddenSolids() {
        if (onDonut()) {
            return Boolean.TRUE;
        } else if (this.darkVotes >= 3 && this.darkVotes > this.glowVotes) {
            return Boolean.TRUE;
        } else {
            return this.glowVotes >= 3 && this.glowVotes > this.darkVotes ? Boolean.FALSE : null;
        }
    }

    private static boolean onDonut() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        String brand = connection == null ? null : connection.serverBrand();
        return brand != null && brand.contains("DonutFolia");
    }

    private static boolean isCounted(BlockState state, int counted) {
        int kind = growthKind(state);
        return kind >= 0 && (counted & 1 << kind) != 0;
    }

    private static int growthKind(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AMETHYST_CLUSTER || isBud(state)) {
            return 0;
        } else if (block == Blocks.KELP) {
            return 1;
        } else if (block == Blocks.CAVE_VINES) {
            return 2;
        } else if (block == Blocks.VINE) {
            return 3;
        } else if (block == Blocks.BAMBOO) {
            return 4;
        } else if (block == Blocks.COCOA) {
            return 5;
        } else {
            return block == Blocks.BEE_NEST ? 6 : -1;
        }
    }

    private static int readSign(ClientLevel level, MutableBlockPos at, BlockState state, int kind) {
        return switch (kind) {
            case 0 -> {
                if (state.is(Blocks.AMETHYST_CLUSTER)) {
                    yield 1;
                } else if (!state.hasProperty(BlockStateProperties.FACING)) {
                    yield 0;
                } else {
                    Direction facing = state.getValue(BlockStateProperties.FACING);
                    yield level.getBlockState(at.move(facing.getOpposite())).is(Blocks.BUDDING_AMETHYST) ? -1 : 1;
                }
            }
            case 1 -> state.getValue(BlockStateProperties.AGE_25) != 25 && level.getBlockState(at.move(Direction.UP)).is(Blocks.WATER) ? -1 : 1;
            case 2 -> state.getValue(BlockStateProperties.AGE_25) != 25 && level.getBlockState(at.move(Direction.DOWN)).isAir() ? -1 : 1;
            case 3 -> {
                BlockState below = level.getBlockState(at.move(Direction.DOWN));
                yield below.is(Blocks.VINE) ? 0 : (!below.isAir() ? 1 : (hangsFromSide(state) ? -1 : 1));
            }
            case 4 -> bambooSign(level, at, state);
            case 5 -> state.getValue(BlockStateProperties.AGE_2) == 2 ? 1 : -1;
            case 6 -> state.getValue(BeehiveBlock.HONEY_LEVEL) == 5 ? 1 : -1;
            default -> 0;
        };
    }

    private static boolean hangsFromSide(BlockState vine) {
        return vine.getValue(BlockStateProperties.NORTH)
            || vine.getValue(BlockStateProperties.EAST)
            || vine.getValue(BlockStateProperties.SOUTH)
            || vine.getValue(BlockStateProperties.WEST);
    }

    private static int bambooSign(ClientLevel level, MutableBlockPos at, BlockState state) {
        int x = at.getX();
        int y = at.getY();
        int z = at.getZ();
        BlockState above = level.getBlockState(at.move(Direction.UP));
        if (above.is(Blocks.BAMBOO)) {
            return 0;
        }

        if (state.getValue(BlockStateProperties.STAGE) == 1) {
            return 1;
        }

        if (above.isAir() && level.getRawBrightness(at, 0) >= 9) {
            int height = 1;

            while (height < 16 && level.getBlockState(at.set(x, y - height, z)).is(Blocks.BAMBOO)) {
                height++;
            }

            return height < 16 ? -1 : 0;
        } else {
            return 0;
        }
    }

    private void collectGeode(long key, LevelChunk chunk, ClientLevel level) {
        this.geodeScanTicks.put(key, this.ticks);
        if (this.geodeSlices.remove(key) != null) {
            this.geodeClustersDirty = true;
        }

        this.geodeWaiting.remove(key);
        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long near = ChunkPos.asLong(cx + dx, cz + dz);
                if ((dx != 0 || dz != 0) && (this.geodeWaiting.get(near) & waitBit(-dx, -dz)) != 0) {
                    this.enqueue(near);
                }
            }
        }

        if (LevelUtil.isOverworld(level)) {
            LevelChunkSection[] sections = chunk.getSections();
            int minSectionY = chunk.getMinSectionY();
            int minX = cx << 4;
            int minZ = cz << 4;
            LongArrayList lining = new LongArrayList();
            LongArrayList budding = new LongArrayList();
            MutableBlockPos cursor = new MutableBlockPos();
            int waiting = 0;

            for (int i = 0; i < sections.length; i++) {
                int baseY = minSectionY + i << 4;
                if (baseY > 47) {
                    break;
                }

                LevelChunkSection section = sections[i];
                if (section != null && !section.hasOnlyAir() && section.maybeHas(SusChunkFinderModule::isLining)) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                BlockState state = section.getBlockState(x, y, z);
                                if (isLining(state)) {
                                    int wx = minX + x;
                                    int wy = baseY + y;
                                    int wz = minZ + z;
                                    boolean exposed = false;
                                    int missing = 0;

                                    for (Direction side : SIDES) {
                                        int nx = wx + side.getStepX();
                                        int ny = wy + side.getStepY();
                                        int nz = wz + side.getStepZ();
                                        if (!level.isOutsideBuildHeight(ny)) {
                                            int ncx = nx >> 4;
                                            int ncz = nz >> 4;
                                            boolean inside = ncx == cx && ncz == cz;
                                            if (!inside && !LevelUtil.isLoaded(level, ncx, ncz)) {
                                                missing |= waitBit(ncx - cx, ncz - cz);
                                            } else {
                                                cursor.set(nx, ny, nz);
                                                if (isOpen(inside ? chunk.getBlockState(cursor) : level.getBlockState(cursor))) {
                                                    exposed = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    if (!exposed) {
                                        waiting |= missing;
                                    } else {
                                        long packed = BlockPos.asLong(wx, wy, wz);
                                        lining.add(packed);
                                        if (state.is(Blocks.BUDDING_AMETHYST)) {
                                            budding.add(packed);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (waiting != 0) {
                this.geodeWaiting.put(key, waiting);
            }

            if (!lining.isEmpty()) {
                this.geodeSlices.put(key, new SusChunkFinderModule.GeodeSlice(lining.toLongArray(), budding.toLongArray()));
                this.geodeClustersDirty = true;
            }
        }
    }

    private static int waitBit(int dx, int dz) {
        return 1 << (dx + 1) * 3 + dz + 1;
    }

    private LongOpenHashSet computeMined(ClientLevel level) {
        if (this.geodeSlices.keySet().removeIf(key -> !LevelUtil.isLoaded(level, ChunkPos.getX(key), ChunkPos.getZ(key)))) {
            this.geodeClustersDirty = true;
        }

        this.geodeWaiting.keySet().removeIf(key -> !LevelUtil.isLoaded(level, ChunkPos.getX(key), ChunkPos.getZ(key)));
        this.geodeScanTicks.keySet().removeIf(key -> !LevelUtil.isLoaded(level, ChunkPos.getX(key), ChunkPos.getZ(key)));
        if (this.geodeClustersDirty) {
            this.geodeClusters = this.clusterGeodes();
            this.geodeClustersDirty = false;
        }

        LongOpenHashSet mined = new LongOpenHashSet();

        for (SusChunkFinderModule.GeodeCluster cluster : this.geodeClusters) {
            boolean large = cluster.budding() > 0 ? cluster.budding() >= 10 : cluster.lining().length >= 160;
            if (large && this.settled(level, cluster) && unlit(level, cluster.lining())) {
                mined.add(cluster.chunk());
            }
        }

        return mined;
    }

    private List<SusChunkFinderModule.GeodeCluster> clusterGeodes() {
        Long2ObjectOpenHashMap<LongArrayList> cells = new Long2ObjectOpenHashMap<>();
        LongOpenHashSet budding = new LongOpenHashSet();

        for (SusChunkFinderModule.GeodeSlice slice : this.geodeSlices.values()) {
            for (long packed : slice.lining()) {
                long cell = BlockPos.asLong(BlockPos.getX(packed) >> 2, BlockPos.getY(packed) >> 2, BlockPos.getZ(packed) >> 2);
                LongArrayList members = cells.get(cell);
                if (members == null) {
                    cells.put(cell, members = new LongArrayList());
                }

                members.add(packed);
            }

            for (long packed : slice.budding()) {
                budding.add(packed);
            }
        }

        List<SusChunkFinderModule.GeodeCluster> clusters = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue frontier = new LongArrayFIFOQueue();

        for (long start : cells.keySet()) {
            if (seen.add(start)) {
                LongArrayList lining = new LongArrayList();
                frontier.enqueue(start);

                while (!frontier.isEmpty()) {
                    long cell = frontier.dequeueLong();
                    lining.addAll(cells.get(cell));
                    int x = BlockPos.getX(cell);
                    int y = BlockPos.getY(cell);
                    int z = BlockPos.getZ(cell);

                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                long next = BlockPos.asLong(x + dx, y + dy, z + dz);
                                if (cells.containsKey(next) && seen.add(next)) {
                                    frontier.enqueue(next);
                                }
                            }
                        }
                    }
                }

                clusters.add(toCluster(lining.toLongArray(), budding));
            }
        }

        return clusters;
    }

    private static SusChunkFinderModule.GeodeCluster toCluster(long[] lining, LongOpenHashSet budding) {
        int count = 0;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        long sumX = 0L;
        long sumZ = 0L;

        for (long packed : lining) {
            if (budding.contains(packed)) {
                count++;
            }

            int x = BlockPos.getX(packed);
            int z = BlockPos.getZ(packed);
            minX = Math.min(minX, x);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxZ = Math.max(maxZ, z);
            sumX += x;
            sumZ += z;
        }

        int centerX = (int)Math.floorDiv(sumX, lining.length);
        int centerZ = (int)Math.floorDiv(sumZ, lining.length);
        return new SusChunkFinderModule.GeodeCluster(lining, count, minX, minZ, maxX, maxZ, ChunkPos.asLong(centerX >> 4, centerZ >> 4));
    }

    private boolean settled(ClientLevel level, SusChunkFinderModule.GeodeCluster cluster) {
        for (int cx = cluster.minX() - 1 >> 4; cx <= cluster.maxX() + 1 >> 4; cx++) {
            for (int cz = cluster.minZ() - 1 >> 4; cz <= cluster.maxZ() + 1 >> 4; cz++) {
                long key = ChunkPos.asLong(cx, cz);
                if (!LevelUtil.isLoaded(level, cx, cz) || !this.geodeScanTicks.containsKey(key)) {
                    return false;
                }

                if (this.ticks - this.geodeScanTicks.get(key) < 40) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean unlit(ClientLevel level, long[] lining) {
        MutableBlockPos cursor = new MutableBlockPos();
        Long2IntOpenHashMap peaks = new Long2IntOpenHashMap();

        for (long packed : lining) {
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);

            for (Direction side : SIDES) {
                cursor.set(x + side.getStepX(), y + side.getStepY(), z + side.getStepZ());
                if (isAmethystGrowth(level.getBlockState(cursor))) {
                    return false;
                }

                int light = level.getBrightness(LightLayer.BLOCK, cursor);
                if (light > 0 && sourceLight(level, cursor, light, peaks) != 14) {
                    return false;
                }
            }
        }

        return true;
    }

    private static int sourceLight(ClientLevel level, BlockPos start, int light, Long2IntOpenHashMap peaks) {
        LongArrayList path = new LongArrayList();
        MutableBlockPos at = start.mutable();
        MutableBlockPos probe = new MutableBlockPos();

        int peak;
        while (true) {
            long key = at.asLong();
            if (peaks.containsKey(key)) {
                peak = peaks.get(key);
                break;
            }

            path.add(key);
            Direction uphill = null;
            int best = light;

            for (Direction side : SIDES) {
                int value = level.getBrightness(LightLayer.BLOCK, probe.setWithOffset(at, side));
                if (value > best) {
                    best = value;
                    uphill = side;
                }
            }

            if (uphill == null) {
                peak = light;
                break;
            }

            at.move(uphill);
            light = best;
        }

        for (int i = 0; i < path.size(); i++) {
            peaks.put(path.getLong(i), peak);
        }

        return peak;
    }

    private static boolean isLining(BlockState state) {
        return state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST);
    }

    private static boolean isOpen(BlockState state) {
        return BuddingAmethystBlock.canClusterGrowAtState(state) || isAmethystGrowth(state);
    }

    private static boolean isBud(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SMALL_AMETHYST_BUD || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.LARGE_AMETHYST_BUD;
    }

    private static boolean isAmethystGrowth(BlockState state) {
        return state.is(Blocks.AMETHYST_CLUSTER) || isBud(state);
    }

    private Long2IntOpenHashMap computeVisible(ClientLevel level) {
        Long2IntOpenHashMap total = new Long2IntOpenHashMap();

        for (int kind = 0; kind < 7; kind++) {
            Long2IntOpenHashMap kindVetoes = this.vetoes[kind];

            for (Entry entry : this.heat[kind].long2IntEntrySet()) {
                if (!kindVetoes.containsKey(entry.getLongKey())) {
                    total.addTo(entry.getLongKey(), entry.getIntValue());
                }
            }
        }

        Long2IntOpenHashMap eligible = new Long2IntOpenHashMap();

        for (Entry entry : total.long2IntEntrySet()) {
            long key = entry.getLongKey();
            int cx = ChunkPos.getX(key);
            int cz = ChunkPos.getZ(key);
            if (LevelUtil.isLoaded(level, cx, cz) && this.scannedNeighbours(cx, cz) >= 3) {
                eligible.put(key, entry.getIntValue());
            }
        }

        int withPacket = this.sensitivity.getInt();
        int reach = this.appliedRadius;
        Long2IntOpenHashMap kept = new Long2IntOpenHashMap();

        for (Entry entry : eligible.long2IntEntrySet()) {
            long key = entry.getLongKey();
            int value = entry.getIntValue();
            boolean reached = value >= withPacket || !this.chunkHasPacket.getOrDefault(key, true) && this.amethystHeat(key) >= 1;
            if (reached && isPeak(eligible, key, value, reach)) {
                kept.put(key, value);
            }
        }

        return kept;
    }

    private int amethystHeat(long key) {
        return this.vetoes[0].containsKey(key) ? 0 : this.heat[0].get(key);
    }

    private static boolean isPeak(Long2IntOpenHashMap field, long key, int value, int reach) {
        int cx = ChunkPos.getX(key);
        int cz = ChunkPos.getZ(key);

        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if ((dx != 0 || dz != 0) && field.get(ChunkPos.asLong(cx + dx, cz + dz)) > value) {
                    return false;
                }
            }
        }

        return true;
    }

    private int scannedNeighbours(int cx, int cz) {
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && this.scannedChunks.contains(ChunkPos.asLong(cx + dx, cz + dz))) {
                    count++;
                }
            }
        }

        return count;
    }

    public LongSet flaggedChunks() {
        return LongSets.unmodifiable(this.visible.keySet());
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        if (Minecraft.getInstance().level != null) {
            this.chunkOverlay.draw(this.visible.keySet(), this.chunkColor.argb(), event.partialTick);
            if (this.tracers.get()) {
                this.chunkAreas.drawTracers(this.visible.keySet(), 63.0, ARGB.opaque(this.chunkColor.argb()));
            }

            this.minedOverlay.draw(this.minedChunks, this.minedColor.argb(), event.partialTick);
            if (this.tracers.get()) {
                this.minedAreas.drawTracers(this.minedChunks, 63.0, ARGB.opaque(this.minedColor.argb()));
            }
        }
    }

        private record ChunkScore(int[] grown, int ungrown, int lightClusters, int lightBuds) {
    }

        private record GeodeCluster(long[] lining, int budding, int minX, int minZ, int maxX, int maxZ, long chunk) {
    }

        private record GeodeSlice(long[] lining, long[] budding) {
    }
}
