package com.dg.client.event.events;

import com.dg.client.event.CancellableEvent;

public final class MouseScrollEvent extends CancellableEvent {
    private final double horizontal;
    private final double vertical;

    public MouseScrollEvent(double horizontal, double vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public double getHorizontal() {
        return horizontal;
    }

    public double getVertical() {
        return vertical;
    }
}
