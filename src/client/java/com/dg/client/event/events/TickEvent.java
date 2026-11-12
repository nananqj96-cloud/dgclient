package com.dg.client.event.events;

public abstract class TickEvent {
    public static final class Pre extends TickEvent {
        public static final Pre INSTANCE = new Pre();
    }

    public static final class Post extends TickEvent {
        public static final Post INSTANCE = new Post();
    }
}
