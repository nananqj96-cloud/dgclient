package com.dg.client.event.events;

import com.dg.client.event.CancellableEvent;

public final class MouseButtonEvent extends CancellableEvent {
    private final int button;
    private final int action;
    private final int modifiers;

    public MouseButtonEvent(int button, int action, int modifiers) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getButton() {
        return button;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }
}
