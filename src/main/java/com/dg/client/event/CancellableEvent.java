package com.dg.client.event;

/**
 * Base class for events that can be cancelled by listeners.
 */
public abstract class CancellableEvent {
    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
