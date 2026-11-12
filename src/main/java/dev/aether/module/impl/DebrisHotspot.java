package dev.aether.module.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class DebrisHotspot {
   static final int SECTION_SIZE = 16;
   static final int HOTSPOT_SIZE = 7;
   private static final int SECTION_VOLUME = 4096;
   private static final int MAX_ORIGIN = 9;
   private static final int MIN_EVIDENCE = 4;
   private static final double MIN_PROMINENCE = 0.025;
   private static final double MIN_DISTINCT_ADVANTAGE = 0.012;
   private static final byte OTHER = 0;
   private static final byte EVIDENCE = 1;
   private static final byte AIR = 2;
   private static final byte LAVA = 3;
   private static final int[][] DIRECTIONS = new int[][]{{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};

   private DebrisHotspot() {
   }

   static DebrisHotspot.Analysis find(BlockState[] blockStates, int i, double d, boolean[] bls) {
      return find(blockStates, i, d, bls, null);
   }

   static DebrisHotspot.Analysis find(BlockState[] blockStates, int i, double d, boolean[] bls, DebrisHotspot.Result result) {
      if (blockStates != null && blockStates.length == 4096 && (bls == null || bls.length == 4096)) {
         DebrisHotspot.Grid var6 = classify(blockStates, bls);
         if (var6 == null) {
            return DebrisHotspot.Analysis.EMPTY;
         } else {
            DebrisHotspot.Result var7 = result == null ? null : validateLocked(var6, i, result);
            if (var7 == null) {
               var7 = selectHotspot(var6, i, d);
            }

            if (var7 == null) {
               var7 = fallbackHotspot(var6, i);
            }

            return new DebrisHotspot.Analysis(var7 == null ? List.of() : List.of(var7));
         }
      } else {
         return DebrisHotspot.Analysis.EMPTY;
      }
   }

   static DebrisHotspot.Result validateLocked(BlockState[] blockStates, int i, boolean[] bls, DebrisHotspot.Result result) {
      if (result == null) {
         return null;
      } else {
         DebrisHotspot.Grid var4 = classify(blockStates, bls);
         return var4 == null ? null : validateLocked(var4, i, result);
      }
   }

   private static DebrisHotspot.Result validateLocked(DebrisHotspot.Grid grid, int i, DebrisHotspot.Result result) {
      DebrisHotspot.Candidate var3 = scoreHotspot(grid, result.x1(), result.y1(), result.z1(), i);
      int var4 = Math.max(4, (int)Math.ceil((double)result.evidenceCount() * 0.5));
      return var3 != null && var3.evidenceCount() >= var4 && !(var3.score() < result.rawScore() * 0.58) ? result : null;
   }

   static boolean isPredictionEvidence(BlockState blockState) {
      return isDebrisEvidence(blockState);
   }

   static boolean isDebrisEvidence(BlockState blockState) {
      return blockState != null
         && (blockState.is(Blocks.NETHERRACK) || blockState.is(Blocks.BASALT) || blockState.is(Blocks.SMOOTH_BASALT) || blockState.is(Blocks.BLACKSTONE));
   }

   private static DebrisHotspot.Grid classify(BlockState[] blockStates, boolean[] bls) {
      if (blockStates != null && blockStates.length == 4096 && (bls == null || bls.length == 4096)) {
         byte[] var2 = new byte[4096];
         int var3 = 0;

         for (int var4 = 0; var4 < 4096; var4++) {
            BlockState var5 = blockStates[var4];
            if ((bls == null || !bls[var4]) && var5 != null) {
               if (isDebrisEvidence(var5)) {
                  var2[var4] = 1;
                  var3++;
               } else if (var5.is(Blocks.LAVA)) {
                  var2[var4] = 3;
               } else if (var5.isAir()) {
                  var2[var4] = 2;
               } else {
                  var2[var4] = 0;
               }
            } else {
               var2[var4] = 0;
            }
         }

         int[] var7 = new int[4096];
         int[] var8 = new int[4096];
         int[] var6 = new int[4096];
         computeLocalMetrics(var2, var7, var8, var6);
         return new DebrisHotspot.Grid(
            var2,
            var3,
            DebrisHotspot.Prefix.of(var2, (byte)1),
            DebrisHotspot.Prefix.of(var2, (byte)2),
            DebrisHotspot.Prefix.of(var2, (byte)3),
            DebrisHotspot.Prefix.ofValues(var7),
            DebrisHotspot.Prefix.ofValues(var8),
            DebrisHotspot.Prefix.ofValues(var6)
         );
      } else {
         return null;
      }
   }

   private static void computeLocalMetrics(byte[] bs, int[] is, int[] js, int[] ks) {
      boolean[] var4 = new boolean[4096];
      int[] var5 = new int[4096];
      int[] var6 = new int[4096];

      for (int var7 = 0; var7 < 4096; var7++) {
         if (bs[var7] == 1) {
            int var8 = var7 >>> 8;
            int var9 = var7 >>> 4 & 15;
            int var10 = var7 & 15;

            for (int[] var14 : DIRECTIONS) {
               int var15 = var10 + var14[0];
               int var16 = var8 + var14[1];
               int var17 = var9 + var14[2];
               if (inside(var15, var16, var17)) {
                  byte var18 = bs[index(var15, var16, var17)];
                  if (var18 == 1) {
                     is[var7]++;
                  } else if (var18 == 3) {
                     js[var7]++;
                  }
               }
            }

            if (!var4[var7]) {
               int var25 = 0;
               int var26 = 0;
               var5[var26++] = var7;
               var4[var7] = true;

               while (var25 < var26) {
                  int var28 = var5[var25++];
                  var6[var25 - 1] = var28;
                  int var30 = var28 >>> 8;
                  int var32 = var28 >>> 4 & 15;
                  int var33 = var28 & 15;

                  for (int[] var20 : DIRECTIONS) {
                     int var21 = var33 + var20[0];
                     int var22 = var30 + var20[1];
                     int var23 = var32 + var20[2];
                     if (inside(var21, var22, var23)) {
                        int var24 = index(var21, var22, var23);
                        if (bs[var24] == 1 && !var4[var24]) {
                           var4[var24] = true;
                           var5[var26++] = var24;
                        }
                     }
                  }
               }

               int var29 = Math.min(var26, 32);

               for (int var31 = 0; var31 < var26; var31++) {
                  ks[var6[var31]] = var29;
               }
            }
         }
      }
   }

   private static DebrisHotspot.Result selectHotspot(DebrisHotspot.Grid grid, int i, double d) {
      if (grid.totalEvidence() >= 4 && grid.totalEvidence() != 4096) {
         ArrayList var4 = new ArrayList(1000);

         for (int var5 = 0; var5 <= 9; var5++) {
            for (int var6 = 0; var6 <= 9; var6++) {
               for (int var7 = 0; var7 <= 9; var7++) {
                  DebrisHotspot.Candidate var8 = scoreHotspot(grid, var7, var5, var6, i);
                  if (var8 != null) {
                     var4.add(var8);
                  }
               }
            }
         }

         if (var4.isEmpty()) {
            return null;
         } else {
            var4.sort(
               Comparator.comparingDouble(DebrisHotspot.Candidate::score)
                  .reversed()
                  .thenComparing(Comparator.comparingInt(DebrisHotspot.Candidate::evidenceCount).reversed())
                  .thenComparingInt(DebrisHotspot.Candidate::y)
                  .thenComparingInt(DebrisHotspot.Candidate::z)
                  .thenComparingInt(DebrisHotspot.Candidate::x)
            );
            DebrisHotspot.Candidate var23 = (DebrisHotspot.Candidate)var4.getFirst();
            double var24 = ((DebrisHotspot.Candidate)var4.get(var4.size() / 2)).score();
            DebrisHotspot.Candidate var25 = null;

            for (int var9 = 1; var9 < var4.size(); var9++) {
               DebrisHotspot.Candidate var10 = (DebrisHotspot.Candidate)var4.get(var9);
               int var11 = Math.max(Math.abs(var10.x() - var23.x()), Math.max(Math.abs(var10.y() - var23.y()), Math.abs(var10.z() - var23.z())));
               if (var11 >= 4) {
                  var25 = var10;
                  break;
               }
            }

            double var26 = Math.max(1.0, Math.abs(var23.score()));
            double var27 = (var23.score() - var24) / var26;
            double var13 = var25 == null ? 1.0 : (var23.score() - var25.score()) / var26;
            double var15 = Math.clamp((double)var23.evidenceCount() / 28.0, 0.0, 1.0);
            double var17 = Math.clamp(var27 * 4.0, 0.0, 1.0);
            double var19 = Math.clamp(var13 * 7.0, 0.0, 1.0);
            double var21 = 0.28 + var15 * 0.24 + var23.connectivity() * 0.18 + var23.centering() * 0.1 + var17 * 0.12 + var19 * 0.08;
            var21 = Math.clamp(var21, 0.01, 0.99);
            return new DebrisHotspot.Result(
               var23.x(), var23.y(), var23.z(), var23.x() + 7, var23.y() + 7, var23.z() + 7, var21, var23.score(), var23.evidenceCount()
            );
         }
      } else {
         return null;
      }
   }

   private static DebrisHotspot.Result fallbackHotspot(DebrisHotspot.Grid grid, int i) {
      int var2 = 4;
      int var3 = 4;
      int var4 = 4;
      int var5 = 0;
      double var6 = Double.NEGATIVE_INFINITY;

      for (int var8 = 0; var8 <= 9; var8++) {
         for (int var9 = 0; var9 <= 9; var9++) {
            for (int var10 = 0; var10 <= 9; var10++) {
               int var11 = grid.evidence().sum(var10, var8, var9, var10 + 7, var8 + 7, var9 + 7);
               int var12 = grid.air().sum(var10, var8, var9, var10 + 7, var8 + 7, var9 + 7);
               int var13 = grid.lava().sum(var10, var8, var9, var10 + 7, var8 + 7, var9 + 7);
               double var14 = (double)var11 - (double)var12 * 1.5 - (double)var13 * 0.5 + averageGenerationPrior(i + var8, i + var8 + 7) * 18.0;
               if (var14 > var6) {
                  var6 = var14;
                  var2 = var10;
                  var3 = var8;
                  var4 = var9;
                  var5 = var11;
               }
            }
         }
      }

      double var16 = (double)var5 / 343.0;
      double var17 = averageGenerationPrior(i + var3, i + var3 + 7);
      double var18 = Math.clamp(0.08 + var16 * 0.2 + (var17 - 0.75) * 0.12, 0.05, 0.35);
      return new DebrisHotspot.Result(var2, var3, var4, var2 + 7, var3 + 7, var4 + 7, var18, var6, var5);
   }

   private static DebrisHotspot.Candidate scoreHotspot(DebrisHotspot.Grid grid, int i, int j, int k, int l) {
      int var5 = i + 7;
      int var6 = j + 7;
      int var7 = k + 7;
      int var8 = grid.evidence().sum(i, j, k, var5, var6, var7);
      if (var8 < 4) {
         return null;
      } else {
         int var9 = grid.air().sum(i, j, k, var5, var6, var7);
         int var10 = grid.lava().sum(i, j, k, var5, var6, var7);
         int var11 = grid.neighbours().sum(i, j, k, var5, var6, var7);
         int var12 = grid.lavaBorders().sum(i, j, k, var5, var6, var7);
         int var13 = grid.componentSupport().sum(i, j, k, var5, var6, var7);
         int var14 = Math.max(0, i - 1);
         int var15 = Math.max(0, j - 1);
         int var16 = Math.max(0, k - 1);
         int var17 = Math.min(16, var5 + 1);
         int var18 = Math.min(16, var6 + 1);
         int var19 = Math.min(16, var7 + 1);
         int var20 = (var17 - var14) * (var18 - var15) * (var19 - var16);
         int var21 = grid.evidence().sum(var14, var15, var16, var17, var18, var19) - var8;
         double var22 = (double)var21 / (double)Math.max(1, var20 - 343);
         int var24 = strongestOutsideSide(grid.evidence(), i, j, k, var5, var6, var7);
         double var25 = 0.0;

         for (int var27 = j; var27 < var6; var27++) {
            for (int var28 = k; var28 < var7; var28++) {
               for (int var29 = i; var29 < var5; var29++) {
                  if (grid.types()[index(var29, var27, var28)] == 1) {
                     int var30 = var29 - i - 3;
                     int var31 = var27 - j - 3;
                     int var32 = var28 - k - 3;
                     var25 += 1.0 / (1.0 + (double)(var30 * var30 + var31 * var31 + var32 * var32) * 0.18);
                  }
               }
            }
         }

         double var43 = (double)var8 / 343.0;
         double var44 = Math.clamp((double)var11 / ((double)var8 * 6.0), 0.0, 1.0);
         double var45 = Math.clamp((double)var13 / ((double)var8 * 32.0), 0.0, 1.0);
         double var33 = var25 / (double)var8;
         double var35 = (double)var9 / 343.0;
         double var37 = (double)var10 / 343.0;
         double var39 = averageGenerationPrior(l + j, l + var6);
         double var41 = (double)var8 * (1.0 + var44 * 2.4 + var45 * 0.9)
            + var43 * 210.0
            + var33 * 105.0
            + var22 * 28.0
            + (double)var12 * 0.75
            + var39 * 18.0
            - var35 * 155.0
            - var37 * 18.0
            - (double)var24 * 0.75;
         return var41 > 0.0 ? new DebrisHotspot.Candidate(i, j, k, var41, var8, var44, var33) : null;
      }
   }

   private static int strongestOutsideSide(DebrisHotspot.Prefix prefix, int i, int j, int k, int l, int m, int n) {
      int var7 = 0;
      if (i > 0) {
         var7 = Math.max(var7, prefix.sum(i - 1, j, k, i, m, n));
      }

      if (l < 16) {
         var7 = Math.max(var7, prefix.sum(l, j, k, l + 1, m, n));
      }

      if (j > 0) {
         var7 = Math.max(var7, prefix.sum(i, j - 1, k, l, j, n));
      }

      if (m < 16) {
         var7 = Math.max(var7, prefix.sum(i, m, k, l, m + 1, n));
      }

      if (k > 0) {
         var7 = Math.max(var7, prefix.sum(i, j, k - 1, l, m, k));
      }

      if (n < 16) {
         var7 = Math.max(var7, prefix.sum(i, j, n, l, m, n + 1));
      }

      return var7;
   }

   private static double averageGenerationPrior(int i, int j) {
      double var2 = 0.0;

      for (int var4 = i; var4 < j; var4++) {
         double var5 = var4 >= 8 && var4 <= 24 ? (9.0 - (double)Math.abs(var4 - 16)) / 9.0 : 0.0;
         double var7 = var4 >= 8 && var4 <= 247 ? 1.0 : 0.0;
         var2 += 0.75 + var5 * 0.65 + var7 * 0.1;
      }

      return var2 / 7.0;
   }

   private static boolean inside(int i, int j, int k) {
      return i >= 0 && i < 16 && j >= 0 && j < 16 && k >= 0 && k < 16;
   }

   private static int index(int i, int j, int k) {
      return (j * 16 + k) * 16 + i;
   }

   static record Analysis(List<DebrisHotspot.Result> predictionResults) {
      private static final DebrisHotspot.Analysis EMPTY = new DebrisHotspot.Analysis(List.of());
   }

   private static record Candidate(int x, int y, int z, double score, int evidenceCount, double connectivity, double centering) {
   }

   private static record Grid(
      byte[] types,
      int totalEvidence,
      DebrisHotspot.Prefix evidence,
      DebrisHotspot.Prefix air,
      DebrisHotspot.Prefix lava,
      DebrisHotspot.Prefix neighbours,
      DebrisHotspot.Prefix lavaBorders,
      DebrisHotspot.Prefix componentSupport
   ) {
   }

   private static final class Prefix {
      private static final int SIZE = 17;
      private final int[] values = new int[4913];

      static DebrisHotspot.Prefix of(byte[] bs, byte b) {
         int[] var2 = new int[4096];

         for (int var3 = 0; var3 < 4096; var3++) {
            var2[var3] = bs[var3] == b ? 1 : 0;
         }

         return ofValues(var2);
      }

      static DebrisHotspot.Prefix ofValues(int[] is) {
         DebrisHotspot.Prefix var1 = new DebrisHotspot.Prefix();

         for (int var2 = 1; var2 < 17; var2++) {
            for (int var3 = 1; var3 < 17; var3++) {
               for (int var4 = 1; var4 < 17; var4++) {
                  int var5 = is[DebrisHotspot.index(var4 - 1, var2 - 1, var3 - 1)];
                  var1.values[prefixIndex(var4, var2, var3)] = var5
                     + var1.get(var4 - 1, var2, var3)
                     + var1.get(var4, var2 - 1, var3)
                     + var1.get(var4, var2, var3 - 1)
                     - var1.get(var4 - 1, var2 - 1, var3)
                     - var1.get(var4 - 1, var2, var3 - 1)
                     - var1.get(var4, var2 - 1, var3 - 1)
                     + var1.get(var4 - 1, var2 - 1, var3 - 1);
               }
            }
         }

         return var1;
      }

      int sum(int i, int j, int k, int l, int m, int n) {
         return this.get(l, m, n)
            - this.get(i, m, n)
            - this.get(l, j, n)
            - this.get(l, m, k)
            + this.get(i, j, n)
            + this.get(i, m, k)
            + this.get(l, j, k)
            - this.get(i, j, k);
      }

      private int get(int i, int j, int k) {
         return this.values[prefixIndex(i, j, k)];
      }

      private static int prefixIndex(int i, int j, int k) {
         return (j * 17 + k) * 17 + i;
      }
   }

   static record Result(int x1, int y1, int z1, int x2, int y2, int z2, double chance, double rawScore, int evidenceCount) {
   }
}
