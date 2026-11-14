package dev.aether.chunkbypass;

/** Port of the Relapse addon's evidence tiers — how strong a leak is as base evidence. */
public enum ChunkBypassTier {
   DIRECT(100),
   IMPLIED(60),
   ACTIVITY(25),
   INFERRED(8);

   public final int weight;

   ChunkBypassTier(int weight) {
      this.weight = weight;
   }
}
