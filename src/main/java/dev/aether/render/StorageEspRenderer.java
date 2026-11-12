package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.StorageEspModule;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class StorageEspRenderer {
   private static final int MAX_CHUNK_RADIUS = 32;
   private static final int MAX_RESULTS = 6000;
   private static final int MAX_PENDING_CHUNKS = 512;
   private static final int PENDING_CHUNKS_PER_TICK = 12;
   private static final int PACKET_RESCAN_PASSES = 6;
   private static final int RETAIN_TICKS = 400;
   private static final float BOX_INFLATE = 0.002F;
   private static final double CHEST_INSET = 0.0625;
   private static final int INTERACTED_RGB = 6579300;
   private static final float LINE_WIDTH = 1.0F;
   private static final float TRACER_WIDTH = 1.15F;
   private static final int TRACER_ALPHA = 180;
   private static final IncrementalScan<StorageEspRenderer.Hit> SCAN = new IncrementalScan<>(48, 80000, 20);
   private static final Map<StorageEspRenderer.Hit, Integer> retainedUntil = new LinkedHashMap<>();
   private static final Map<StorageEspRenderer.ChunkPos, Integer> pendingChunks = new LinkedHashMap<>();
   private static final Object PENDING_CHUNKS_LOCK = new Object();
   private static volatile List<StorageEspRenderer.Hit> retained = List.of();
   private static int retentionTick;
   private static StorageEspModule lastModule;
   private static ClientLevel scanLevel;

   private StorageEspRenderer() {
   }

   public static void clear() {
      SCAN.clear();
      retainedUntil.clear();
      synchronized (PENDING_CHUNKS_LOCK) {
         pendingChunks.clear();
      }

      retained = List.of();
      retentionTick = 0;
      if (lastModule != null) {
         lastModule.clearInteractions();
         lastModule = null;
      }

      scanLevel = null;
   }

   public static int cachedCount() {
      return retained.size();
   }

   public static long cachedShulkerCount() {
      return retained.stream().filter(var0 -> var0.type() == StorageEspModule.StorageType.SHULKER).count();
   }

   public static void markDirty() {
      SCAN.markDirty();
   }

   public static void markChunkDirty(int var0, int var1) {
      StorageEspRenderer.ChunkPos var2 = new StorageEspRenderer.ChunkPos(var0, var1);
      synchronized (PENDING_CHUNKS_LOCK) {
         if (pendingChunks.containsKey(var2) || pendingChunks.size() < 512) {
            pendingChunks.put(var2, 6);
         }
      }
   }

   public static void scan(StorageEspModule var0) {
      ClientLevel var1 = Minecraft.getInstance().level;
      if (scanLevel != var1) {
         clear();
         scanLevel = var1;
      }

      lastModule = var0;
      double var2 = var0.range.get();
      double var4 = var2 * var2;
      int var6 = (int)Math.ceil(var2 / 16.0) + 1;
      int var7 = Math.min(32, var6);
      retentionTick++;
      scanPendingChunks();
      SCAN.tick(var7, (var2x, var3) -> scanChunk(var2x, var4, var3));
      refreshRetained();
   }

   private static void refreshRetained() {
      for (StorageEspRenderer.Hit var1 : SCAN.get()) {
         retain(var1, false);
      }

      Iterator var2 = retainedUntil.entrySet().iterator();

      while (var2.hasNext()) {
         if ((Integer)((Entry)var2.next()).getValue() <= retentionTick) {
            var2.remove();
         }
      }

      retained = List.copyOf(retainedUntil.keySet());
   }

   private static void scanPendingChunks() {
      Minecraft var0 = Minecraft.getInstance();
      if (var0.level != null) {
         ArrayList<StorageEspRenderer.PendingChunk> var1 = new ArrayList<>();
         synchronized (PENDING_CHUNKS_LOCK) {
            Iterator var3 = pendingChunks.entrySet().iterator();

            while (var3.hasNext() && var1.size() < 12) {
               Entry var4 = (Entry)var3.next();
               var1.add(new StorageEspRenderer.PendingChunk((StorageEspRenderer.ChunkPos)var4.getKey(), (Integer)var4.getValue()));
               var3.remove();
            }
         }

         if (!var1.isEmpty()) {
            ArrayList<StorageEspRenderer.PendingChunk> var14 = new ArrayList<>();

            for (StorageEspRenderer.PendingChunk var17 : var1) {
               StorageEspRenderer.ChunkPos var5 = var17.pos();
               int var6 = var17.passes();
               LevelChunk var7 = var0.level.getChunkSource().getChunk(var5.x(), var5.z(), false);
               if (var7 != null) {
                  ArrayList<StorageEspRenderer.Hit> var8 = new ArrayList<>();
                  scanChunk(var7, 0.0, var8, false);
                  if (!var8.isEmpty() || var6 == 1) {
                     retainedUntil.keySet().removeIf(var1x -> var1x.x() >> 4 == var5.x() && var1x.z() >> 4 == var5.z());

                     for (StorageEspRenderer.Hit var10 : var8) {
                        retain(var10, true);
                     }
                  }
               }

               if (var6 > 1) {
                  var14.add(new StorageEspRenderer.PendingChunk(var5, var6 - 1));
               }
            }

            synchronized (PENDING_CHUNKS_LOCK) {
               for (StorageEspRenderer.PendingChunk var19 : var14) {
                  pendingChunks.merge(var19.pos(), var19.passes(), Math::max);
               }
            }
         }
      }
   }

   private static void retain(StorageEspRenderer.Hit var0, boolean var1) {
      if (retainedUntil.containsKey(var0)) {
         retainedUntil.put(var0, retentionTick + 400);
      } else {
         if (retainedUntil.size() >= 6000) {
            if (!var1) {
               return;
            }

            Iterator var2 = retainedUntil.keySet().iterator();
            if (var2.hasNext()) {
               var2.next();
               var2.remove();
            }
         }

         retainedUntil.put(var0, retentionTick + 400);
      }
   }

   private static int scanChunk(LevelChunk var0, double var1, List<StorageEspRenderer.Hit> var3) {
      return scanChunk(var0, var1, var3, true);
   }

   private static int scanChunk(LevelChunk var0, double var1, List<StorageEspRenderer.Hit> var3, boolean var4) {
      LocalPlayer var5 = Minecraft.getInstance().player;
      if (var5 == null) {
         return 0;
      } else {
         LevelChunkSection[] var6 = var0.getSections();
         int var7 = var0.getMinSectionY();
         int var8 = var0.getPos().getMinBlockX();
         int var9 = var0.getPos().getMinBlockZ();
         short var10 = 0;
         if (var3.size() >= 6000) {
            return 0;
         } else {
            for (int var11 = 0; var11 < var6.length; var11++) {
               LevelChunkSection var12 = var6[var11];
               if (!var12.hasOnlyAir() && var12.maybeHas(StorageEspRenderer::isStorage)) {
                  int var13 = var7 + var11 << 4;
                  var10 = (short)(var10 + 4096);

                  for (int var14 = 0; var14 < 16; var14++) {
                     for (int var15 = 0; var15 < 16; var15++) {
                        for (int var16 = 0; var16 < 16; var16++) {
                           int var17 = var8 + var16;
                           int var18 = var13 + var14;
                           int var19 = var9 + var15;
                           BlockState var20 = var12.getBlockState(var16, var14, var15);
                           StorageEspModule.StorageType var21 = classify(var20.getBlock());
                           double var22 = (double)var17 + 0.5 - var5.getX();
                           double var24 = (double)var19 + 0.5 - var5.getZ();
                           if (var21 != null && (!var4 || !(var22 * var22 + var24 * var24 > var1)) && !isSecondaryChestHalf(var20, var17, var18, var19)) {
                              var3.add(new StorageEspRenderer.Hit(var17, var18, var19, var21));
                              if (var3.size() >= 6000) {
                                 return var10;
                              }
                           }
                        }
                     }
                  }
               }
            }

            return var10;
         }
      }
   }

   public static void render(BufferSource var0, PoseStack var1, Vec3 var2, StorageEspModule var3) {
      List<StorageEspRenderer.Hit> var4 = retained;
      if (!var4.isEmpty()) {
         Minecraft var5 = Minecraft.getInstance();
         ClientLevel var6 = var5.level;
         if (var6 != null) {
            boolean var7 = var3.mode.is("Full");
            int var8 = Math.max(0, Math.min(255, var3.highlightAlpha.getInt())) << 24;
            boolean var9 = var3.tracers.get();
            boolean var10 = var3.hideOpened();
            Vector3fc var11 = var5.gameRenderer.getMainCamera().forwardVector();

            for (StorageEspRenderer.Hit var13 : var4) {
               StorageEspModule.StorageType var15 = var13.type();
               boolean var14;
               if (var3.isTypeEnabled(var15) && (!(var14 = var3.isInteracted(var13.x(), var13.y(), var13.z())) || !var10)) {
                  int var16 = var14 ? 6579300 : var3.colorFor(var15) & 16777215;
                  int var17 = var8 | var16;
                  double var18 = (double)var13.x();
                  double var20 = (double)var13.y();
                  double var22 = (double)var13.z();
                  double var24 = var18 + 1.0;
                  double var26 = var20 + 1.0;
                  double var28 = var22 + 1.0;
                  if (var15 == StorageEspModule.StorageType.CHEST
                     || var15 == StorageEspModule.StorageType.TRAPPED
                     || var15 == StorageEspModule.StorageType.ENDER) {
                     var18 += 0.0625;
                     var22 += 0.0625;
                     var24 -= 0.0625;
                     var26 -= 0.125;
                     var28 -= 0.0625;
                     BlockState var30;
                     if ((var15 == StorageEspModule.StorageType.CHEST || var15 == StorageEspModule.StorageType.TRAPPED)
                        && (var30 = var6.getBlockState(new BlockPos(var13.x(), var13.y(), var13.z()))).getBlock() instanceof ChestBlock
                        && var30.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        Direction var32 = (Direction)var30.getValue(ChestBlock.FACING);
                        ChestType var33 = (ChestType)var30.getValue(ChestBlock.TYPE);
                        Direction var31 = var33 == ChestType.LEFT ? var32.getClockWise() : var32.getCounterClockWise();
                        if (var31 == Direction.WEST) {
                           var18 = (double)var13.x() - 1.0 + 0.0625;
                        } else if (var31 == Direction.EAST) {
                           var24 = (double)var13.x() + 2.0 - 0.0625;
                        } else if (var31 == Direction.NORTH) {
                           var22 = (double)var13.z() - 1.0 + 0.0625;
                        } else if (var31 == Direction.SOUTH) {
                           var28 = (double)var13.z() + 2.0 - 0.0625;
                        }
                     }
                  }

                  if (var7) {
                     EspBoxRenderer.fill(
                        var0, var1, var2, var18 - 0.002F, var20 - 0.002F, var22 - 0.002F, var24 + 0.002F, var26 + 0.002F, var28 + 0.002F, var17
                     );
                  } else {
                     EspBoxRenderer.outline(var0, var1, var2, var18, var20, var22, var24, var26, var28, var17, 1.0F);
                  }

                  if (var9) {
                     int var35 = -1275068416 | var16;
                     EspBoxRenderer.tracer(var0, var1, var2, var11, (var18 + var24) / 2.0, (var20 + var26) / 2.0, (var22 + var28) / 2.0, var35, 1.15F);
                  }
               }
            }

            EspBoxRenderer.flush(var0);
         }
      }
   }

   private static boolean isStorage(BlockState var0) {
      return classify(var0.getBlock()) != null;
   }

   private static boolean isSecondaryChestHalf(BlockState var0, int var1, int var2, int var3) {
      if (!(var0.getBlock() instanceof ChestBlock)) {
         return false;
      } else {
         ChestType var4 = (ChestType)var0.getValue(ChestBlock.TYPE);
         if (var4 == ChestType.SINGLE) {
            return false;
         } else {
            Direction var5 = (Direction)var0.getValue(ChestBlock.FACING);
            Direction var6 = var4 == ChestType.LEFT ? var5.getClockWise() : var5.getCounterClockWise();
            BlockPos var7 = new BlockPos(var1, var2, var3).relative(var6);
            return var1 > var7.getX() || var1 == var7.getX() && var3 > var7.getZ();
         }
      }
   }

   private static StorageEspModule.StorageType classify(Block var0) {
      if (var0 instanceof TrappedChestBlock) {
         return StorageEspModule.StorageType.TRAPPED;
      } else if (var0 instanceof ChestBlock) {
         return StorageEspModule.StorageType.CHEST;
      } else if (var0 instanceof EnderChestBlock) {
         return StorageEspModule.StorageType.ENDER;
      } else if (var0 instanceof ShulkerBoxBlock) {
         return StorageEspModule.StorageType.SHULKER;
      } else if (var0 instanceof BarrelBlock) {
         return StorageEspModule.StorageType.BARREL;
      } else if (var0 instanceof SpawnerBlock) {
         return StorageEspModule.StorageType.SPAWNER;
      } else if (var0 instanceof HopperBlock) {
         return StorageEspModule.StorageType.HOPPER;
      } else if (var0 instanceof AbstractFurnaceBlock) {
         return StorageEspModule.StorageType.FURNACE;
      } else if (var0 instanceof BrewingStandBlock) {
         return StorageEspModule.StorageType.FURNACE;
      } else {
         return var0 instanceof DispenserBlock ? StorageEspModule.StorageType.HOPPER : null;
      }
   }

   private static record ChunkPos(int x, int z) {
   }

   private static record Hit(int x, int y, int z, StorageEspModule.StorageType type) {
   }

   private static record PendingChunk(StorageEspRenderer.ChunkPos pos, int passes) {
   }
}
