package com.haloclient.client.event.events;

import com.haloclient.client.event.Event;
import net.minecraft.network.protocol.Packet;

/**
 * Fired when a packet is about to be sent to the server.
 * Can be cancelled to prevent the packet from being sent.
 */
public class PacketEvent extends Event {
    private Packet<?> packet;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
