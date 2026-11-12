package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.event.events.PacketEvent;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void dgclient$onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            PacketEvent.Receive event = inst.getEventBus().post(new PacketEvent.Receive(packet));
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void dgclient$onSendPacket(Packet<?> packet, @Nullable ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            PacketEvent.Send event = inst.getEventBus().post(new PacketEvent.Send(packet));
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
