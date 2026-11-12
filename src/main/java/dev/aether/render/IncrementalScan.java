package dev.aether.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

public final class IncrementalScan<H> {
   private final int chunksPerTick;
   private final int blockBudgetPerTick;
   private final int idleTicks;
   private volatile List<H> published = List.of();
   private List<H> building = new ArrayList<>();
   private int cursor;
   private int[] order = new int[0];
   private int orderRadius = -1;
   private int sweepPcx = Integer.MIN_VALUE;
   private int sweepPcz = Integer.MIN_VALUE;
   private ClientLevel sweepLevel;
   private int cooldown;
   private boolean dirty;

   public IncrementalScan(int var1, int var2, int var3) {
      this.chunksPerTick = var1;
      this.blockBudgetPerTick = var2;
      this.idleTicks = var3;
   }

   public List<H> get() {
      return this.published;
   }

   public void markDirty() {
      this.dirty = true;
   }

   public void clear() {
      this.published = List.of();
      this.building = new ArrayList<>();
      this.cursor = 0;
      this.cooldown = 0;
      this.dirty = false;
      this.sweepPcz = Integer.MIN_VALUE;
      this.sweepPcx = Integer.MIN_VALUE;
      this.sweepLevel = null;
   }

   public void tick(int var1, IncrementalScan.ChunkScanner<H> var2) {
      Minecraft var3 = Minecraft.getInstance();
      ClientLevel var4 = var3.level;
      LocalPlayer var5 = var3.player;
      if (var4 != null && var5 != null) {
         if (this.sweepLevel != var4) {
            this.published = List.of();
            this.building = new ArrayList<>();
            this.cursor = 0;
            this.cooldown = 0;
            this.dirty = true;
            this.sweepPcx = Integer.MIN_VALUE;
            this.sweepPcz = Integer.MIN_VALUE;
            this.sweepLevel = var4;
         }

         if (this.orderRadius != var1) {
            this.ensureOrder(var1);
            this.dirty = true;
         }

         int var6 = var5.chunkPosition().x;
         int var7 = var5.chunkPosition().z;
         if (this.sweepPcx != Integer.MIN_VALUE && Math.max(Math.abs(var6 - this.sweepPcx), Math.abs(var7 - this.sweepPcz)) >= 2) {
            this.dirty = true;
         }

         if (this.cursor == 0) {
            if (!this.dirty && this.cooldown > 0) {
               this.cooldown--;
               return;
            }

            this.sweepPcx = var6;
            this.sweepPcz = var7;
            this.building = new ArrayList<>();
            this.dirty = false;
         } else if (this.dirty) {
            this.sweepPcx = var6;
            this.sweepPcz = var7;
            this.cursor = 0;
            this.building = new ArrayList<>();
            this.dirty = false;
         }

         int var8 = this.order.length;
         int var9 = 0;

         for (int var10 = 0; this.cursor < var8 && var9 < this.chunksPerTick && var10 < this.blockBudgetPerTick; var9++) {
            int var11 = this.order[this.cursor];
            short var12 = (short)(var11 >> 16);
            short var13 = (short)(var11 & 65535);
            LevelChunk var14 = var4.getChunkSource().getChunk(this.sweepPcx + var12, this.sweepPcz + var13, false);
            if (var14 != null) {
               var10 += var2.scan(var14, this.building);
            }

            this.cursor++;
         }

         if (this.cursor >= var8) {
            this.published = List.copyOf(this.building);
            this.building = new ArrayList<>();
            this.cursor = 0;
            this.cooldown = this.idleTicks;
         } else if (!this.building.isEmpty()) {
            LinkedHashSet var15 = new LinkedHashSet<>(this.published);
            var15.addAll(this.building);
            this.published = List.copyOf(var15);
         }
      }
   }

   private void ensureOrder(int var1) {
      if (this.orderRadius != var1) {
         int var2 = 2 * var1 + 1;
         Integer[] var3 = new Integer[var2 * var2];
         int var4 = 0;

         for (int var5 = -var1; var5 <= var1; var5++) {
            for (int var6 = -var1; var6 <= var1; var6++) {
               var3[var4++] = (var5 & 65535) << 16 | var6 & 65535;
            }
         }

         Arrays.sort(var3, (var0, var1x) -> {
            short var2x = (short)(var0 >> 16);
            short var3x = (short)(var0 & 65535);
            short var4x = (short)(var1x >> 16);
            short var5x = (short)(var1x & 65535);
            return Integer.compare(var2x * var2x + var3x * var3x, var4x * var4x + var5x * var5x);
         });
         int[] var7 = new int[var3.length];

         for (int var8 = 0; var8 < var3.length; var8++) {
            var7[var8] = var3[var8];
         }

         this.order = var7;
         this.orderRadius = var1;
      }
   }

   public interface ChunkScanner<H> {
      int scan(LevelChunk levelChunk, List<H> list);
   }
}
