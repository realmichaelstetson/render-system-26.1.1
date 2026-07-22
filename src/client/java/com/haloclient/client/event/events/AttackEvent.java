package com.haloclient.client.event.events;

import com.haloclient.client.event.Event;
import net.minecraft.world.entity.Entity;

/**
 * Fired before (PRE) and after (POST) an entity attack.
 * PRE can be cancelled to prevent the attack.
 */
public class AttackEvent extends Event {
    public enum State { PRE, POST }

    private final State state;
    private final Entity target;

    public AttackEvent(State state, Entity target) {
        this.state = state;
        this.target = target;
    }

    public State getState() { return state; }
    public Entity getTarget() { return target; }
}
