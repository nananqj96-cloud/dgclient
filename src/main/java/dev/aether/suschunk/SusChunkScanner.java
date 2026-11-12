package dev.aether.suschunk;

import dev.aether.AetherClient;
import dev.aether.module.Modules;
import dev.aether.util.UiSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class SusChunkScanner {
   private static final int[] SENSITIVITY_THRESHOLDS = new int[]{25, 32, 39, 46, 54, 61, 68, 75, 82, 89, 96, 104, 111, 118, 125};
   private static final int GEODE_LINK_DISTANCE = 12;
   private static final int EVIDENCE_SPACING = 4;
   private static final int MAX_REMEMBERED_CHUNKS = 8192;
   private static final double GEODE_SCORE_CAP = 512.0;
   private static final double NEARBY_BASE_BONUS = 12.0;
   private static final double NEARBY_LOCAL_RATIO = 0.75;
   private static final double FULLNESS_SPAN_TARGET = 8.0;
   private static final double FULLNESS_ACTIVITY_TARGET = 54.0;
   private static final double CAP_VERTICAL_SPAN_TARGET = 5.0;
   private static final double CAP_ACTIVITY_TARGET = 36.0;
   private static final double FULL_ROOF_SPAN_TARGET = 8.0;
   private static final double FULL_ROOF_ACTIVITY_TARGET = 18.0;
   private static final double HALF_RENDERED_ROOF_MULTIPLIER = 2.0;
   private static final boolean DEBUG_LOG = Boolean.getBoolean("aether.sus.debug");
   private final Modules.SusChunkFinderModule module;
   private final Map<Long, SusChunkScanner.ChunkScore> scores = new ConcurrentHashMap<>();
   private final Deque<Long> queue = new ArrayDeque<>();
   private final Set<Long> lightRescans = ConcurrentHashMap.newKeySet();
   private final Set<Long> alertedChunks = new HashSet<>();
   private volatile List<SusChunkScanner.Flag> flags = List.of();
   private volatile List<SusChunkScanner.Zone> zones = List.of();
   private ChunkPos lastQueueCenter;
   private ClientLevel scoreLevel;
   private int tickCounter;
   private int lastSignalFingerprint = Integer.MIN_VALUE;

   public SusChunkScanner(Modules.SusChunkFinderModule var1) {
      this.module = var1;
      ServerLightCache.get().addDirtyListener(this.lightRescans::add);
   }

   public List<SusChunkScanner.Flag> flags() {
      return this.flags;
   }

   public List<SusChunkScanner.Zone> zones() {
      return this.zones;
   }

   public int threshold() {
      return thresholdForSensitivity(this.module.sensitivity.getInt());
   }

   public static int thresholdForSensitivity(int var0) {
      int var1 = Math.clamp((long)var0, 1, 15);
      return SENSITIVITY_THRESHOLDS[var1 - 1];
   }

   public void requestRescan(int var1, int var2) {
      this.lightRescans.add(ChunkPos.asLong(var1, var2));
   }

   public void clear() {
      this.scores.clear();
      this.queue.clear();
      this.lightRescans.clear();
      this.alertedChunks.clear();
      this.flags = List.of();
      this.zones = List.of();
      this.lastQueueCenter = null;
      this.scoreLevel = null;
   }

   public void tick() {
      Minecraft var1 = Minecraft.getInstance();
      if (var1.level != null && var1.player != null) {
         try {
            if (this.scoreLevel != var1.level) {
               this.clear();
               ServerLightCache.get().clear();
               this.scoreLevel = var1.level;
            }

            this.invalidateForSignalChanges();
            this.refillQueueIfNeeded(var1);
            int var2 = this.module.scanSpeed.getInt();
            long var3 = System.nanoTime() + 2000000L;
            int var5 = this.scanLightRescans(var1, var2, var3);
            this.scanQueue(var1, var2 - var5, var3);
            if (++this.tickCounter % 20 == 0) {
               this.rebuild(var1);
            }
         } catch (Exception var61) {
            AetherClient.LOGGER.warn("SusChunkFinder scan error: {}", var61.toString());
         }
      }
   }

   private void invalidateForSignalChanges() {
      int var1 = 1;
      var1 = 31 * var1 + (this.module.amethyst.get() ? 1 : 0);
      var1 = 31 * var1 + (this.module.kelp.get() ? 1 : 0);
      var1 = 31 * var1 + (this.module.bamboo.get() ? 1 : 0);
      var1 = 31 * var1 + (this.module.berries.get() ? 1 : 0);
      var1 = 31 * var1 + (this.module.vines.get() ? 1 : 0);
      var1 = 31 * var1 + (this.module.dripstone.get() ? 1 : 0);
      if (var1 != this.lastSignalFingerprint) {
         this.lastSignalFingerprint = var1;
         this.clear();
      }
   }

   private int scanLightRescans(Minecraft var1, int var2, long var3) {
      if (this.lightRescans.isEmpty()) {
         return 0;
      } else {
         int var5 = 0;
         ArrayList var6 = new ArrayList();
         Iterator var7 = this.lightRescans.iterator();

         while (var7.hasNext() && var5 < var2 && System.nanoTime() < var3) {
            long var8 = (Long)var7.next();
            var7.remove();
            LevelChunk var10 = var1.level.getChunkSource().getChunk(ChunkPos.getX(var8), ChunkPos.getZ(var8), false);
            if (var10 == null) {
               var6.add(var8);
            } else {
               this.scores.put(var8, this.scanChunk(var1, var10));
               var5++;
            }
         }

         this.lightRescans.addAll(var6);
         return var5;
      }
   }

   private void scanQueue(Minecraft var1, int var2, long var3) {
      int var5 = 0;

      for (int var6 = 0; var5 < var2 && var6 < 128 && !this.queue.isEmpty() && System.nanoTime() < var3; var6++) {
         long var8 = this.queue.pollFirst();
         LevelChunk var7;
         if (!this.scores.containsKey(var8) && (var7 = var1.level.getChunkSource().getChunk(ChunkPos.getX(var8), ChunkPos.getZ(var8), false)) != null) {
            this.scores.put(var8, this.scanChunk(var1, var7));
            var5++;
         }
      }
   }

   private void refillQueueIfNeeded(Minecraft var1) {
      ChunkPos var2 = var1.player.chunkPosition();
      if (this.queue.isEmpty()
         || this.lastQueueCenter == null
         || Math.max(Math.abs(var2.x - this.lastQueueCenter.x), Math.abs(var2.z - this.lastQueueCenter.z)) >= 3) {
         this.lastQueueCenter = var2;
         this.queue.clear();
         int var3 = Math.min((Integer)var1.options.renderDistance().get() + 1, 16);
         ArrayList<Long> var4 = new ArrayList<>();

         for (int var5 = -var3; var5 <= var3; var5++) {
            for (int var6 = -var3; var6 <= var3; var6++) {
               long var7 = ChunkPos.asLong(var2.x + var5, var2.z + var6);
               if (!this.scores.containsKey(var7)) {
                  var4.add(var7);
               }
            }
         }

         var4.sort(Comparator.comparingDouble(var1x -> Math.hypot((double)(ChunkPos.getX(var1x) - var2.x), (double)(ChunkPos.getZ(var1x) - var2.z))));
         this.queue.addAll(var4);
      }
   }

   private static boolean isPlantTarget(BlockState var0) {
      Block var1 = var0.getBlock();
      return var1 == Blocks.KELP
         || var1 == Blocks.KELP_PLANT
         || var1 == Blocks.BAMBOO
         || var1 == Blocks.SWEET_BERRY_BUSH
         || var1 == Blocks.VINE
         || var1 == Blocks.POINTED_DRIPSTONE;
   }

   private static boolean isAmethystStructure(BlockState var0) {
      return var0.is(Blocks.BUDDING_AMETHYST) || var0.is(Blocks.AMETHYST_BLOCK);
   }

   private static boolean isAmethystSignal(BlockState var0) {
      Block var1 = var0.getBlock();
      return var1 == Blocks.AMETHYST_CLUSTER || var1 == Blocks.LARGE_AMETHYST_BUD || var1 == Blocks.MEDIUM_AMETHYST_BUD || var1 == Blocks.SMALL_AMETHYST_BUD;
   }

   private static boolean isGeodeEvidence(BlockState var0) {
      return isAmethystSignal(var0) || isAmethystStructure(var0) || var0.is(Blocks.SMOOTH_BASALT);
   }

   private static void addGeodeEvidence(Map<BlockPos, Double> var0, BlockPos var1, double var2) {
      Iterator var4 = var0.entrySet().iterator();

      while (var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         BlockPos var6 = (BlockPos)var5.getKey();
         int var7 = Math.max(Math.abs(var6.getX() - var1.getX()), Math.max(Math.abs(var6.getY() - var1.getY()), Math.abs(var6.getZ() - var1.getZ())));
         if (var7 < 4) {
            if (var2 <= (Double)var5.getValue()) {
               return;
            }

            var4.remove();
            break;
         }
      }

      if (var0.size() >= SusChunkScanner.SignalType.AMETHYST.cap) {
         Entry var8 = null;

         for (Entry var10 : var0.entrySet()) {
            if (var8 == null || (Double)var10.getValue() < (Double)var8.getValue()) {
               var8 = var10;
            }
         }

         if (var8 == null || var2 <= (Double)var8.getValue()) {
            return;
         }

         var0.remove(var8.getKey());
      }

      var0.put(var1, var2);
   }

   private SusChunkScanner.ChunkScore scanChunk(Minecraft var1, LevelChunk var2) {
      SusChunkScanner.ChunkScore var3 = new SusChunkScanner.ChunkScore(var2.getPos().toLong());
      if (this.module.amethyst.get()) {
         this.detectAmethyst(var1, var2, var3);
      }

      this.detectGrowth(var1, var2, var3);
      var3.computeScore();
      if (DEBUG_LOG && var3.score > 0.0) {
         AetherClient.LOGGER.info("[sus] chunk {} score={} hits={}", new Object[]{var2.getPos(), var3.score, var3.hits});
      }

      return var3;
   }

   private void detectAmethyst(Minecraft var1, LevelChunk var2, SusChunkScanner.ChunkScore var3) {
      HashMap<BlockPos, Double> var4 = new HashMap<>();
      LevelChunkSection[] var5 = var2.getSections();
      int var6 = var2.getMinSectionY();
      int var7 = var2.getPos().getMinBlockX();
      int var8 = var2.getPos().getMinBlockZ();
      MutableBlockPos var9 = new MutableBlockPos();
      MutableBlockPos var10 = new MutableBlockPos();

      for (int var11 = 0; var11 < var5.length; var11++) {
         LevelChunkSection var12 = var5[var11];
         if (!var12.hasOnlyAir() && var12.getStates().maybeHas(SusChunkScanner::isGeodeEvidence)) {
            int var13 = var6 + var11 << 4;

            for (int var14 = 0; var14 < 16; var14++) {
               for (int var15 = 0; var15 < 16; var15++) {
                  for (int var16 = 0; var16 < 16; var16++) {
                     BlockState var17 = var12.getBlockState(var16, var14, var15);
                     if (isGeodeEvidence(var17)) {
                        var9.set(var7 + var16, var13 + var14, var8 + var15);
                        if (isAmethystStructure(var17)) {
                           addGeodeEvidence(var4, var9.immutable(), 2.0);
                        } else if (var17.is(Blocks.SMOOTH_BASALT)) {
                           addGeodeEvidence(var4, var9.immutable(), 1.0);
                        } else if (hasAmethystNeighbour(var1, var9, var10)) {
                           BlockPos var18 = var9.immutable();
                           addGeodeEvidence(var4, var18, 3.0);
                           if (DEBUG_LOG) {
                              AetherClient.LOGGER.info("[SusChunk] amethyst growth at {}", var18);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      ServerLightCache var19 = ServerLightCache.get();

      for (BlockPos var22 : var19.light5Positions(var7 >> 4, var8 >> 4)) {
         if (isLevelFiveLightSource(var19, var22)) {
            addGeodeEvidence(var4, var22, 5.0);
            if (DEBUG_LOG) {
               AetherClient.LOGGER.info("[SusChunk] hidden level-5 amethyst candidate at {}", var22);
            }
         }
      }

      for (Entry<BlockPos, Double> var23 : var4.entrySet()) {
         var3.amethystCells.add(new SusChunkScanner.AmethystEvidence(var23.getKey(), var23.getValue()));
      }
   }

   private static boolean isLevelFiveLightSource(ServerLightCache var0, BlockPos var1) {
      int var2 = var1.getX();
      int var3 = var1.getY();
      int var4 = var1.getZ();
      return var0.serverBlockLight(var2 + 1, var3, var4) <= 5
         && var0.serverBlockLight(var2 - 1, var3, var4) <= 5
         && var0.serverBlockLight(var2, var3 + 1, var4) <= 5
         && var0.serverBlockLight(var2, var3 - 1, var4) <= 5
         && var0.serverBlockLight(var2, var3, var4 + 1) <= 5
         && var0.serverBlockLight(var2, var3, var4 - 1) <= 5;
   }

   private static boolean hasAmethystNeighbour(Minecraft var0, BlockPos var1, MutableBlockPos var2) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               if (var3 != 0 || var4 != 0 || var5 != 0) {
                  var2.set(var1.getX() + var3, var1.getY() + var4, var1.getZ() + var5);
                  if (isAmethystStructure(var0.level.getBlockState(var2))) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private void detectGrowth(Minecraft var1, LevelChunk var2, SusChunkScanner.ChunkScore var3) {
      LevelChunkSection[] var4 = var2.getSections();
      int var5 = var2.getMinSectionY();
      int var6 = SectionPos.blockToSectionCoord(-12);
      int var7 = SectionPos.blockToSectionCoord(80);
      int var8 = var2.getPos().getMinBlockX();
      int var9 = var2.getPos().getMinBlockZ();
      MutableBlockPos var10 = new MutableBlockPos();

      for (int var11 = 0; var11 < var4.length; var11++) {
         int var13 = var5 + var11;
         LevelChunkSection var12;
         if (var13 >= var6 && var13 <= var7 && !(var12 = var4[var11]).hasOnlyAir() && var12.getStates().maybeHas(SusChunkScanner::isPlantTarget)) {
            int var14 = var13 << 4;

            for (int var15 = 0; var15 < 16; var15++) {
               int var16 = var14 + var15;
               if (var16 >= -12 && var16 <= 80) {
                  for (int var17 = 0; var17 < 16; var17++) {
                     for (int var18 = 0; var18 < 16; var18++) {
                        BlockState var19 = var12.getBlockState(var18, var15, var17);
                        if (isPlantTarget(var19)) {
                           var10.set(var8 + var18, var16, var9 + var17);
                           this.inspectPlant(var1, var19, var10, var3);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void inspectPlant(Minecraft var1, BlockState var2, BlockPos var3, SusChunkScanner.ChunkScore var4) {
      Block var5 = var2.getBlock();
      if ((var5 == Blocks.KELP || var5 == Blocks.KELP_PLANT) && this.module.kelp.get()) {
         BlockState var7 = var1.level.getBlockState(var3.above());
         if (var7.is(Blocks.KELP) || var7.is(Blocks.KELP_PLANT)) {
            return;
         }

         int var8 = columnLength(var1, var3, Direction.DOWN, Blocks.KELP, Blocks.KELP_PLANT);
         boolean var12 = var5 == Blocks.KELP && var2.hasProperty(BlockStateProperties.AGE_25) && (Integer)var2.getValue(BlockStateProperties.AGE_25) == 25;
         if (var12 && var8 >= 8 || var8 >= 14) {
            var4.add(SusChunkScanner.SignalType.KELP, var3);
            if (DEBUG_LOG) {
               AetherClient.LOGGER.info("[sus] KELP @{} height={} maxAge={}", new Object[]{var3, var8, var12});
            }
         }
      } else if (var5 == Blocks.BAMBOO && this.module.bamboo.get()) {
         if (var1.level.getBlockState(var3.below()).is(Blocks.BAMBOO)) {
            return;
         }

         int var11 = columnLength(var1, var3, Direction.UP, Blocks.BAMBOO);
         if (var11 >= 12) {
            var4.add(SusChunkScanner.SignalType.BAMBOO, var3);
            if (DEBUG_LOG) {
               AetherClient.LOGGER.info("[sus] BAMBOO @{} height={}", var3, var11);
            }
         }
      } else if (var5 == Blocks.SWEET_BERRY_BUSH && this.module.berries.get()) {
         if (var2.hasProperty(BlockStateProperties.AGE_3) && (Integer)var2.getValue(BlockStateProperties.AGE_3) == 3) {
            var4.add(SusChunkScanner.SignalType.BERRIES, var3);
            if (DEBUG_LOG) {
               AetherClient.LOGGER.info("[sus] BERRIES max age @{}", var3);
            }
         }
      } else if (var5 == Blocks.VINE && this.module.vines.get()) {
         if (var1.level.getBlockState(var3.above()).is(Blocks.VINE)) {
            return;
         }

         int var10 = columnLength(var1, var3, Direction.DOWN, Blocks.VINE);
         if (var10 >= 7) {
            var4.add(SusChunkScanner.SignalType.VINES, var3);
            if (DEBUG_LOG) {
               AetherClient.LOGGER.info("[sus] VINES @{} hang={}", var3, var10);
            }
         }
      } else if (var5 == Blocks.POINTED_DRIPSTONE && this.module.dripstone.get()) {
         if (var1.level.getBlockState(var3.above()).is(Blocks.POINTED_DRIPSTONE) || !var1.level.getBlockState(var3.below()).is(Blocks.POINTED_DRIPSTONE)) {
            return;
         }

         int var6 = columnLength(var1, var3, Direction.DOWN, Blocks.POINTED_DRIPSTONE);
         if (var6 >= 5) {
            var4.add(SusChunkScanner.SignalType.DRIPSTONE, var3);
            if (DEBUG_LOG) {
               AetherClient.LOGGER.info("[sus] DRIPSTONE @{} length={}", var3, var6);
            }
         }
      }
   }

   private static int columnLength(Minecraft var0, BlockPos var1, Direction var2, Block... var3) {
      MutableBlockPos var5 = var1.mutable();

      int var4;
      for (var4 = 1; var4 < 40; var4++) {
         var5.move(var2);
         BlockState var6 = var0.level.getBlockState(var5);

         for (Block var10 : var3) {
            if (var6.is(var10)) {
               break;
            }
         }
      }

      return var4;
   }

   private void rebuild(Minecraft var1) {
      this.trimRememberedScores(var1);
      int var2 = this.threshold();
      int var3 = Math.min(var2, 50);
      HashMap<Long, SusChunkScanner.FlagAggregate> var4 = new HashMap<>();

      for (SusChunkScanner.ChunkScore var6 : this.scores.values()) {
         if (var6.score >= (double)var3) {
            SusChunkScanner.FlagAggregate var7 = var4.computeIfAbsent(var6.chunkKey, SusChunkScanner.FlagAggregate::new);
            var7.score = Math.max(var7.score, var6.score);
            var7.hitWeight = var7.hitWeight + var6.hitWeight;
            var7.hitX = var7.hitX + var6.hitX;
            var7.hitZ = var7.hitZ + var6.hitZ;
         }
      }

      for (SusChunkScanner.Geode var24 : this.clusterGeodes()) {
         HashMap<Long, Double> var26 = new HashMap<>();

         for (SusChunkScanner.AmethystEvidence var9 : var24.cells()) {
            BlockPos var10 = var9.pos();
            long var11 = ChunkPos.asLong(var10.getX() >> 4, var10.getZ() >> 4);
            var26.merge(var11, var9.weight(), Double::sum);
         }

         HashSet<Long> var28 = new HashSet<>();

         for (Entry<Long, Double> var31 : var26.entrySet()) {
            long var33 = var31.getKey();
            double var13 = var31.getValue() * var24.fullnessMultiplier();
            double var15 = Math.max(0.0, var24.score() - var13);
            double var17 = Math.min(var15, Math.max(12.0 * var24.fullnessMultiplier(), var13 * 0.75));
            double var19 = var13 + var17;
            if (!(var19 < (double)var2)) {
               SusChunkScanner.FlagAggregate var21 = var4.computeIfAbsent(var33, SusChunkScanner.FlagAggregate::new);
               var21.score = Math.max(var21.score, var19);
               var28.add(var33);
            }
         }

         for (SusChunkScanner.AmethystEvidence var32 : var24.cells()) {
            BlockPos var34 = var32.pos();
            long var12 = ChunkPos.asLong(var34.getX() >> 4, var34.getZ() >> 4);
            if (var28.contains(var12)) {
               SusChunkScanner.FlagAggregate var14 = var4.get(var12);
               double var35 = var32.weight();
               var14.hitWeight += var35;
               var14.hitX = var14.hitX + ((double)var34.getX() + 0.5) * var35;
               var14.hitZ = var14.hitZ + ((double)var34.getZ() + 0.5) * var35;
            }
         }

         if (DEBUG_LOG) {
            AetherClient.LOGGER
               .info(
                  "[sus] GEODE {} cells / {} chunks, score {} -> FLAG",
                  new Object[]{var24.cells().size(), var24.chunks().size(), var24.score()}
               );
            AetherClient.LOGGER.info("[SusChunk] geode fullness={} multiplier={}", var24.fullness(), var24.fullnessMultiplier());
         }
      }

      ArrayList<SusChunkScanner.Flag> var23 = new ArrayList<>();

      for (SusChunkScanner.FlagAggregate var27 : var4.values()) {
         var23.add(new SusChunkScanner.Flag(var27.chunkKey, var27.score));
      }

      this.flags = List.copyOf(var23);
      this.zones = this.buildZones(var23, var4);
      this.fireAlerts(var1);
   }

   private void trimRememberedScores(Minecraft var1) {
      if (this.scores.size() > 8192) {
         ChunkPos var2 = var1.player.chunkPosition();
         ArrayList<Long> var3 = new ArrayList<>(this.scores.keySet());
         var3.sort(Comparator.comparingLong(var1x -> {
            long var2x = (long)ChunkPos.getX(var1x) - (long)var2.x;
            long var4x = (long)ChunkPos.getZ(var1x) - (long)var2.z;
            return var2x * var2x + var4x * var4x;
         }));

         for (int var4 = 8192; var4 < var3.size(); var4++) {
            long var5 = var3.get(var4);
            this.scores.remove(var5);
            this.alertedChunks.remove(var5);
         }
      }
   }

   private List<SusChunkScanner.Geode> clusterGeodes() {
      ArrayList<SusChunkScanner.AmethystEvidence> var2 = new ArrayList<>();
      HashMap<SusChunkScanner.EvidenceBucket, List<Integer>> var3 = new HashMap<>();

      for (SusChunkScanner.ChunkScore var5 : this.scores.values()) {
         for (SusChunkScanner.AmethystEvidence var7 : var5.amethystCells) {
            addGlobalEvidence(var2, var3, var7);
         }
      }

      int var36 = var2.size();
      if (var36 == 0) {
         return List.of();
      } else {
         int[] var37 = new int[var36];
         int var1 = 0;

         while (var1 < var36) {
            var37[var1] = var1++;
         }

         HashMap<SusChunkScanner.EvidenceBucket, List<Integer>> var38 = new HashMap<>();

         for (int var35 = 0; var35 < var36; var35++) {
            BlockPos var39 = var2.get(var35).pos();
            SusChunkScanner.EvidenceBucket var8 = bucket(var39, 12);

            for (int var9 = -1; var9 <= 1; var9++) {
               for (int var10 = -1; var10 <= 1; var10++) {
                  for (int var11 = -1; var11 <= 1; var11++) {
                     List<Integer> var12 = var38.get(new SusChunkScanner.EvidenceBucket(var8.x() + var9, var8.y() + var10, var8.z() + var11));
                     if (var12 != null) {
                        for (int var14 : var12) {
                           BlockPos var15 = var2.get(var14).pos();
                           int var16 = Math.max(
                              Math.abs(var39.getX() - var15.getX()), Math.max(Math.abs(var39.getY() - var15.getY()), Math.abs(var39.getZ() - var15.getZ()))
                           );
                           if (var16 <= 12) {
                              var37[find(var37, var35)] = find(var37, var14);
                           }
                        }
                     }
                  }
               }
            }

            var38.computeIfAbsent(var8, var0 -> new ArrayList<>()).add(var35);
         }

         HashMap<Integer, List<SusChunkScanner.AmethystEvidence>> var40 = new HashMap<>();

         for (int var41 = 0; var41 < var36; var41++) {
            var40.computeIfAbsent(find(var37, var41), var0 -> new ArrayList<>()).add(var2.get(var41));
         }

         ArrayList<SusChunkScanner.Geode> var42 = new ArrayList<>();

         for (List<SusChunkScanner.AmethystEvidence> var44 : var40.values()) {
            HashSet var45 = new HashSet();
            double var46 = 0.0;
            double var47 = 0.0;
            double var48 = 0.0;
            double var18 = 0.0;

            for (SusChunkScanner.AmethystEvidence var21 : var44) {
               BlockPos var22 = var21.pos();
               var45.add(ChunkPos.asLong(var22.getX() >> 4, var22.getZ() >> 4));
               var46 += ((double)var22.getX() + 0.5) * var21.weight();
               var47 += ((double)var22.getY() + 0.5) * var21.weight();
               var48 += ((double)var22.getZ() + 0.5) * var21.weight();
               var18 += var21.weight();
            }

            double var49 = Math.max(1.0, var18);
            double var50 = var46 / var49;
            double var24 = var47 / var49;
            double var26 = var48 / var49;
            SusChunkScanner.GeodeShape var28 = geodeFullness(var44, var50, var24, var26, var18);
            double var29 = var28.fullness();
            double var31 = var28.multiplier();
            double var33 = Math.min(var18 * var31, 512.0);
            var42.add(new SusChunkScanner.Geode(List.copyOf(var44), Set.copyOf(var45), var33, var50, var26, var29, var31));
         }

         return var42;
      }
   }

   private static SusChunkScanner.GeodeShape geodeFullness(List<SusChunkScanner.AmethystEvidence> var0, double var1, double var3, double var5, double var7) {
      if (var0.size() < 4) {
         return new SusChunkScanner.GeodeShape(0.0, 1.0);
      } else {
         byte var9 = 0;
         int var10 = 0;
         int var11 = 0;
         int var12 = Integer.MAX_VALUE;
         int var13 = Integer.MAX_VALUE;
         int var14 = Integer.MAX_VALUE;
         int var15 = Integer.MIN_VALUE;
         int var16 = Integer.MIN_VALUE;
         int var17 = Integer.MIN_VALUE;

         for (SusChunkScanner.AmethystEvidence var19 : var0) {
            BlockPos var20 = var19.pos();
            var12 = Math.min(var12, var20.getX());
            var13 = Math.min(var13, var20.getY());
            var14 = Math.min(var14, var20.getZ());
            var15 = Math.max(var15, var20.getX());
            var16 = Math.max(var16, var20.getY());
            var17 = Math.max(var17, var20.getZ());
            double var21 = (double)var20.getX() + 0.5 - var1;
            double var23 = (double)var20.getY() + 0.5 - var3;
            double var25 = (double)var20.getZ() + 0.5 - var5;
            if (var21 >= 1.5) {
               var9 = (byte)(var9 | 1);
            }

            if (var21 <= -1.5) {
               var9 = (byte)(var9 | 2);
            }

            if (var23 >= 1.5) {
               var9 = (byte)(var9 | 4);
            }

            if (var23 <= -1.5) {
               var9 = (byte)(var9 | 8);
            }

            if (var25 >= 1.5) {
               var9 = (byte)(var9 | 16);
            }

            if (var25 <= -1.5) {
               var9 = (byte)(var9 | 32);
            }

            if (Math.abs(var21) >= 1.0 && Math.abs(var23) >= 1.0 && Math.abs(var25) >= 1.0) {
               int var27 = (var21 >= 0.0 ? 1 : 0) | (var23 >= 0.0 ? 2 : 0) | (var25 >= 0.0 ? 4 : 0);
               var10 |= 1 << var27;
            }

            if (Math.abs(var21) >= 1.0 && Math.abs(var25) >= 1.0) {
               int var91 = (var21 >= 0.0 ? 1 : 0) | (var25 >= 0.0 ? 2 : 0);
               var11 |= 1 << var91;
            }
         }

         double var87 = (double)Integer.bitCount(var9) / 6.0;
         double var88 = (double)Integer.bitCount(var10) / 8.0;
         int var22 = Math.min(var15 - var12 + 1, Math.min(var16 - var13 + 1, var17 - var14 + 1));
         double var89 = Math.clamp((double)var22 / 8.0, 0.0, 1.0);
         double var90 = Math.clamp(var7 / 54.0, 0.0, 1.0);
         double var92 = var87 * 0.35 + var88 * 0.35 + var89 * 0.2 + var90 * 0.1;
         double var29 = Math.clamp((var92 - 0.3) / 0.7, 0.0, 1.0);
         int var31 = var9 & 51;
         double var32 = (double)Integer.bitCount(var31) / 4.0;
         double var34 = (double)Integer.bitCount(var11) / 4.0;
         int var36 = Math.min(var15 - var12 + 1, var17 - var14 + 1);
         int var37 = var16 - var13 + 1;
         double var38 = Math.clamp((double)var36 / 8.0, 0.0, 1.0);
         double var40 = Math.clamp((double)var37 / 5.0, 0.0, 1.0);
         double var42 = Math.clamp(var7 / 36.0, 0.0, 1.0);
         double var44 = (double)var13 + (double)(var16 - var13) * 0.55;
         double var46 = 0.0;
         double var48 = 0.0;
         int var50 = 0;
         int var51 = 0;
         int var52 = 0;
         byte var53 = 0;
         int var54 = 0;
         int var55 = 0;
         double var56 = 0.0;
         int var58 = Integer.MAX_VALUE;
         int var59 = Integer.MAX_VALUE;
         int var60 = Integer.MIN_VALUE;
         int var61 = Integer.MIN_VALUE;
         double var62 = (double)var13 + (double)(var16 - var13) * 0.55;

         for (SusChunkScanner.AmethystEvidence var65 : var0) {
            BlockPos var66 = var65.pos();
            double var67 = (double)var66.getX() + 0.5 - var1;
            double var69 = (double)var66.getZ() + 0.5 - var5;
            double var71 = Math.hypot(var67, var69);
            if ((double)var66.getY() > var44) {
               var46 += var71;
               var50++;
            } else {
               var48 += var71;
               var51++;
            }

            if ((double)var66.getY() >= var62 && var65.weight() >= 2.0) {
               var55++;
               var56 += var65.weight();
               var58 = Math.min(var58, var66.getX());
               var59 = Math.min(var59, var66.getZ());
               var60 = Math.max(var60, var66.getX());
               var61 = Math.max(var61, var66.getZ());
               if (var67 >= 1.5) {
                  var53 = (byte)(var53 | 1);
               }

               if (var67 <= -1.5) {
                  var53 = (byte)(var53 | 2);
               }

               if (var69 >= 1.5) {
                  var53 = (byte)(var53 | 4);
               }

               if (var69 <= -1.5) {
                  var53 = (byte)(var53 | 8);
               }

               if (Math.abs(var67) >= 1.0 && Math.abs(var69) >= 1.0) {
                  int var73 = (var67 >= 0.0 ? 1 : 0) | (var69 >= 0.0 ? 2 : 0);
                  var54 |= 1 << var73;
               }
            }

            if (var66.getY() <= var13 + 2 && !(Math.abs(var67) < 1.0) && !(Math.abs(var69) < 1.0)) {
               int var97 = (var67 >= 0.0 ? 1 : 0) | (var69 >= 0.0 ? 2 : 0);
               var52 |= 1 << var97;
            }
         }

         double var93 = (double)Integer.bitCount(var52) / 4.0;
         double var94 = 0.0;
         if (var50 > 0 && var51 > 0) {
            double var68 = var46 / (double)var50;
            double var70 = var48 / (double)var51;
            var94 = Math.clamp((var70 - var68) / Math.max(2.0, (double)var36 * 0.25), 0.0, 1.0);
         }

         double var95 = var32 * 0.2 + var34 * 0.25 + var38 * 0.15 + var40 * 0.1 + var42 * 0.15 + var93 * 0.1 + var94 * 0.05;
         double var96 = Math.clamp((var95 - 0.5) / 0.4, 0.0, 1.0);
         int var72 = var55 == 0 ? 0 : var60 - var58 + 1;
         int var98 = var55 == 0 ? 0 : var61 - var59 + 1;
         double var74 = Math.clamp((double)Math.min(var72, var98) / 8.0, 0.0, 1.0);
         double var76 = Math.clamp(var56 / 18.0, 0.0, 1.0);
         int var78 = Integer.bitCount(var53);
         int var79 = Integer.bitCount(var54);
         boolean var80 = false;

         for (int var81 = 0; var81 < 3 && !var80; var81++) {
            var80 = isConvincingHalfShell(var0, var1, var3, var5, var81, -1) || isConvincingHalfShell(var0, var1, var3, var5, var81, 1);
         }

         if (var80) {
            var96 = Math.max(var96, 0.92);
         }

         boolean var99 = var55 >= 6 && var78 == 4 && var79 == 4 && var74 >= 1.0 && var76 >= 1.0 && var40 >= 0.6;
         boolean var82 = var55 >= 5 && var78 >= 3 && var79 >= 3 && var74 >= 1.0 && var76 >= 0.85 && var40 >= 0.6 && var95 >= 0.6;
         if (var99 || var82) {
            var96 = 1.0;
         }

         double var83 = Math.max(var29, var96);
         double var85 = 1.0 + Math.pow(var83, 1.5);
         if (var80 && !var99) {
            var85 = Math.max(var85, 2.0);
         }

         return new SusChunkScanner.GeodeShape(var83, var85);
      }
   }

   private static boolean isConvincingHalfShell(List<SusChunkScanner.AmethystEvidence> var0, double var1, double var3, double var5, int var7, int var8) {
      double var9 = Double.POSITIVE_INFINITY;
      double var11 = Double.NEGATIVE_INFINITY;

      for (SusChunkScanner.AmethystEvidence var14 : var0) {
         double var15 = coordinate(var14.pos(), var7);
         var9 = Math.min(var9, var15);
         var11 = Math.max(var11, var15);
      }

      if (var11 - var9 < 3.0) {
         return false;
      } else {
         double var58 = var8 > 0 ? var9 + (var11 - var9) * 0.55 : var9 + (var11 - var9) * 0.45;
         double var59 = var8 > 0 ? var9 + (var11 - var9) * 0.45 : var9 + (var11 - var9) * 0.55;
         int var17 = (var7 + 1) % 3;
         int var18 = (var7 + 2) % 3;
         double var19 = var17 == 0 ? var1 : (var17 == 1 ? var3 : var5);
         double var21 = var18 == 0 ? var1 : (var18 == 1 ? var3 : var5);
         byte var23 = 0;
         int var24 = 0;
         int var25 = 0;
         double var26 = 0.0;
         double var28 = 0.0;
         double var30 = Double.POSITIVE_INFINITY;
         double var32 = Double.NEGATIVE_INFINITY;
         double var34 = Double.POSITIVE_INFINITY;
         double var36 = Double.NEGATIVE_INFINITY;
         double var38 = Double.POSITIVE_INFINITY;
         double var40 = Double.NEGATIVE_INFINITY;

         for (SusChunkScanner.AmethystEvidence var43 : var0) {
            if (!(var43.weight() < 2.0)) {
               BlockPos var44 = var43.pos();
               double var45 = coordinate(var44, var7);
               boolean var47 = var8 > 0 ? var45 >= var58 : var45 <= var58;
               boolean var48 = var8 > 0 ? var45 <= var59 : var45 >= var59;
               if (var48) {
                  var28 += var43.weight();
               }

               if (var47) {
                  var25++;
                  var26 += var43.weight();
                  var38 = Math.min(var38, var45);
                  var40 = Math.max(var40, var45);
                  double var49 = coordinate(var44, var17);
                  double var51 = coordinate(var44, var18);
                  var30 = Math.min(var30, var49);
                  var32 = Math.max(var32, var49);
                  var34 = Math.min(var34, var51);
                  var36 = Math.max(var36, var51);
                  double var53 = var49 + 0.5 - var19;
                  double var55 = var51 + 0.5 - var21;
                  if (var53 >= 1.5) {
                     var23 = (byte)(var23 | 1);
                  }

                  if (var53 <= -1.5) {
                     var23 = (byte)(var23 | 2);
                  }

                  if (var55 >= 1.5) {
                     var23 = (byte)(var23 | 4);
                  }

                  if (var55 <= -1.5) {
                     var23 = (byte)(var23 | 8);
                  }

                  if (Math.abs(var53) >= 1.0 && Math.abs(var55) >= 1.0) {
                     int var57 = (var53 >= 0.0 ? 1 : 0) | (var55 >= 0.0 ? 2 : 0);
                     var24 |= 1 << var57;
                  }
               }
            }
         }

         if (var25 == 0) {
            return false;
         } else {
            double var60 = var32 - var30 + 1.0;
            double var61 = var36 - var34 + 1.0;
            double var46 = var40 - var38 + 1.0;
            double var62 = Math.clamp(Math.min(var60, var61) / 8.0, 0.0, 1.0);
            double var50 = Math.clamp(var26 / 18.0, 0.0, 1.0);
            boolean var52 = var28 <= var26 * 0.5;
            return var25 >= 5 && Integer.bitCount(var23) >= 3 && Integer.bitCount(var24) >= 3 && var62 >= 0.85 && var50 >= 0.8 && var46 >= 2.0 && var52;
         }
      }
   }

   private static double coordinate(BlockPos var0, int var1) {
      return switch (var1) {
         case 0 -> (double)var0.getX();
         case 1 -> (double)var0.getY();
         default -> (double)var0.getZ();
      };
   }

   private static void addGlobalEvidence(
      List<SusChunkScanner.AmethystEvidence> var0, Map<SusChunkScanner.EvidenceBucket, List<Integer>> var1, SusChunkScanner.AmethystEvidence var2
   ) {
      SusChunkScanner.EvidenceBucket var3 = bucket(var2.pos(), 4);

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               List<Integer> var7 = var1.get(new SusChunkScanner.EvidenceBucket(var3.x() + var4, var3.y() + var5, var3.z() + var6));
               if (var7 != null) {
                  for (int var9 : var7) {
                     SusChunkScanner.AmethystEvidence var10 = var0.get(var9);
                     BlockPos var11 = var10.pos();
                     BlockPos var12 = var2.pos();
                     int var13 = Math.max(
                        Math.abs(var11.getX() - var12.getX()), Math.max(Math.abs(var11.getY() - var12.getY()), Math.abs(var11.getZ() - var12.getZ()))
                     );
                     if (var13 < 4) {
                        if (var2.weight() > var10.weight()) {
                           var0.set(var9, new SusChunkScanner.AmethystEvidence(var10.pos(), var2.weight()));
                        }

                        return;
                     }
                  }
               }
            }
         }
      }

      int var14 = var0.size();
      var0.add(var2);
      var1.computeIfAbsent(var3, var0x -> new ArrayList<>()).add(var14);
   }

   private static SusChunkScanner.EvidenceBucket bucket(BlockPos var0, int var1) {
      return new SusChunkScanner.EvidenceBucket(Math.floorDiv(var0.getX(), var1), Math.floorDiv(var0.getY(), var1), Math.floorDiv(var0.getZ(), var1));
   }

   private static int find(int[] var0, int var1) {
      while (var0[var1] != var1) {
         var0[var1] = var0[var0[var1]];
         var1 = var0[var1];
      }

      return var1;
   }

   private List<SusChunkScanner.Zone> buildZones(List<SusChunkScanner.Flag> var1, Map<Long, SusChunkScanner.FlagAggregate> var2) {
      int var3 = Math.max(1, this.module.mergeRadius.getInt());
      HashMap var4 = new HashMap();

      for (SusChunkScanner.Flag var6 : var1) {
         var4.put(var6.chunkKey(), var6);
      }

      ArrayList var17 = new ArrayList();
      HashSet var18 = new HashSet();

      for (SusChunkScanner.Flag var8 : var1) {
         if (var18.add(var8.chunkKey())) {
            ArrayList var9 = new ArrayList();
            ArrayDeque var10 = new ArrayDeque<>(List.of(var8));

            while (!var10.isEmpty()) {
               SusChunkScanner.Flag var11 = (SusChunkScanner.Flag)var10.poll();
               var9.add(var11);
               int var12 = ChunkPos.getX(var11.chunkKey());
               int var13 = ChunkPos.getZ(var11.chunkKey());

               for (int var14 = -var3; var14 <= var3; var14++) {
                  for (int var15 = -var3; var15 <= var3; var15++) {
                     SusChunkScanner.Flag var16;
                     if ((var14 != 0 || var15 != 0)
                        && (var16 = (SusChunkScanner.Flag)var4.get(ChunkPos.asLong(var12 + var14, var13 + var15))) != null
                        && var18.add(var16.chunkKey())) {
                        var10.add(var16);
                     }
                  }
               }
            }

            var17.add(this.makeZone(var9, var2));
         }
      }

      var17.sort(Comparator.comparingDouble(SusChunkScanner.Zone::totalScore).reversed());
      return var17;
   }

   private SusChunkScanner.Zone makeZone(List<SusChunkScanner.Flag> var1, Map<Long, SusChunkScanner.FlagAggregate> var2) {
      HashSet var3 = new HashSet();
      double var4 = 0.0;
      double var6 = 0.0;
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;
      double var14 = 0.0;
      double var16 = 0.0;

      for (SusChunkScanner.Flag var19 : var1) {
         var3.add(var19.chunkKey());
         var4 += var19.score();
         var6 = Math.max(var6, var19.score());
         SusChunkScanner.FlagAggregate var20 = var2.get(var19.chunkKey());
         if (var20 != null && var20.hitWeight > 0.0) {
            var8 += var20.hitWeight;
            var10 += var20.hitX;
            var12 += var20.hitZ;
         }

         var14 += (double)(ChunkPos.getX(var19.chunkKey()) * 16 + 8) * var19.score();
         var16 += (double)(ChunkPos.getZ(var19.chunkKey()) * 16 + 8) * var19.score();
      }

      double var22 = var8 > 0.0 ? var10 / var8 : var14 / var4;
      double var23 = var8 > 0.0 ? var12 / var8 : var16 / var4;
      return new SusChunkScanner.Zone(Set.copyOf(var3), var22, var23, var4, var6);
   }

   private void fireAlerts(Minecraft var1) {
      for (SusChunkScanner.Zone var3 : this.zones) {
         boolean var4 = true;

         for (long var6 : var3.members()) {
            if (this.alertedChunks.contains(var6)) {
               var4 = false;
               break;
            }
         }

         if (!var4) {
            this.alertedChunks.addAll(var3.members());
         } else {
            this.alertedChunks.addAll(var3.members());
            if (!this.module.notifications.is("Off")) {
               int var9 = (int)Math.round(var3.centroidX());
               int var10 = (int)Math.round(var3.centroidZ());
               int var7 = (int)Math.hypot((double)var9 - var1.player.getX(), (double)var10 - var1.player.getZ());
               String var8 = "Sus zone · " + var7 + "m · " + var9 + ", " + var10;
               if (this.module.notifications.is("Toast") && AetherClient.notifications() != null) {
                  AetherClient.notifications().pushInfo(var8);
                  UiSounds.notification(true);
               } else if (this.module.notifications.is("Chat")) {
                  var1.player.displayClientMessage(Component.literal("§b[Aether] §f" + var8), false);
               }
            }
         }
      }
   }

   public static record AmethystEvidence(BlockPos pos, double weight) {
   }

   public static final class ChunkScore {
      public final long chunkKey;
      public final EnumMap<SusChunkScanner.SignalType, Integer> hits = new EnumMap<>(SusChunkScanner.SignalType.class);
      public final List<SusChunkScanner.AmethystEvidence> amethystCells = new ArrayList<>();
      public double score;
      double hitWeight;
      double hitX;
      double hitZ;

      ChunkScore(long var1) {
         this.chunkKey = var1;
      }

      void add(SusChunkScanner.SignalType var1, BlockPos var2) {
         this.hits.merge(var1, Integer.valueOf(1), Integer::sum);
         this.hitWeight = this.hitWeight + var1.weight;
         this.hitX = this.hitX + ((double)var2.getX() + 0.5) * var1.weight;
         this.hitZ = this.hitZ + ((double)var2.getZ() + 0.5) * var1.weight;
      }

      void computeScore() {
         double var1 = 0.0;

         for (Entry var4 : this.hits.entrySet()) {
            var1 += ((SusChunkScanner.SignalType)var4.getKey()).weight
               * (double)Math.min((Integer)var4.getValue(), ((SusChunkScanner.SignalType)var4.getKey()).cap);
         }

         this.score = var1;
      }
   }

   private static record EvidenceBucket(int x, int y, int z) {
   }

   public static record Flag(long chunkKey, double score) {
   }

   private static final class FlagAggregate {
      final long chunkKey;
      double score;
      double hitWeight;
      double hitX;
      double hitZ;

      FlagAggregate(long var1) {
         this.chunkKey = var1;
      }
   }

   public static record Geode(
      List<SusChunkScanner.AmethystEvidence> cells,
      Set<Long> chunks,
      double score,
      double centroidX,
      double centroidZ,
      double fullness,
      double fullnessMultiplier
   ) {
   }

   private static record GeodeShape(double fullness, double multiplier) {
   }

   public static enum SignalType {
      AMETHYST(2.0, 64),
      KELP(2.0, 6),
      BAMBOO(2.0, 6),
      BERRIES(2.0, 5),
      VINES(2.0, 6),
      DRIPSTONE(2.0, 5);

      public final double weight;
      public final int cap;

      private SignalType(double nullxx, int nullxxx) {
         this.weight = nullxx;
         this.cap = nullxxx;
      }
   }

   public static record Zone(Set<Long> members, double centroidX, double centroidZ, double totalScore, double maxScore) {
   }
}
