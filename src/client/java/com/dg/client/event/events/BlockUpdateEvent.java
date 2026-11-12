package com.dg.client.event.events;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public final class BlockUpdateEvent {
    private final BlockPos pos;
    private final BlockState oldState;
    private final BlockState newState;

    public BlockUpdateEvent(BlockPos pos, BlockState oldState, BlockState newState) {
        this.pos = pos.toImmutable();
        this.oldState = oldState;
        this.newState = newState;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getOldState() {
        return oldState;
    }

    public BlockState getNewState() {
        return newState;
    }
}
