package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.ClusterEspModule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class ClusterEspRenderer {
   private static final int[][] DIRECTIONS = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
   private static final int MAX_CLUSTERS = 512;
   private static final int MAX_RESULTS = 12000;
   private static final int RETAIN_TICKS = 400;
   private static final IncrementalScan<ClusterEspRenderer.Hit> SCAN = new IncrementalScan<>(48, 80000, 20);
   private static final Map<ClusterEspRenderer.Hit, Integer> retainedUntil = new LinkedHashMap<>();
   private static final Set<ClusterEspRenderer.ChunkPos> pendingChunks = new LinkedHashSet<>();
   private static final Object PENDING_LOCK = new Object();
   private static volatile List<ClusterEspRenderer.Hit> retained = List.of();
   private static int retentionTick;
   private static long revision;
   private static volatile List<ClusterEspRenderer.Cluster> clusters = List.of();
   private static long lastRevision = Long.MIN_VALUE;
   private static int lastFingerprint = Integer.MIN_VALUE;

   private ClusterEspRenderer() {
   }

   public static void clear() {
      SCAN.clear();
      retainedUntil.clear();
      synchronized (PENDING_LOCK) {
         pendingChunks.clear();
      }

      retained = List.of();
      retentionTick = 0;
      revision++;
      clusters = List.of();
      lastRevision = Long.MIN_VALUE;
      lastFingerprint = Integer.MIN_VALUE;
   }

   public static int cachedCount() {
      int var0 = 0;

      for (ClusterEspRenderer.Cluster var2 : clusters) {
         var0 += var2.hits().size();
      }

      return var0;
   }

   public static void update(ClusterEspModule var0) {
      retentionTick++;
      scanPendingChunks();
      int var1 = Math.min(32, (Integer)Minecraft.getInstance().options.renderDistance().get());
      SCAN.tick(var1, ClusterEspRenderer::scanChunk);
      refreshRetained();
      int var2 = var0.minimumBlocks.getInt();
      if (revision != lastRevision || var2 != lastFingerprint) {
         lastRevision = revision;
         lastFingerprint = var2;
         clusters = build(retained, var2);
      }
   }

   public static void markChunkDirty(int var0, int var1) {
      synchronized (PENDING_LOCK) {
         if (pendingChunks.size() < 512) {
            pendingChunks.add(new ClusterEspRenderer.ChunkPos(var0, var1));
         }
      }
   }

   public static void markDirty() {
      SCAN.markDirty();
   }

   private static boolean isAmethyst(Block var0) {
      return var0 == Blocks.AMETHYST_CLUSTER;
   }

   private static int scanChunk(LevelChunk var0, List<ClusterEspRenderer.Hit> var1) {
      LevelChunkSection[] var2 = var0.getSections();
      int var3 = var0.getMinSectionY();
      int var4 = var0.getPos().getMinBlockX();
      int var5 = var0.getPos().getMinBlockZ();
      short var6 = 0;
      int var7 = Integer.MIN_VALUE;
      int var8 = Integer.MIN_VALUE;
      int var9 = Integer.MIN_VALUE;

      for (int var10 = 0; var10 < var2.length && var1.size() < 12000; var10++) {
         LevelChunkSection var11 = var2[var10];
         if (!var11.hasOnlyAir() && var11.maybeHas(var0x -> {
            Block var1x = var0x.getBlock();
            return isAmethyst(var1x) || isGeodeAnchor(var1x);
         })) {
            int var12 = var3 + var10 << 4;
            var6 = (short)(var6 + 4096);

            for (int var13 = 0; var13 < 16; var13++) {
               for (int var14 = 0; var14 < 16; var14++) {
                  for (int var15 = 0; var15 < 16; var15++) {
                     Block var16 = var11.getBlockState(var15, var13, var14).getBlock();
                     int var17 = var4 + var15;
                     int var18 = var12 + var13;
                     int var19 = var5 + var14;
                     if (isAmethyst(var16)) {
                        var1.add(new ClusterEspRenderer.Hit(var17, var18, var19));
                     }

                     if (var7 == Integer.MIN_VALUE && isGeodeAnchor(var16)) {
                        var7 = var17;
                        var8 = var18;
                        var9 = var19;
                     }

                     if (var1.size() >= 12000) {
                        return var6;
                     }
                  }
               }
            }
         }
      }

      if (var7 != Integer.MIN_VALUE) {
         scanMaskedClusters(var7, var8, var9, var1);
      }

      return var6;
   }

   private static boolean isGeodeAnchor(Block var0) {
      return var0 == Blocks.AMETHYST_BLOCK || var0 == Blocks.BUDDING_AMETHYST;
   }

   private static void scanMaskedClusters(int var0, int var1, int var2, List<ClusterEspRenderer.Hit> var3) {
      Minecraft var4 = Minecraft.getInstance();
      if (var4.level != null) {
         byte var5 = 8;

         for (int var6 = var1 - var5; var6 <= var1 + var5 && var3.size() < 12000; var6++) {
            for (int var7 = var0 - var5; var7 <= var0 + var5 && var3.size() < 12000; var7++) {
               for (int var8 = var2 - var5; var8 <= var2 + var5 && var3.size() < 12000; var8++) {
                  BlockPos var9 = new BlockPos(var7, var6, var8);
                  BlockState var10 = var4.level.getBlockState(var9);
                  if (var10.isAir() || var10.is(Blocks.AMETHYST_CLUSTER)) {
                     int var11 = var4.level.getLightEngine().getRawBrightness(var9, 0);
                     if (var11 == 4) {
                        int var12 = 0;
                        boolean var13 = false;

                        for (int[] var17 : DIRECTIONS) {
                           BlockPos var18 = var9.offset(var17[0], var17[1], var17[2]);
                           int var19 = var4.level.getLightEngine().getRawBrightness(var18, 0);
                           var12 = Math.max(var12, var19);
                           BlockState var20 = var4.level.getBlockState(var18);
                           if (var19 == 4 && (var20.isAir() || var20.is(Blocks.AMETHYST_CLUSTER))) {
                              var13 = true;
                           }
                        }

                        if (var12 == 4 && var13) {
                           var3.add(new ClusterEspRenderer.Hit(var7, var6, var8));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void scanPendingChunks() {
      Minecraft var0 = Minecraft.getInstance();
      if (var0.level != null) {
         ArrayList<ClusterEspRenderer.ChunkPos> var1 = new ArrayList<>();
         synchronized (PENDING_LOCK) {
            Iterator var3 = pendingChunks.iterator();

            while (var3.hasNext() && var1.size() < 4) {
               var1.add((ClusterEspRenderer.ChunkPos)var3.next());
               var3.remove();
            }
         }

         ArrayList<ClusterEspRenderer.ChunkPos> var12 = new ArrayList<>();

         for (ClusterEspRenderer.ChunkPos var4 : var1) {
            LevelChunk var5 = var0.level.getChunkSource().getChunk(var4.x(), var4.z(), false);
            if (var5 == null) {
               var12.add(var4);
            } else {
               retainedUntil.keySet().removeIf(var1x -> var1x.x() >> 4 == var4.x() && var1x.z() >> 4 == var4.z());
               ArrayList<ClusterEspRenderer.Hit> var6 = new ArrayList<>();
               scanChunk(var5, var6);

               for (ClusterEspRenderer.Hit var8 : var6) {
                  if (retainedUntil.containsKey(var8) || retainedUntil.size() < 12000) {
                     retainedUntil.put(var8, retentionTick + 400);
                  }
               }
            }
         }

         synchronized (PENDING_LOCK) {
            pendingChunks.addAll(var12);
         }
      }
   }

   private static void refreshRetained() {
      for (ClusterEspRenderer.Hit var1 : SCAN.get()) {
         if (retainedUntil.containsKey(var1) || retainedUntil.size() < 12000) {
            retainedUntil.put(var1, retentionTick + 400);
         }
      }

      retainedUntil.entrySet().removeIf(var0 -> var0.getValue() <= retentionTick);
      List var2 = List.copyOf(retainedUntil.keySet());
      if (!var2.equals(retained)) {
         retained = var2;
         revision++;
      }
   }

   private static List<ClusterEspRenderer.Cluster> build(List<ClusterEspRenderer.Hit> var0, int var1) {
      if (var0.isEmpty()) {
         return List.of();
      } else {
         HashMap<ClusterEspRenderer.ChunkPos, ClusterEspRenderer.Accumulator> var2 = new HashMap<>();

         for (ClusterEspRenderer.Hit var4 : var0) {
            var2.computeIfAbsent(new ClusterEspRenderer.ChunkPos(var4.x() >> 4, var4.z() >> 4), var0x -> new ClusterEspRenderer.Accumulator()).add(var4);
         }

         ArrayList<ClusterEspRenderer.Cluster> var6 = new ArrayList<>();

         for (ClusterEspRenderer.Accumulator var5 : var2.values()) {
            if (var5.count >= var1) {
               var6.add(var5.finish());
            }
         }

         var6.sort(Comparator.comparingInt(ClusterEspRenderer.Cluster::count).reversed());
         if (var6.size() > 512) {
            var6.subList(512, var6.size()).clear();
         }

         return List.copyOf(var6);
      }
   }

   public static void render(BufferSource var0, PoseStack var1, Vec3 var2, ClusterEspModule var3) {
      List<ClusterEspRenderer.Cluster> var4 = clusters;
      if (!var4.isEmpty()) {
         int var5 = var3.color.get() & 16777215;
         int var6 = var5 | Math.clamp((long)var3.fillAlpha.getInt(), 0, 255) << 24;
         int var7 = var5 | Math.clamp((long)var3.outlineAlpha.getInt(), 0, 255) << 24;
         boolean var8 = var3.tracers.get();
         Vector3fc var9 = var8 ? Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector() : null;

         for (ClusterEspRenderer.Cluster var11 : var4) {
            for (ClusterEspRenderer.Hit var13 : var11.hits()) {
               double var14 = 0.025;
               EspBoxRenderer.fill(
                  var0,
                  var1,
                  var2,
                  (double)var13.x() + var14,
                  (double)var13.y() + var14,
                  (double)var13.z() + var14,
                  (double)var13.x() + 1.0 - var14,
                  (double)var13.y() + 1.0 - var14,
                  (double)var13.z() + 1.0 - var14,
                  var6
               );
               EspBoxRenderer.outline(
                  var0,
                  var1,
                  var2,
                  (double)var13.x() + var14,
                  (double)var13.y() + var14,
                  (double)var13.z() + var14,
                  (double)var13.x() + 1.0 - var14,
                  (double)var13.y() + 1.0 - var14,
                  (double)var13.z() + 1.0 - var14,
                  var7,
                  1.8F
               );
            }

            if (var8) {
               EspBoxRenderer.tracer(var0, var1, var2, var9, var11.centerX(), var11.centerY(), var11.centerZ(), var7, 1.25F);
            }
         }

         EspBoxRenderer.flush(var0);
      }
   }

   private static final class Accumulator {
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;
      int maxZ = Integer.MIN_VALUE;
      int count;
      final ArrayList<ClusterEspRenderer.Hit> hits = new ArrayList<>();

      void add(ClusterEspRenderer.Hit var1) {
         this.minX = Math.min(this.minX, var1.x());
         this.minY = Math.min(this.minY, var1.y());
         this.minZ = Math.min(this.minZ, var1.z());
         this.maxX = Math.max(this.maxX, var1.x());
         this.maxY = Math.max(this.maxY, var1.y());
         this.maxZ = Math.max(this.maxZ, var1.z());
         this.hits.add(var1);
         this.count++;
      }

      ClusterEspRenderer.Cluster finish() {
         return new ClusterEspRenderer.Cluster(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, this.count, List.copyOf(this.hits));
      }
   }

   private static record ChunkPos(int x, int z) {
   }

   private static record Cluster(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int count, List<ClusterEspRenderer.Hit> hits) {
      double centerX() {
         return ((double)this.minX + (double)this.maxX + 1.0) * 0.5;
      }

      double centerY() {
         return ((double)this.minY + (double)this.maxY + 1.0) * 0.5;
      }

      double centerZ() {
         return ((double)this.minZ + (double)this.maxZ + 1.0) * 0.5;
      }
   }

   private static record Hit(int x, int y, int z) {
   }
}
