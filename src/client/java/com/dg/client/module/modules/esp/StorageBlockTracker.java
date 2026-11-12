package com.dg.client.module.modules.esp;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.BlockUpdateEvent;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.util.ColorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Event-driven loaded-chunk storage container tracker shared by StorageESP and StorageTracers.
 *
 * <p>Avoids scanning a 2D chunk grid every render frame by indexing storage block entities per
 * loaded chunk on {@link ChunkDataEvent.Load}/{@link ChunkDataEvent.Unload}/{@link BlockUpdateEvent},
 * while validating live {@link BlockEntity} state so broken blocks or unloaded chunks disappear
 * on the exact frame they leave the world.
 */
public final class StorageBlockTracker {
    public enum StorageKind {
        CHEST("Chests", ColorUtil.rgba(255, 196, 64, 235), ColorUtil.MINT_GREEN),
        TRAPPED_CHEST("Trapped Chests", ColorUtil.rgba(255, 102, 72, 240), ColorUtil.rgba(120, 255, 150, 235)),
        BARREL("Barrels", ColorUtil.rgba(224, 162, 92, 230), ColorUtil.MINT_GREEN),
        SHULKER_BOX("Shulker Boxes", ColorUtil.rgba(236, 98, 255, 250), ColorUtil.PURE_WHITE),
        ENDER_CHEST("Ender Chests", ColorUtil.rgba(150, 108, 255, 245), ColorUtil.NEON_GREEN),
        HOPPER("Hoppers", ColorUtil.rgba(168, 184, 198, 215), ColorUtil.rgba(96, 206, 138, 215)),
        FURNACE("Furnaces / Smokers", ColorUtil.rgba(255, 142, 74, 215), ColorUtil.rgba(88, 214, 132, 215)),
        DISPENSER("Dispensers / Droppers", ColorUtil.rgba(146, 174, 214, 215), ColorUtil.rgba(84, 198, 126, 215)),
        BREWING_STAND("Brewing Stands", ColorUtil.rgba(104, 224, 255, 225), ColorUtil.rgba(110, 240, 165, 225)),
        SHELF("Shelves / Chiseled", ColorUtil.rgba(196, 228, 120, 215), ColorUtil.rgba(116, 232, 146, 215)),
        DECORATED_POT("Decorated Pots", ColorUtil.rgba(214, 136, 102, 205), ColorUtil.rgba(92, 196, 128, 205));

        private final String displayName;
        private final int distinctColor;
        private final int themeColor;

        StorageKind(String displayName, int distinctColor, int themeColor) {
            this.displayName = displayName;
            this.distinctColor = distinctColor;
            this.themeColor = themeColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getDistinctColor() {
            return distinctColor;
        }

        public int getThemeColor() {
            return themeColor;
        }
    }

    public record ResolvedStorage(
            BlockPos pos,
            StorageKind kind,
            Box renderBox,
            Vec3d center,
            double distanceSq
    ) {
    }

    private record CachedEntry(BlockPos pos, StorageKind kind) {
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ConcurrentHashMap<Long, List<CachedEntry>> chunkStorage = new ConcurrentHashMap<>();
    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private ClientWorld trackedWorld = null;
    private int syncTicker = 0;

    public static StorageKind classify(BlockEntity be) {
        if (be == null || be.isRemoved()) {
            return null;
        }
        if (be instanceof TrappedChestBlockEntity) {
            return StorageKind.TRAPPED_CHEST;
        }
        if (be instanceof ChestBlockEntity) {
            return StorageKind.CHEST;
        }
        if (be instanceof BarrelBlockEntity) {
            return StorageKind.BARREL;
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return StorageKind.SHULKER_BOX;
        }
        if (be instanceof EnderChestBlockEntity) {
            return StorageKind.ENDER_CHEST;
        }
        if (be instanceof HopperBlockEntity) {
            return StorageKind.HOPPER;
        }
        if (be instanceof AbstractFurnaceBlockEntity) {
            return StorageKind.FURNACE;
        }
        if (be instanceof DispenserBlockEntity) {
            return StorageKind.DISPENSER;
        }
        if (be instanceof BrewingStandBlockEntity) {
            return StorageKind.BREWING_STAND;
        }
        if (be instanceof ShelfBlockEntity || be instanceof ChiseledBookshelfBlockEntity) {
            return StorageKind.SHELF;
        }
        if (be instanceof DecoratedPotBlockEntity) {
            return StorageKind.DECORATED_POT;
        }
        return null;
    }

