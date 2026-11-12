package com.dg.client.event.events;

import com.dg.client.event.CancellableEvent;
import net.minecraft.network.packet.Packet;

public abstract class PacketEvent extends CancellableEvent {
    private final Packet<?> packet;

    protected PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public static final class Receive extends PacketEvent {
        public Receive(Packet<?> packet) {
            super(packet);
        }
    }

    public static final class Send extends PacketEvent {
        public Send(Packet<?> packet) {
            super(packet);
        }
    }
}
