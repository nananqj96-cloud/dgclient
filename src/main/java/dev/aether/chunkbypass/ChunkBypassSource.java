package dev.aether.chunkbypass;

/** Port of the Relapse addon's evidence sources — what the server accidentally reveals. */
public enum ChunkBypassSource {
   BLOCK_UPDATE,
   SECTION_UPDATE,
   BREAK_PARTICLE,
   WORLD_EVENT,
   BLOCK_ENTITY,
   BLOCK_EVENT,
   ENTITY_SPAWN,
   BREAK_PROGRESS,
   SOUND,
   REDSTONE,
   SPAWNER,
   STRUCTURE;
}