    @EventHandler
    public void onChunkLoad(ChunkDataEvent.Load event) {
        indexChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkDataEvent.Unload event) {
        long key = event.getChunk().getPos().toLong();
        chunkStorage.remove(key);
        dirtyChunks.remove(key);
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos pos = event.getPos();
        dirtyChunks.add(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) {
            if (trackedWorld != null) {
                clear();
                trackedWorld = null;
            }
            return;
        }
        if (world != trackedWorld) {
            clear();
            trackedWorld = world;
            bootstrapLoadedChunks(world);
            return;
        }

        if (!dirtyChunks.isEmpty()) {
            Set<Long> snapshot = new HashSet<>(dirtyChunks);
            dirtyChunks.removeAll(snapshot);
            for (long key : snapshot) {
                int cx = ChunkPos.getPackedX(key);
                int cz = ChunkPos.getPackedZ(key);
                if (world.getChunkManager().isChunkLoaded(cx, cz)) {
                    WorldChunk wc = world.getChunkManager().getWorldChunk(cx, cz);
                    if (wc != null && !wc.isEmpty()) {
                        indexChunk(wc);
                    } else {
                        chunkStorage.remove(key);
                    }
                } else {
                    chunkStorage.remove(key);
                }
            }
        }

        // Periodic sweep every 20 ticks catches block entities added client-side without BlockUpdateEvent
        // and prunes any chunks unloaded silently outside ChunkDataEvent.Unload
        if (++syncTicker >= 20) {
            syncTicker = 0;
            chunkStorage.keySet().removeIf(key ->
                    !world.getChunkManager().isChunkLoaded(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key)));
            bootstrapLoadedChunks(world);
        }
    }

    public void ensureBootstrapped() {
        if (mc.world != null && (trackedWorld != mc.world || chunkStorage.isEmpty())) {
            trackedWorld = mc.world;
            bootstrapLoadedChunks(mc.world);
        }
    }

    public void clear() {
        chunkStorage.clear();
        dirtyChunks.clear();
    }

    private void bootstrapLoadedChunks(ClientWorld world) {
        if (mc.player == null) {
            return;
        }
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;
        int r = Math.min(18, mc.options.getClampedViewDistance() + 2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                if (!world.getChunkManager().isChunkLoaded(cx, cz)) {
                    chunkStorage.remove(ChunkPos.toLong(cx, cz));
                    continue;
                }
                WorldChunk wc = world.getChunkManager().getWorldChunk(cx, cz);
                if (wc != null && !wc.isEmpty()) {
                    indexChunk(wc);
                }
            }
        }
    }

    public void indexChunk(WorldChunk chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        long key = chunk.getPos().toLong();
        var beMap = chunk.getBlockEntities();
        if (beMap.isEmpty()) {
            chunkStorage.remove(key);
            return;
        }
        List<CachedEntry> entries = null;
        for (BlockEntity be : beMap.values()) {
            StorageKind kind = classify(be);
            if (kind != null) {
                if (entries == null) {
                    entries = new ArrayList<>(Math.min(16, beMap.size()));
                }
                entries.add(new CachedEntry(be.getPos().toImmutable(), kind));
            }
        }
        if (entries == null || entries.isEmpty()) {
            chunkStorage.remove(key);
        } else {
            chunkStorage.put(key, entries);
        }
    }

