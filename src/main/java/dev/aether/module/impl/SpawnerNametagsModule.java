package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ColorSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public final class SpawnerNametagsModule extends Module {
   public final ColorSetting color = this.addSetting(new ColorSetting("Color", "Spawner beam color", -44976));
   private final CopyOnWriteArrayList<BlockPos> spawners = new CopyOnWriteArrayList<>();
   private final Set<BlockPos> known = ConcurrentHashMap.newKeySet();
   private long lastSoundAt;
   private int ticks;

   public SpawnerNametagsModule() {
      super("Spawner Notifier", "Finds loaded mob spawners and marks them with a beam", Category.RENDER);
   }

   @Override
   protected void onEnable() {
      this.spawners.clear();
      this.known.clear();
      this.ticks = 0;
   }

   @Override
   protected void onDisable() {
      this.spawners.clear();
      this.known.clear();
   }

   @Override
   public void onTick() {
      Minecraft var1 = Minecraft.getInstance();
      if (var1.level != null && var1.player != null && ++this.ticks % 40 == 0) {
         this.ticks = 0;
         ChunkPos var2 = var1.player.chunkPosition();
         int var3 = Math.min(8, (Integer)var1.options.renderDistance().get());
         ArrayList var4 = new ArrayList();

         for (int var5 = -var3; var5 <= var3; var5++) {
            for (int var6 = -var3; var6 <= var3; var6++) {
               LevelChunk var7 = var1.level.getChunkSource().getChunk(var2.x + var5, var2.z + var6, false);
               if (var7 != null) {
                  for (BlockEntity var9 : var7.getBlockEntities().values()) {
                     if (var9 instanceof SpawnerBlockEntity) {
                        BlockPos var10 = var9.getBlockPos();
                        BlockPos var11 = new BlockPos(var10.getX(), var10.getY(), var10.getZ());
                        var4.add(var11);
                        if (this.known.add(var11)) {
                           this.announce(var1, var11);
                        }
                     }
                  }
               }
            }
         }

         this.known.retainAll(new HashSet(var4));
         this.spawners.clear();
         this.spawners.addAll(var4);
      }
   }

   private void announce(Minecraft minecraft, BlockPos blockPos) {
      long var3 = System.currentTimeMillis();
      if (var3 - this.lastSoundAt > 5000L) {
         this.lastSoundAt = var3;
         minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
      }

      String var5 = "§a[SpawnerNotifier] §fSpawner found at §e" + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();

      for (int var6 = 0; var6 < 4; var6++) {
         minecraft.gui.getChat().addMessage(Component.literal(var5));
      }
   }

   public List<BlockPos> visiblePositions() {
      return List.copyOf(this.spawners);
   }

   public void markDirty() {
   }

   public void markChunkDirty(int i, int j) {
   }

   public void clear() {
      this.spawners.clear();
      this.known.clear();
      this.ticks = 0;
   }
}
