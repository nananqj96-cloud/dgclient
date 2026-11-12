package dev.aether.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class BlockScanCache {
   private final Predicate<BlockState> predicate;
   private final int intervalTicks;
   private final int maxChunkRadius;
   private final int maxResults;
   private final double rangeSq;
   private volatile List<BlockPos> cache = List.of();
   private int tickCounter;

   public BlockScanCache(Predicate<BlockState> predicate, int intervalTicks, int maxChunkRadius, int maxResults, double range) {
      this.predicate = predicate;
      this.intervalTicks = intervalTicks;
      this.maxChunkRadius = maxChunkRadius;
      this.maxResults = maxResults;
      this.rangeSq = range * range;
   }

   public List<BlockPos> get() {
      return this.cache;
   }

   public void clear() {
      this.cache = List.of();
      this.tickCounter = 0;
   }

   public void scan() {
      if (this.tickCounter++ % this.intervalTicks == 0) {
         Minecraft mc = Minecraft.getInstance();
         ClientLevel level = mc.level;
         LocalPlayer player = mc.player;
         if (level != null && player != null) {
            int radius = Math.min(this.maxChunkRadius, (Integer)mc.options.renderDistance().get());
            int pcx = player.chunkPosition().x;
            int pcz = player.chunkPosition().z;
            List<BlockPos> found = new ArrayList<>();

            for (int cx = pcx - radius; cx <= pcx + radius && found.size() < this.maxResults; cx++) {
               for (int cz = pcz - radius; cz <= pcz + radius && found.size() < this.maxResults; cz++) {
                  LevelChunk chunk = level.getChunk(cx, cz);
                  LevelChunkSection[] sections = chunk.getSections();
                  int minSectionY = chunk.getMinSectionY();
                  int baseX = chunk.getPos().getMinBlockX();
                  int baseZ = chunk.getPos().getMinBlockZ();

                  for (int s = 0; s < sections.length; s++) {
                     LevelChunkSection section = sections[s];
                     if (!section.hasOnlyAir() && section.maybeHas(this.predicate)) {
                        int baseY = minSectionY + s << 4;

                        for (int y = 0; y < 16; y++) {
                           for (int z = 0; z < 16; z++) {
                              for (int x = 0; x < 16; x++) {
                                 if (this.predicate.test(section.getBlockState(x, y, z))) {
                                    int wx = baseX + x;
                                    int wy = baseY + y;
                                    int wz = baseZ + z;
                                    if (!(player.distanceToSqr((double)wx + 0.5, (double)wy + 0.5, (double)wz + 0.5) > this.rangeSq)) {
                                       found.add(new BlockPos(wx, wy, wz));
                                       if (found.size() >= this.maxResults) {
                                          break;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.cache = found;
         }
      }
   }
}