    /**
     * Collects active storage blocks within {@code maxDistanceBlocks}, verifying live chunk/block-entity
     * presence, optionally merging double chests into a single centered target, and sorting nearest-first.
     */
    public List<ResolvedStorage> collectVisible(
            double maxDistanceBlocks,
            int maxResults,
            boolean mergeDoubleChests,
            Predicate<StorageKind> filter
    ) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) {
            return List.of();
        }
        ensureBootstrapped();

        Vec3d eyePos = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getCameraPos()
                : mc.player.getEyePos();
        double maxDistSq = maxDistanceBlocks * maxDistanceBlocks;
        int maxChunkRadius = ((int) Math.ceil(maxDistanceBlocks) >> 4) + 1;
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;

        List<ResolvedStorage> out = new ArrayList<>(Math.min(maxResults, 128));
        for (var mapEntry : chunkStorage.entrySet()) {
            long key = mapEntry.getKey();
            int cx = ChunkPos.getPackedX(key);
            int cz = ChunkPos.getPackedZ(key);
            if (Math.abs(cx - pcx) > maxChunkRadius || Math.abs(cz - pcz) > maxChunkRadius) {
                continue;
            }
            if (!world.getChunkManager().isChunkLoaded(cx, cz)) {
                continue;
            }
            for (CachedEntry ce : mapEntry.getValue()) {
                if (!filter.test(ce.kind)) {
                    continue;
                }
                BlockPos pos = ce.pos;
                double dx = (pos.getX() + 0.5) - eyePos.x;
                double dy = (pos.getY() + 0.5) - eyePos.y;
                double dz = (pos.getZ() + 0.5) - eyePos.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > maxDistSq) {
                    continue;
                }

                // Live verification: immediately drop broken or replaced storage blocks on this exact frame
                BlockEntity liveBe = world.getBlockEntity(pos);
                StorageKind liveKind = classify(liveBe);
                if (liveKind == null || !filter.test(liveKind)) {
                    dirtyChunks.add(key);
                    continue;
                }

                Box box = computeStorageBox(world, pos, liveKind, mergeDoubleChests);
                if (box == null) {
                    // Secondary half of a merged double chest -> skip duplicate box/tracer
                    continue;
                }
                Vec3d center = new Vec3d(
                        (box.minX + box.maxX) * 0.5,
                        (box.minY + box.maxY) * 0.5,
                        (box.minZ + box.maxZ) * 0.5
                );
                out.add(new ResolvedStorage(pos, liveKind, box, center, center.squaredDistanceTo(eyePos)));
            }
        }

        if (out.size() > 1) {
            out.sort(Comparator.comparingDouble(ResolvedStorage::distanceSq));
        }
        if (out.size() > maxResults) {
            return new ArrayList<>(out.subList(0, maxResults));
        }
        return out;
    }

    private static Box computeStorageBox(ClientWorld world, BlockPos pos, StorageKind kind, boolean mergeDoubleChests) {
        if (kind == StorageKind.CHEST || kind == StorageKind.TRAPPED_CHEST) {
            BlockState state = world.getBlockState(pos);
            if (mergeDoubleChests && state.getBlock() instanceof ChestBlock && state.contains(ChestBlock.CHEST_TYPE)) {
                ChestType type = state.get(ChestBlock.CHEST_TYPE);
                if (type != ChestType.SINGLE) {
                    Direction dirToPartner = ChestBlock.getFacing(state);
                    BlockPos partner = pos.offset(dirToPartner);
                    if (world.getChunkManager().isChunkLoaded(partner.getX() >> 4, partner.getZ() >> 4)
                            && classify(world.getBlockEntity(partner)) == kind) {
                        if (type == ChestType.RIGHT) {
                            return null;
                        }
                        double minX = Math.min(pos.getX(), partner.getX()) + 0.0625;
                        double minZ = Math.min(pos.getZ(), partner.getZ()) + 0.0625;
                        double maxX = Math.max(pos.getX(), partner.getX()) + 0.9375;
                        double maxZ = Math.max(pos.getZ(), partner.getZ()) + 0.9375;
                        return new Box(minX, pos.getY(), minZ, maxX, pos.getY() + 0.875, maxZ);
                    }
                }
            }
            return new Box(
                    pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                    pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375
            );
        }
        if (kind == StorageKind.ENDER_CHEST) {
            return new Box(
                    pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625,
                    pos.getX() + 0.9375, pos.getY() + 0.875, pos.getZ() + 0.9375
            );
        }
        return new Box(pos);
    }
}
