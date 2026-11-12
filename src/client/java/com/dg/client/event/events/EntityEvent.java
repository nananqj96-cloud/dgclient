package com.dg.client.event.events;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

public abstract class EntityEvent {
    private final Entity entity;
    private final ClientWorld world;

    protected EntityEvent(Entity entity, ClientWorld world) {
        this.entity = entity;
        this.world = world;
    }

    public Entity getEntity() {
        return entity;
    }

    public ClientWorld getWorld() {
        return world;
    }

    public static final class Add extends EntityEvent {
        public Add(Entity entity, ClientWorld world) {
            super(entity, world);
        }
    }

    public static final class Remove extends EntityEvent {
        public Remove(Entity entity, ClientWorld world) {
            super(entity, world);
        }
    }
}
