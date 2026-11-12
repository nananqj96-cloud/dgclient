package com.dg.client.event;

/**
 * Listener execution order, from first (HIGHEST) to last (LOWEST).
 */
public enum EventPriority {
    HIGHEST(0),
    HIGH(1),
    NORMAL(2),
    LOW(3),
    LOWEST(4);

    private final int order;

    EventPriority(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
