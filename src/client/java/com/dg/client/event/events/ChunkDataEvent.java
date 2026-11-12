package com.dg.client.event.events;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;

public abstract class ChunkDataEvent {
    private final ClientWorld world;
    private final WorldChunk chunk;

    protected ChunkDataEvent(ClientWorld world, WorldChunk chunk) {
        this.world = world;
        this.chunk = chunk;
    }

    public ClientWorld getWorld() {
        return world;
    }

    public WorldChunk getChunk() {
        return chunk;
    }

    public static final class Load extends ChunkDataEvent {
        public Load(ClientWorld world, WorldChunk chunk) {
            super(world, chunk);
        }
    }

    public static final class Unload extends ChunkDataEvent {
        public Unload(ClientWorld world, WorldChunk chunk) {
            super(world, chunk);
        }
    }
}
