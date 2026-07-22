package com.haloclient.client.event.events;

import com.haloclient.client.event.Event;

/**
 * Fired before (PRE) and after (POST) the player's motion update.
 * PRE: Modify yaw/pitch before they are sent to the server.
 * POST: Restore original rotation.
 */
public class MotionEvent extends Event {
    public enum State { PRE, POST }

    private final State state;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public MotionEvent(State state, float yaw, float pitch, boolean onGround) {
        this.state = state;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public State getState() { return state; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
}
