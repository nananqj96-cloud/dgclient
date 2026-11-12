package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.BlockEspModule;
import dev.aether.settings.BlockListSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class BlockEspRenderer {
   private static final int MAX_CHUNK_RADIUS = 32;
   private static final int MAX_RESULTS = 8000;
   private static final int RETAIN_TICKS = 400;
   private static final double INSET = 0.002;
   private static final IncrementalScan<BlockEspRenderer.Hit> SCAN = new IncrementalScan<>(48, 80000, 20);
   private static Set<Block> lastWanted = Set.of();
   private static final Map<BlockEspRenderer.Hit, Integer> retainedUntil = new LinkedHashMap<>();
   private static final Set<BlockEspRenderer.ChunkPos> pendingChunks = new LinkedHashSet<>();
   private static final Object PENDING_CHUNKS_LOCK = new Object();
   private static volatile List<BlockEspRenderer.Hit> retained = List.of();
   private static int retentionTick;
   private static long retainedRevision;

   private BlockEspRenderer() {
   }

   public static void clear() {
      SCAN.clear();
      lastWanted = Set.of();
      retainedUntil.clear();
      synchronized (PENDING_CHUNKS_LOCK) {
         pendingChunks.clear();
      }

      retained = List.of();
      retentionTick = 0;
      retainedRevision++;
   }

   public static int cachedCount() {
      return retained.size();
   }

   public static List<BlockEspRenderer.Hit> snapshotHits() {
      return retained;
   }

   public static long revision() {
      return retainedRevision;
   }

   public static void markDirty() {
      SCAN.markDirty();
   }

   public static void markChunkDirty(int var0, int var1) {
      synchronized (PENDING_CHUNKS_LOCK) {
         if (pendingChunks.size() < 512) {
            pendingChunks.add(new BlockEspRenderer.ChunkPos(var0, var1));
         }
      }
   }

   public static void scan(BlockEspModule var0) {
      HashSet var1 = new HashSet();

      for (BlockListSetting.Target var3 : var0.targets.targets()) {
         if (var3.enabled.get() && var3.block() != null) {
            var1.add(var3.block());
         }
      }

      if (var1.isEmpty()) {
         SCAN.clear();
         lastWanted = Set.of();
         clearRetained();
      } else {
         if (!var1.equals(lastWanted)) {
            lastWanted = var1;
            SCAN.markDirty();
            clearRetained();
         }

         int var4 = var0.rangeExtraChunks.getInt();
         int var5 = Math.min(32, (Integer)Minecraft.getInstance().options.renderDistance().get() + var4);
         retentionTick++;
         scanPendingChunks(var1);
         SCAN.tick(var5, (var1xx, var2) -> scanChunk(var1xx, var1, var2));
         refreshRetained();
      }
   }

   private static void clearRetained() {
      retainedUntil.clear();
      synchronized (PENDING_CHUNKS_LOCK) {
         pendingChunks.clear();
      }

      retained = List.of();
      retentionTick = 0;
      retainedRevision++;
   }

   private static void refreshRetained() {
      for (BlockEspRenderer.Hit var1 : SCAN.get()) {
         if (retainedUntil.containsKey(var1) || retainedUntil.size() < 8000) {
            retainedUntil.put(var1, retentionTick + 400);
         }
      }

      Iterator var2 = retainedUntil.entrySet().iterator();

      while (var2.hasNext()) {
         if ((Integer)((Entry)var2.next()).getValue() <= retentionTick) {
            var2.remove();
         }
      }

      List var3 = List.copyOf(retainedUntil.keySet());
      if (!var3.equals(retained)) {
         retained = var3;
         retainedRevision++;
      }
   }

   private static void scanPendingChunks(Set<Block> var0) {
      Minecraft var1 = Minecraft.getInstance();
      if (var1.level != null) {
         ArrayList<BlockEspRenderer.ChunkPos> var2 = new ArrayList<>();
         synchronized (PENDING_CHUNKS_LOCK) {
            Iterator var4 = pendingChunks.iterator();

            while (var4.hasNext() && var2.size() < 4) {
               var2.add((BlockEspRenderer.ChunkPos)var4.next());
               var4.remove();
            }
         }

         if (!var2.isEmpty()) {
            ArrayList<BlockEspRenderer.ChunkPos> var13 = new ArrayList<>();

            for (BlockEspRenderer.ChunkPos var5 : var2) {
               LevelChunk var6 = var1.level.getChunkSource().getChunk(var5.x(), var5.z(), false);
               if (var6 == null) {
                  var13.add(var5);
               } else {
                  retainedUntil.keySet().removeIf(var1x -> var1x.x() >> 4 == var5.x() && var1x.z() >> 4 == var5.z());
                  ArrayList<BlockEspRenderer.Hit> var7 = new ArrayList<>();
                  scanChunk(var6, var0, var7);

                  for (BlockEspRenderer.Hit var9 : var7) {
                     if (retainedUntil.containsKey(var9) || retainedUntil.size() < 8000) {
                        retainedUntil.put(var9, retentionTick + 400);
                     }
                  }
               }
            }

            synchronized (PENDING_CHUNKS_LOCK) {
               pendingChunks.addAll(var13);
            }
         }
      }
   }

   private static int scanChunk(LevelChunk var0, Set<Block> var1, List<BlockEspRenderer.Hit> var2) {
      LevelChunkSection[] var3 = var0.getSections();
      int var4 = var0.getMinSectionY();
      int var5 = var0.getPos().getMinBlockX();
      int var6 = var0.getPos().getMinBlockZ();
      short var7 = 0;
      if (var2.size() >= 8000) {
         return 0;
      } else {
         for (int var8 = 0; var8 < var3.length; var8++) {
            LevelChunkSection var9 = var3[var8];
            if (!var9.hasOnlyAir() && var9.maybeHas(var1xx -> var1.contains(var1xx.getBlock()))) {
               int var10 = var4 + var8 << 4;
               var7 = (short)(var7 + 4096);

               for (int var11 = 0; var11 < 16; var11++) {
                  for (int var12 = 0; var12 < 16; var12++) {
                     for (int var13 = 0; var13 < 16; var13++) {
                        Block var14 = var9.getBlockState(var13, var11, var12).getBlock();
                        if (var1.contains(var14)) {
                           var2.add(new BlockEspRenderer.Hit(var5 + var13, var10 + var11, var6 + var12, var14));
                           if (var2.size() >= 8000) {
                              return var7;
                           }
                        }
                     }
                  }
               }
            }
         }

         return var7;
      }
   }

   public static void render(BufferSource var0, PoseStack var1, Vec3 var2, BlockEspModule var3) {
      List<BlockEspRenderer.Hit> var4 = retained;
      if (!var4.isEmpty()) {
         HashMap<Block, Integer> var5 = new HashMap<>();

         for (BlockListSetting.Target var7 : var3.targets.targets()) {
            if (var7.block() != null) {
               var5.put(var7.block(), var7.color.get());
            }
         }

         int var17 = var3.lineColor.get();
         int var18 = Math.clamp((long)var3.highlightAlpha.getInt(), 0, 255);
         boolean var8 = var3.shapeMode.is("Both") || var3.shapeMode.is("Lines");
         boolean var9 = var3.shapeMode.is("Both") || var3.shapeMode.is("Sides");
         boolean var10 = var3.tracers.get() && var3.tracer.get();

         for (BlockEspRenderer.Hit var12 : var4) {
            int var13 = var5.getOrDefault(var12.block(), var17) & 16777215;
            int var14 = var13 | var18 << 24;
            if (var9) {
               EspBoxRenderer.fill(
                  var0,
                  var1,
                  var2,
                  (double)var12.x() + 0.002,
                  (double)var12.y() + 0.002,
                  (double)var12.z() + 0.002,
                  (double)(var12.x() + 1) - 0.002,
                  (double)(var12.y() + 1) - 0.002,
                  (double)(var12.z() + 1) - 0.002,
                  var14
               );
            }

            if (var8) {
               EspBoxRenderer.outline(
                  var0,
                  var1,
                  var2,
                  (double)var12.x() + 0.002,
                  (double)var12.y() + 0.002,
                  (double)var12.z() + 0.002,
                  (double)(var12.x() + 1) - 0.002,
                  (double)(var12.y() + 1) - 0.002,
                  (double)(var12.z() + 1) - 0.002,
                  var14,
                  1.6F
               );
            }
         }

         if (var10) {
            int var19 = var3.tracerColor.get() >>> 24 & 0xFF;
            if (var19 == 0) {
               var19 = 200;
            }

            Vector3fc var20 = Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector();

            for (BlockEspRenderer.Hit var22 : var4) {
               int var15 = var5.getOrDefault(var22.block(), var17) & 16777215;
               int var16 = var15 | var19 << 24;
               EspBoxRenderer.tracer(var0, var1, var2, var20, (double)var22.x() + 0.5, (double)var22.y() + 0.5, (double)var22.z() + 0.5, var16, 1.2F);
            }
         }

         EspBoxRenderer.flush(var0);
      }
   }

   private static record ChunkPos(int x, int z) {
   }

   public static record Hit(int x, int y, int z, Block block) {
   }
}
