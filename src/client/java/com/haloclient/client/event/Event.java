package com.haloclient.client.event;

/**
 * Base event class for the HaloClient event system.
 * Events can be cancelled to prevent further processing.
 */
public abstract class Event {
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
