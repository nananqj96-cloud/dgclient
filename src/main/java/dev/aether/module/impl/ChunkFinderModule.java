package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class ChunkFinderModule extends Module {
   private static final int MIN_CHUNK_AGE_TICKS = 200;
   private static final int CHUNKS_PER_TICK = 8;
   public final SliderSetting mergeRadius = this.addSetting(
      new SliderSetting(
         "Merge Radius",
         "Hives within this many chunks of each other merge to one middle flag",
         4.0,
         1.0,
         8.0,
         1.0,
         " ch"
      )
   );
   private final Set<Long> beehiveChunks = ConcurrentHashMap.newKeySet();
   private final Set<Long> notifiedChunks = ConcurrentHashMap.newKeySet();
   private final Map<Long, Integer> firstLoadedTicks = new ConcurrentHashMap<>();
   private volatile Set<Long> displayChunks = Set.of();
   private int scanCursor;
   private int tickCounter;
   private boolean displayDirty;
   private int lastMergeRadius;

   public ChunkFinderModule() {
      super(
         "Chunk Finder",
         "Highlights Donut SMP chunks that match a rare world signal worth checking.",
         Category.RENDER
      );
   }

   @Override
   protected void onEnable() {
      this.clear();
   }

   @Override
   protected void onDisable() {
      this.clear();
   }

   @Override
   public void onTick() {
      Minecraft var1 = Minecraft.getInstance();
      if (var1.level != null && var1.player != null) {
         this.tickCounter++;
         int var2 = (Integer)var1.options.renderDistance().get();
         ChunkPos var3 = var1.player.chunkPosition();
         int var4 = var2 * 2 + 1;
         int var5 = var4 * var4;
         if (var5 > 0) {
            for (int var6 = 0; var6 < 8; var6++) {
               int var7 = this.scanCursor % var5;
               this.scanCursor = (this.scanCursor + 1) % var5;
               int var8 = var7 % var4 - var2;
               int var9 = var7 / var4 - var2;
               int var10 = var3.x + var8;
               int var11 = var3.z + var9;
               LevelChunk var12 = var1.level.getChunkSource().getChunk(var10, var11, false);
               if (var12 != null && !var12.isEmpty()) {
                  long var13 = ChunkPos.asLong(var10, var11);
                  this.firstLoadedTicks.putIfAbsent(var13, this.tickCounter);
                  boolean var15 = this.tickCounter - this.firstLoadedTicks.get(var13) >= 200;
                  if (var15 && hasTargetSignal(var12)) {
                     if (this.beehiveChunks.add(var13)) {
                        this.displayDirty = true;
                     }

                     if (this.notifiedChunks.add(var13)) {
                        this.showToast(new ChunkPos(var10, var11));
                     }
                  } else if (this.beehiveChunks.remove(var13)) {
                     this.displayDirty = true;
                  }
               }
            }

            if (this.beehiveChunks.removeIf(long_ -> outOfRange(long_, var3, var2))) {
               this.displayDirty = true;
            }

            this.firstLoadedTicks.keySet().removeIf(long_ -> outOfRange(long_, var3, var2));
            if (this.displayDirty || this.mergeRadius.getInt() != this.lastMergeRadius) {
               this.rebuildDisplay();
            }
         }
      }
   }

   private void rebuildDisplay() {
      this.displayDirty = false;
      this.lastMergeRadius = this.mergeRadius.getInt();
      HashSet<Long> var1 = new HashSet<>(this.beehiveChunks);
      if (var1.size() < 2) {
         this.displayChunks = Set.copyOf(var1);
      } else {
         int var2 = Math.max(1, this.lastMergeRadius);
         HashSet<Long> var3 = new HashSet<>();
         HashSet<Long> var4 = new HashSet<>();

         for (long var6 : var1) {
            if (var4.add(var6)) {
               ArrayList<Long> var8 = new ArrayList<>();
               ArrayDeque<Long> var9 = new ArrayDeque<>();
               var9.add(var6);

               while (!var9.isEmpty()) {
                  long var10 = (Long)var9.poll();
                  var8.add(var10);
                  int var12 = ChunkPos.getX(var10);
                  int var13 = ChunkPos.getZ(var10);

                  for (int var14 = -var2; var14 <= var2; var14++) {
                     for (int var15 = -var2; var15 <= var2; var15++) {
                        if (var14 != 0 || var15 != 0) {
                           long var16 = ChunkPos.asLong(var12 + var14, var13 + var15);
                           if (var1.contains(var16) && var4.add(var16)) {
                              var9.add(var16);
                           }
                        }
                     }
                  }
               }

               var3.add(var8.size() == 1 ? (Long)var8.get(0) : middleChunk(var8));
            }
         }

         this.displayChunks = Set.copyOf(var3);
      }
   }

   private static long middleChunk(List<Long> list) {
      long var1 = 0L;
      long var3 = 0L;

      for (long var6 : list) {
         var1 += (long)ChunkPos.getX(var6);
         var3 += (long)ChunkPos.getZ(var6);
      }

      int var8 = (int)Math.round((double)var1 / (double)list.size());
      int var9 = (int)Math.round((double)var3 / (double)list.size());
      return ChunkPos.asLong(var8, var9);
   }

   private static boolean hasTargetSignal(LevelChunk levelChunk) {
      for (LevelChunkSection var4 : levelChunk.getSections()) {
         if (var4 != null && !var4.hasOnlyAir() && var4.getStates().maybeHas(ChunkFinderModule::isFullHoney)) {
            for (int var5 = 0; var5 < 16; var5++) {
               for (int var6 = 0; var6 < 16; var6++) {
                  for (int var7 = 0; var7 < 16; var7++) {
                     if (isFullHoney(var4.getBlockState(var5, var7, var6))) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean isFullHoney(BlockState blockState) {
      return (blockState.is(Blocks.BEEHIVE) || blockState.is(Blocks.BEE_NEST))
         && blockState.hasProperty(BlockStateProperties.LEVEL_HONEY)
         && (Integer)blockState.getValue(BlockStateProperties.LEVEL_HONEY) == 5;
   }

   private static boolean outOfRange(long l, ChunkPos chunkPos, int i) {
      return Math.abs(ChunkPos.getX(l) - chunkPos.x) > i || Math.abs(ChunkPos.getZ(l) - chunkPos.z) > i;
   }

   private void showToast(ChunkPos chunkPos) {
      if (AetherClient.notifications() != null) {
         AetherClient.notifications().pushInfo("Chunk Finder · X " + chunkPos.getMiddleBlockX() + " Z " + chunkPos.getMiddleBlockZ());
      }
   }

   public Set<Long> flaggedChunks() {
      return this.displayChunks;
   }

   public boolean isFlagged(int i, int j) {
      long var3 = ChunkPos.asLong(i, j);
      return this.beehiveChunks.contains(var3) || this.displayChunks.contains(var3);
   }

   public void clear() {
      this.beehiveChunks.clear();
      this.notifiedChunks.clear();
      this.firstLoadedTicks.clear();
      this.displayChunks = Set.of();
      this.scanCursor = 0;
      this.tickCounter = 0;
      this.displayDirty = false;
      this.lastMergeRadius = this.mergeRadius.getInt();
   }
}
