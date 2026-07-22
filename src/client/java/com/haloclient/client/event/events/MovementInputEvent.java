package com.haloclient.client.event.events;

import com.haloclient.client.event.Event;

/**
 * Fired after keyboard movement input is processed.
 * Allows modification of forward/strafe impulses for movement fix.
 */
public class MovementInputEvent extends Event {
    private float forward;
    private float strafe;

    public MovementInputEvent(float forward, float strafe) {
        this.forward = forward;
        this.strafe = strafe;
    }

    public float getForward() { return forward; }
    public void setForward(float forward) { this.forward = forward; }
    public float getStrafe() { return strafe; }
    public void setStrafe(float strafe) { this.strafe = strafe; }
}
