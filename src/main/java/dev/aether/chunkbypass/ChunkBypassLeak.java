package dev.aether.chunkbypass;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A single recorded piece of leaked evidence. Port of the Relapse addon's Leak. */
public final class ChunkBypassLeak {
   public final BlockPos pos;
   public final BlockState state;
   public final ChunkBypassSource source;
   public final ChunkBypassTier tier;
   public final String note;
   public final long time;
   public int hits;

   public ChunkBypassLeak(BlockPos pos, BlockState state, ChunkBypassSource source, ChunkBypassTier tier, String note) {
      this.pos = pos;
      this.state = state;
      this.source = source;
      this.tier = tier;
      this.note = note == null ? "" : note;
      this.time = System.currentTimeMillis();
      this.hits = 1;
   }

   public String describe() {
      String block = this.state == null ? "-" : this.state.getBlock().toString();
      return this.pos.getX()
         + " "
         + this.pos.getY()
         + " "
         + this.pos.getZ()
         + " ["
         + this.source
         + "/"
         + this.tier
         + "] "
         + block
         + (this.note.isEmpty() ? "" : " " + this.note)
         + (this.hits > 1 ? " x" + this.hits : "");
   }
}
